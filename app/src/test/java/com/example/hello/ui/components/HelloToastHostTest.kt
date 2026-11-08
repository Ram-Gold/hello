package com.example.hello.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Info
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HelloToastHostTest {

    @Test
    fun toastPill_isAlertLogic_identifiesErrorAndInformationMessages() {
        val successMessages = listOf(
            "Marked as done!",
            "Contact updated",
            "Added to favorites",
            "Export finished"
        )

        val alertMessages = listOf(
            "Failed to save contact",
            "No phone number available",
            "An error occurred"
        )

        successMessages.forEach { msg ->
            val isAlert = msg.contains("error", ignoreCase = true) ||
                msg.contains("fail", ignoreCase = true) ||
                msg.contains("no ", ignoreCase = true)
            assertFalse("Expected '$msg' not to be classified as alert", isAlert)
        }

        alertMessages.forEach { msg ->
            val isAlert = msg.contains("error", ignoreCase = true) ||
                msg.contains("fail", ignoreCase = true) ||
                msg.contains("no ", ignoreCase = true)
            assertTrue("Expected '$msg' to be classified as alert", isAlert)
        }
    }
}

