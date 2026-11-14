package com.elyeproj.locationdetect

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DistanceCalculatorTest {
    @Test
    fun parsesLatitudeThenLongitude() {
        assertEquals(
            Coordinates(latitude = 37.7749, longitude = -122.4194),
            DistanceCalculator.parseLatitudeLongitude(" 37.7749, -122.4194 ")
        )
    }

    @Test
    fun rejectsInvalidCoordinateInput() {
        assertNull(DistanceCalculator.parseLatitudeLongitude("not coordinates"))
        assertNull(DistanceCalculator.parseLatitudeLongitude("91, 0"))
        assertNull(DistanceCalculator.parseLatitudeLongitude("0, 181"))
        assertNull(DistanceCalculator.parseLatitudeLongitude("NaN, 0"))
    }

    @Test
    fun parsesTheAustralianCoordinateOrder() {
        assertEquals(
            Coordinates(latitude = -37.843128, longitude = 145.180627),
            DistanceCalculator.parseLatitudeLongitude("-37.843128, 145.180627")
        )
    }

    @Test
    fun identicalCoordinatesHaveZeroDistance() {
        val location = Coordinates(latitude = 51.5072, longitude = -0.1276)

        assertEquals(0.0, DistanceCalculator.distanceKilometers(location, location), 0.001)
    }

    @Test
    fun calculatesRoughDistanceAlongTheEquator() {
        val start = Coordinates(latitude = 0.0, longitude = 0.0)
        val destination = Coordinates(latitude = 0.0, longitude = 1.0)

        assertEquals(111.2, DistanceCalculator.distanceKilometers(start, destination), 0.2)
    }
}
