package com.example.focuscontrol.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import java.util.Locale

enum class TravelEligibility {
    INELIGIBLE_TOO_CLOSE, // < 2 km
    LOCAL_TRAVEL,         // >= 2 km && <= 30 km
    REGULAR_TRAVEL        // > 30 km
}

object LocationVerificationHelper {
    private const val TAG = "LocationVerification"

    const val HOME_ARRIVAL_RADIUS_METERS = 200.0 // 200m arrival threshold
    const val MIN_TRAVEL_DISTANCE_METERS = 2000.0 // 2 km
    const val LOCAL_TRAVEL_MAX_DISTANCE_METERS = 30000.0 // 30 km

    // Part 5: VPN Detection
    fun isVpnActive(context: Context): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val network = cm.activeNetwork ?: return false
                val caps = cm.getNetworkCapabilities(network) ?: return false
                caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) || !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
            } else {
                @Suppress("DEPRECATION")
                val info = cm.activeNetworkInfo
                info != null && info.type == ConnectivityManager.TYPE_VPN
            }
        } catch (e: Exception) {
            Log.e(TAG, "VPN check failed", e)
            false
        }
    }

    // Part 5: Mock Location Detection
    fun isLocationMocked(location: Location): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            location.isMock
        } else {
            @Suppress("DEPRECATION")
            location.isFromMockProvider
        }
    }

    fun calculateDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0].toDouble()
    }

    fun evaluateTravelEligibility(distanceMeters: Double): TravelEligibility {
        return when {
            distanceMeters < MIN_TRAVEL_DISTANCE_METERS -> TravelEligibility.INELIGIBLE_TOO_CLOSE
            distanceMeters <= LOCAL_TRAVEL_MAX_DISTANCE_METERS -> TravelEligibility.LOCAL_TRAVEL
            else -> TravelEligibility.REGULAR_TRAVEL
        }
    }

    fun getCurrentLocationFix(context: Context, onResult: (Location?, String) -> Unit) {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            onResult(null, "Location Permission Denied")
            return
        }

        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) && 
            !locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            onResult(null, "Location Services Disabled in System Settings")
            return
        }

        // Fast path: get last known location if reasonably recent (< 15 minutes) and acceptable accuracy (<= 200m)
        var bestLocation: Location? = null
        val providers = locationManager.getProviders(true)
        for (provider in providers) {
            val loc = locationManager.getLastKnownLocation(provider)
            if (loc != null) {
                if (bestLocation == null || loc.time > bestLocation.time) {
                    bestLocation = loc
                }
            }
        }

        val now = System.currentTimeMillis()
        if (bestLocation != null &&
            (now - bestLocation.time) < 15 * 60 * 1000L &&
            (!bestLocation.hasAccuracy() || bestLocation.accuracy <= 200f)
        ) {
            Log.d(TAG, "Using fresh cached location fix: ${bestLocation.latitude}, ${bestLocation.longitude} (accuracy ${bestLocation.accuracy}m)")
            onResult(bestLocation, "Success (Cached)")
            return
        }

        // Robust multi-provider request: listen on BOTH GPS_PROVIDER and NETWORK_PROVIDER concurrently!
        try {
            val handler = Handler(Looper.getMainLooper())
            var locationReceived = false

            val activeListeners = mutableListOf<Pair<String, LocationListener>>()

            fun finishWithLocation(loc: Location, status: String) {
                if (!locationReceived) {
                    locationReceived = true
                    for ((p, l) in activeListeners) {
                        try {
                            locationManager.removeUpdates(l)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error removing listener for $p", e)
                        }
                    }
                    onResult(loc, status)
                }
            }

            val availableProviders = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
                .filter { locationManager.isProviderEnabled(it) }

            if (availableProviders.isEmpty()) {
                if (bestLocation != null) {
                    onResult(bestLocation, "Success (Cached Fallback)")
                } else {
                    onResult(null, "Location Services Disabled in System Settings")
                }
                return
            }

            for (p in availableProviders) {
                val listener = LocationListener { loc ->
                    if (!locationReceived) {
                        Log.d(TAG, "Location update received from provider '$p': lat=${loc.latitude}, lng=${loc.longitude}, acc=${loc.accuracy}")
                        finishWithLocation(loc, "Success")
                    }
                }
                activeListeners.add(Pair(p, listener))
                try {
                    locationManager.requestLocationUpdates(p, 500L, 0f, listener, Looper.getMainLooper())
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to request location updates on provider $p", e)
                }
            }

            // Timeout after 20 seconds
            handler.postDelayed({
                if (!locationReceived) {
                    locationReceived = true
                    for ((p, l) in activeListeners) {
                        try {
                            locationManager.removeUpdates(l)
                        } catch (_: Exception) {}
                    }
                    if (bestLocation != null) {
                        Log.d(TAG, "Location request timed out. Using best cached location as fallback.")
                        onResult(bestLocation, "Success (Cached Fallback)")
                    } else {
                        Log.w(TAG, "Location request timed out and no cached location was available.")
                        onResult(null, "Location Request Timed Out or Signal Insufficiently Accurate")
                    }
                }
            }, 20000L)

        } catch (e: Exception) {
            Log.e(TAG, "Error acquiring location", e)
            if (bestLocation != null) {
                onResult(bestLocation, "Success (Cached Fallback)")
            } else {
                onResult(null, "Error acquiring location: ${e.message}")
            }
        }
    }

    fun getGeocodedAddressName(context: Context, lat: Double, lng: Double): String {
        try {
            if (Geocoder.isPresent()) {
                val geocoder = Geocoder(context, Locale.getDefault())
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(lat, lng, 1)
                if (!addresses.isNullOrEmpty()) {
                    val address = addresses[0]
                    val locality = address.locality ?: address.subAdminArea ?: address.adminArea
                    val country = address.countryName
                    if (!locality.isNullOrBlank()) {
                        return if (!country.isNullOrBlank()) "$locality, $country" else locality
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Geocoder lookup failed", e)
        }
        return String.format(Locale.getDefault(), "%.4f, %.4f", lat, lng)
    }

    fun checkIsOutsideHome(
        context: Context,
        homeLat: Double,
        homeLng: Double,
        homeRadiusKm: Double,
        onResult: (isOutside: Boolean, locationDescription: String, location: Location?) -> Unit
    ) {
        if (isVpnActive(context)) {
            onResult(false, "VPN detected. Turn off your VPN and verify your current device location to continue.", null)
            return
        }

        getCurrentLocationFix(context) { location, statusMessage ->
            if (location == null) {
                onResult(false, statusMessage, null)
                return@getCurrentLocationFix
            }

            if (isLocationMocked(location)) {
                onResult(false, "Mocked location detected. Please disable location spoofing apps to proceed.", null)
                return@getCurrentLocationFix
            }

            val distanceMeters = calculateDistanceMeters(location.latitude, location.longitude, homeLat, homeLng)
            val distanceKm = distanceMeters / 1000.0
            val isOutside = distanceMeters >= MIN_TRAVEL_DISTANCE_METERS
            val placeName = getGeocodedAddressName(context, location.latitude, location.longitude)

            val description = if (isOutside) {
                "$placeName (${String.format(Locale.getDefault(), "%.1f", distanceKm)} km from home)"
            } else {
                "$placeName (${String.format(Locale.getDefault(), "%.1f", distanceKm)} km from home - Less than 2 km)"
            }

            onResult(isOutside, description, location)
        }
    }
}
