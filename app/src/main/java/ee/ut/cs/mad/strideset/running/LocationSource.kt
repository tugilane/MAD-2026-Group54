package ee.ut.cs.mad.strideset.running

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper

interface LocationSource {
    fun start(onFix: (LocationFix) -> Unit, onUnavailable: () -> Unit)
    fun stop()
}

class GpsLocationSource(context: Context) : LocationSource {
    private val manager = context.getSystemService(LocationManager::class.java)
    private var listener: LocationListener? = null

    @SuppressLint("MissingPermission") // The visible activity and service both check precise permission.
    override fun start(onFix: (LocationFix) -> Unit, onUnavailable: () -> Unit) {
        stop()
        check(manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) { "GPS is disabled" }
        val updates = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                if (!location.hasAccuracy()) return
                onFix(LocationFix(location.latitude, location.longitude, location.accuracy.toDouble(),
                    location.elapsedRealtimeNanos / 1_000_000))
            }
            override fun onProviderDisabled(provider: String) = onUnavailable()
            override fun onProviderEnabled(provider: String) = Unit
            @Deprecated("Required by older Android versions")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
        }
        listener = updates
        try {
            manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0f, updates, Looper.getMainLooper())
        } catch (error: RuntimeException) {
            stop()
            throw error
        }
    }

    override fun stop() {
        listener?.let { manager.removeUpdates(it) }
        listener = null
    }
}
