package com.example.hello.platform.location

import com.example.hello.domain.model.Place
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationStateTest {

    @Test
    fun locationState_successHoldsNearbyPlaces() {
        val place = Place(id = "p1", label = "Philz Coffee", latitude = 37.77, longitude = -122.41)
        val nearby = listOf(
            PlaceDistance(place = place, distanceMeters = 85.0, formattedDistance = "85m")
        )
        val state = LocationState.Success(
            latitude = 37.77,
            longitude = -122.41,
            accuracy = 10f,
            nearbyPlaces = nearby
        )

        assertEquals(37.77, state.latitude, 0.001)
        assertEquals(-122.41, state.longitude, 0.001)
        assertEquals(10f, state.accuracy, 0.01f)
        assertEquals(1, state.nearbyPlaces.size)
        assertEquals("Philz Coffee", state.nearbyPlaces[0].place.label)
        assertEquals("85m", state.nearbyPlaces[0].formattedDistance)
    }

    @Test
    fun locationState_hierarchyInstances() {
        val idle: LocationState = LocationState.Idle
        val searching: LocationState = LocationState.Searching
        val timedOut: LocationState = LocationState.TimedOut
        val denied: LocationState = LocationState.PermissionDenied
        val disabled: LocationState = LocationState.ProviderDisabled

        assertTrue(idle is LocationState.Idle)
        assertTrue(searching is LocationState.Searching)
        assertTrue(timedOut is LocationState.TimedOut)
        assertTrue(denied is LocationState.PermissionDenied)
        assertTrue(disabled is LocationState.ProviderDisabled)
    }
}
