package com.example.hello.platform.avatar

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID
import kotlin.math.max
import kotlin.math.min

/**
 * Pure Kotlin geometry math and 512x512 Warm White (#FFFFFF) PNG rendering for 1:1 circular Contact Avatars.
 */
object AvatarCropGeometry {
    const val MIN_ZOOM = 1f
    const val MAX_ZOOM = 4f
    const val OUTPUT_SIZE_PX = 512
    const val WARM_WHITE_ARGB: Int = 0xFFFFFFFF.toInt()

    /**
     * Composites an ARGB pixel over solid Warm White (#FFFFFF), guaranteeing full opacity (0xFF alpha).
     */
    fun flattenPixelOverWarmWhite(argbPixel: Int): Int {
        val alpha = (argbPixel ushr 24) and 0xFF
        if (alpha == 255) return argbPixel or (0xFF shl 24)
        if (alpha == 0) return WARM_WHITE_ARGB
        val invAlpha = 255 - alpha
        val r = (((argbPixel ushr 16) and 0xFF) * alpha + 255 * invAlpha + 127) / 255
        val g = (((argbPixel ushr 8) and 0xFF) * alpha + 255 * invAlpha + 127) / 255
        val b = ((argbPixel and 0xFF) * alpha + 255 * invAlpha + 127) / 255
        return (0xFF shl 24) or (r.coerceIn(0, 255) shl 16) or (g.coerceIn(0, 255) shl 8) or b.coerceIn(0, 255)
    }

    fun calculateBaseScale(
        imageWidth: Float,
        imageHeight: Float,
        viewportSize: Float,
        fitWholeImage: Boolean
    ): Float {
        if (imageWidth <= 0f || imageHeight <= 0f || viewportSize <= 0f) return 1f
        return if (fitWholeImage) {
            // For circular aperture (diameter = viewportSize), inscribe the entire rectangular image
            // within the circular boundary so none of the corners or edges are cropped out.
            // diagonal = hypot(scaledWidth, scaledHeight) = scale * hypot(imageWidth, imageHeight) <= viewportSize
            val diagonal = kotlin.math.hypot(imageWidth.toDouble(), imageHeight.toDouble()).toFloat()
            if (diagonal > 0f) viewportSize / diagonal else 1f
        } else {
            max(viewportSize / imageWidth, viewportSize / imageHeight)
        }
    }

    fun clampPanOffset(
        offsetX: Float,
        offsetY: Float,
        imageWidth: Float,
        imageHeight: Float,
        viewportSize: Float,
        zoom: Float,
        fitWholeImage: Boolean
    ): Pair<Float, Float> {
        val coercedZoom = zoom.coerceIn(MIN_ZOOM, MAX_ZOOM)
        val baseScale = calculateBaseScale(imageWidth, imageHeight, viewportSize, fitWholeImage)
        val scaledWidth = imageWidth * baseScale * coercedZoom
        val scaledHeight = imageHeight * baseScale * coercedZoom

        val maxDx = max(0f, (scaledWidth - viewportSize) / 2f)
        val maxDy = max(0f, (scaledHeight - viewportSize) / 2f)

        return Pair(
            offsetX.coerceIn(-maxDx, maxDx),
            offsetY.coerceIn(-maxDy, maxDy)
        )
    }

    /**
     * Renders the visible 1:1 circular crop region onto a [outputSizePx]x[outputSizePx] bitmap
     * pre-filled with solid Warm White (#FFFFFF) so transparent PNGs have a clean solid backing.
     */
    fun renderCroppedAvatarPng(
        source: Bitmap,
        viewportSizePx: Float,
        zoom: Float,
        offsetX: Float,
        offsetY: Float,
        fitWholeImage: Boolean,
        outputSizePx: Int = OUTPUT_SIZE_PX
    ): ByteArray {
        val safeViewport = if (viewportSizePx > 0f) viewportSizePx else outputSizePx.toFloat()
        val coercedZoom = zoom.coerceIn(MIN_ZOOM, MAX_ZOOM)
        val (clampedX, clampedY) = clampPanOffset(
            offsetX = offsetX,
            offsetY = offsetY,
            imageWidth = source.width.toFloat(),
            imageHeight = source.height.toFloat(),
            viewportSize = safeViewport,
            zoom = coercedZoom,
            fitWholeImage = fitWholeImage
        )

        val outputBitmap = Bitmap.createBitmap(outputSizePx, outputSizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(outputBitmap)

        // Fill base with solid Warm White (#FFFFFF) per ADR-0022
        canvas.drawColor(android.graphics.Color.WHITE)

        val baseScale = calculateBaseScale(
            imageWidth = source.width.toFloat(),
            imageHeight = source.height.toFloat(),
            viewportSize = safeViewport,
            fitWholeImage = fitWholeImage
        )
        val viewportToOutputRatio = outputSizePx.toFloat() / safeViewport
        val totalScale = baseScale * coercedZoom * viewportToOutputRatio

        val matrix = Matrix().apply {
            // 1. Center source bitmap at origin
            postTranslate(-source.width / 2f, -source.height / 2f)
            // 2. Apply combined baseScale * zoom * output ratio
            postScale(totalScale, totalScale)
            // 3. Translate to center of output canvas + scaled user pan offset
            postTranslate(
                outputSizePx / 2f + clampedX * viewportToOutputRatio,
                outputSizePx / 2f + clampedY * viewportToOutputRatio
            )
        }

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(source, matrix, paint)

        val out = ByteArrayOutputStream()
        outputBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        return out.toByteArray()
    }
}

/**
 * Manages atomic staged and canonical avatar files in app-private storage (`filesDir/avatars/`).
 */
class AvatarStorageManager(val avatarsDir: File) {

    constructor(context: Context) : this(File(context.filesDir, "avatars"))

    init {
        if (!avatarsDir.exists()) {
            avatarsDir.mkdirs()
        }
    }

    /**
     * Atomically writes cropped PNG bytes to `staged_<uuid>.png` and cleans up [previousStagedPath] if present.
     */
    fun writeStagedBytes(bytes: ByteArray, previousStagedPath: String? = null): String {
        if (!avatarsDir.exists()) {
            avatarsDir.mkdirs()
        }
        if (previousStagedPath != null) {
            val prevFile = resolveFile(previousStagedPath)
            if (prevFile != null && prevFile.parentFile == avatarsDir && prevFile.name.startsWith("staged_")) {
                prevFile.delete()
                AvatarBitmapCache.evict(previousStagedPath)
            }
        }

        val uuid = UUID.randomUUID().toString()
        val tempFile = File(avatarsDir, "staged_${uuid}.tmp")
        val targetFile = File(avatarsDir, "staged_${uuid}.png")

        FileOutputStream(tempFile).use { fos ->
            fos.write(bytes)
            fos.fd.sync()
        }
        if (!tempFile.renameTo(targetFile)) {
            tempFile.copyTo(targetFile, overwrite = true)
            tempFile.delete()
        }
        return targetFile.absolutePath
    }

    /**
     * Commits a staged avatar (or photo removal) when the user taps Save on Edit Contact:
     * - Promotes `staged_<uuid>.png` to `contact_<contactId>_<timestamp>.png`
     * - Deletes any superseded canonical file in [avatarsDir]
     */
    fun commitStagedAvatar(
        contactId: String,
        currentPhotoUri: String?,
        originalPhotoUri: String?,
        nowMillis: Long = System.currentTimeMillis()
    ): String? {
        // Case 1: User removed the photo
        if (currentPhotoUri.isNullOrBlank()) {
            deleteLocalAvatarFile(originalPhotoUri)
            return null
        }

        // Case 2: Photo unchanged
        if (currentPhotoUri == originalPhotoUri) {
            return originalPhotoUri
        }

        val stagedFile = resolveFile(currentPhotoUri)
        if (stagedFile != null && stagedFile.exists() && stagedFile.parentFile == avatarsDir) {
            val sanitizedId = sanitizeContactId(contactId)
            val canonicalFile = File(avatarsDir, "contact_${sanitizedId}_${nowMillis}.png")
            if (stagedFile.absolutePath != canonicalFile.absolutePath) {
                if (!stagedFile.renameTo(canonicalFile)) {
                    stagedFile.copyTo(canonicalFile, overwrite = true)
                    stagedFile.delete()
                }
            }
            AvatarBitmapCache.evict(currentPhotoUri)
            if (originalPhotoUri != null && originalPhotoUri != canonicalFile.absolutePath) {
                deleteLocalAvatarFile(originalPhotoUri)
            }
            return canonicalFile.absolutePath
        }

        // Fallback if a non-managed URI was passed
        return currentPhotoUri
    }

    /**
     * Cleans up an uncommitted staged file when the user discards edits, preserving [originalPhotoUri].
     */
    fun discardStagedAvatar(stagedPhotoUri: String?, originalPhotoUri: String?) {
        if (stagedPhotoUri.isNullOrBlank() || stagedPhotoUri == originalPhotoUri) return
        val file = resolveFile(stagedPhotoUri)
        if (file != null && file.parentFile == avatarsDir && file.name.startsWith("staged_")) {
            file.delete()
            AvatarBitmapCache.evict(stagedPhotoUri)
        }
    }

    /**
     * Deletes a local avatar file if it lives inside [avatarsDir].
     */
    fun deleteLocalAvatarFile(photoUri: String?) {
        if (photoUri.isNullOrBlank()) return
        val file = resolveFile(photoUri)
        if (file != null && file.parentFile == avatarsDir && file.exists()) {
            file.delete()
            AvatarBitmapCache.evict(photoUri)
        }
    }

    companion object {
        fun sanitizeContactId(contactId: String): String =
            contactId.replace(Regex("[^a-zA-Z0-9_-]"), "_")

        fun resolveFile(photoUri: String?): File? {
            if (photoUri.isNullOrBlank()) return null
            val cleanPath = photoUri.removePrefix("file://")
            return if (cleanPath.startsWith("/")) File(cleanPath) else null
        }

        fun deleteIfManagedAvatarFile(photoUri: String?) {
            val file = resolveFile(photoUri) ?: return
            if (file.exists() && (file.name.startsWith("contact_") || file.name.startsWith("staged_"))) {
                file.delete()
                AvatarBitmapCache.evict(photoUri)
            }
        }
    }
}

/**
 * Zero-dependency in-memory LRU cache for decoded local Contact Avatar [ImageBitmap]s.
 */
object AvatarBitmapCache {
    private val cache = object : LruCache<String, ImageBitmap>(64) {}

    fun get(photoUri: String?): ImageBitmap? {
        if (photoUri.isNullOrBlank()) return null
        synchronized(cache) {
            return cache.get(photoUri)
        }
    }

    fun put(photoUri: String, bitmap: ImageBitmap) {
        synchronized(cache) {
            cache.put(photoUri, bitmap)
        }
    }

    fun evict(photoUri: String?) {
        if (photoUri.isNullOrBlank()) return
        synchronized(cache) {
            cache.remove(photoUri)
        }
    }

    suspend fun loadOrDecode(context: Context, photoUri: String?): ImageBitmap? {
        if (photoUri.isNullOrBlank()) return null
        get(photoUri)?.let { return it }

        return withContext(Dispatchers.IO) {
            get(photoUri)?.let { return@withContext it }
            val decoded = decodeSampledBitmap(context, photoUri, maxDimension = 512)
            val imageBitmap = decoded?.asImageBitmap()
            if (imageBitmap != null) {
                put(photoUri, imageBitmap)
            }
            imageBitmap
        }
    }

    /**
     * Decodes a source URI or local file path safely with bounds downsampling.
     */
    fun decodeSampledBitmap(context: Context, uriOrPath: String, maxDimension: Int = 2048): Bitmap? {
        return try {
            val localFile = AvatarStorageManager.resolveFile(uriOrPath)
            if (localFile != null) {
                if (!localFile.exists()) return null
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(localFile.absolutePath, bounds)
                if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

                val options = BitmapFactory.Options().apply {
                    inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, maxDimension)
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                BitmapFactory.decodeFile(localFile.absolutePath, options)
            } else {
                val uri = Uri.parse(uriOrPath)
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, bounds)
                }
                if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

                val options = BitmapFactory.Options().apply {
                    inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, maxDimension)
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                val rawBitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, options)
                } ?: return null

                val exifOrientation = context.contentResolver.openInputStream(uri)?.use { stream ->
                    readExifOrientation(stream)
                } ?: 1

                if (exifOrientation != 1) {
                    val matrix = Matrix().apply {
                        when (exifOrientation) {
                            2 -> postScale(-1f, 1f)
                            3 -> postRotate(180f)
                            4 -> {
                                postRotate(180f)
                                postScale(-1f, 1f)
                            }
                            5 -> {
                                postRotate(90f)
                                postScale(-1f, 1f)
                            }
                            6 -> postRotate(90f)
                            7 -> {
                                postRotate(270f)
                                postScale(-1f, 1f)
                            }
                            8 -> postRotate(270f)
                        }
                    }
                    Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true)
                } else {
                    rawBitmap
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun calculateInSampleSize(width: Int, height: Int, maxDimension: Int): Int {
        var inSampleSize = 1
        val largest = max(width, height)
        while (largest / inSampleSize > maxDimension) {
            inSampleSize *= 2
        }
        return inSampleSize
    }

    /**
     * Bounded 64KB JPEG EXIF orientation reader (APP1 marker) supporting all 8 rotation/flip orientations
     * without reading the entire image stream into memory.
     */
    internal fun readExifOrientation(input: InputStream): Int {
        return try {
            val maxHeaderBytes = 64 * 1024
            val buffer = ByteArray(maxHeaderBytes)
            var totalRead = 0
            while (totalRead < maxHeaderBytes) {
                val read = input.read(buffer, totalRead, maxHeaderBytes - totalRead)
                if (read <= 0) break
                totalRead += read
            }
            val bytes = buffer.copyOf(totalRead)
            if (bytes.size < 14 || (bytes[0].toInt() and 0xFF) != 0xFF || (bytes[1].toInt() and 0xFF) != 0xD8) {
                return 1
            }
            var offset = 2
            while (offset + 4 < bytes.size && (bytes[offset].toInt() and 0xFF) == 0xFF) {
                val marker = bytes[offset + 1].toInt() and 0xFF
                if (marker == 0xDA || marker == 0xD9) break
                val length = ((bytes[offset + 2].toInt() and 0xFF) shl 8) or (bytes[offset + 3].toInt() and 0xFF)
                if (length < 2 || offset + 2 + length > bytes.size) break
                if (marker == 0xE1 && length >= 14) {
                    val exifHeader = String(bytes, offset + 4, 4, Charsets.US_ASCII)
                    if (exifHeader == "Exif") {
                        val tiffStart = offset + 10
                        val littleEndian = bytes[tiffStart].toInt().toChar() == 'I'
                        val readUInt16 = { pos: Int ->
                            if (littleEndian) {
                                (bytes[pos].toInt() and 0xFF) or ((bytes[pos + 1].toInt() and 0xFF) shl 8)
                            } else {
                                ((bytes[pos].toInt() and 0xFF) shl 8) or (bytes[pos + 1].toInt() and 0xFF)
                            }
                        }
                        val readUInt32 = { pos: Int ->
                            if (littleEndian) {
                                (bytes[pos].toInt() and 0xFF) or
                                    ((bytes[pos + 1].toInt() and 0xFF) shl 8) or
                                    ((bytes[pos + 2].toInt() and 0xFF) shl 16) or
                                    ((bytes[pos + 3].toInt() and 0xFF) shl 24)
                            } else {
                                ((bytes[pos].toInt() and 0xFF) shl 24) or
                                    ((bytes[pos + 1].toInt() and 0xFF) shl 16) or
                                    ((bytes[pos + 2].toInt() and 0xFF) shl 8) or
                                    (bytes[pos + 3].toInt() and 0xFF)
                            }
                        }
                        val ifdOffset = readUInt32(tiffStart + 4)
                        val dirStart = tiffStart + ifdOffset
                        if (dirStart + 2 <= bytes.size) {
                            val numEntries = readUInt16(dirStart)
                            for (i in 0 until numEntries) {
                                val entryOffset = dirStart + 2 + i * 12
                                if (entryOffset + 12 > bytes.size) break
                                val tag = readUInt16(entryOffset)
                                if (tag == 0x0112) {
                                    return readUInt16(entryOffset + 8).coerceIn(1, 8)
                                }
                            }
                        }
                    }
                }
                offset += 2 + length
            }
            1
        } catch (_: Exception) {
            1
        }
    }
}

