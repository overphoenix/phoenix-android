package tech.nagual.phoenix.tools.gps.helpers

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.SystemClock
import tech.nagual.common.permissions.Permission
import tech.nagual.common.permissions.isAllGranted
import tech.nagual.app.locationManager
import tech.nagual.phoenix.tools.gps.GeneralLocationListener
import tech.nagual.phoenix.tools.gps.Keys
import tech.nagual.phoenix.tools.gps.data.Track
import tech.nagual.phoenix.tools.gps.preferences.GpsPreferences
import java.util.*
import kotlin.math.pow
import kotlin.math.sqrt

object LocationHelper {
    fun getLocationWithAdjustedAltitude(loc: Location): Location {
        if (!loc.hasAltitude()) {
            return loc
        }
        if (GpsPreferences.adjustAltitudeFromGeoIdHeight && loc.extras != null) {
            val geoidheight = loc.extras.getString(GeneralLocationListener.GEOIDHEIGHT)
            if (!geoidheight.isNullOrEmpty()) {
                loc.altitude = loc.altitude - geoidheight!!.toDouble()
            } else {
                //If geoid height not present for adjustment, don't record an elevation at all.
                loc.removeAltitude()
            }
        }
        if (loc.hasAltitude() && GpsPreferences.subtractAltitudeOffset != 0) {
            loc.altitude = loc.altitude - GpsPreferences.subtractAltitudeOffset
        }
        return loc
    }

    fun getLocationAdjustedForGPSWeekRollover(loc: Location): Location {
        var recordedTime = loc.time
        //If the date is before April 6, 23:59:59, there's a GPS week rollover problem
        if (recordedTime < 1554595199000L) {
            recordedTime += 619315200000L //add 1024 weeks
            loc.time = recordedTime
        }
        return loc
    }

    fun getDefaultLocation(): Location {
        val defaultLocation = Location(LocationManager.NETWORK_PROVIDER)
        defaultLocation.latitude = Keys.DEFAULT_LATITUDE
        defaultLocation.longitude = Keys.DEFAULT_LONGITUDE
        defaultLocation.accuracy = Keys.DEFAULT_ACCURACY
        defaultLocation.altitude = Keys.DEFAULT_ALTITUDE
        defaultLocation.time = Keys.DEFAULT_DATE.time
        return defaultLocation
    }

    /* Checks if a location is older than one minute */
    fun isOldLocation(location: Location): Boolean =
        GregorianCalendar.getInstance().time.time - location.time > Keys.SIGNIFICANT_TIME_DIFFERENCE

    /* Tries to return the last location that the system has stored */
    @SuppressLint("MissingPermission")
    fun getLastKnownLocation(context: Context): Location {
        var lastKnownLocation: Location = PreferencesHelper.loadCurrentBestLocation()

        if (context.isAllGranted(Permission.ACCESS_FINE_LOCATION)) {
            val lastKnownLocationGps: Location =
                locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    ?: lastKnownLocation
            val lastKnownLocationNetwork: Location =
                locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                    ?: lastKnownLocation
            lastKnownLocation =
                when (isBetterLocation(lastKnownLocationGps, lastKnownLocationNetwork)) {
                    true -> lastKnownLocationGps
                    false -> lastKnownLocationNetwork
                }
        }
        return lastKnownLocation
    }


    /* Determines whether one location reading is better than the current location fix */
    fun isBetterLocation(location: Location, currentBestLocation: Location?): Boolean {
        // Credit: https://developer.android.com/guide/topics/location/strategies.html#BestEstimate

        if (currentBestLocation == null) {
            // a new location is always better than no location
            return true
        }

        // check whether the new location fix is newer or older
        val timeDelta: Long = location.time - currentBestLocation.time
        val isSignificantlyNewer: Boolean = timeDelta > Keys.SIGNIFICANT_TIME_DIFFERENCE
        val isSignificantlyOlder: Boolean = timeDelta < -Keys.SIGNIFICANT_TIME_DIFFERENCE

        when {
            // if it's been more than two minutes since the current location, use the new location because the user has likely moved
            isSignificantlyNewer -> return true
            // if the new location is more than two minutes older, it must be worse
            isSignificantlyOlder -> return false
        }

        // check whether the new location fix is more or less accurate
        val isNewer: Boolean = timeDelta > 0L
        val accuracyDelta: Float = location.accuracy - currentBestLocation.accuracy
        val isLessAccurate: Boolean = accuracyDelta > 0f
        val isMoreAccurate: Boolean = accuracyDelta < 0f
        val isSignificantlyLessAccurate: Boolean = accuracyDelta > 200f

        // check if the old and new location are from the same provider
        val isFromSameProvider: Boolean = location.provider == currentBestLocation.provider

        // determine location quality using a combination of timeliness and accuracy
        return when {
            isMoreAccurate -> true
            isNewer && !isLessAccurate -> true
            isNewer && !isSignificantlyLessAccurate && isFromSameProvider -> true
            else -> false
        }
    }

    /* Checks if given location is new */
    fun isRecentEnough(location: Location): Boolean {
        val locationAge: Long = SystemClock.elapsedRealtimeNanos() - location.elapsedRealtimeNanos
        return locationAge < Keys.DEFAULT_THRESHOLD_LOCATION_AGE
    }


    fun isAccurateEnough(location: Location, locationAccuracyThreshold: Int): Boolean =
        when (location.provider) {
            LocationManager.GPS_PROVIDER -> location.accuracy < locationAccuracyThreshold
            else -> location.accuracy < locationAccuracyThreshold + 10 // a bit more relaxed when location comes from network provider
        }


    /* Checks if the first location of track is plausible */
    fun isFirstLocationPlausible(secondLocation: Location, track: Track): Boolean {
        // speed in km/h
        val speed: Double = calculateSpeed(
            firstLocation = track.wayPoints[0].toLocation(),
            secondLocation = secondLocation,
            firstTimestamp = track.recordingStart.time,
            secondTimestamp = GregorianCalendar.getInstance().time.time
        )
        // plausible = speed under 250 km/h
        return speed < Keys.IMPLAUSIBLE_TRACK_START_SPEED
    }
    
    private fun calculateSpeed(
        firstLocation: Location,
        secondLocation: Location,
        firstTimestamp: Long,
        secondTimestamp: Long,
        useImperial: Boolean = false
    ): Double {
        // time difference in seconds
        val timeDifference: Long = (secondTimestamp - firstTimestamp) / 1000L
        // distance in meters
        val distance: Float = calculateDistance(firstLocation, secondLocation)
        // speed in either km/h (default) or mph
        return LengthUnitHelper.convertMetersPerSecond(distance / timeDifference, useImperial)
    }

    /* Checks if given location is different enough compared to previous location */
    fun isDifferentEnough(
        previousLocation: Location?,
        location: Location
    ): Boolean {
        // check if previous location is (not) available
        if (previousLocation == null) return true

        // location.accuracy is given as 1 standard deviation, with a 68% chance
        // that the true position is within a circle of this radius.
        // These formulas determine if the difference between the last point and
        // new point is statistically significant.
        val accuracy: Float =
            if (location.accuracy != 0.0f) location.accuracy else Keys.DEFAULT_THRESHOLD_DISTANCE
        val previousAccuracy: Float =
            if (previousLocation.accuracy != 0.0f) previousLocation.accuracy else Keys.DEFAULT_THRESHOLD_DISTANCE
        val accuracyDelta: Double =
            sqrt((accuracy.pow(2) + previousAccuracy.pow(2)).toDouble())
        val distance: Float = calculateDistance(previousLocation, location)

        // With 1*accuracyDelta we have 68% confidence that the points are
        // different. We can multiply this number to increase confidence but
        // decrease point recording frequency if needed.
        return distance > accuracyDelta * 2
    }


    /* Calculates distance in meters between two locations */
    fun calculateDistance(previousLocation: Location?, location: Location): Float {
        var distance: Float = 0f
        // two data points needed to calculate distance
        if (previousLocation != null) {
            // add up distance
            distance = previousLocation.distanceTo(location)
        }
        return distance
    }

    fun getNumberOfSatellites(location: Location): Int {
        val numberOfSatellites: Int
        val extras: Bundle? = location.extras
        if (extras != null && extras.containsKey("satellites")) {
            numberOfSatellites = extras.getInt("satellites", 0)
        } else {
            numberOfSatellites = 0
        }
        return numberOfSatellites
    }

    fun getFormattedLatLon(location: Location): String =
        String.format(Locale.ENGLISH, "%.6f %.6f", location.latitude, location.longitude)
}
