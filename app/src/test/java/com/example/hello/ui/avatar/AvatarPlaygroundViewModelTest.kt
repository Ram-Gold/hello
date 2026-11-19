package com.example.hello.ui.avatar

import androidx.lifecycle.ViewModelStore
import com.example.hello.domain.model.NotionistAvatarConfig
import com.example.hello.domain.model.NotionistTraitCategory
import com.example.hello.platform.avatar.NotionistAvatarFetcher
import com.example.hello.platform.avatar.NotionistFetchResult
import com.example.hello.platform.avatar.NotionistThumbnailDiskCache
import java.nio.file.Files
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AvatarPlaygroundViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val activeViewModels = mutableListOf<AvatarPlaygroundViewModel>()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        activeViewModels.forEach { vm ->
            // Clear viewModelScope cleanly so StateFlow collectors never outlive the test
            val store = ViewModelStore()
            val putMethod = ViewModelStore::class.java.getDeclaredMethod(
                "put",
                String::class.java,
                androidx.lifecycle.ViewModel::class.java
            )
            putMethod.isAccessible = true
            putMethod.invoke(store, vm.hashCode().toString(), vm)
            store.clear()
        }
        activeViewModels.clear()
        Dispatchers.resetMain()
    }

    private fun createViewModel(
        contactName: String = "Ada Lovelace",
        initialConfigJson: String?,
        fetcher: NotionistAvatarFetcher,
        debounceMs: Long = 200L
    ): AvatarPlaygroundViewModel = AvatarPlaygroundViewModel(
        contactName = contactName,
        initialConfigJson = initialConfigJson,
        fetcher = fetcher,
        debounceMs = debounceMs
    ).also { activeViewModels += it }

    private class FakeNotionistAvatarFetcher(
        initialOnline: Boolean = true
    ) : NotionistAvatarFetcher {
        val connectivity = MutableStateFlow(initialOnline)
        val requestedUrls = mutableListOf<String>()
        var nextResultProvider: (String) -> NotionistFetchResult = { url ->
            NotionistFetchResult.Success(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47) + url.length.toByte())
        }

        override fun isOnline(): Boolean = connectivity.value
        override fun observeConnectivity(): Flow<Boolean> = connectivity
        override suspend fun fetchPng(url: String): NotionistFetchResult {
            requestedUrls += url
            return nextResultProvider(url)
        }
    }

    @Test
    fun offlineOnEntry_showsOfflineGateWithoutFetchingAndRecoversOnRetry() = runTest {
        val fetcher = FakeNotionistAvatarFetcher(initialOnline = false)
        val initial = NotionistAvatarConfig(seed = "seedoffline1")
        val vm = createViewModel(
            contactName = "Ada Lovelace",
            initialConfigJson = initial.toJson(),
            fetcher = fetcher,
            debounceMs = 200L
        )
        advanceUntilIdle()

        assertTrue(vm.uiState.value.isOffline)
        assertNull(vm.uiState.value.preview)
        assertFalse(vm.uiState.value.canApply)
        assertEquals(0, fetcher.requestedUrls.size)

        // Retry while still offline stays in offline gate
        vm.onEvent(AvatarPlaygroundUiEvent.OnRetry)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.isOffline)
        assertEquals(0, fetcher.requestedUrls.size)

        // Reconnect triggers recovery and loads preview
        fetcher.connectivity.value = true
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isOffline)
        assertNotNull(vm.uiState.value.preview)
        assertTrue(vm.uiState.value.canApply)
        assertEquals(1, fetcher.requestedUrls.size)
    }

    @Test
    fun rapidTraitChanges_coalesceViaDebounceIntoSingleNetworkCall() = runTest {
        val fetcher = FakeNotionistAvatarFetcher(initialOnline = true)
        val initial = NotionistAvatarConfig(seed = "seeddebounce", hair = "variant01")
        val vm = createViewModel(
            contactName = "Ada",
            initialConfigJson = initial.toJson(),
            fetcher = fetcher,
            debounceMs = 200L
        )
        advanceUntilIdle()
        assertEquals(1, fetcher.requestedUrls.size)

        // Rapid taps within debounce window
        vm.onEvent(AvatarPlaygroundUiEvent.OnSelectOption("variant02"))
        advanceTimeBy(80L)
        vm.onEvent(AvatarPlaygroundUiEvent.OnSelectOption("variant03"))
        advanceTimeBy(80L)
        vm.onEvent(AvatarPlaygroundUiEvent.OnSelectOption("variant04"))
        advanceTimeBy(199L)
        // Still debouncing
        assertEquals(1, fetcher.requestedUrls.size)

        advanceTimeBy(1L)
        advanceUntilIdle()
        assertEquals(2, fetcher.requestedUrls.size)
        assertEquals("variant04", vm.uiState.value.config.hair)
        assertTrue(fetcher.requestedUrls.last().contains("hair=variant04"))
        assertTrue(vm.uiState.value.hasUnsavedChanges)
        assertTrue(vm.uiState.value.canApply)
    }

    @Test
    fun midEditConnectivityLoss_preservesSelectionsAndRecoversWhenOnline() = runTest {
        val fetcher = FakeNotionistAvatarFetcher(initialOnline = true)
        val initial = NotionistAvatarConfig(seed = "seedmidedit1", glasses = null)
        val vm = createViewModel(
            contactName = "Ada",
            initialConfigJson = initial.toJson(),
            fetcher = fetcher,
            debounceMs = 200L
        )
        advanceUntilIdle()
        val initialBytes = vm.uiState.value.preview!!.bytes

        // Connectivity drops mid-edit
        fetcher.connectivity.value = false
        advanceUntilIdle()
        assertTrue(vm.uiState.value.isOffline)
        assertFalse(vm.uiState.value.canApply)
        assertArrayEquals(initialBytes, vm.uiState.value.preview?.bytes)

        // User still picks glasses while offline — selections are preserved!
        vm.onEvent(AvatarPlaygroundUiEvent.OnSelectCategory(NotionistTraitCategory.GLASSES))
        vm.onEvent(AvatarPlaygroundUiEvent.OnSelectOption("variant05"))
        advanceUntilIdle()
        assertEquals("variant05", vm.uiState.value.config.glasses)
        assertTrue(vm.uiState.value.isOffline)
        assertFalse(vm.uiState.value.canApply)

        // Connectivity returns -> fetches updated config automatically
        fetcher.connectivity.value = true
        advanceUntilIdle()
        assertFalse(vm.uiState.value.isOffline)
        assertEquals("variant05", vm.uiState.value.config.glasses)
        assertTrue(fetcher.requestedUrls.last().contains("glasses=variant05"))
        assertTrue(vm.uiState.value.canApply)
    }

    @Test
    fun rateLimitedError_showsFriendlyMessageAndAllowsRetry() = runTest {
        val fetcher = FakeNotionistAvatarFetcher(initialOnline = true)
        val initial = NotionistAvatarConfig(seed = "seedratelim1")
        val vm = createViewModel(
            contactName = "Ada",
            initialConfigJson = initial.toJson(),
            fetcher = fetcher,
            debounceMs = 200L
        )
        advanceUntilIdle()

        fetcher.nextResultProvider = { NotionistFetchResult.RateLimited }
        vm.onEvent(AvatarPlaygroundUiEvent.OnSelectOption("variant09"))
        advanceUntilIdle()

        assertNotNull(vm.uiState.value.errorMessage)
        assertFalse(vm.uiState.value.canApply)

        // Retry succeeds
        val okBytes = byteArrayOf(1, 2, 3, 4)
        fetcher.nextResultProvider = { NotionistFetchResult.Success(okBytes) }
        vm.onEvent(AvatarPlaygroundUiEvent.OnRetry)
        advanceUntilIdle()

        assertNull(vm.uiState.value.errorMessage)
        assertArrayEquals(okBytes, vm.uiState.value.preview?.bytes)
        assertTrue(vm.uiState.value.canApply)
    }

    @Test
    fun backClick_opensDiscardDialogOnlyWhenModified() = runTest {
        val fetcher = FakeNotionistAvatarFetcher(initialOnline = true)
        val initial = NotionistAvatarConfig(seed = "seeddiscard1", hair = "variant01")
        val vm = createViewModel(
            contactName = "Ada",
            initialConfigJson = initial.toJson(),
            fetcher = fetcher,
            debounceMs = 200L
        )
        advanceUntilIdle()

        // Untouched -> no discard dialog
        vm.onEvent(AvatarPlaygroundUiEvent.OnBackClick)
        assertFalse(vm.uiState.value.showDiscardDialog)

        // Modify trait -> discard dialog opens
        vm.onEvent(AvatarPlaygroundUiEvent.OnSelectOption("variant07"))
        assertTrue(vm.uiState.value.hasUnsavedChanges)
        vm.onEvent(AvatarPlaygroundUiEvent.OnBackClick)
        assertTrue(vm.uiState.value.showDiscardDialog)

        vm.onEvent(AvatarPlaygroundUiEvent.OnDismissDiscardDialog)
        assertFalse(vm.uiState.value.showDiscardDialog)
    }

    @Test
    fun thumbnailDiskCache_persistsValidPngsAndEvictsCorruptFiles() {
        val tempDir = Files.createTempDirectory("notionist_thumbs_test").toFile()
        try {
            val cache = NotionistThumbnailDiskCache(tempDir)
            val validPng = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3)

            assertNull(cache.get("hair_hat"))
            cache.put("hair_hat", validPng)
            assertArrayEquals(validPng, cache.get("hair_hat"))

            // Corrupt data is ignored on put and evicted if present on disk
            cache.put("eyes_variant01", byteArrayOf(0, 1, 2))
            assertNull(cache.get("eyes_variant01"))

            java.io.File(tempDir, "broken.png").writeBytes(byteArrayOf(5, 6, 7, 8))
            assertNull(cache.get("broken"))
            assertFalse(java.io.File(tempDir, "broken.png").exists())
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun loadTraitThumbnail_delegatesToFetcherAndRetryIncrementsGeneration() = runTest {
        val fetcher = FakeNotionistAvatarFetcher(initialOnline = true)
        val initial = NotionistAvatarConfig(seed = "seedthumb123")
        val vm = createViewModel(
            contactName = "Ada",
            initialConfigJson = initial.toJson(),
            fetcher = fetcher,
            debounceMs = 200L
        )
        advanceUntilIdle()

        val bytes = vm.loadTraitThumbnail(NotionistTraitCategory.EYES, "variant02")
        assertNotNull(bytes)
        assertTrue(fetcher.requestedUrls.last().contains("notionists-neutral/png"))
        assertTrue(fetcher.requestedUrls.last().contains("size=128"))
        assertTrue(fetcher.requestedUrls.last().contains("eyes=variant02"))

        val genBefore = vm.uiState.value.thumbnailRetryGeneration
        vm.onEvent(AvatarPlaygroundUiEvent.OnRetry)
        advanceUntilIdle()
        assertEquals(genBefore + 1, vm.uiState.value.thumbnailRetryGeneration)
    }
}
