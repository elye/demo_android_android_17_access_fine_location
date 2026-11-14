package com.elyeproj.locationdetect

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class Coordinates(val latitude: Double, val longitude: Double)

object DistanceCalculator {
    fun parseLatitudeLongitude(input: String): Coordinates? {
        val values = input.split(',')
        if (values.size != 2) return null

        val latitude = values[0].trim().toDoubleOrNull() ?: return null
        val longitude = values[1].trim().toDoubleOrNull() ?: return null
        if (!longitude.isFinite() || !latitude.isFinite()) return null
        if (longitude !in -180.0..180.0 || latitude !in -90.0..90.0) return null

        return Coordinates(latitude = latitude, longitude = longitude)
    }

    fun distanceKilometers(from: Coordinates, to: Coordinates): Double {
        val earthRadiusKm = 6371.0088
        val fromLatitude = Math.toRadians(from.latitude)
        val toLatitude = Math.toRadians(to.latitude)
        val latitudeDelta = toLatitude - fromLatitude
        val longitudeDelta = Math.toRadians(to.longitude - from.longitude)

        val haversine = sin(latitudeDelta / 2) * sin(latitudeDelta / 2) +
            cos(fromLatitude) * cos(toLatitude) *
            sin(longitudeDelta / 2) * sin(longitudeDelta / 2)
        val boundedHaversine = haversine.coerceIn(0.0, 1.0)
        val centralAngle = 2 * atan2(sqrt(boundedHaversine), sqrt(1 - boundedHaversine))
        return earthRadiusKm * centralAngle
    }
}
