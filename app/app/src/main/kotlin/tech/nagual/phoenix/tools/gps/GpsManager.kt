package tech.nagual.phoenix.tools.gps

import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.location.Location
import androidx.core.content.ContextCompat
import org.osmdroid.config.Configuration
import tech.nagual.phoenix.BuildConfig
import tech.nagual.app.application
import tech.nagual.app.defaultSharedPreferences
import tech.nagual.settings.ParcelValueSettingLiveData
import tech.nagual.settings.SettingLiveData
import tech.nagual.common.R
import tech.nagual.app.externalDataPath
import tech.nagual.phoenix.tools.gps.dashboard.GpsIndicator
import tech.nagual.phoenix.tools.gps.data.Track
import tech.nagual.phoenix.tools.gps.helpers.LocationHelper
import java.io.File

class GpsManager {
    var track: Track = Track()
    var trackingState: Int = Keys.STATE_TRACKING_IDLE

    var previousLocationInfo: Location? = null
    var currentLocationInfo: Location? = null
    val currentBestLocation: Location
        get() = currentLocationInfo ?: LocationHelper.getLastKnownLocation(
            application
        )

    private operator fun get(key: String, defaultValue: String): String =
        defaultSharedPreferences.getString("SESSION_$key", defaultValue)!!

    private operator fun set(key: String, value: String) {
        defaultSharedPreferences.edit().putString("SESSION_$key", value).apply()
    }

    var isNetworkEnabled: Boolean
        get() = java.lang.Boolean.valueOf(get("networkEnabled", "false"))
        set(enabled) {
            set("networkEnabled", enabled.toString())
        }

    var isGpsEnabled: Boolean
        get() = java.lang.Boolean.valueOf(get("gpsEnabled", "false"))
        set(gpsEnabled) {
            set("gpsEnabled", gpsEnabled.toString())
        }

    var isServiceStarted: Boolean
        get() = java.lang.Boolean.valueOf(get("LOGGING_STARTED", "false"))
        set(isStarted) {
            set("LOGGING_STARTED", isStarted.toString())
            if (isStarted) {
                set("startTimeStamp", System.currentTimeMillis().toString())
            }
        }

    var isBoundToService: Boolean
        get() = java.lang.Boolean.valueOf(get("isBound", "false"))
        set(isBound) {
            set("isBound", isBound.toString())
        }

    fun setUsingGps(isUsingGps: Boolean) {
        set("isUsingGps", isUsingGps.toString())
    }

    /**
     * @param satellites sets the number of visible satellites
     */
    fun setVisibleSatelliteCount(satellites: Int) {
        set("satellites", satellites.toString())
    }

    val currentLatitude: Double
        get() = if (currentLocationInfo != null) {
            currentLocationInfo!!.latitude
        } else {
            0.0
        }
    val currentLongitude: Double
        get() = if (currentLocationInfo != null) {
            currentLocationInfo!!.longitude
        } else {
            0.0
        }
    val previousLatitude: Double
        get() {
            val loc = previousLocationInfo
            return loc?.latitude ?: 0.0
        }
    val previousLongitude: Double
        get() {
            val loc = previousLocationInfo
            return loc?.longitude ?: 0.0
        }

    fun hasValidLocation(): Boolean {
        return currentLocationInfo != null && currentLatitude != 0.0 && currentLongitude != 0.0
    }

    var latestTimeStamp: Long
        get() = get("latestTimeStamp", "0").toLong()
        set(latestTimeStamp) {
            set("latestTimeStamp", latestTimeStamp.toString())
        }

    var firstRetryTimeStamp: Long
        get() = get("firstRetryTimeStamp", "0").toLong()
        set(firstRetryTimeStamp) {
            set("firstRetryTimeStamp", firstRetryTimeStamp.toString())
        }

    fun init() {
        // set user agent to prevent getting banned from the osm servers
        Configuration.getInstance().userAgentValue = BuildConfig.APPLICATION_ID
        // set the path for osmdroid's files (e.g. tile cache)
        Configuration.getInstance().osmdroidBasePath =
            File(externalDataPath, "osmdroid")
    }

    fun startService() {
        serviceIntent = Intent(application, GpsService::class.java)
        // Start the service in case it isn't already running
        ContextCompat.startForegroundService(application.applicationContext, serviceIntent!!)
    }

    fun stopServiceIfRequired() {
        if (!isServiceStarted && serviceIntent != null) {
            try {
                application.stopService(serviceIntent)
                serviceIntent = null
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun bindToService(conn: ServiceConnection): Boolean {
        boundToService = application.bindService(
            Intent(application, GpsService::class.java),
            conn,
            Context.BIND_AUTO_CREATE
        )
        return boundToService
    }

    fun unbindFromService(conn: ServiceConnection): Boolean {
        if (boundToService) {
            application.unbindService(conn)
            boundToService = false
            return true
        }
        return false
    }

    companion object {
        val indicatorLocation: GpsIndicator = GpsIndicator(
            name = application.getString(R.string.gps_indicator_coordinates),
            iconRes = R.drawable.gps_indicator_location_icon_24dp,
            isVisible = true
        )
        val indicatorAccuracy: GpsIndicator = GpsIndicator(
            name = application.getString(R.string.gps_indicator_accuracy),
            iconRes = R.drawable.gps_indicator_accuracy_icon_24dp,
            isVisible = true
        )
        val indicatorDirection: GpsIndicator = GpsIndicator(
            name = application.getString(R.string.gps_indicator_direction),
            iconRes = R.drawable.gps_indicator_compas_icon_24dp,
            isVisible = true
        )
        val indicatorAltitude: GpsIndicator = GpsIndicator(
            name = application.getString(R.string.gps_indicator_altitude),
            iconRes = R.drawable.gps_indicator_altitude_icon_24dp,
            isVisible = true
        )
        val indicatorSpeed: GpsIndicator = GpsIndicator(
            name = application.getString(R.string.gps_indicator_speed),
            iconRes = R.drawable.gps_indicator_speed_icon_24dp,
            isVisible = true
        )
        val indicatorSatellites: GpsIndicator = GpsIndicator(
            name = application.getString(R.string.gps_indicator_satellites),
            iconRes = R.drawable.gps_indicator_satellites_icon_24dp,
            isVisible = true
        )
        val indicatorDistance: GpsIndicator = GpsIndicator(
            name = application.getString(R.string.gps_indicator_distance),
            iconRes = R.drawable.gps_indicator_distance_icon_24dp,
            isVisible = true
        )
        val indicatorDuration: GpsIndicator = GpsIndicator(
            name = application.getString(R.string.gps_indicator_duration),
            iconRes = R.drawable.gps_indicator_duration_icon_24dp,
            isVisible = true
        )
        val indicatorWaypoints: GpsIndicator = GpsIndicator(
            name = application.getString(R.string.gps_indicator_waypoints),
            iconRes = R.drawable.gps_indicator_waypoints_icon_24dp,
            isVisible = true
        )


        val INDICATORS: SettingLiveData<List<GpsIndicator>> =
            ParcelValueSettingLiveData(
                R.string.pref_key_gps_indicators,
                listOf(
                    indicatorLocation,
                    indicatorAccuracy,
                    indicatorDirection,
                    indicatorAltitude,
                    indicatorSpeed,
                    indicatorDistance,
                    indicatorWaypoints,
                    indicatorDuration,
                    indicatorSatellites
                )
            )

        private var serviceIntent: Intent? = null
        private var boundToService: Boolean = false

        private lateinit var INSTANCE: GpsManager
        fun getInstance(): GpsManager {
            if (!this::INSTANCE.isInitialized) {
                synchronized(GpsManager::class.java) {
                    if (!this::INSTANCE.isInitialized) {
                        INSTANCE = GpsManager()
                    }
                }
            }
            return INSTANCE
        }
    }
}