package tech.nagual.phoenix.tools.gps.preferences

import tech.nagual.app.defaultSharedPreferences
import tech.nagual.common.extensions.toInt

object GpsPreferences {
    var useSatelliteLocations: Boolean
        get() = defaultSharedPreferences.getBoolean(PreferenceKeys.GPS_SATELLITE_LOCATIONS, true)
        set(value) = defaultSharedPreferences.edit()
            .putBoolean(PreferenceKeys.GPS_SATELLITE_LOCATIONS, value).apply()

    var useNetworkLocations: Boolean
        get() = defaultSharedPreferences.getBoolean(PreferenceKeys.GPS_NETWORK_LOCATIONS, true)
        set(value) = defaultSharedPreferences.edit()
            .putBoolean(PreferenceKeys.GPS_NETWORK_LOCATIONS, value)
            .apply()

    var usePassiveLocations: Boolean
        get() = defaultSharedPreferences.getBoolean(PreferenceKeys.GPS_PASSIVE_LOCATIONS, false)
        set(value) = defaultSharedPreferences.edit()
            .putBoolean(PreferenceKeys.GPS_PASSIVE_LOCATIONS, value)
            .apply()

    var minimumInterval: Int
        get() = defaultSharedPreferences.getString(
            PreferenceKeys.GPS_MINIMUM_INTERVAL, "30"
        )?.toInt(30) ?: 30
        set(minimumSeconds) = defaultSharedPreferences.edit()
            .putString(PreferenceKeys.GPS_MINIMUM_INTERVAL, minimumSeconds.toString()).apply()

    var minimumDistance: Int
        get() = defaultSharedPreferences.getString(
            PreferenceKeys.GPS_MINIMUM_DISTANCE,
            "0"
        )?.toInt(0) ?: 0
        set(value) = defaultSharedPreferences.edit()
            .putString(PreferenceKeys.GPS_MINIMUM_DISTANCE, value.toString())
            .apply()

    var minimumAccuracy: Int
        get() = defaultSharedPreferences.getString(
            PreferenceKeys.GPS_MINIMUM_ACCURACY,
            "25"
        )?.toInt(25) ?: 25
        set(minimumAccuracy) {
            defaultSharedPreferences.edit()
                .putString(PreferenceKeys.GPS_MINIMUM_ACCURACY, minimumAccuracy.toString())
                .apply()
        }

    var detectingRetryPeriod: Int
        get() = defaultSharedPreferences.getString(
            PreferenceKeys.GPS_DETECTING_RETRY_TIME,
            "60"
        )?.toInt(60) ?: 60
        set(retryInterval) {
            defaultSharedPreferences.edit()
                .putString(PreferenceKeys.GPS_DETECTING_RETRY_TIME, retryInterval.toString())
                .apply()
        }

    var absoluteTimeoutForAcquiringPosition: Int
        get() = defaultSharedPreferences.getString(
            PreferenceKeys.GPS_ABSOLUTE_TIMEOUT,
            "120"
        )?.toInt(120) ?: 120
        set(absoluteTimeout) {
            defaultSharedPreferences.edit()
                .putString(PreferenceKeys.GPS_ABSOLUTE_TIMEOUT, absoluteTimeout.toString())
                .apply()
        }

    var adjustAltitudeFromGeoIdHeight: Boolean
        get() = defaultSharedPreferences.getBoolean(
            PreferenceKeys.GPS_ALTITUDE_SHOULD_ADJUST,
            false
        )
        set(value) = defaultSharedPreferences.edit()
            .putBoolean(PreferenceKeys.GPS_ALTITUDE_SHOULD_ADJUST, value)
            .apply()

    var subtractAltitudeOffset: Int
        get() = defaultSharedPreferences.getString(
            PreferenceKeys.GPS_ALTITUDE_SUBTRACT_OFFSET,
            "0"
        )?.toInt(0) ?: 0
        set(value) = defaultSharedPreferences.edit()
            .putString(PreferenceKeys.GPS_ALTITUDE_SUBTRACT_OFFSET, value.toString())
            .apply()
}