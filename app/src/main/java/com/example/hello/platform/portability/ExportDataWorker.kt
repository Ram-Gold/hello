package com.example.hello.platform.portability

import android.content.Context
import android.net.Uri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.hello.data.local.HelloDatabase
import com.example.hello.data.local.entity.toDomain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * WorkManager worker executing the asynchronous export pipeline (ADR-0006).
 * Compiles canonical SQLite data into a timestamped Export Bundle ZIP containing:
 * - Machine-readable JSON schemas (*.json)
 * - Individual Obsidian-compatible Markdown notes (contacts/[name].md with YAML frontmatter)
 * - Tabular CSV spreadsheets (*.csv)
 */
class ExportDataWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val db = HelloDatabase.getInstance(applicationContext)

            val contacts = db.contactDao().getAllContactsFlow().firstOrNull().orEmpty().map { it.toDomain() }
            val encounters = db.encounterDao().getAllEncountersFlow().firstOrNull().orEmpty().map { it.toDomain() }
            val places = db.placeDao().getAllPlacesFlow().firstOrNull().orEmpty().map { it.toDomain() }
            val tags = db.tagDao().getAllTagsFlow().firstOrNull().orEmpty().map { it.toDomain() }
            val followUps = db.followUpDao().getPendingFollowUpsFlow().firstOrNull().orEmpty().map { it.toDomain() }

            val uriString = inputData.getString(KEY_OUTPUT_URI)
            val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
            val outputDescription: String

            if (!uriString.isNullOrBlank()) {
                val uri = Uri.parse(uriString)
                val outputStream = applicationContext.contentResolver.openOutputStream(uri)
                    ?: return@withContext Result.failure(workDataOf(KEY_ERROR to "Could not open SAF output stream"))

                outputStream.use { out ->
                    ExportBundleGenerator.buildZipBundle(
                        outputStream = out,
                        contacts = contacts,
                        encounters = encounters,
                        places = places,
                        tags = tags,
                        followUps = followUps
                    )
                }
                outputDescription = uriString
            } else {
                // Fallback to internal/external export directory
                val exportDir = File(applicationContext.filesDir, "exports").apply { mkdirs() }
                val exportFile = File(exportDir, "hello-export-$timestamp.zip")

                FileOutputStream(exportFile).use { out ->
                    ExportBundleGenerator.buildZipBundle(
                        outputStream = out,
                        contacts = contacts,
                        encounters = encounters,
                        places = places,
                        tags = tags,
                        followUps = followUps
                    )
                }
                outputDescription = exportFile.absolutePath
            }

            Result.success(
                workDataOf(
                    KEY_RESULT_PATH to outputDescription,
                    KEY_CONTACT_COUNT to contacts.size,
                    KEY_ENCOUNTER_COUNT to encounters.size
                )
            )
        } catch (e: Exception) {
            Result.failure(workDataOf(KEY_ERROR to (e.message ?: "Export failed")))
        }
    }

    companion object {
        const val KEY_OUTPUT_URI = "key_output_uri"
        const val KEY_RESULT_PATH = "key_result_path"
        const val KEY_CONTACT_COUNT = "key_contact_count"
        const val KEY_ENCOUNTER_COUNT = "key_encounter_count"
        const val KEY_ERROR = "key_error"

        const val WORK_TAG_EXPORT = "hello_data_export"
    }
}
