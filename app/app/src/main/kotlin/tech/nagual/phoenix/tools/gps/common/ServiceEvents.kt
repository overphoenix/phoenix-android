package tech.nagual.phoenix.tools.gps.common

import android.location.Location

class ServiceEvents {
    /**
     * New location
     */
    class LocationUpdate(var location: Location)

    /**
     * Number of visible satellites
     */
    class SatellitesVisible(var satelliteCount: Int)

    /**
     * Indicates that GPS/Network location services have temporarily gone away
     */
    class LocationServicesUnavailable

    /**
     * Whether GPS tracking has started
     */
    class TrackingStatus(var loggingStarted: Boolean)
}