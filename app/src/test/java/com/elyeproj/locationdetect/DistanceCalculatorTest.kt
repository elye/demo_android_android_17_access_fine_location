package com.elyeproj.locationdetect

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DistanceCalculatorTest {
    @Test
    fun parsesLongitudeThenLatitude() {
        assertEquals(
            Coordinates(latitude = 37.7749, longitude = -122.4194),
            DistanceCalculator.parseLongitudeLatitude(" -122.4194, 37.7749 ")
        )
    }

    @Test
    fun rejectsInvalidCoordinateInput() {
        assertNull(DistanceCalculator.parseLongitudeLatitude("not coordinates"))
        assertNull(DistanceCalculator.parseLongitudeLatitude("181, 0"))
        assertNull(DistanceCalculator.parseLongitudeLatitude("0, -91"))
        assertNull(DistanceCalculator.parseLongitudeLatitude("NaN, 0"))
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
