package com.example.hello.platform.location

import com.example.hello.domain.model.Place
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class HaversineDistanceCalculatorTest {

    @Test
    fun calculateDistanceMeters_calculatesAccurateDistance() {
        // San Francisco Ferry Building to Coit Tower is ~1.4 km (1400m)
        val ferryLat = 37.7955
        val ferryLng = -122.3937
        val coitLat = 37.8024
        val coitLng = -122.4058

        val distance = HaversineDistanceCalculator.calculateDistanceMeters(
            lat1 = ferryLat,
            lon1 = ferryLng,
            lat2 = coitLat,
            lon2 = coitLng
        )

        // Expected distance is approximately 1310m - 1350m
        assertTrue("Distance should be around 1330m, but was $distance", distance in 1300.0..1360.0)
    }

    @Test
    fun calculateDistanceMeters_sameCoordinates_returnsZero() {
        val distance = HaversineDistanceCalculator.calculateDistanceMeters(
            lat1 = 37.7749,
            lon1 = -122.4194,
            lat2 = 37.7749,
            lon2 = -122.4194
        )
        assertEquals(0.0, distance, 0.001)
    }

    @Test
    fun findNearbyPlaces_filtersByRadiusAndSortsCorrectly() {
        val userLat = 37.7749
        val userLng = -122.4194

        val placeClose = Place(
            id = "p-close",
            label = "Philz Coffee",
            latitude = 37.7752, // ~40m away
            longitude = -122.4192,
            useCount = 5,
            lastUsedAt = Instant.now()
        )

        val placeMid = Place(
            id = "p-mid",
            label = "Blue Bottle",
            latitude = 37.7770, // ~250m away
            longitude = -122.4180,
            useCount = 10,
            lastUsedAt = Instant.now()
        )

        val placeFar = Place(
            id = "p-far",
            label = "Distant Park",
            latitude = 37.7900, // ~1700m away
            longitude = -122.4100,
            useCount = 20,
            lastUsedAt = Instant.now()
        )

        val placeNoCoords = Place(
            id = "p-no-coords",
            label = "Online Call",
            latitude = null,
            longitude = null
        )

        val places = listOf(placeFar, placeClose, placeNoCoords, placeMid)

        val nearby = HaversineDistanceCalculator.findNearbyPlaces(
            places = places,
            latitude = userLat,
            longitude = userLng,
            radiusMeters = 500.0
        )

        assertEquals(2, nearby.size)
        assertEquals("p-close", nearby[0].place.id)
        assertEquals("p-mid", nearby[1].place.id)
        assertTrue(nearby[0].distanceMeters < nearby[1].distanceMeters)
        assertEquals("Philz Coffee", nearby[0].place.label)
        assertTrue(nearby[0].formattedDistance.endsWith("m"))
    }

    @Test
    fun formatDistance_formatsMetersAndKilometers() {
        assertEquals("< 10m", HaversineDistanceCalculator.formatDistance(5.0))
        assertEquals("150m", HaversineDistanceCalculator.formatDistance(150.2))
        assertEquals("1.2km", HaversineDistanceCalculator.formatDistance(1240.0))
    }
}
