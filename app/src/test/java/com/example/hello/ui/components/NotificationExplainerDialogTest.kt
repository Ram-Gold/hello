package com.example.hello.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class NotificationExplainerDialogTest {

    @Test
    fun dueDateReminderVectorDrawable_existsWithValidViewportAndPaths() {
        val resRoot = File("src/main/res/drawable").takeIf { it.exists() }
            ?: File("app/src/main/res/drawable")
        val vectorFile = File(resRoot, "ic_due_date_reminder.xml")
        assertTrue("Expected ic_due_date_reminder.xml to exist", vectorFile.exists())

        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(vectorFile)
        val vectorEl = doc.documentElement
        assertEquals("vector", vectorEl.tagName)
        assertEquals("194dp", vectorEl.getAttribute("android:width"))
        assertEquals("173dp", vectorEl.getAttribute("android:height"))
        assertEquals("194", vectorEl.getAttribute("android:viewportWidth"))
        assertEquals("173", vectorEl.getAttribute("android:viewportHeight"))

        val paths = vectorEl.getElementsByTagName("path")
        assertEquals(2, paths.length)
        for (i in 0 until paths.length) {
            val pathEl = paths.item(i) as org.w3c.dom.Element
            assertTrue(pathEl.getAttribute("android:pathData").isNotBlank())
            assertEquals("#FF000000", pathEl.getAttribute("android:fillColor"))
        }
    }

    @Test
    fun notificationExplainerCopy_resolvesRuntimeAndSystemSettingsVariants() {
        val defaultCopy = resolveNotificationExplainerCopy(openSystemSettings = false)
        assertEquals("Never Miss a Reminder", defaultCopy.title)
        assertEquals(
            "Hello sends local notifications strictly for reminders you schedule. Everything runs 100% on your device with no servers or tracking.",
            defaultCopy.body
        )
        assertEquals("Allow", defaultCopy.confirmLabel)
        assertEquals("Not Now", defaultCopy.dismissLabel)

        val settingsCopy = resolveNotificationExplainerCopy(openSystemSettings = true)
        assertEquals("Enable Reminder Alerts", settingsCopy.title)
        assertEquals(
            "Your reminder is saved in Hello, but notifications are currently turned off in Android settings. Enable them to receive on-device alerts when reminders are due.",
            settingsCopy.body
        )
        assertEquals("Open Settings", settingsCopy.confirmLabel)
        assertEquals("Not Now", settingsCopy.dismissLabel)
    }
}
