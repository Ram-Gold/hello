package com.example.hello.ui.components

import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Attaches a BringIntoViewRequester and automatically triggers bringIntoView()
 * whenever the composable gains focus, ensuring the soft keyboard does not occlude the input field.
 * Dispatches multiple requests across the soft keyboard animation window (0ms, 120ms, 300ms)
 * to smoothly accommodate viewport height changes.
 */
fun Modifier.bringIntoViewOnFocus(
    coroutineScope: CoroutineScope,
    requester: BringIntoViewRequester
): Modifier = this
    .bringIntoViewRequester(requester)
    .onFocusChanged { focusState ->
        if (focusState.isFocused) {
            coroutineScope.launch {
                requester.bringIntoView()
                delay(120)
                requester.bringIntoView()
                delay(200)
                requester.bringIntoView()
            }
        }
    }
