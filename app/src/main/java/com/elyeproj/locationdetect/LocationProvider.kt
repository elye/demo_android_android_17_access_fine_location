package com.elyeproj.locationdetect

import android.content.Context
import android.location.Location
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.Granularity
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import java.util.concurrent.CancellationException

class LocationProvider(context: Context) {
    private val fusedLocationClient =
        LocationServices.getFusedLocationProviderClient(context.applicationContext)

    fun requestCurrentLocation(
        useFineProvider: Boolean,
        onResult: (Result<Location>) -> Unit
    ): () -> Unit {
        val cancellationTokenSource = CancellationTokenSource()
        var isFinished = false

        fun finish(result: Result<Location>) {
            if (isFinished) return
            isFinished = true
            onResult(result)
        }

        val request = CurrentLocationRequest.Builder()
            .setPriority(
                if (useFineProvider) Priority.PRIORITY_HIGH_ACCURACY
                else Priority.PRIORITY_BALANCED_POWER_ACCURACY
            )
            .setGranularity(Granularity.GRANULARITY_PERMISSION_LEVEL)
            .setMaxUpdateAgeMillis(0L)
            .setDurationMillis(LOCATION_TIMEOUT_MILLIS)
            .build()

        try {
            fusedLocationClient.getCurrentLocation(request, cancellationTokenSource.token)
                .addOnSuccessListener { location ->
                    if (location == null) {
                        finish(Result.failure(IllegalStateException("No fresh location was available within 30 seconds.")))
                    } else {
                        finish(Result.success(location))
                    }
                }
                .addOnFailureListener { exception ->
                    finish(Result.failure(exception))
                }
                .addOnCanceledListener {
                    finish(Result.failure(CancellationException("Location request was cancelled.")))
                }
        } catch (exception: SecurityException) {
            finish(Result.failure(exception))
        }

        return {
            if (!isFinished) {
                isFinished = true
                cancellationTokenSource.cancel()
            }
        }
    }

    private companion object {
        const val LOCATION_TIMEOUT_MILLIS = 30_000L
    }
}
