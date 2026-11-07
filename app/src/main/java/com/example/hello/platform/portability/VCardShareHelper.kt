package com.example.hello.platform.portability

import android.content.Context
import com.example.hello.domain.export.VCardExporter
import com.example.hello.domain.model.ContactSummary

/**
 * Platform convenience bridge to [VCardExporter.shareVCard].
 */
object VCardShareHelper {
    fun shareVCard(context: Context, contacts: List<ContactSummary>) {
        VCardExporter.shareVCard(context, contacts)
    }
}

fun shareVCard(context: Context, contacts: List<ContactSummary>) {
    VCardShareHelper.shareVCard(context, contacts)
}
