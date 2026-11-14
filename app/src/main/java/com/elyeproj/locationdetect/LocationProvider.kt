package com.elyeproj.locationdetect

import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executor

class LocationProvider(context: Context) {
    private val manager = context.applicationContext
        .getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val handler = Handler(Looper.getMainLooper())

    fun requestCurrentLocation(
        useFineProvider: Boolean,
        onResult: (Result<Location>) -> Unit
    ): () -> Unit {
        var isFinished = false
        var listener: LocationListener? = null
        var cancellationSignal: CancellationSignal? = null
        lateinit var timeout: Runnable

        fun finish(result: Result<Location>) {
            if (isFinished) return
            isFinished = true
            handler.removeCallbacks(timeout)
            listener?.let(manager::removeUpdates)
            onResult(result)
        }

        timeout = Runnable {
            finish(Result.failure(IllegalStateException("Timed out waiting for a location.")))
        }

        val enabledProviders = try {
            manager.getProviders(true)
        } catch (exception: SecurityException) {
            finish(Result.failure(exception))
            return { }
        }
        val preferredProviders = if (useFineProvider) {
            listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        } else {
            listOf(LocationManager.NETWORK_PROVIDER)
        }
        val provider = preferredProviders.firstOrNull(enabledProviders::contains)
        if (provider == null) {
            finish(Result.failure(IllegalStateException("No location provider is enabled for this access level.")))
            return { }
        }

        handler.postDelayed(timeout, LOCATION_TIMEOUT_MILLIS)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val signal = CancellationSignal()
                cancellationSignal = signal
                manager.getCurrentLocation(
                    provider,
                    signal,
                    Executor { command -> handler.post(command) }
                ) { location ->
                    if (location == null) {
                        finish(Result.failure(IllegalStateException("The device could not determine a location.")))
                    } else {
                        finish(Result.success(location))
                    }
                }
            } else {
                val locationListener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        finish(Result.success(location))
                    }

                    @Deprecated("Deprecated in Android")
                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit

                    override fun onProviderEnabled(provider: String) = Unit

                    override fun onProviderDisabled(provider: String) = Unit
                }
                listener = locationListener
                @Suppress("DEPRECATION")
                manager.requestSingleUpdate(provider, locationListener, Looper.getMainLooper())
            }
        } catch (exception: SecurityException) {
            finish(Result.failure(exception))
        } catch (exception: IllegalArgumentException) {
            finish(Result.failure(exception))
        }

        return {
            if (!isFinished) {
                isFinished = true
                handler.removeCallbacks(timeout)
                listener?.let(manager::removeUpdates)
                cancellationSignal?.cancel()
            }
        }
    }

    private companion object {
        const val LOCATION_TIMEOUT_MILLIS = 30_000L
    }
}
