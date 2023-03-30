package tech.nagual.phoenix.tools.gps

import java.util.*

object Keys {
    // intent actions
    const val ACTION_STOP: String = "tech.nagual.tools.gps.action.STOP"
    const val ACTION_RESUME: String = "tech.nagual.tools.gps.action.RESUME"

    // args
    const val ARG_TRACK_TITLE: String = "ArgTrackTitle"
    const val ARG_TRACK_FILE_URI: String = "ArgTrackFileUri"
    const val ARG_GPX_FILE_URI: String = "ArgGpxFileUri"
    const val ARG_TRACK_ID: String = "ArgTrackId"

    // preferences
    const val PREF_ONE_TIME_HOUSEKEEPING_NECESSARY =
        "ONE_TIME_HOUSEKEEPING_NECESSARY_VERSIONCODE_38" // increment to current app version code to trigger housekeeping that runs only once

    const val PREF_CURRENT_BEST_LOCATION_PROVIDER: String = "key_gps_current_best_location_provider"
    const val PREF_CURRENT_BEST_LOCATION_LATITUDE: String = "key_gps_current_best_location_latitude"
    const val PREF_CURRENT_BEST_LOCATION_LONGITUDE: String =
        "key_gps_current_best_location_longitude"
    const val PREF_CURRENT_BEST_LOCATION_ACCURACY: String = "key_gps_current_best_location_accuracy"
    const val PREF_CURRENT_BEST_LOCATION_ALTITUDE: String = "key_gps_current_best_location_altitude"
    const val PREF_CURRENT_BEST_LOCATION_TIME: String = "key_gps_current_best_location_time"
    const val PREF_MAP_ZOOM_LEVEL: String = "key_gps_map_zoom_level"
    const val PREF_TRACKING_STATE: String = "key_gps_tracking_state"

    // states
    const val STATE_TRACKING_IDLE: Int = 0
    const val STATE_TRACKING_ACTIVE: Int = 1
    const val STATE_TRACKING_STOPPED: Int = 2

    // dialog types
    const val DIALOG_EMPTY_RECORDING: Int = 0
    const val DIALOG_DELETE_TRACK: Int = 1
    const val DIALOG_DELETE_NON_STARRED: Int = 2

    // dialog results
    const val DIALOG_EMPTY_PAYLOAD_STRING: String = ""
    const val DIALOG_EMPTY_PAYLOAD_INT: Int = -1

    // folder names
    const val FOLDER_TEMP: String = "gps_temp"
    const val FOLDER_TRACKS: String = "tracks"
    const val FOLDER_GPX: String = "gpx"

    // file names and extensions
    const val MIME_TYPE_GPX: String = "application/gpx+xml"
    const val GPX_FILE_EXTENSION: String = ".gpx"
    const val TRACKBOOK_LEGACY_FILE_EXTENSION: String = ".trackbook"
    const val TRACKBOOK_FILE_EXTENSION: String = ".json"
    const val TEMP_FILE: String = "track.json"
    const val TRACKLIST_FILE: String = "tracklist.json"

    // view types
    const val VIEW_TYPE_TRACK: Int = 1

    // default values
    val DEFAULT_DATE: Date = Date(0L)
    const val DEFAULT_RFC2822_DATE: String = "Thu, 01 Jan 1970 01:00:00 +0100"  // --> Date(0)
    const val ONE_HOUR_IN_MILLISECONDS: Int = 3600000
    const val EMPTY_STRING_RESOURCE: Int = 0
    const val REQUEST_CURRENT_LOCATION_INTERVAL: Long = 1000L // 1 second in milliseconds
    const val ADD_WAYPOINT_TO_TRACK_INTERVAL: Long = 1000L // 1 second in milliseconds
    const val SAVE_TEMP_TRACK_INTERVAL: Long = 10000L // 10 seconds in milliseconds
    const val SIGNIFICANT_TIME_DIFFERENCE: Long = 60000L // 1 minute in milliseconds
    const val STOP_OVER_THRESHOLD: Long = 300000L // 5 minutes in milliseconds
    const val IMPLAUSIBLE_TRACK_START_SPEED: Double = 250.0 // 250 km/h
    const val DEFAULT_LATITUDE: Double = 71.172500 // latitude Nordkapp, Norway
    const val DEFAULT_LONGITUDE: Double = 25.784444 // longitude Nordkapp, Norway
    const val DEFAULT_ACCURACY: Float = 300f // in meters
    const val DEFAULT_ALTITUDE: Double = 0.0
    const val DEFAULT_TIME: Long = 0L
    const val DEFAULT_ALTITUDE_SMOOTHING_VALUE: Int = 13
    const val DEFAULT_THRESHOLD_LOCATION_ACCURACY: Int = 30 // 30 meters
    const val DEFAULT_THRESHOLD_LOCATION_AGE: Long = 60000000000L // one minute in nanoseconds
    const val DEFAULT_THRESHOLD_DISTANCE: Float = 15f // 15 meters
    const val DEFAULT_ZOOM_LEVEL: Double = 16.0
    const val MIN_NUMBER_OF_WAYPOINTS_FOR_ELEVATION_CALCULATION: Int = 5
    const val MAX_NUMBER_OF_WAYPOINTS_FOR_ELEVATION_CALCULATION: Int = 20
    const val ALTITUDE_MEASUREMENT_ERROR_THRESHOLD =
        10 // altitude changes of 10 meter or more (per 15 seconds) are being discarded

    // notification
    const val TRACKER_SERVICE_NOTIFICATION_ID: Int = 1
    const val NOTIFICATION_CHANNEL_RECORDING: String = "notificationChannelIdRecordingChannel"
}
