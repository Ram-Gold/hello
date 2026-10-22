package com.example.hello.domain.mock

import com.example.hello.domain.model.Circle
import com.example.hello.domain.model.Contact
import com.example.hello.domain.model.Encounter
import com.example.hello.domain.model.FollowUp
import com.example.hello.domain.model.Place
import com.example.hello.domain.model.Tag
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Realistic mock data fixtures for offline development, tests, and preview rendering.
 */
object MockDataProvider {

    val tagTech = Tag(
        id = "tag-tech",
        name = "Tech",
        colorHex = "#E06D3B",
        useCount = 5,
        lastUsedAt = Instant.now().minus(2, ChronoUnit.DAYS)
    )

    val tagDesign = Tag(
        id = "tag-design",
        name = "Design",
        colorHex = "#F3A261",
        useCount = 3,
        lastUsedAt = Instant.now().minus(5, ChronoUnit.DAYS)
    )

    val tagCoffee = Tag(
        id = "tag-coffee",
        name = "Coffee",
        colorHex = "#8D6E63",
        useCount = 4,
        lastUsedAt = Instant.now().minus(1, ChronoUnit.DAYS)
    )

    val tagAi = Tag(
        id = "tag-ai",
        name = "AI",
        colorHex = "#4A6572",
        useCount = 2,
        lastUsedAt = Instant.now().minus(3, ChronoUnit.DAYS)
    )

    val tags: List<Tag> = listOf(tagTech, tagDesign, tagCoffee, tagAi)

    val placeBlueBottle = Place(
        id = "place-blue-bottle",
        label = "Blue Bottle Coffee",
        latitude = 37.7825,
        longitude = -122.4075,
        useCount = 4,
        lastUsedAt = Instant.now().minus(1, ChronoUnit.DAYS)
    )

    val placeMoscone = Place(
        id = "place-moscone",
        label = "Moscone Convention Center",
        latitude = 37.7842,
        longitude = -122.4016,
        useCount = 2,
        lastUsedAt = Instant.now().minus(14, ChronoUnit.DAYS)
    )

    val placeSightglass = Place(
        id = "place-sightglass",
        label = "Sightglass Coffee",
        latitude = 37.7773,
        longitude = -122.4085,
        useCount = 1,
        lastUsedAt = Instant.now().minus(30, ChronoUnit.DAYS)
    )

    val places: List<Place> = listOf(placeBlueBottle, placeMoscone, placeSightglass)

    val contactSarah = Contact(
        id = "contact-sarah-lin",
        name = "Sarah Lin",
        aliases = listOf("S-Lin", "Sarah"),
        circles = listOf(Circle.WORK),
        tagIds = listOf(tagTech.id, tagAi.id, tagCoffee.id),
        notes = "Co-founder & Staff Engineer. Loves light roast pour-overs and distributed systems.",
        photoUri = null,
        firstMetAt = Instant.now().minus(90, ChronoUnit.DAYS),
        lastMetAt = Instant.now().minus(1, ChronoUnit.DAYS),
        meetCount = 3,
        cadenceDays = 30
    )

    val contactAlex = Contact(
        id = "contact-alex-rivera",
        name = "Alex Rivera",
        aliases = listOf("Rivera"),
        circles = listOf(Circle.FRIENDS),
        tagIds = listOf(tagDesign.id),
        notes = "Senior Product Designer working on accessible UI design systems.",
        photoUri = null,
        firstMetAt = Instant.now().minus(45, ChronoUnit.DAYS),
        lastMetAt = Instant.now().minus(5, ChronoUnit.DAYS),
        meetCount = 1,
        cadenceDays = 14
    )

    val contactElena = Contact(
        id = "contact-elena-gomez",
        name = "Elena Gomez",
        aliases = listOf("Mom", "Elena"),
        circles = listOf(Circle.FAMILY),
        tagIds = emptyList(),
        notes = "Weekly Sunday dinner and family gardening check-in.",
        photoUri = null,
        firstMetAt = Instant.now().minus(365, ChronoUnit.DAYS),
        lastMetAt = Instant.now().minus(3, ChronoUnit.DAYS),
        meetCount = 12,
        cadenceDays = 7
    )

    val contacts: List<Contact> = listOf(contactSarah, contactAlex, contactElena)

    val encounterSarah1 = Encounter(
        id = "enc-sarah-1",
        contactId = contactSarah.id,
        occurredAt = Instant.now().minus(90, ChronoUnit.DAYS),
        placeId = placeMoscone.id,
        latitude = 37.7842,
        longitude = -122.4016,
        eventLabel = "Tech Summit 2026",
        notes = "First met at the local-first distributed systems panel. Discussed CRDTs and offline caching."
    )

    val encounterSarah2 = Encounter(
        id = "enc-sarah-2",
        contactId = contactSarah.id,
        occurredAt = Instant.now().minus(35, ChronoUnit.DAYS),
        placeId = placeSightglass.id,
        latitude = 37.7773,
        longitude = -122.4085,
        eventLabel = "Architecture Sync",
        notes = "Deep-dive on Room SQLite vs Markdown frontmatter storage tradeoffs."
    )

    val encounterSarah3 = Encounter(
        id = "enc-sarah-3",
        contactId = contactSarah.id,
        occurredAt = Instant.now().minus(1, ChronoUnit.DAYS),
        placeId = placeBlueBottle.id,
        latitude = 37.7825,
        longitude = -122.4075,
        eventLabel = "Quick Catch-up",
        notes = "Caught up on Android 15 edge-to-edge guidelines and Compose performance."
    )

    val encounterAlex1 = Encounter(
        id = "enc-alex-1",
        contactId = contactAlex.id,
        occurredAt = Instant.now().minus(5, ChronoUnit.DAYS),
        placeId = placeBlueBottle.id,
        latitude = 37.7825,
        longitude = -122.4075,
        eventLabel = "Coffee Chat",
        notes = "Reviewed tactile design tokens and squircle border radius animations."
    )

    val encounters: List<Encounter> = listOf(
        encounterSarah1,
        encounterSarah2,
        encounterSarah3,
        encounterAlex1
    )

    val followUpSarah = FollowUp(
        id = "fu-sarah-1",
        contactId = contactSarah.id,
        dueAt = Instant.now().plus(2, ChronoUnit.DAYS),
        note = "Send link to offline PMTiles vector map sample repo.",
        doneAt = null
    )

    val followUpAlex = FollowUp(
        id = "fu-alex-1",
        contactId = contactAlex.id,
        dueAt = Instant.now().minus(2, ChronoUnit.DAYS),
        note = "Share design token export json.",
        doneAt = Instant.now().minus(1, ChronoUnit.DAYS)
    )

    val followUps: List<FollowUp> = listOf(followUpSarah, followUpAlex)
}
