package com.andrerinas.openheadunit

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import kotlin.math.roundToInt

class GpsSpeedTracker(
    context: Context,
    private val onSpeedChanged: (kmh: Int, hasSignal: Boolean) -> Unit
) : LocationListener {

    private val locationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private var smoothedSpeed = 0f

    @SuppressLint("MissingPermission")
    fun start() {
        try {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    500L,
                    0f,
                    this
                )
            }
        } catch (_: Exception) {
            onSpeedChanged(0, false)
        }
    }

    fun stop() {
        try {
            locationManager.removeUpdates(this)
        } catch (_: Exception) {
        }
    }

    override fun onLocationChanged(location: Location) {
        if (!location.hasSpeed()) {
            onSpeedChanged(0, true)
            return
        }
        val rawKmh = location.speed * 3.6f
        if (rawKmh < 3.0f) {
            smoothedSpeed = 0f
            onSpeedChanged(0, true)
        } else {
            smoothedSpeed = (smoothedSpeed * 0.3f) + (rawKmh * 0.7f)
            onSpeedChanged(smoothedSpeed.roundToInt(), true)
        }
    }

    override fun onProviderDisabled(provider: String) = onSpeedChanged(0, false)
    override fun onProviderEnabled(provider: String) = onSpeedChanged(0, true)
    @Deprecated("Deprecated in Java")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
}
