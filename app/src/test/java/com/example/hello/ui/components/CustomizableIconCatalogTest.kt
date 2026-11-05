package com.example.hello.ui.components

import com.example.hello.data.repository.ContactRepository
import com.example.hello.domain.model.Circle
import com.example.hello.domain.model.CircleGroupConfig
import com.example.hello.domain.model.Contact
import com.example.hello.domain.model.GroupIconRegistry
import com.example.hello.domain.model.HelloIconKey
import com.example.hello.domain.model.extractAlphanumericInitials
import com.example.hello.ui.contacts.ContactsViewModel
import com.example.hello.ui.home.HomeUiEvent
import com.example.hello.ui.home.HomeViewModel
import com.example.hello.ui.quickaction.QuickActionViewModel
import com.example.hello.ui.settings.SettingsUiState
import com.example.hello.ui.timeline.TimelineViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class CustomizableIconCatalogTest {

    private fun containsEmoji(text: String): Boolean {
        return text.codePoints().anyMatch { cp ->
            cp in 0x1F300..0x1FAFF ||
                cp in 0x2600..0x27BF ||
                cp in 0x2300..0x23FF ||
                cp == 0x2B50 ||
                cp == 0x2728 ||
                cp == 0xFE0F
        }
    }

    @Test
    fun circleDefaultIconsAndCustomOverrides_resolveThroughHelloIconKeyCatalog() {
        assertEquals(HelloIconKey.BRIEFCASE, Circle.WORK.defaultIconKey)
        assertEquals(HelloIconKey.COFFEE, Circle.FRIENDS.defaultIconKey)
        assertEquals(HelloIconKey.HOME, Circle.FAMILY.defaultIconKey)
        assertEquals(HelloIconKey.GRADUATION_CAP, Circle.UNI.defaultIconKey)

        // Every HelloIconKey resolves to a non-null ImageVector
        HelloIconKey.entries.forEach { key ->
            assertNotNull("Expected ImageVector for $key", key.resolveImageVector())
        }

        // User can customize a Group's icon via CircleGroupConfig in HomeViewModel
        val homeViewModel = HomeViewModel()
        val initialWorkConfig = homeViewModel.uiState.value.circleConfigs[Circle.WORK]
        assertNotNull(initialWorkConfig)
        assertEquals(HelloIconKey.BRIEFCASE, initialWorkConfig?.iconKey)

        val customizedWorkConfig = initialWorkConfig!!.copy(iconKey = HelloIconKey.CODE)
        homeViewModel.onEvent(HomeUiEvent.OnUpdateCircleConfig(customizedWorkConfig))

        assertEquals(
            HelloIconKey.CODE,
            homeViewModel.uiState.value.circleConfigs[Circle.WORK]?.iconKey
        )
        assertEquals(HelloIconKey.CODE, GroupIconRegistry.getIconKey(Circle.WORK))
        assertTrue(HelloIconKey.customizableGroupIcons.contains(HelloIconKey.CODE))
        GroupIconRegistry.reset()
    }

    @Test
    fun blankContactNamesAndSameDayBirthdays_produceEmojiFreeInitialsAndLabels() {
        val repository = ContactRepository()
        val addedBlank = repository.addContactWithInitialEncounter(
            name = "   ",
            circles = setOf(Circle.FRIENDS),
            tagIds = emptySet(),
            placeId = null,
            customPlaceName = null,
            note = "Met at gallery",
            followUpDays = null
        )
        assertEquals("", addedBlank.avatarInitials)
        assertEquals("", extractAlphanumericInitials("???"))
        assertFalse(
            "Avatar initials must not contain emojis: ${addedBlank.avatarInitials}",
            containsEmoji(addedBlank.avatarInitials)
        )

        val today = LocalDate.of(2026, 11, 6)
        val bdayContact = Contact(
            id = "c-bday-today",
            name = "Nora Vance",
            circles = listOf(Circle.FRIENDS),
            birthday = LocalDate.of(1996, 11, 6),
            firstMetAt = Instant.now(),
            lastMetAt = Instant.now()
        )
        val (futureNodes, _) = TimelineViewModel.buildTimelineNodes(
            contactsMap = mapOf(bdayContact.id to bdayContact),
            encounters = emptyList(),
            placesMap = emptyMap(),
            followUps = emptyList(),
            today = today,
            zoneId = ZoneOffset.UTC
        )
        assertEquals(1, futureNodes.size)
        assertEquals("Today!", futureNodes.first().dateLabel)
        assertFalse(containsEmoji(futureNodes.first().dateLabel))
    }

    @Test
    fun codebaseAndInitialViewModelStates_containZeroEmojiLiterals() {
        assertFalse(containsEmoji(HomeViewModel().uiState.value.toString()))
        assertFalse(containsEmoji(TimelineViewModel().uiState.value.toString()))
        assertFalse(containsEmoji(ContactsViewModel().uiState.value.toString()))
        assertFalse(containsEmoji(QuickActionViewModel().uiState.value.toString()))
        assertFalse(containsEmoji(SettingsUiState().toString()))

        val srcRoot = File("src/main/java").takeIf { it.exists() }
            ?: File("app/src/main/java")
        assertTrue("Source directory must exist", srcRoot.exists())

        val violations = mutableListOf<String>()
        srcRoot.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .forEach { file ->
                file.readLines().forEachIndexed { idx, line ->
                    if (containsEmoji(line)) {
                        violations += "${file.path}:${idx + 1}: ${line.trim()}"
                    }
                }
            }

        assertTrue(
            "Expected zero emojis in production Kotlin sources, but found:\n${violations.joinToString("\n")}",
            violations.isEmpty()
        )
    }
}
