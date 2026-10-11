package com.example.focuscontrol.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object LocationVerificationHelper {
    private const val TAG = "LocationVerification"

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

        // Fast path: get last known location if fresh (< 10 minutes)
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

        if (bestLocation != null && (System.currentTimeMillis() - bestLocation.time) < 10 * 60 * 1000) {
            onResult(bestLocation, "Success")
            return
        }

        // Request fresh location fix
        try {
            val provider = if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                LocationManager.GPS_PROVIDER
            } else {
                LocationManager.NETWORK_PROVIDER
            }

            val handler = Handler(Looper.getMainLooper())
            var locationReceived = false

            val locationListener = LocationListener { loc ->
                if (!locationReceived) {
                    locationReceived = true
                    onResult(loc, "Success")
                }
            }

            locationManager.requestSingleUpdate(provider, locationListener, Looper.getMainLooper())

            handler.postDelayed({
                if (!locationReceived) {
                    locationReceived = true
                    locationManager.removeUpdates(locationListener)
                    if (bestLocation != null) {
                        onResult(bestLocation, "Success (Cached)")
                    } else {
                        onResult(null, "Location Request Timed Out (Weak Signal)")
                    }
                }
            }, 15000)

        } catch (e: Exception) {
            Log.e(TAG, "Error acquiring location", e)
            onResult(null, "Error acquiring location: ${e.message}")
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
        getCurrentLocationFix(context) { location, statusMessage ->
            if (location == null) {
                onResult(false, statusMessage, null)
                return@getCurrentLocationFix
            }

            val distanceKm = calculateHaversineDistanceKm(location.latitude, location.longitude, homeLat, homeLng)
            val isOutside = distanceKm > homeRadiusKm
            val placeName = getGeocodedAddressName(context, location.latitude, location.longitude)

            val description = if (isOutside) {
                "$placeName (${String.format(Locale.getDefault(), "%.1f", distanceKm)} km from home)"
            } else {
                "$placeName (${String.format(Locale.getDefault(), "%.1f", distanceKm)} km from home - Inside Radius)"
            }

            onResult(isOutside, description, location)
        }
    }

    fun calculateHaversineDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0 // Earth radius in kilometers
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }
}
