package com.example.hello.platform.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Looper
import androidx.core.content.ContextCompat
import com.example.hello.domain.model.Place
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import java.util.concurrent.Executor
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
        val nearbyPlaces: List<PlaceDistance>,
        val resolvedLabel: String? = null
    ) : LocationState
    data object TimedOut : LocationState
    data object PermissionDenied : LocationState
    data object ProviderDisabled : LocationState
}

/**
 * Native platform LocationService utilizing Android's LocationManager.
 * Strictly adheres to Prime Directive 3 (foreground-only GPS capture, zero background location).
 */
open class LocationService(
    private val context: Context?,
    private val locationManager: LocationManager? = try {
        context?.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    } catch (_: Throwable) {
        null
    }
) {

    /**
     * Checks if foreground location permissions are granted.
     */
    fun hasLocationPermission(): Boolean {
        val ctx = context ?: return false
        val fine = ContextCompat.checkSelfPermission(
            ctx,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            ctx,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    /**
     * Checks if either GPS or Network location providers are enabled.
     */
    fun isAnyProviderEnabled(): Boolean {
        val lm = locationManager ?: return false
        val gps = try {
            lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
        } catch (_: Exception) {
            false
        }
        val network = try {
            lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        } catch (_: Exception) {
            false
        }
        return gps || network
    }

    /**
     * Returns the best available provider (GPS preferred, falling back to Network).
     */
    fun getBestProvider(): String? {
        val lm = locationManager ?: return null
        return when {
            try { lm.isProviderEnabled(LocationManager.GPS_PROVIDER) } catch (_: Exception) { false } -> LocationManager.GPS_PROVIDER
            try { lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) } catch (_: Exception) { false } -> LocationManager.NETWORK_PROVIDER
            else -> null
        }
    }

    /**
     * Non-blocking coroutine flow acquiring a foreground GPS fix within [timeoutMillis].
     * Evaluates offline Place proximity using [HaversineDistanceCalculator] within a 500m radius.
     */
    open fun getLocationFlow(
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
            val provider = getBestProvider() ?: LocationManager.PASSIVE_PROVIDER
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                suspendCancellableCoroutine<Location?> { cont ->
                    val cancellationSignal = CancellationSignal()
                    val executor = context?.let { ContextCompat.getMainExecutor(it) }
                        ?: Executor { command -> command.run() }
                    try {
                        @SuppressLint("MissingPermission")
                        locationManager?.getCurrentLocation(
                            provider,
                            cancellationSignal,
                            executor
                        ) { loc ->
                            if (cont.isActive) {
                                cont.resume(loc)
                            }
                        } ?: run {
                            if (cont.isActive) cont.resume(null)
                        }
                    } catch (_: SecurityException) {
                        if (cont.isActive) cont.resume(null)
                    } catch (_: Exception) {
                        if (cont.isActive) cont.resume(null)
                    }

                    cont.invokeOnCancellation {
                        cancellationSignal.cancel()
                    }
                }
            } else {
                suspendCancellableCoroutine<Location?> { cont ->
                    val listener = object : LocationListener {
                        override fun onLocationChanged(loc: Location) {
                            locationManager?.removeUpdates(this)
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
                        locationManager?.requestLocationUpdates(
                            provider,
                            500L,
                            0f,
                            listener,
                            Looper.getMainLooper()
                        ) ?: run {
                            if (cont.isActive) cont.resume(null)
                        }
                    } catch (_: SecurityException) {
                        if (cont.isActive) cont.resume(null)
                    } catch (_: Exception) {
                        if (cont.isActive) cont.resume(null)
                    }

                    cont.invokeOnCancellation {
                        try {
                            locationManager?.removeUpdates(listener)
                        } catch (_: Exception) {}
                    }
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
            val resolvedLabel = if (nearby.isEmpty()) {
                reverseGeocodeLabel(resolvedLocation.latitude, resolvedLocation.longitude)
                    ?: formatCoordinates(resolvedLocation.latitude, resolvedLocation.longitude)
            } else {
                nearby.first().place.label
            }
            emit(
                LocationState.Success(
                    latitude = resolvedLocation.latitude,
                    longitude = resolvedLocation.longitude,
                    accuracy = resolvedLocation.accuracy,
                    nearbyPlaces = nearby,
                    resolvedLabel = resolvedLabel
                )
            )
        } else {
            emit(LocationState.TimedOut)
        }
    }

    /**
     * Resolves a concise human-readable label via Android's Geocoder when available,
     * returning null offline or on error so callers can fall back to formatted coordinates.
     */
    private suspend fun reverseGeocodeLabel(latitude: Double, longitude: Double): String? {
        val ctx = context ?: return null
        if (!Geocoder.isPresent()) return null
        return withTimeoutOrNull(2500L) {
            withContext(Dispatchers.IO) {
                try {
                    val geocoder = Geocoder(ctx, Locale.getDefault())
                    val address: Address? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        suspendCancellableCoroutine { cont ->
                            geocoder.getFromLocation(latitude, longitude, 1, object : Geocoder.GeocodeListener {
                                override fun onGeocode(addresses: MutableList<Address>) {
                                    if (cont.isActive) cont.resume(addresses.firstOrNull())
                                }

                                override fun onError(errorMessage: String?) {
                                    if (cont.isActive) cont.resume(null)
                                }
                            })
                        }
                    } else {
                        @Suppress("DEPRECATION")
                        geocoder.getFromLocation(latitude, longitude, 1)?.firstOrNull()
                    }
                    formatAddressLabel(address)
                } catch (_: Exception) {
                    null
                }
            }
        }
    }

    companion object {
        fun formatCoordinates(latitude: Double, longitude: Double): String =
            String.format(Locale.US, "%.4f, %.4f", latitude, longitude)

        fun formatAddressLabel(address: Address?): String? {
            if (address == null) return null
            val parts = listOfNotNull(
                address.subLocality?.takeIf { it.isNotBlank() }
                    ?: address.thoroughfare?.takeIf { it.isNotBlank() }
                    ?: address.featureName?.takeIf { it.isNotBlank() && !it.all { ch -> ch.isDigit() } },
                address.locality?.takeIf { it.isNotBlank() }
            ).distinct()
            return parts.take(2).joinToString(", ").ifBlank { null }
        }
    }

    /**
     * Fallback to retrieve the best available last known location across providers.
     */
    @SuppressLint("MissingPermission")
    fun getLastKnownLocationFallback(): Location? {
        val lm = locationManager ?: return null
        if (!hasLocationPermission()) return null
        val providers = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        )
        for (provider in providers) {
            try {
                val loc = lm.getLastKnownLocation(provider)
                if (loc != null) return loc
            } catch (_: Exception) {}
        }
        return null
    }
}
