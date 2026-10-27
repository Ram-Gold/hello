package com.example.hello.platform.portability

import com.example.hello.domain.model.Contact
import com.example.hello.domain.model.Encounter
import com.example.hello.domain.model.FollowUp
import com.example.hello.domain.model.Place
import com.example.hello.domain.model.Tag
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Pure Kotlin multi-format generator compiling SQLite data into a portable Export Bundle (ADR-0006).
 * Produces:
 * 1. Lossless machine-readable JSON files (*.json)
 * 2. Individual human-readable Obsidian/PKM notes (contacts/[name].md with YAML frontmatter)
 * 3. Tabular CSV files (*.csv) for spreadsheet analysis
 */
object ExportBundleGenerator {

    /**
     * Generates machine-readable JSON schemas for all entities.
     */
    fun generateJsonFiles(
        contacts: List<Contact>,
        encounters: List<Encounter>,
        places: List<Place>,
        tags: List<Tag>,
        followUps: List<FollowUp>
    ): Map<String, String> {
        val files = mutableMapOf<String, String>()

        // contacts.json
        files["contacts.json"] = buildString {
            append("[\n")
            contacts.forEachIndexed { i, c ->
                append("  {\n")
                append("    \"id\": ${quoteJson(c.id)},\n")
                append("    \"name\": ${quoteJson(c.name)},\n")
                append("    \"aliases\": [${c.aliases.joinToString(", ") { quoteJson(it) }}],\n")
                append("    \"circles\": [${c.circles.joinToString(", ") { quoteJson(it.name) }}],\n")
                append("    \"tagIds\": [${c.tagIds.joinToString(", ") { quoteJson(it) }}],\n")
                append("    \"phone\": ${quoteJson(c.phone)},\n")
                append("    \"email\": ${quoteJson(c.email)},\n")
                append("    \"notes\": ${quoteJson(c.notes)},\n")
                append("    \"firstMetAt\": ${quoteJson(c.firstMetAt?.toString())},\n")
                append("    \"lastMetAt\": ${quoteJson(c.lastMetAt?.toString())},\n")
                append("    \"meetCount\": ${c.meetCount},\n")
                append("    \"cadenceDays\": ${c.cadenceDays ?: "null"},\n")
                append("    \"isArchived\": ${c.isArchived}\n")
                append("  }${if (i < contacts.size - 1) "," else ""}\n")
            }
            append("]\n")
        }

        // encounters.json
        files["encounters.json"] = buildString {
            append("[\n")
            encounters.forEachIndexed { i, e ->
                append("  {\n")
                append("    \"id\": ${quoteJson(e.id)},\n")
                append("    \"contactId\": ${quoteJson(e.contactId)},\n")
                append("    \"occurredAt\": ${quoteJson(e.occurredAt.toString())},\n")
                append("    \"placeId\": ${quoteJson(e.placeId)},\n")
                append("    \"latitude\": ${e.latitude ?: "null"},\n")
                append("    \"longitude\": ${e.longitude ?: "null"},\n")
                append("    \"eventLabel\": ${quoteJson(e.eventLabel)},\n")
                append("    \"notes\": ${quoteJson(e.notes)}\n")
                append("  }${if (i < encounters.size - 1) "," else ""}\n")
            }
            append("]\n")
        }

        // places.json
        files["places.json"] = buildString {
            append("[\n")
            places.forEachIndexed { i, p ->
                append("  {\n")
                append("    \"id\": ${quoteJson(p.id)},\n")
                append("    \"label\": ${quoteJson(p.label)},\n")
                append("    \"latitude\": ${p.latitude ?: "null"},\n")
                append("    \"longitude\": ${p.longitude ?: "null"},\n")
                append("    \"useCount\": ${p.useCount},\n")
                append("    \"lastUsedAt\": ${quoteJson(p.lastUsedAt?.toString())}\n")
                append("  }${if (i < places.size - 1) "," else ""}\n")
            }
            append("]\n")
        }

        // tags.json
        files["tags.json"] = buildString {
            append("[\n")
            tags.forEachIndexed { i, t ->
                append("  {\n")
                append("    \"id\": ${quoteJson(t.id)},\n")
                append("    \"name\": ${quoteJson(t.name)},\n")
                append("    \"colorHex\": ${quoteJson(t.colorHex)},\n")
                append("    \"useCount\": ${t.useCount},\n")
                append("    \"lastUsedAt\": ${quoteJson(t.lastUsedAt?.toString())}\n")
                append("  }${if (i < tags.size - 1) "," else ""}\n")
            }
            append("]\n")
        }

        // followups.json
        files["followups.json"] = buildString {
            append("[\n")
            followUps.forEachIndexed { i, fu ->
                append("  {\n")
                append("    \"id\": ${quoteJson(fu.id)},\n")
                append("    \"contactId\": ${quoteJson(fu.contactId)},\n")
                append("    \"dueAt\": ${quoteJson(fu.dueAt.toString())},\n")
                append("    \"note\": ${quoteJson(fu.note)},\n")
                append("    \"doneAt\": ${quoteJson(fu.doneAt?.toString())}\n")
                append("  }${if (i < followUps.size - 1) "," else ""}\n")
            }
            append("]\n")
        }

        return files
    }

    /**
     * Generates individual Markdown files with YAML frontmatter for each Contact.
     * Saved under contacts/{Contact Name}.md for native Obsidian / Logseq interoperability.
     */
    fun generateMarkdownNotes(
        contacts: List<Contact>,
        encounters: List<Encounter>,
        places: List<Place>,
        tags: List<Tag>
    ): Map<String, String> {
        val tagMap = tags.associate { it.id to it.name }
        val placeMap = places.associate { it.id to it.label }
        val notesMap = mutableMapOf<String, String>()

        contacts.forEach { contact ->
            val contactEncounters = encounters
                .filter { it.contactId == contact.id }
                .sortedByDescending { it.occurredAt }

            val resolvedTags = contact.tagIds.mapNotNull { tagMap[it] }

            val content = buildString {
                append("---\n")
                append("id: \"${contact.id}\"\n")
                append("name: \"${contact.name}\"\n")
                if (contact.aliases.isNotEmpty()) {
                    append("aliases: [${contact.aliases.joinToString(", ") { "\"$it\"" }}]\n")
                }
                append("circles: [${contact.circles.joinToString(", ") { "\"${it.displayName}\"" }}]\n")
                if (resolvedTags.isNotEmpty()) {
                    append("tags: [${resolvedTags.joinToString(", ") { "\"$it\"" }}]\n")
                }
                if (!contact.phone.isNullOrBlank()) {
                    append("phone: \"${contact.phone}\"\n")
                }
                if (!contact.email.isNullOrBlank()) {
                    append("email: \"${contact.email}\"\n")
                }
                if (contact.cadenceDays != null) {
                    append("cadence_days: ${contact.cadenceDays}\n")
                }
                append("meet_count: ${contact.meetCount}\n")
                if (contact.firstMetAt != null) {
                    append("first_met_at: \"${contact.firstMetAt}\"\n")
                }
                if (contact.lastMetAt != null) {
                    append("last_met_at: \"${contact.lastMetAt}\"\n")
                }
                append("---\n\n")

                if (contact.notes.isNotBlank()) {
                    append("${contact.notes.trim()}\n\n")
                }

                if (contactEncounters.isNotEmpty()) {
                    append("## Encounters\n\n")
                    contactEncounters.forEach { encounter ->
                        val dateStr = DateTimeFormatter.ISO_INSTANT.format(encounter.occurredAt).take(10)
                        val placeLabel = encounter.placeId?.let { placeMap[it] } ?: encounter.eventLabel
                        val header = if (placeLabel != null) "### $dateStr • $placeLabel" else "### $dateStr"
                        append("$header\n")
                        if (encounter.notes.isNotBlank()) {
                            append("${encounter.notes.trim()}\n")
                        }
                        append("\n")
                    }
                }
            }

            val sanitizedName = contact.name.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim()
            val filename = "contacts/${sanitizedName.ifEmpty { contact.id }}.md"
            notesMap[filename] = content
        }

        return notesMap
    }

    /**
     * Generates tabular CSV sheets for spreadsheet and data science analysis.
     */
    fun generateCsvFiles(
        contacts: List<Contact>,
        encounters: List<Encounter>
    ): Map<String, String> {
        val files = mutableMapOf<String, String>()

        // contacts.csv
        files["contacts.csv"] = buildString {
            append("id,name,circles,phone,email,notes,meet_count,last_met_at\n")
            contacts.forEach { c ->
                val circlesStr = c.circles.joinToString(";") { it.name }
                append("${escapeCsv(c.id)},")
                append("${escapeCsv(c.name)},")
                append("${escapeCsv(circlesStr)},")
                append("${escapeCsv(c.phone.orEmpty())},")
                append("${escapeCsv(c.email.orEmpty())},")
                append("${escapeCsv(c.notes)},")
                append("${c.meetCount},")
                append("${escapeCsv(c.lastMetAt?.toString().orEmpty())}\n")
            }
        }

        // encounters.csv
        files["encounters.csv"] = buildString {
            append("id,contact_id,occurred_at,event_label,notes,latitude,longitude\n")
            encounters.forEach { e ->
                append("${escapeCsv(e.id)},")
                append("${escapeCsv(e.contactId)},")
                append("${escapeCsv(e.occurredAt.toString())},")
                append("${escapeCsv(e.eventLabel.orEmpty())},")
                append("${escapeCsv(e.notes)},")
                append("${e.latitude ?: ""},")
                append("${e.longitude ?: ""}\n")
            }
        }

        return files
    }

    /**
     * Compiles all generated JSON, Markdown, and CSV files into a compressed ZIP stream.
     */
    fun buildZipBundle(
        outputStream: OutputStream,
        contacts: List<Contact>,
        encounters: List<Encounter>,
        places: List<Place>,
        tags: List<Tag>,
        followUps: List<FollowUp>
    ) {
        val jsonFiles = generateJsonFiles(contacts, encounters, places, tags, followUps)
        val markdownFiles = generateMarkdownNotes(contacts, encounters, places, tags)
        val csvFiles = generateCsvFiles(contacts, encounters)

        val allFiles = jsonFiles + markdownFiles + csvFiles

        ZipOutputStream(outputStream).use { zipOut ->
            allFiles.forEach { (path, content) ->
                val entry = ZipEntry(path)
                zipOut.putNextEntry(entry)
                zipOut.write(content.toByteArray(StandardCharsets.UTF_8))
                zipOut.closeEntry()
            }
            zipOut.finish()
        }
    }

    private fun quoteJson(value: String?): String {
        if (value == null) return "null"
        val escaped = value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\b", "\\b")
            .replace("\u000C", "\\f")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
        return "\"$escaped\""
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(',') || value.contains('"') || value.contains('\n') || value.contains('\r')) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }
}
