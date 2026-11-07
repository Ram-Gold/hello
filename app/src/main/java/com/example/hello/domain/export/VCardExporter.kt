package com.example.hello.domain.export

import android.app.Activity
import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.hello.domain.model.ContactSummary
import java.io.File

/**
 * Pure Kotlin utility for generating standard, RFC-compliant vCard 3.0 payloads (ADR-0014)
 * and triggering Android Share Sheet integration.
 */
object VCardExporter {

    /**
     * Generates a combined RFC-compliant vCard 3.0 string for the given contacts list.
     * Each contact is represented by a complete BEGIN:VCARD ... END:VCARD block.
     * If multiple contacts are supplied, blocks are concatenated with clean newlines.
     */
    fun generateCombinedVCard(contacts: List<ContactSummary>): String {
        if (contacts.isEmpty()) return ""
        return contacts.joinToString("\n") { generateVCard(it) }
    }

    /**
     * Generates a single RFC-compliant vCard 3.0 string for an individual [ContactSummary].
     */
    fun generateVCard(contact: ContactSummary): String = buildString {
        appendLine("BEGIN:VCARD")
        appendLine("VERSION:3.0")

        val trimmedName = contact.name.trim()
        appendLine("FN:${escapeText(trimmedName)}")

        val tokens = trimmedName.split(Regex("\\s+")).filter { it.isNotBlank() }
        val (family, given) = when {
            tokens.isEmpty() -> "" to ""
            tokens.size == 1 -> tokens[0] to ""
            else -> tokens.last() to tokens.dropLast(1).joinToString(" ")
        }
        appendLine("N:${escapeText(family)};${escapeText(given)};;;")

        val phone = contact.phone?.trim()
        if (!phone.isNullOrBlank()) {
            appendLine("TEL;TYPE=CELL:$phone")
        }

        val email = contact.email?.trim()
        if (!email.isNullOrBlank()) {
            appendLine("EMAIL;TYPE=INTERNET:$email")
        }

        val notes = contact.notes.trim()
        if (notes.isNotBlank()) {
            appendLine("NOTE:${escapeText(notes)}")
        }

        val categories = contact.tags.map { it.name }
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
        if (categories.isNotEmpty()) {
            appendLine("CATEGORIES:${categories.joinToString(",") { escapeText(it) }}")
        }

        append("END:VCARD")
    }

    /**
     * Escapes characters in text values per RFC 2426 vCard 3.0 specification:
     * - Backslash: `\` -> `\\`
     * - Semicolon: `;` -> `\;`
     * - Comma: `,` -> `\,`
     * - Newlines: `\r\n`, `\r`, `\n` -> `\n`
     */
    fun escapeText(value: String): String {
        return value
            .replace("\\", "\\\\")
            .replace(";", "\\;")
            .replace(",", "\\,")
            .replace("\r\n", "\\n")
            .replace("\r", "\\n")
            .replace("\n", "\\n")
    }

    /**
     * Creates an Intent to share contacts as a single combined .vcf file.
     * Writes the vCard data to cacheDir/vcards/contacts_export_${contacts.size}.vcf.
     */
    fun createShareVCardIntent(context: Context, contacts: List<ContactSummary>): Intent? {
        if (contacts.isEmpty()) return null

        val vcfContent = generateCombinedVCard(contacts)
        val vcardsDir = File(context.cacheDir, "vcards")
        if (!vcardsDir.exists()) {
            vcardsDir.mkdirs()
        }
        val file = File(vcardsDir, "contacts_export_${contacts.size}.vcf")
        file.writeText(vcfContent)

        val authority = "${context.packageName}.fileprovider"
        val uri = FileProvider.getUriForFile(context, authority, file)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/x-vcard"
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newRawUri("vCard", uri)
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }

        val chooserTitle = if (contacts.size == 1) "Share 1 contact" else "Share ${contacts.size} contacts"
        return Intent.createChooser(intent, chooserTitle).apply {
            if (context !is Activity) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
    }

    /**
     * Helper to export contacts to a temporary .vcf file and launch the Android Share Sheet.
     */
    fun shareVCard(context: Context, contacts: List<ContactSummary>) {
        val chooser = createShareVCardIntent(context, contacts) ?: return
        context.startActivity(chooser)
    }
}

/**
 * Top-level convenience function for [VCardExporter.shareVCard].
 */
fun shareVCard(context: Context, contacts: List<ContactSummary>) {
    VCardExporter.shareVCard(context, contacts)
}
