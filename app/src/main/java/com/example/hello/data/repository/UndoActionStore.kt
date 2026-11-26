package com.example.hello.data.repository

import com.example.hello.domain.model.Contact
import com.example.hello.domain.model.Encounter
import com.example.hello.domain.model.FollowUp
import com.example.hello.domain.model.Tag

/**
 * In-memory recovery store for ADR-0027 transient Undo Actions across Contacts,
 * Encounters, Tags, and FollowUps.
 */
class UndoActionStore {

    data class DeletedContactsSnapshot(
        val contacts: List<Contact>,
        val encounters: List<Encounter>,
        val followUps: List<FollowUp>
    )

    data class DeletedTagSnapshot(
        val tag: Tag,
        val affectedContactIds: Set<String>
    )

    data class FollowUpUndoSnapshot(
        val followUps: List<FollowUp>,
        val wasCompleted: Boolean
    )

    private var lastArchiveOperations: List<Pair<String, Boolean>> = emptyList()
    private var lastDeletedContacts: DeletedContactsSnapshot? = null
    private var lastDeletedEncounters: List<Encounter> = emptyList()
    private var lastDeletedTag: DeletedTagSnapshot? = null
    private var lastFollowUpMutation: FollowUpUndoSnapshot? = null

    fun recordArchiveBatch(operations: List<Pair<String, Boolean>>) {
        lastArchiveOperations = operations
    }

    fun consumeArchiveBatch(): List<Pair<String, Boolean>> {
        val ops = lastArchiveOperations
        lastArchiveOperations = emptyList()
        return ops
    }

    fun recordDeletedContacts(
        contacts: List<Contact>,
        encounters: List<Encounter>,
        followUps: List<FollowUp>
    ) {
        if (contacts.isNotEmpty()) {
            lastDeletedContacts = DeletedContactsSnapshot(contacts, encounters, followUps)
        }
    }

    fun consumeDeletedContacts(): DeletedContactsSnapshot? {
        val snapshot = lastDeletedContacts
        lastDeletedContacts = null
        return snapshot
    }

    fun recordDeletedEncounters(encounters: List<Encounter>) {
        if (encounters.isNotEmpty()) {
            lastDeletedEncounters = encounters
        }
    }

    fun consumeDeletedEncounters(): List<Encounter> {
        val snapshot = lastDeletedEncounters
        lastDeletedEncounters = emptyList()
        return snapshot
    }

    fun recordDeletedTag(tag: Tag, affectedContactIds: Set<String>) {
        lastDeletedTag = DeletedTagSnapshot(tag, affectedContactIds)
    }

    fun consumeDeletedTag(): DeletedTagSnapshot? {
        val snapshot = lastDeletedTag
        lastDeletedTag = null
        return snapshot
    }

    fun recordFollowUpMutation(followUps: List<FollowUp>, wasCompleted: Boolean) {
        if (followUps.isNotEmpty()) {
            lastFollowUpMutation = FollowUpUndoSnapshot(followUps, wasCompleted)
        }
    }

    fun consumeFollowUpMutation(): FollowUpUndoSnapshot? {
        val snapshot = lastFollowUpMutation
        lastFollowUpMutation = null
        return snapshot
    }
}
