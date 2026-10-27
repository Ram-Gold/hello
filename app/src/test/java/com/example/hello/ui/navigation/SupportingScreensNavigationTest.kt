package com.example.hello.ui.navigation

import com.example.hello.domain.model.RelationshipTerminology
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class SupportingScreensNavigationTest {

    @Test
    fun routes_haveExpectedNames() {
        assertEquals("settings", SettingsRoute.ROUTE_NAME)
        assertEquals("onboarding", OnboardingRoute.ROUTE_NAME)
        assertEquals("home", HomeRoute.ROUTE_NAME)
        assertEquals("people", PeopleRoute.ROUTE_NAME)
    }

    @Test
    fun relationshipTerminology_formatsTitlesAndCountersAccurately() {
        // People
        val people = RelationshipTerminology.PEOPLE
        assertEquals("Person", people.singularTitle)
        assertEquals("People", people.pluralTitle)
        assertEquals("1 person in your network", people.networkCounterText(1))
        assertEquals("12 people in your network", people.networkCounterText(12))
        assertEquals("1 person", people.recentCounterText(1))
        assertEquals("3 people", people.recentCounterText(3))
        assertEquals("Add Person", people.addActionTitle())
        assertEquals("Your People directory is empty", people.emptyDirectoryTitle())

        // Friends
        val friends = RelationshipTerminology.FRIENDS
        assertEquals("Friend", friends.singularTitle)
        assertEquals("Friends", friends.pluralTitle)
        assertEquals("1 friend in your network", friends.networkCounterText(1))
        assertEquals("8 friends in your network", friends.networkCounterText(8))
        assertEquals("Add Friend", friends.addActionTitle())

        // Connections
        val connections = RelationshipTerminology.CONNECTIONS
        assertEquals("Connection", connections.singularTitle)
        assertEquals("Connections", connections.pluralTitle)
        assertEquals("Add Connection", connections.addActionTitle())

        // Contacts
        val contacts = RelationshipTerminology.CONTACTS
        assertEquals("Contact", contacts.singularTitle)
        assertEquals("Contacts", contacts.pluralTitle)
        assertEquals("Add Contact", contacts.addActionTitle())
    }

    @Test
    fun relationshipTerminology_fromId_handlesUnknownAndNullWithDefault() {
        assertEquals(RelationshipTerminology.PEOPLE, RelationshipTerminology.fromId(null))
        assertEquals(RelationshipTerminology.PEOPLE, RelationshipTerminology.fromId("unknown"))
        assertEquals(RelationshipTerminology.FRIENDS, RelationshipTerminology.fromId("friends"))
        assertEquals(RelationshipTerminology.CONNECTIONS, RelationshipTerminology.fromId("CONNECTIONS"))
        assertEquals(RelationshipTerminology.CONTACTS, RelationshipTerminology.fromId("contacts"))
    }
}
