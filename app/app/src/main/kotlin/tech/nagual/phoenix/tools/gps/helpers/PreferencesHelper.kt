package tech.nagual.phoenix.tools.gps.helpers

import android.content.SharedPreferences
import android.location.Location
import android.location.LocationManager
import androidx.core.content.edit
import tech.nagual.app.defaultSharedPreferences
import tech.nagual.phoenix.tools.gps.Keys
import tech.nagual.common.extensions.getDouble
import tech.nagual.common.extensions.putDouble

object PreferencesHelper {

    fun loadZoomLevel(): Double =
        defaultSharedPreferences.getDouble(Keys.PREF_MAP_ZOOM_LEVEL, Keys.DEFAULT_ZOOM_LEVEL)

    fun saveZoomLevel(zoomLevel: Double) {
        defaultSharedPreferences.edit { putDouble(Keys.PREF_MAP_ZOOM_LEVEL, zoomLevel) }
    }

    fun loadTrackingState(): Int =
        defaultSharedPreferences.getInt(Keys.PREF_TRACKING_STATE, Keys.STATE_TRACKING_IDLE)

    fun saveTrackingState(trackingState: Int) {
        defaultSharedPreferences.edit { putInt(Keys.PREF_TRACKING_STATE, trackingState) }
    }

    fun loadCurrentBestLocation(): Location {
        val provider: String = defaultSharedPreferences.getString(
            Keys.PREF_CURRENT_BEST_LOCATION_PROVIDER,
            LocationManager.NETWORK_PROVIDER
        ) ?: LocationManager.NETWORK_PROVIDER
        return Location(provider).apply {
            latitude = defaultSharedPreferences.getDouble(
                Keys.PREF_CURRENT_BEST_LOCATION_LATITUDE,
                Keys.DEFAULT_LATITUDE
            )
            longitude = defaultSharedPreferences.getDouble(
                Keys.PREF_CURRENT_BEST_LOCATION_LONGITUDE,
                Keys.DEFAULT_LONGITUDE
            )
            accuracy = defaultSharedPreferences.getFloat(
                Keys.PREF_CURRENT_BEST_LOCATION_ACCURACY,
                Keys.DEFAULT_ACCURACY
            )
            altitude = defaultSharedPreferences.getDouble(
                Keys.PREF_CURRENT_BEST_LOCATION_ALTITUDE,
                Keys.DEFAULT_ALTITUDE
            )
            time = defaultSharedPreferences.getLong(
                Keys.PREF_CURRENT_BEST_LOCATION_TIME,
                Keys.DEFAULT_TIME
            )
        }
    }

    fun saveCurrentBestLocation(currentBestLocation: Location) {
        defaultSharedPreferences.edit {
            putDouble(Keys.PREF_CURRENT_BEST_LOCATION_LATITUDE, currentBestLocation.latitude)
            putDouble(Keys.PREF_CURRENT_BEST_LOCATION_LONGITUDE, currentBestLocation.longitude)
            putFloat(Keys.PREF_CURRENT_BEST_LOCATION_ACCURACY, currentBestLocation.accuracy)
            putDouble(Keys.PREF_CURRENT_BEST_LOCATION_ALTITUDE, currentBestLocation.altitude)
            putLong(Keys.PREF_CURRENT_BEST_LOCATION_TIME, currentBestLocation.time)
        }
    }

    /* Checks if housekeeping work needs to be done - used usually in DownloadWorker "REQUEST_UPDATE_COLLECTION" */
    fun isHouseKeepingNecessary(): Boolean {
        return defaultSharedPreferences.getBoolean(Keys.PREF_ONE_TIME_HOUSEKEEPING_NECESSARY, true)
    }

    /* Saves state of housekeeping */
    fun saveHouseKeepingNecessaryState(state: Boolean = false) {
        defaultSharedPreferences.edit {
            putBoolean(
                Keys.PREF_ONE_TIME_HOUSEKEEPING_NECESSARY,
                state
            )
        }

    }

    fun registerPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        defaultSharedPreferences.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregisterPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        defaultSharedPreferences.unregisterOnSharedPreferenceChangeListener(listener)
    }
}
