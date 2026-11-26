package com.example.hello.domain.mock

import com.example.hello.domain.model.Contact
import com.example.hello.domain.model.ContactSummary
import com.example.hello.domain.model.Encounter
import com.example.hello.domain.model.UpcomingAlert
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Pure domain seed factory providing canonical sample Contacts, Summaries, Encounters,
 * and initial Home alerts with zero UI-layer imports.
 */
object SeedDataFactory {

    fun createSampleContactSummaries(): List<ContactSummary> = listOf(
        ContactSummary(
            id = "c-1",
            name = "Alex Rivera",
            contextSubtitle = "Product Designer & Tinkerer",
            avatarInitials = "AR",
            avatarBgIndex = 0,
            lastEncounterText = "Met today",
            notes = "Working on design system tokens and micro-interactions. Loves pour-over Ethiopian roast.",
            tags = listOf(MockDataProvider.tagDesign, MockDataProvider.tagTech),
            nicknames = listOf("Rivera")
        ),
        ContactSummary(
            id = "c-2",
            name = "David Kim",
            contextSubtitle = "Mobile Dev, discussed Compose gestures",
            avatarInitials = "DK",
            avatarBgIndex = 1,
            lastEncounterText = "10 days ago",
            notes = "Discussed Compose gestures and physics-based spring transitions.",
            tags = listOf(MockDataProvider.tagTech),
            nicknames = listOf("Dave")
        ),
        ContactSummary(
            id = "c-3",
            name = "Elena Rostova",
            contextSubtitle = "Neighbor & Botanist, exchanged plants",
            avatarInitials = "ER",
            avatarBgIndex = 2,
            lastEncounterText = "Last Sunday",
            notes = "Exchanged monstera cuttings and discussed balcony gardening setup.",
            tags = listOf(MockDataProvider.tagCoffee),
            nicknames = listOf("Lena")
        ),
        ContactSummary(
            id = "c-4",
            name = "Liam Chen",
            contextSubtitle = "Architect, coffee and urban photography",
            avatarInitials = "LC",
            avatarBgIndex = 3,
            lastEncounterText = "2 weeks ago",
            notes = "Architect exploring urban geometry and brutalist structures.",
            tags = listOf(MockDataProvider.tagDesign, MockDataProvider.tagCoffee)
        ),
        ContactSummary(
            id = "c-5",
            name = "Marcus Vance",
            contextSubtitle = "Founder @ Studio Arc, met at Coffee Lab",
            avatarInitials = "MV",
            avatarBgIndex = 4,
            lastEncounterText = "4 days ago",
            notes = "Founder at Studio Arc. Brainstormed local-first privacy models.",
            tags = listOf(MockDataProvider.tagTech, MockDataProvider.tagAi)
        ),
        ContactSummary(
            id = "c-6",
            name = "Maya Patel",
            contextSubtitle = "CS '27, met yesterday at Hackathon",
            avatarInitials = "MP",
            avatarBgIndex = 5,
            lastEncounterText = "Met yesterday",
            notes = "Hackathon project on on-device vector search without cloud sync.",
            tags = listOf(MockDataProvider.tagAi, MockDataProvider.tagTech)
        ),
        ContactSummary(
            id = "c-7",
            name = "Noah Albright",
            contextSubtitle = "High School Friend, catch up quarterly",
            avatarInitials = "NA",
            avatarBgIndex = 0,
            lastEncounterText = "1 month ago",
            notes = "High School Friend catching up quarterly over dinner.",
            tags = emptyList()
        ),
        ContactSummary(
            id = "c-8",
            name = "Sarah Lin",
            contextSubtitle = "Biotech Researcher, tea tasting buddy",
            avatarInitials = "SL",
            avatarBgIndex = 1,
            lastEncounterText = "45 days ago",
            notes = "Biotech Researcher and tea tasting buddy. Exploring fermented oolong varieties.",
            tags = listOf(MockDataProvider.tagCoffee),
            nicknames = listOf("S-Lin", "Sarah")
        ),
        ContactSummary(
            id = "c-9",
            name = "Sophia Martinez",
            contextSubtitle = "UX Researcher, working on accessibility tooling",
            avatarInitials = "SM",
            avatarBgIndex = 2,
            lastEncounterText = "Yesterday",
            notes = "UX Researcher working on tactile accessibility tooling.",
            tags = listOf(MockDataProvider.tagDesign)
        ),
        ContactSummary(
            id = "c-10",
            name = "Zoe Washington",
            contextSubtitle = "Ceramics artist & Studio host",
            avatarInitials = "ZW",
            avatarBgIndex = 3,
            lastEncounterText = "3 weeks ago",
            notes = "Ceramics artist and studio host. Firing stoneware pottery.",
            tags = listOf(MockDataProvider.tagCoffee, MockDataProvider.tagDesign)
        )
    )

    fun createInitialRecentContacts(): List<ContactSummary> = listOf(
        ContactSummary(
            id = "c-1",
            name = "Maya Patel",
            contextSubtitle = "CS '27, met yesterday at Hackathon",
            avatarInitials = "MP",
            avatarBgIndex = 0,
            lastEncounterText = "Met yesterday"
        ),
        ContactSummary(
            id = "c-2",
            name = "Marcus Vance",
            contextSubtitle = "Founder @ Studio Arc, met at Coffee Lab",
            avatarInitials = "MV",
            avatarBgIndex = 1,
            lastEncounterText = "4 days ago"
        ),
        ContactSummary(
            id = "c-3",
            name = "Elena Rostova",
            contextSubtitle = "Neighbor & Botanist, exchanged plants last Sunday",
            avatarInitials = "ER",
            avatarBgIndex = 2,
            lastEncounterText = "Last Sunday"
        ),
        ContactSummary(
            id = "c-4",
            name = "David Kim",
            contextSubtitle = "Mobile Dev, discussed Compose gestures & spring physics",
            avatarInitials = "DK",
            avatarBgIndex = 3,
            lastEncounterText = "10 days ago"
        )
    )

    fun createInitialUpcomingAlerts(): List<UpcomingAlert> = listOf(
        UpcomingAlert.BirthdayAlert(
            id = "alert-1",
            contactName = "Maya Patel",
            dateText = "Tomorrow"
        ),
        UpcomingAlert.FollowUpReminder(
            id = "alert-2",
            contactName = "Alex R.",
            taskDescription = "Send design feedback from hackathon",
            dueText = "In 3 days"
        ),
        UpcomingAlert.SerendipityPrompt(
            id = "alert-3",
            contactName = "Sarah L.",
            daysSinceLastMet = 45,
            contextText = "Last met 6 weeks ago",
            contactId = "c-8"
        )
    )

    fun createInitialFullContacts(): Map<String, Contact> {
        val map = mutableMapOf<String, Contact>()
        MockDataProvider.contacts.forEach { contact ->
            map[contact.id] = contact
        }
        val sampleSummaries = createSampleContactSummaries()
        val sampleDetails = mapOf(
            "c-1" to Contact(
                id = "c-1",
                name = "Alex Rivera",
                aliases = listOf("Rivera"),
                phone = "+1 (555) 876-5432",
                email = "alex.rivera@example.com",
                tagIds = listOf("tag-design"),
                notes = "Senior Product Designer working on accessible UI design systems. Coffee lover.",
                firstMetAt = Instant.now().minus(45, ChronoUnit.DAYS),
                lastMetAt = Instant.now().minus(5, ChronoUnit.DAYS),
                meetCount = 1
            ),
            "c-2" to Contact(
                id = "c-2",
                name = "David Kim",
                aliases = listOf("Dave"),
                phone = "+1 (555) 432-1098",
                email = "dkim@example.com",
                tagIds = listOf("tag-tech"),
                notes = "Mobile Dev, discussed Compose gestures and physics.",
                firstMetAt = Instant.now().minus(60, ChronoUnit.DAYS),
                lastMetAt = Instant.now().minus(10, ChronoUnit.DAYS),
                meetCount = 4
            ),
            "c-3" to Contact(
                id = "c-3",
                name = "Elena Rostova",
                aliases = listOf("Lena"),
                phone = "+1 (555) 345-6789",
                email = "elena.r@example.org",
                tagIds = emptyList(),
                notes = "Neighbor & Botanist, exchanged rare monstera cuttings.",
                firstMetAt = Instant.now().minus(120, ChronoUnit.DAYS),
                lastMetAt = Instant.now().minus(7, ChronoUnit.DAYS),
                meetCount = 6
            ),
            "c-8" to Contact(
                id = "c-8",
                name = "Sarah Lin",
                aliases = listOf("S-Lin", "Sarah"),
                phone = "+1 (555) 234-5678",
                email = "sarah.lin@example.com",
                tagIds = listOf("tag-tech", "tag-ai", "tag-coffee"),
                notes = "Biotech Researcher, tea tasting buddy. Loves light roast pour-overs.",
                firstMetAt = Instant.now().minus(90, ChronoUnit.DAYS),
                lastMetAt = Instant.now().minus(1, ChronoUnit.DAYS),
                meetCount = 3
            )
        )
        sampleDetails.forEach { (id, contact) -> map[id] = contact }
        sampleSummaries.forEach { summary ->
            if (!map.containsKey(summary.id)) {
                map[summary.id] = Contact(
                    id = summary.id,
                    name = summary.name,
                    notes = summary.contextSubtitle,
                    lastMetAt = Instant.now().minus(14, ChronoUnit.DAYS)
                )
            }
        }
        return map
    }

    fun createInitialEncounters(): List<Encounter> {
        val list = MockDataProvider.encounters.toMutableList()
        MockDataProvider.encounters.filter { it.contactId == MockDataProvider.contactSarah.id }.forEach { enc ->
            list.add(enc.copy(id = "enc-c8-${enc.id}", contactId = "c-8"))
        }
        MockDataProvider.encounters.filter { it.contactId == MockDataProvider.contactAlex.id }.forEach { enc ->
            list.add(enc.copy(id = "enc-c1-${enc.id}", contactId = "c-1"))
        }
        return list
    }
}
