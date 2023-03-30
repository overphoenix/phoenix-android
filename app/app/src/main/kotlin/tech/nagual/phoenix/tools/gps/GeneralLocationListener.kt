package tech.nagual.phoenix.tools.gps

import android.location.GnssStatus
import android.location.Location
import android.location.LocationListener
import android.os.Bundle

internal class GeneralLocationListener(
    private var loggingService: GpsService,
    private val listenerName: String
) : LocationListener, GnssStatus.Callback() {
    private var latestHdop: String? = null
    private var latestPdop: String? = null
    private var latestVdop: String? = null
    private var geoIdHeight: String? = null
    private var ageOfDgpsData: String? = null
    private var dgpsId: String? = null
    private var satellitesUsedInFix = 0

    override fun onLocationChanged(loc: Location) {
        try {
            val b = Bundle()
            b.putString(HDOP, latestHdop)
            b.putString(PDOP, latestPdop)
            b.putString(VDOP, latestVdop)
            b.putString(GEOIDHEIGHT, geoIdHeight)
            b.putString(AGEOFDGPSDATA, ageOfDgpsData)
            b.putString(DGPSID, dgpsId)
            b.putBoolean(
                PASSIVE,
                listenerName.equals(PASSIVE, ignoreCase = true)
            )
            b.putString(LISTENER, listenerName)
            b.putInt(SATELLITES_FIX, satellitesUsedInFix)
            loc.extras = b
            loggingService.onLocationChanged(loc)

            latestHdop = ""
            latestPdop = ""
            latestVdop = ""
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    override fun onProviderDisabled(provider: String) {
        loggingService.onProviderDisabled(provider)
    }

    override fun onProviderEnabled(provider: String) {
        loggingService.onProviderEnabled(provider)
    }

    override fun onSatelliteStatusChanged(status: GnssStatus) {
        var satellitesVisible = 0
        satellitesUsedInFix = 0
        for (i in 0 until status.satelliteCount) {
            if (status.usedInFix(i))
                satellitesUsedInFix++
            satellitesVisible++
        }
        loggingService.setSatelliteInfo(satellitesVisible)
    }

    companion object {
        const val HDOP = "HDOP"
        const val PDOP = "PDOP"
        const val VDOP = "VDOP"
        const val GEOIDHEIGHT = "GEOIDHEIGHT"
        const val AGEOFDGPSDATA = "AGEOFDGPSDATA"
        const val DGPSID = "DGPSID"
        const val PASSIVE = "PASSIVE"
        const val LISTENER = "LISTENER"
        const val SATELLITES_FIX = "SATELLITES_FIX"
    }
}