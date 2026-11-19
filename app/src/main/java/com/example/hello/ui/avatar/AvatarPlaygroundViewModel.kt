package com.example.hello.ui.avatar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hello.domain.model.NotionistAvatarConfig
import com.example.hello.domain.model.NotionistThumbnailUrlBuilder
import com.example.hello.domain.model.NotionistTraitCategory
import com.example.hello.platform.avatar.NotionistAvatarFetcher
import com.example.hello.platform.avatar.NotionistFetchResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Unidirectional state holder for the opt-in Notionist Avatar Playground (ADR-0035).
 * Debounces live preview requests to stay well within DiceBear's 10 req/s PNG rate limit,
 * preserves trait selections across transient connectivity drops, and tracks dirty state for discard confirmation.
 */
class AvatarPlaygroundViewModel(
    contactName: String,
    initialConfigJson: String?,
    private val fetcher: NotionistAvatarFetcher,
    private val debounceMs: Long = DEFAULT_DEBOUNCE_MS,
    private val randomProvider: () -> NotionistAvatarConfig = { NotionistAvatarConfig.random() }
) : ViewModel() {

    private val initialConfig: NotionistAvatarConfig =
        NotionistAvatarConfig.fromJson(initialConfigJson) ?: randomProvider()

    private val _uiState = MutableStateFlow(
        AvatarPlaygroundUiState(
            contactName = contactName,
            config = initialConfig,
            isOffline = !fetcher.isOnline()
        )
    )
    val uiState: StateFlow<AvatarPlaygroundUiState> = _uiState.asStateFlow()

    private var fetchJob: Job? = null

    init {
        if (fetcher.isOnline()) {
            schedulePreviewFetch(immediate = true)
        }

        viewModelScope.launch {
            fetcher.observeConnectivity().collect { online ->
                handleConnectivityChange(online)
            }
        }
    }

    fun onEvent(event: AvatarPlaygroundUiEvent) {
        when (event) {
            is AvatarPlaygroundUiEvent.OnSelectCategory -> {
                _uiState.update { it.copy(activeCategory = event.category) }
            }
            is AvatarPlaygroundUiEvent.OnSelectOption -> {
                updateConfig { current -> current.withOption(_uiState.value.activeCategory, event.optionId) }
            }
            is AvatarPlaygroundUiEvent.OnStepOption -> {
                updateConfig { current -> current.stepOption(_uiState.value.activeCategory, event.delta) }
            }
            AvatarPlaygroundUiEvent.OnRandomize -> {
                updateConfig { randomProvider() }
            }
            AvatarPlaygroundUiEvent.OnRetry -> {
                if (fetcher.isOnline()) {
                    _uiState.update {
                        it.copy(
                            isOffline = false,
                            errorMessage = null,
                            thumbnailRetryGeneration = it.thumbnailRetryGeneration + 1
                        )
                    }
                    schedulePreviewFetch(immediate = true)
                } else {
                    _uiState.update { it.copy(isOffline = true, isLoadingPreview = false, errorMessage = null) }
                }
            }
            AvatarPlaygroundUiEvent.OnBackClick -> {
                if (_uiState.value.hasUnsavedChanges) {
                    _uiState.update { it.copy(showDiscardDialog = true) }
                }
            }
            AvatarPlaygroundUiEvent.OnConfirmDiscard -> {
                _uiState.update { it.copy(showDiscardDialog = false) }
            }
            AvatarPlaygroundUiEvent.OnDismissDiscardDialog -> {
                _uiState.update { it.copy(showDiscardDialog = false) }
            }
        }
    }

    private fun handleConnectivityChange(online: Boolean) {
        if (!online) {
            fetchJob?.cancel()
            _uiState.update {
                it.copy(
                    isOffline = true,
                    isLoadingPreview = false,
                    errorMessage = null
                )
            }
        } else {
            val current = _uiState.value
            val targetUrl = current.config.toDiceBearPngUrl()
            val needsFetch = current.isOffline || current.preview == null || current.preview.requestUrl != targetUrl
            val wasOffline = current.isOffline
            _uiState.update {
                it.copy(
                    isOffline = false,
                    errorMessage = null,
                    thumbnailRetryGeneration = if (wasOffline) it.thumbnailRetryGeneration + 1 else it.thumbnailRetryGeneration
                )
            }
            if (needsFetch) {
                schedulePreviewFetch(immediate = true)
            }
        }
    }

    /**
     * Loads a canonical 48px PNG thumbnail for a single visual trait tile.
     * Returns cached or freshly fetched PNG bytes, or null if currently offline/unavailable.
     */
    suspend fun loadTraitThumbnail(
        category: NotionistTraitCategory,
        optionId: String
    ): ByteArray? {
        val cacheKey = NotionistThumbnailUrlBuilder.cacheKey(category, optionId)
        val url = NotionistThumbnailUrlBuilder.buildThumbnailUrl(category, optionId)
        return when (val result = fetcher.fetchThumbnail(cacheKey, url)) {
            is NotionistFetchResult.Success -> result.bytes
            else -> null
        }
    }

    private fun updateConfig(transform: (NotionistAvatarConfig) -> NotionistAvatarConfig) {
        var changed = false
        _uiState.update { current ->
            val updated = transform(current.config)
            if (updated == current.config) {
                current
            } else {
                changed = true
                current.copy(
                    config = updated,
                    hasUnsavedChanges = updated != initialConfig,
                    errorMessage = null
                )
            }
        }
        if (changed) {
            schedulePreviewFetch(immediate = false)
        }
    }

    private fun schedulePreviewFetch(immediate: Boolean) {
        fetchJob?.cancel()
        val targetUrl = _uiState.value.config.toDiceBearPngUrl()

        if (!fetcher.isOnline()) {
            _uiState.update {
                it.copy(
                    isOffline = true,
                    isLoadingPreview = false,
                    errorMessage = null
                )
            }
            return
        }

        fetchJob = viewModelScope.launch {
            if (!immediate && debounceMs > 0L) {
                delay(debounceMs)
            }

            if (!fetcher.isOnline()) {
                _uiState.update {
                    it.copy(
                        isOffline = true,
                        isLoadingPreview = false,
                        errorMessage = null
                    )
                }
                return@launch
            }

            _uiState.update {
                it.copy(
                    isLoadingPreview = true,
                    isOffline = false,
                    errorMessage = null
                )
            }

            when (val result = fetcher.fetchPng(targetUrl)) {
                is NotionistFetchResult.Success -> {
                    _uiState.update {
                        it.copy(
                            preview = AvatarPreviewData(bytes = result.bytes, requestUrl = targetUrl),
                            isLoadingPreview = false,
                            isOffline = false,
                            errorMessage = null
                        )
                    }
                }
                NotionistFetchResult.Offline -> {
                    _uiState.update {
                        it.copy(
                            isLoadingPreview = false,
                            isOffline = true,
                            errorMessage = null
                        )
                    }
                }
                NotionistFetchResult.RateLimited -> {
                    _uiState.update {
                        it.copy(
                            isLoadingPreview = false,
                            errorMessage = "Too many preview updates at once. Pause a second and tap Retry."
                        )
                    }
                }
                is NotionistFetchResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoadingPreview = false,
                            errorMessage = result.message
                        )
                    }
                }
            }
        }
    }

    companion object {
        const val DEFAULT_DEBOUNCE_MS = 200L
    }
}
