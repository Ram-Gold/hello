package com.example.hello.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class HelloToastHostTest {

    @Test
    fun resolveToastSemanticType_mapsMessagesToCorrectSemanticTypes() {
        assertEquals(ToastSemanticType.ARCHIVE, resolveToastSemanticType("Contact archived"))
        assertEquals(ToastSemanticType.ARCHIVE, resolveToastSemanticType("Archived 1 contact"))

        assertEquals(ToastSemanticType.DELETE, resolveToastSemanticType("Contact deleted"))
        assertEquals(ToastSemanticType.DELETE, resolveToastSemanticType("Deleted 2 contacts"))
        assertEquals(ToastSemanticType.DELETE, resolveToastSemanticType("Deleted 1 encounter"))
        assertEquals(ToastSemanticType.DELETE, resolveToastSemanticType("Photo removed"))

        assertEquals(ToastSemanticType.FAVORITE, resolveToastSemanticType("Added to favorites"))
        assertEquals(ToastSemanticType.FAVORITE, resolveToastSemanticType("Removed from favorites"))
        assertEquals(ToastSemanticType.FAVORITE, resolveToastSemanticType("Starred contact"))

        assertEquals(ToastSemanticType.ALERT, resolveToastSemanticType("Failed to save contact"))
        assertEquals(ToastSemanticType.ALERT, resolveToastSemanticType("No phone number available"))
        assertEquals(ToastSemanticType.ALERT, resolveToastSemanticType("An error occurred"))

        assertEquals(ToastSemanticType.SUCCESS, resolveToastSemanticType("Contact updated"))
        assertEquals(ToastSemanticType.SUCCESS, resolveToastSemanticType("Contact restored"))
        assertEquals(ToastSemanticType.SUCCESS, resolveToastSemanticType("Marked as done!"))
        assertEquals(ToastSemanticType.SUCCESS, resolveToastSemanticType("Export finished"))
    }
}

