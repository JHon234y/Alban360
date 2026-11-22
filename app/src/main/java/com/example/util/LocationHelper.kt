package com.example.util

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

data class GpsCoordinates(val latitude: Double, val longitude: Double) {
    val isValid: Boolean
        get() = latitude in -90.0..90.0 &&
            longitude in -180.0..180.0 &&
            !(latitude == 0.0 && longitude == 0.0)
}

@SuppressLint("MissingPermission")
suspend fun fetchCurrentLocation(context: Context): GpsCoordinates? {
    val fused = withTimeoutOrNull(7500L) {
        fetchFusedLocation(context)
    }
    if (fused != null && fused.isValid) return fused

    return fetchBestLastKnownManagerLocation(context)?.takeIf { it.isValid }
}

@SuppressLint("MissingPermission")
private suspend fun fetchFusedLocation(context: Context): GpsCoordinates? =
    suspendCancellableCoroutine { continuation ->
        val client = LocationServices.getFusedLocationProviderClient(context)
        val cts = CancellationTokenSource()
        continuation.invokeOnCancellation { cts.cancel() }
        client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
            .addOnSuccessListener { location ->
                val direct = location?.toGpsOrNull()
                if (direct != null) {
                    if (continuation.isActive) continuation.resume(direct)
                } else {
                    client.lastLocation
                        .addOnSuccessListener { last ->
                            if (continuation.isActive) {
                                continuation.resume(last?.toGpsOrNull())
                            }
                        }
                        .addOnFailureListener {
                            if (continuation.isActive) continuation.resume(null)
                        }
                }
            }
            .addOnFailureListener {
                client.lastLocation
                    .addOnSuccessListener { last ->
                        if (continuation.isActive) {
                            continuation.resume(last?.toGpsOrNull())
                        }
                    }
                    .addOnFailureListener {
                        if (continuation.isActive) continuation.resume(null)
                    }
            }
    }

@SuppressLint("MissingPermission")
private fun fetchBestLastKnownManagerLocation(context: Context): GpsCoordinates? {
    val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
    val providers = listOf(
        LocationManager.GPS_PROVIDER,
        LocationManager.NETWORK_PROVIDER,
        LocationManager.PASSIVE_PROVIDER,
    )
    var best: Location? = null
    for (provider in providers) {
        val loc = runCatching { manager.getLastKnownLocation(provider) }.getOrNull() ?: continue
        val coords = loc.toGpsOrNull() ?: continue
        if (best == null || loc.time > best.time) {
            best = loc
        } else if (coords.isValid && loc.accuracy < best.accuracy) {
            best = loc
        }
    }
    return best?.toGpsOrNull()
}

private fun Location.toGpsOrNull(): GpsCoordinates? {
    val coords = GpsCoordinates(latitude, longitude)
    return coords.takeIf { it.isValid }
}
