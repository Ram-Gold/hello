package com.example.hello.platform.avatar

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException
import java.util.concurrent.atomic.AtomicInteger

sealed interface NotionistFetchResult {
    data class Success(val bytes: ByteArray) : NotionistFetchResult {
        override fun equals(other: Any?): Boolean =
            this === other || (other is Success && bytes.contentEquals(other.bytes))

        override fun hashCode(): Int = bytes.contentHashCode()
    }

    data object Offline : NotionistFetchResult
    data object RateLimited : NotionistFetchResult
    data class Error(val message: String) : NotionistFetchResult
}

interface NotionistAvatarFetcher {
    fun isOnline(): Boolean
    fun observeConnectivity(): Flow<Boolean>
    suspend fun fetchPng(url: String): NotionistFetchResult

    /**
     * Fetches a canonical 48px trait thumbnail using shared memory/disk cache first,
     * falling back to a low-priority throttled network slot when uncached.
     */
    suspend fun fetchThumbnail(cacheKey: String, url: String): NotionistFetchResult = fetchPng(url)
}

/**
 * Zero-dependency HTTP client, shared thumbnail disk cache, and connectivity monitor strictly
 * scoped to the opt-in Notionist Avatar Playground (ADR-0035).
 */
class AndroidNotionistAvatarFetcher(
    context: Context
) : NotionistAvatarFetcher {

    private val appContext = context.applicationContext
    private val connectivityManager =
        appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    private val thumbnailDiskCache = NotionistThumbnailDiskCache(File(appContext.cacheDir, "notionist_thumbs"))

    override fun isOnline(): Boolean {
        val cm = connectivityManager ?: return false
        val activeNetwork = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false
        val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        val isValidated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        return hasInternet && isValidated
    }

    override fun observeConnectivity(): Flow<Boolean> = callbackFlow {
        trySend(isOnline())
        val cm = connectivityManager
        if (cm == null) {
            close()
            return@callbackFlow
        }

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(isOnline())
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                trySend(isOnline())
            }

            override fun onLost(network: Network) {
                trySend(isOnline())
            }

            override fun onUnavailable() {
                trySend(false)
            }
        }

        val registered = runCatching {
            cm.registerDefaultNetworkCallback(callback)
        }.isSuccess

        awaitClose {
            if (registered) {
                runCatching { cm.unregisterNetworkCallback(callback) }
            }
        }
    }.distinctUntilChanged()

    override suspend fun fetchPng(url: String): NotionistFetchResult {
        NotionistPngMemoryCache.get(url)?.let { cachedBytes ->
            return NotionistFetchResult.Success(cachedBytes)
        }

        if (!isOnline()) {
            return NotionistFetchResult.Offline
        }

        activeHighPriorityRequests.incrementAndGet()
        return try {
            withContext(Dispatchers.IO) {
                NotionistPngMemoryCache.get(url)?.let { cachedBytes ->
                    return@withContext NotionistFetchResult.Success(cachedBytes)
                }

                when (val result = executeHttpPngFetch(url)) {
                    is NotionistFetchResult.Success -> {
                        NotionistPngMemoryCache.put(url, result.bytes)
                        result
                    }
                    else -> result
                }
            }
        } finally {
            activeHighPriorityRequests.decrementAndGet()
        }
    }

    override suspend fun fetchThumbnail(cacheKey: String, url: String): NotionistFetchResult {
        // 1. Fast in-memory hit (zero disk I/O, zero network, works offline)
        NotionistThumbnailMemoryCache.get(cacheKey)?.let { cachedBytes ->
            return NotionistFetchResult.Success(cachedBytes)
        }

        // 2. Persistent disk cache hit in cacheDir/notionist_thumbs/ (zero network, works offline)
        val diskBytes = withContext(Dispatchers.IO) {
            thumbnailDiskCache.get(cacheKey)
        }
        if (diskBytes != null) {
            NotionistThumbnailMemoryCache.put(cacheKey, diskBytes)
            return NotionistFetchResult.Success(diskBytes)
        }

        if (!isOnline()) {
            return NotionistFetchResult.Offline
        }

        // 3. Low-priority throttled network fetch (preempted by high-priority main 256px preview)
        return thumbnailSemaphore.withPermit {
            while (activeHighPriorityRequests.get() > 0) {
                delay(HIGH_PRIORITY_YIELD_MS)
            }

            // Re-check cache in case another coroutine populated it while waiting
            NotionistThumbnailMemoryCache.get(cacheKey)?.let { cachedBytes ->
                return@withPermit NotionistFetchResult.Success(cachedBytes)
            }

            if (!isOnline()) {
                return@withPermit NotionistFetchResult.Offline
            }

            // Pace outgoing thumbnail requests safely under DiceBear's 10 req/s raster ceiling
            thumbnailRateLimitMutex.withLock {
                val now = System.currentTimeMillis()
                val elapsed = now - lastThumbnailRequestAtMs
                if (elapsed < MIN_THUMBNAIL_SPACING_MS) {
                    delay(MIN_THUMBNAIL_SPACING_MS - elapsed)
                }
                lastThumbnailRequestAtMs = System.currentTimeMillis()
            }

            withContext(Dispatchers.IO) {
                when (val result = executeHttpPngFetch(url)) {
                    is NotionistFetchResult.Success -> {
                        NotionistThumbnailMemoryCache.put(cacheKey, result.bytes)
                        thumbnailDiskCache.put(cacheKey, result.bytes)
                        result
                    }
                    else -> result
                }
            }
        }
    }

    private fun executeHttpPngFetch(url: String): NotionistFetchResult {
        var connection: HttpURLConnection? = null
        return try {
            connection = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                instanceFollowRedirects = true
                setRequestProperty("Accept", "image/png")
                setRequestProperty("User-Agent", USER_AGENT)
            }

            when (val code = connection.responseCode) {
                HttpURLConnection.HTTP_OK -> {
                    val bytes = connection.inputStream.use { stream ->
                        readBoundedBytes(stream, MAX_PNG_BYTES)
                    } ?: return NotionistFetchResult.Error("Avatar image was too large.")

                    if (!isValidPngHeader(bytes)) {
                        return NotionistFetchResult.Error("Received an invalid image from DiceBear.")
                    }

                    NotionistFetchResult.Success(bytes)
                }
                429 -> NotionistFetchResult.RateLimited
                else -> NotionistFetchResult.Error("Could not load avatar (HTTP $code). Please retry.")
            }
        } catch (_: UnknownHostException) {
            NotionistFetchResult.Offline
        } catch (_: SocketTimeoutException) {
            if (!isOnline()) {
                NotionistFetchResult.Offline
            } else {
                NotionistFetchResult.Error("Connection timed out. Please retry.")
            }
        } catch (_: IOException) {
            if (!isOnline()) {
                NotionistFetchResult.Offline
            } else {
                NotionistFetchResult.Error("Network error while loading avatar. Please retry.")
            }
        } catch (_: Exception) {
            NotionistFetchResult.Error("Unexpected error while loading avatar.")
        } finally {
            connection?.disconnect()
        }
    }

    companion object {
        private const val CONNECT_TIMEOUT_MS = 8_000
        private const val READ_TIMEOUT_MS = 8_000
        private const val MAX_PNG_BYTES = 2 * 1024 * 1024
        private const val MIN_THUMBNAIL_SPACING_MS = 140L
        private const val HIGH_PRIORITY_YIELD_MS = 60L
        private const val USER_AGENT = "HelloAndroid/1.0"
        private val PNG_MAGIC = byteArrayOf(
            0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
        )

        private val activeHighPriorityRequests = AtomicInteger(0)
        private val thumbnailSemaphore = Semaphore(2)
        private val thumbnailRateLimitMutex = Mutex()
        @Volatile
        private var lastThumbnailRequestAtMs = 0L

        internal fun isValidPngHeader(bytes: ByteArray): Boolean {
            if (bytes.size < PNG_MAGIC.size) return false
            for (i in PNG_MAGIC.indices) {
                if (bytes[i] != PNG_MAGIC[i]) return false
            }
            return true
        }

        private fun readBoundedBytes(stream: java.io.InputStream, maxBytes: Int): ByteArray? {
            val out = ByteArrayOutputStream()
            val buffer = ByteArray(8 * 1024)
            var total = 0
            while (true) {
                val read = stream.read(buffer)
                if (read <= 0) break
                total += read
                if (total > maxBytes) return null
                out.write(buffer, 0, read)
            }
            return out.toByteArray()
        }
    }
}

/**
 * Atomic disk cache for shared 48px Notionist trait thumbnails under `cacheDir/notionist_thumbs/`.
 * OS-managed and never included in Export Bundle.
 */
class NotionistThumbnailDiskCache(private val thumbsDir: File) {

    fun get(cacheKey: String): ByteArray? {
        val file = resolveFile(cacheKey)
        if (!file.exists() || !file.isFile) return null
        val bytes = runCatching { file.readBytes() }.getOrNull() ?: return null
        if (!AndroidNotionistAvatarFetcher.isValidPngHeader(bytes)) {
            runCatching { file.delete() }
            return null
        }
        return bytes
    }

    fun put(cacheKey: String, bytes: ByteArray) {
        if (!AndroidNotionistAvatarFetcher.isValidPngHeader(bytes)) return
        runCatching {
            if (!thumbsDir.exists()) {
                thumbsDir.mkdirs()
            }
            val targetFile = resolveFile(cacheKey)
            val tempFile = File(thumbsDir, "${targetFile.name}.tmp")
            FileOutputStream(tempFile).use { fos ->
                fos.write(bytes)
                fos.fd.sync()
            }
            if (!tempFile.renameTo(targetFile)) {
                tempFile.copyTo(targetFile, overwrite = true)
                tempFile.delete()
            }
        }
    }

    private fun resolveFile(cacheKey: String): File {
        val safeName = cacheKey.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        return File(thumbsDir, "$safeName.png")
    }
}

/**
 * In-memory LRU cache for main 256px DiceBear PNG previews keyed by canonical URL.
 */
object NotionistPngMemoryCache {
    private val cache = object : LruCache<String, ByteArray>(48) {}

    fun get(url: String): ByteArray? = synchronized(cache) {
        cache.get(url)
    }

    fun put(url: String, bytes: ByteArray) {
        synchronized(cache) {
            cache.put(url, bytes)
        }
    }

    fun clear() {
        synchronized(cache) {
            cache.evictAll()
        }
    }
}

/**
 * In-memory LRU cache for shared 48px trait thumbnails keyed by `<category>_<optionId>`.
 */
object NotionistThumbnailMemoryCache {
    private val cache = object : LruCache<String, ByteArray>(96) {}

    fun get(cacheKey: String): ByteArray? = synchronized(cache) {
        cache.get(cacheKey)
    }

    fun put(cacheKey: String, bytes: ByteArray) {
        synchronized(cache) {
            cache.put(cacheKey, bytes)
        }
    }

    fun clear() {
        synchronized(cache) {
            cache.evictAll()
        }
    }
}
