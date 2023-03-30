package tech.nagual.phoenix.tools.gps.data

import android.location.Location
import android.os.Parcelable
import androidx.annotation.Keep
import com.google.gson.annotations.Expose
import kotlinx.parcelize.Parcelize
import tech.nagual.phoenix.tools.gps.helpers.LocationHelper

@Keep
@Parcelize
data class WayPoint(
    @Expose val provider: String,
    @Expose val latitude: Double,
    @Expose val longitude: Double,
    @Expose val altitude: Double,
    @Expose val accuracy: Float,
    @Expose val time: Long,
    @Expose val distanceToStartingPoint: Double = 0.0,
    @Expose val numberSatellites: Int = 0,
    @Expose var isStopOver: Boolean = false,
    @Expose var starred: Boolean = false
) : Parcelable {

    constructor(location: Location) : this(
        location.provider,
        location.latitude,
        location.longitude,
        location.altitude,
        location.accuracy,
        location.time
    )

    constructor(location: Location, distanceToStartingPoint: Double) : this(
        location.provider,
        location.latitude,
        location.longitude,
        location.altitude,
        location.accuracy,
        location.time,
        distanceToStartingPoint,
        LocationHelper.getNumberOfSatellites(location)
    )

    fun toLocation(): Location {
        val location = Location(provider)
        location.latitude = latitude
        location.longitude = longitude
        location.altitude = altitude
        location.accuracy = accuracy
        location.time = time
        return location
    }
}
