package com.elyeproj.locationdetect

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import java.util.Locale

private enum class LocationAccess {
    COARSE,
    FINE
}

@Composable
fun LocationScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val locationProvider = remember(context) { LocationProvider(context) }
    var destinationInput by rememberSaveable { mutableStateOf("") }
    var currentLocation by remember { mutableStateOf<Location?>(null) }
    var statusMessage by rememberSaveable { mutableStateOf("Choose a permission to request a device location.") }
    var pendingAccess by remember { mutableStateOf<LocationAccess?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var cancelLocationRequest by remember { mutableStateOf<(() -> Unit)?>(null) }

    fun hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    fun fetchLocation(access: LocationAccess, finePermissionGranted: Boolean) {
        cancelLocationRequest?.invoke()
        currentLocation = null
        isLoading = true
        statusMessage = "Getting device location…"
        cancelLocationRequest = locationProvider.requestCurrentLocation(
            useFineProvider = access == LocationAccess.FINE && finePermissionGranted
        ) { result ->
            isLoading = false
            cancelLocationRequest = null
            result.fold(
                onSuccess = { location ->
                    currentLocation = location
                    statusMessage = when {
                        access == LocationAccess.FINE && !finePermissionGranted ->
                            "Approximate access only; precise location was not granted."
                        access == LocationAccess.FINE -> "Fine location access granted."
                        else -> "Coarse location access granted."
                    }
                },
                onFailure = { exception ->
                    statusMessage = exception.message ?: "Unable to get a location."
                }
            )
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        val access = pendingAccess
        pendingAccess = null
        if (access == null) return@rememberLauncherForActivityResult

        val coarseGranted = hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
        val fineGranted = hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        if (!coarseGranted) {
            isLoading = false
            statusMessage = "Location permission was not granted."
        } else {
            fetchLocation(access, finePermissionGranted = access == LocationAccess.FINE && fineGranted)
        }
    }

    fun requestAccess(access: LocationAccess) {
        val coarseGranted = hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
        val fineGranted = hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        if (coarseGranted && (access == LocationAccess.COARSE || fineGranted)) {
            fetchLocation(access, finePermissionGranted = access == LocationAccess.FINE && fineGranted)
            return
        }

        pendingAccess = access
        val permissions = if (access == LocationAccess.FINE) {
            arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        } else {
            arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        permissionLauncher.launch(permissions)
    }

    DisposableEffect(locationProvider) {
        onDispose { cancelLocationRequest?.invoke() }
    }

    val destination = remember(destinationInput) {
        DistanceCalculator.parseLongitudeLatitude(destinationInput)
    }
    val distanceKilometers = if (currentLocation != null && destination != null) {
        DistanceCalculator.distanceKilometers(
            from = Coordinates(currentLocation!!.latitude, currentLocation!!.longitude),
            to = destination
        )
    } else {
        null
    }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Location access demo", style = MaterialTheme.typography.headlineMedium)
        Text("Request a one-time device location and compare it with a destination.")

        OutlinedTextField(
            value = destinationInput,
            onValueChange = { destinationInput = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Destination (longitude, latitude)") },
            placeholder = { Text("-122.4194, 37.7749") },
            supportingText = { Text("Enter longitude first, then latitude.") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii)
        )

        Button(
            onClick = { requestAccess(LocationAccess.COARSE) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            Text("ACCESS_COARSE_LOCATION")
        }
        Button(
            onClick = { requestAccess(LocationAccess.FINE) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            Text("ACCESS_FINE_LOCATION")
        }

        Text(statusMessage, style = MaterialTheme.typography.bodyMedium)

        currentLocation?.let { location ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Location result", style = MaterialTheme.typography.titleMedium)
                    Text("Longitude: ${location.longitude.formatCoordinate()}")
                    Text("Latitude: ${location.latitude.formatCoordinate()}")
                    if (location.accuracy > 0f) {
                        Text("Reported accuracy: ± ${location.accuracy.formatAccuracy()} m")
                    }
                    when {
                        destinationInput.isBlank() ->
                            Text("Enter destination coordinates to calculate distance.")
                        destination == null ->
                            Text(
                                "Use valid longitude, latitude values, e.g. -122.4194, 37.7749.",
                                color = MaterialTheme.colorScheme.error
                            )
                        distanceKilometers != null ->
                            Text("Approximate distance: ${distanceKilometers.formatDistance()} km")
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

private fun Double.formatCoordinate(): String = String.format(Locale.US, "%.6f", this)

private fun Float.formatAccuracy(): String = String.format(Locale.US, "%.0f", this)

private fun Double.formatDistance(): String = String.format(Locale.US, "%.2f", this)
