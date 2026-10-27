package com.example.hello.platform.location

import com.example.hello.domain.model.Place
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Data class representing a cataloged Place with its calculated distance from current coordinates.
 */
data class PlaceDistance(
    val place: Place,
    val distanceMeters: Double,
    val formattedDistance: String
)

/**
 * Pure Kotlin Haversine distance calculator for offline Place proximity suggestions.
 * Follows Hello domain model: Place (avoiding Venue, Location, Spot).
 */
object HaversineDistanceCalculator {

    private const val EARTH_RADIUS_METERS = 6371000.0

    /**
     * Calculates the great-circle distance between two points in meters using the Haversine formula.
     */
    fun calculateDistanceMeters(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val originLatRad = Math.toRadians(lat1)
        val destLatRad = Math.toRadians(lat2)

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(originLatRad) * cos(destLatRad) *
                sin(dLon / 2) * sin(dLon / 2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_METERS * c
    }

    /**
     * Finds and ranks cataloged Places within [radiusMeters] of given coordinates.
     * Sorted ascending by distance, secondary sort by useCount descending.
     */
    fun findNearbyPlaces(
        places: List<Place>,
        latitude: Double,
        longitude: Double,
        radiusMeters: Double = 500.0
    ): List<PlaceDistance> {
        return places.mapNotNull { place ->
            val pLat = place.latitude ?: return@mapNotNull null
            val pLng = place.longitude ?: return@mapNotNull null
            val dist = calculateDistanceMeters(latitude, longitude, pLat, pLng)
            if (dist <= radiusMeters) {
                PlaceDistance(
                    place = place,
                    distanceMeters = dist,
                    formattedDistance = formatDistance(dist)
                )
            } else {
                null
            }
        }.sortedWith(
            compareBy<PlaceDistance> { it.distanceMeters }
                .thenByDescending { it.place.useCount }
        )
    }

    /**
     * Formats distance into a human-readable badge text (e.g. "< 10m", "150m", "1.2km").
     */
    fun formatDistance(meters: Double): String {
        return when {
            meters < 10.0 -> "< 10m"
            meters < 1000.0 -> "${meters.roundToInt()}m"
            else -> String.format(Locale.US, "%.1fkm", meters / 1000.0)
        }
    }
}
