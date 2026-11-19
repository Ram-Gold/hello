package com.example.hello.ui.avatar

import com.example.hello.domain.model.NotionistAvatarConfig
import com.example.hello.domain.model.NotionistTraitCategory

/**
 * Value wrapper holding fetched PNG bytes alongside the canonical DiceBear request URL
 * so StateFlow structural equality works cleanly without unnecessary recompositions.
 */
data class AvatarPreviewData(
    val bytes: ByteArray,
    val requestUrl: String
) {
    override fun equals(other: Any?): Boolean =
        this === other || (other is AvatarPreviewData && requestUrl == other.requestUrl && bytes.contentEquals(other.bytes))

    override fun hashCode(): Int = 31 * requestUrl.hashCode() + bytes.contentHashCode()
}

data class AvatarPlaygroundUiState(
    val contactName: String = "",
    val config: NotionistAvatarConfig = NotionistAvatarConfig(),
    val activeCategory: NotionistTraitCategory = NotionistTraitCategory.HAIR,
    val preview: AvatarPreviewData? = null,
    val isLoadingPreview: Boolean = false,
    val isOffline: Boolean = false,
    val errorMessage: String? = null,
    val thumbnailRetryGeneration: Int = 0,
    val hasUnsavedChanges: Boolean = false,
    val showDiscardDialog: Boolean = false
) {
    val canApply: Boolean
        get() = preview != null &&
            !isLoadingPreview &&
            !isOffline &&
            errorMessage == null &&
            preview.requestUrl == config.toDiceBearPngUrl()
}

sealed interface AvatarPlaygroundUiEvent {
    data class OnSelectCategory(val category: NotionistTraitCategory) : AvatarPlaygroundUiEvent
    data class OnSelectOption(val optionId: String?) : AvatarPlaygroundUiEvent
    data class OnStepOption(val delta: Int) : AvatarPlaygroundUiEvent
    data object OnRandomize : AvatarPlaygroundUiEvent
    data object OnRetry : AvatarPlaygroundUiEvent
    data object OnBackClick : AvatarPlaygroundUiEvent
    data object OnConfirmDiscard : AvatarPlaygroundUiEvent
    data object OnDismissDiscardDialog : AvatarPlaygroundUiEvent
}
