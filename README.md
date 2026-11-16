# LocationDetect

LocationDetect is a small Android example app for requesting a device location in response to a user action. It demonstrates coarse and precise location requests, displays the resulting coordinates and reported accuracy, and can calculate the distance to coordinates entered by the user.

This project is intended as an example implementation informed by Android's [Location Button guide](https://developer.android.com/guide/topics/permissions/private-alternatives/location-button), in response to Google Play's upcoming Android 17 location-permission requirements.

## Android 17 and Google Play policy

Google Play's [preview policy for location permissions](https://support.google.com/googleplay/android-developer/answer/16909972#location-permissions) says that, effective **January 27, 2027**, apps targeting Android 17 (API level 37) or higher must use the Android location button when precise location is needed only for a one-time, user-initiated action. The policy also says apps should request the minimum location scope needed, and may request `ACCESS_FINE_LOCATION` only when neither the location button nor `ACCESS_COARSE_LOCATION` can support the feature.

The Android location button provides session-scoped precise location access after the user taps it. Consult the linked Android guide for its implementation details, including the required manifest declarations and Jetpack library.

> **Implementation note:** The current app code demonstrates the standard coarse/fine runtime permission flow. Its manifest does not yet declare the location-button permissions/flag, and the app currently targets API 36. Treat this repository as a learning example, not as a completed Android 17 location-button integration or a guarantee of Google Play policy compliance.

## What the app demonstrates

- User-initiated requests for coarse or precise location.
- A one-time current-location result, including reported accuracy when available.
- Graceful messaging when permission is denied or only approximate access is granted.
- Distance calculation from the current location to a latitude/longitude entered in the app.

## Code structure

The app is a single-module Android project. Its main location flow is split between the Compose screen, a location-service wrapper, and coordinate utilities:

- `app/src/main/java/com/elyeproj/locationdetect/MainActivity.kt` sets up the activity, applies `LocationDetectTheme`, and hosts `LocationScreen` inside a Material 3 `Scaffold`.
- `app/src/main/java/com/elyeproj/locationdetect/LocationScreen.kt` contains the UI and interaction flow. It offers coarse and fine access buttons, requests the corresponding runtime permissions, reports the result or denial, and displays the current coordinates and accuracy. It also accepts destination coordinates and shows their distance from the current location. An in-progress location request is cancelled when the screen is disposed.
- `app/src/main/java/com/elyeproj/locationdetect/LocationProvider.kt` wraps Google Play services' fused location provider. It requests a fresh current location at permission-appropriate granularity, applies a 30-second timeout, reports success or failure through a `Result<Location>` callback, and returns a cancellation function.
- `app/src/main/java/com/elyeproj/locationdetect/DistanceCalculator.kt` parses and validates `latitude, longitude` input, then calculates the distance using the Haversine formula. It is independent of Android UI and location APIs.
- `app/src/main/AndroidManifest.xml` currently declares `ACCESS_COARSE_LOCATION` and `ACCESS_FINE_LOCATION`. These are the standard permissions used by the current demo; the Android location-button declarations described in the guide have not yet been added.
- `app/src/test/java/com/elyeproj/locationdetect/DistanceCalculatorTest.kt` contains unit tests for coordinate parsing and distance calculations.

In short, `LocationScreen` owns permission and presentation state, `LocationProvider` obtains the device location, and `DistanceCalculator` handles user-entered coordinates and distance math. This separation makes it easier to replace the current permission-request UI with the Android location button while keeping location retrieval and distance calculations distinct.

## Build and run

Open the project in Android Studio, let Gradle sync, then run the `app` configuration on an Android device or emulator. Alternatively, build the debug APK from the project root with:

```sh
./gradlew :app:assembleDebug
```

The project requires the Android SDK configured for the Gradle build. The current app configuration uses `minSdk 24`, `compileSdk 36.1`, and `targetSdk 36`.

## References

- [Android Location Button: private alternatives to permissions](https://developer.android.com/guide/topics/permissions/private-alternatives/location-button)
- [Google Play preview: Permissions and APIs that Access Sensitive Information — Location Permissions](https://support.google.com/googleplay/android-developer/answer/16909972#location-permissions)
