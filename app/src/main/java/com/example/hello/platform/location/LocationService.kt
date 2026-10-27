package com.example.hello.platform.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat
import com.example.hello.domain.model.Place
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * Reactive states emitted during non-blocking foreground GPS acquisition.
 */
sealed interface LocationState {
    data object Idle : LocationState
    data object Searching : LocationState
    data class Success(
        val latitude: Double,
        val longitude: Double,
        val accuracy: Float,
        val nearbyPlaces: List<PlaceDistance>
    ) : LocationState
    data object TimedOut : LocationState
    data object PermissionDenied : LocationState
    data object ProviderDisabled : LocationState
}

/**
 * Native platform LocationService utilizing Android's LocationManager.
 * Strictly adheres to Prime Directive 3 (foreground-only GPS capture, zero background location).
 */
class LocationService(
    private val context: Context,
    private val locationManager: LocationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
) {

    /**
     * Checks if foreground location permissions are granted.
     */
    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    /**
     * Checks if either GPS or Network location providers are enabled.
     */
    fun isAnyProviderEnabled(): Boolean {
        val gps = try {
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        } catch (_: Exception) {
            false
        }
        val network = try {
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        } catch (_: Exception) {
            false
        }
        return gps || network
    }

    /**
     * Returns the best available provider (GPS preferred, falling back to Network).
     */
    fun getBestProvider(): String? {
        return when {
            try { locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) } catch (_: Exception) { false } -> LocationManager.GPS_PROVIDER
            try { locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) } catch (_: Exception) { false } -> LocationManager.NETWORK_PROVIDER
            else -> null
        }
    }

    /**
     * Non-blocking coroutine flow acquiring a foreground GPS fix within [timeoutMillis].
     * Evaluates offline Place proximity using [HaversineDistanceCalculator] within a 500m radius.
     */
    fun getLocationFlow(
        places: List<Place>,
        timeoutMillis: Long = 5000L,
        radiusMeters: Double = 500.0
    ): Flow<LocationState> = flow {
        if (!hasLocationPermission()) {
            emit(LocationState.PermissionDenied)
            return@flow
        }

        if (!isAnyProviderEnabled()) {
            emit(LocationState.ProviderDisabled)
            return@flow
        }

        emit(LocationState.Searching)

        val location = withTimeoutOrNull(timeoutMillis) {
            suspendCancellableCoroutine<Location?> { cont ->
                val provider = getBestProvider() ?: LocationManager.PASSIVE_PROVIDER
                val listener = object : LocationListener {
                    override fun onLocationChanged(loc: Location) {
                        locationManager.removeUpdates(this)
                        if (cont.isActive) {
                            cont.resume(loc)
                        }
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                    override fun onProviderEnabled(provider: String) {}
                    override fun onProviderDisabled(provider: String) {}
                }

                try {
                    @SuppressLint("MissingPermission")
                    locationManager.requestLocationUpdates(
                        provider,
                        500L,
                        0f,
                        listener,
                        Looper.getMainLooper()
                    )
                } catch (e: SecurityException) {
                    if (cont.isActive) cont.resume(null)
                } catch (e: Exception) {
                    if (cont.isActive) cont.resume(null)
                }

                cont.invokeOnCancellation {
                    try {
                        locationManager.removeUpdates(listener)
                    } catch (_: Exception) {}
                }
            }
        }

        val resolvedLocation = location ?: getLastKnownLocationFallback()

        if (resolvedLocation != null) {
            val nearby = HaversineDistanceCalculator.findNearbyPlaces(
                places = places,
                latitude = resolvedLocation.latitude,
                longitude = resolvedLocation.longitude,
                radiusMeters = radiusMeters
            )
            emit(
                LocationState.Success(
                    latitude = resolvedLocation.latitude,
                    longitude = resolvedLocation.longitude,
                    accuracy = resolvedLocation.accuracy,
                    nearbyPlaces = nearby
                )
            )
        } else {
            emit(LocationState.TimedOut)
        }
    }

    /**
     * Fallback to retrieve the best available last known location across providers.
     */
    @SuppressLint("MissingPermission")
    fun getLastKnownLocationFallback(): Location? {
        if (!hasLocationPermission()) return null
        val providers = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        )
        for (provider in providers) {
            try {
                val loc = locationManager.getLastKnownLocation(provider)
                if (loc != null) return loc
            } catch (_: Exception) {}
        }
        return null
    }
}
