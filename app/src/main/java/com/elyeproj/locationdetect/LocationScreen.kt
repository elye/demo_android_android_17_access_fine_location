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
import androidx.core.locationbutton.compose.LocationButton
import androidx.core.locationbutton.compose.LocationButtonTextType
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
        cancelLocationRequest = locationProvider.requestCurrentLocation { result ->
            isLoading = false
            cancelLocationRequest = null
            result.fold(
                onSuccess = { location ->
                    currentLocation = location
                    statusMessage = when {
                        access == LocationAccess.FINE && !finePermissionGranted ->
                            "Approximate access only; precise location was not granted."
                        access == LocationAccess.COARSE && finePermissionGranted ->
                            "Fine permission is already granted; Android may return a precise location."
                        finePermissionGranted -> "Fine location access granted."
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
            fetchLocation(access, finePermissionGranted = fineGranted)
        }
    }

    fun requestCoarseLocation() {
        val coarseGranted = hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
        val fineGranted = hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        if (coarseGranted) {
            fetchLocation(LocationAccess.COARSE, finePermissionGranted = fineGranted)
            return
        }

        pendingAccess = LocationAccess.COARSE
        permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION))
    }

    DisposableEffect(locationProvider) {
        onDispose { cancelLocationRequest?.invoke() }
    }

    val destination = remember(destinationInput) {
        DistanceCalculator.parseLatitudeLongitude(destinationInput)
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
        Text("Use approximate access or the location button for precise, session-based access.")

        OutlinedTextField(
            value = destinationInput,
            onValueChange = { destinationInput = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Destination (latitude, longitude)") },
            placeholder = { Text("37.7749, -122.4194") },
            supportingText = { Text("Enter latitude first, then longitude.") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii)
        )

        Button(
            onClick = { requestCoarseLocation() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            Text("ACCESS_COARSE_LOCATION")
        }
        LocationButton(
            onPermissionResult = { granted ->
                if (granted && hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    fetchLocation(LocationAccess.FINE, finePermissionGranted = true)
                } else {
                    currentLocation = null
                    statusMessage = "Precise location access was not granted."
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            textType = LocationButtonTextType.UsePreciseLocation
        )

        Text(statusMessage, style = MaterialTheme.typography.bodyMedium)

        currentLocation?.let { location ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Location result", style = MaterialTheme.typography.titleMedium)
                    Text("Latitude: ${location.latitude.formatCoordinate()}")
                    Text("Longitude: ${location.longitude.formatCoordinate()}")
                    if (location.accuracy > 0f) {
                        Text("Reported accuracy: ± ${location.accuracy.formatAccuracy()} m")
                    }
                    when {
                        destinationInput.isBlank() ->
                            Text("Enter destination coordinates to calculate distance.")
                        destination == null ->
                            Text(
                                "Use valid latitude, longitude values, e.g. 37.7749, -122.4194.",
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
