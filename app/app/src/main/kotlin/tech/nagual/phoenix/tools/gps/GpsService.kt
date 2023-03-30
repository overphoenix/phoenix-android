package tech.nagual.phoenix.tools.gps

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationManager
import android.os.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import tech.nagual.app.*
import tech.nagual.common.flowbus.GlobalBus
import tech.nagual.common.permissions.Permission
import tech.nagual.common.permissions.isAllGranted
import tech.nagual.phoenix.tools.gps.common.IntentConstants
import tech.nagual.phoenix.tools.gps.common.ServiceEvents.*
import tech.nagual.phoenix.tools.gps.data.Track
import tech.nagual.phoenix.tools.gps.data.WayPoint
import tech.nagual.phoenix.tools.gps.helpers.*
import tech.nagual.phoenix.tools.gps.preferences.GpsPreferences
import java.util.*
import kotlin.math.abs

@SuppressLint("MissingPermission")
@AndroidEntryPoint
class GpsService : Service(), SensorEventListener {
    val manager = GpsManager.getInstance()

    private lateinit var gpsLocationListener: GeneralLocationListener
    private lateinit var networkLocationListener: GeneralLocationListener
    private var passiveLocationListener: GeneralLocationListener? = null
    var gpsLocationListenerRegistered: Boolean = false
    var networkLocationListenerRegistered: Boolean = false
    private val alarmIntent: Intent? = null
    private val handler: Handler = Handler(Looper.getMainLooper())

    // TrackerService ------------------------------------
    private var lastSave: Date = Keys.DEFAULT_DATE
    var stepCountOffset: Float = 0f
    var resumed: Boolean = false
    private lateinit var notificationHelper: NotificationHelper

    inner class GpsServiceBinder : Binder() {
        val service: GpsService
            get() = this@GpsService
    }

    private val binder: IBinder = GpsServiceBinder()

    override fun onBind(arg0: Intent): IBinder? {
        addGpsLocationListener()
        addNetworkLocationListener()
        return binder
    }

    override fun onRebind(intent: Intent?) {
        addGpsLocationListener()
        addNetworkLocationListener()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        if (manager.trackingState != Keys.STATE_TRACKING_ACTIVE) {
            removeGpsLocationListener()
            removeNetworkLocationListener()
        }
        // ensures onRebind is called
        return true
    }

    override fun onCreate() {
        super.onCreate()

        updateGpsStatus()
        updateNetworkStatus()

        gpsLocationListener = GeneralLocationListener(this, "GPS")
        networkLocationListener = GeneralLocationListener(this, "CELL")

        notificationHelper = NotificationHelper(this)
        manager.trackingState = PreferencesHelper.loadTrackingState()
        manager.track = Track.readFromFile(FileHelper.getTempTrackFileUri(this))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        startForegroundSafe()

        if (intent == null) {
            if (manager.trackingState == Keys.STATE_TRACKING_ACTIVE) {
                resumeTracking()
            }
        } else if (Keys.ACTION_STOP == intent.action) {
            stopTracking()
        } else if (Keys.ACTION_RESUME == intent.action) {
            resumeTracking()
        } else {
            val bundle = intent.extras
            if (bundle != null) {
                if (!isAllGranted(
                        Permission.ACCESS_FINE_LOCATION,
                        Permission.ACCESS_COARSE_LOCATION
                    )
                ) {
                    stopTracking()
                    stopSelf()
                    return START_STICKY
                }
                if (bundle.getBoolean(IntentConstants.IMMEDIATE_START)) {
                    startTracking()
                }
                if (bundle.getBoolean(IntentConstants.IMMEDIATE_STOP)) {
                    stopTracking()
                }
                if (bundle.getBoolean(IntentConstants.GET_NEXT_POINT)) {
                    if (manager.isServiceStarted) {
                        checkGpsManagers()
                    }
                }
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        notificationManager.cancelAll()
        super.onDestroy()
        if (manager.isServiceStarted) {
            val broadcastIntent = Intent(applicationContext, RestarterReceiver::class.java)
            broadcastIntent.putExtra("was_running", true)
            sendBroadcast(broadcastIntent)
        }

        if (manager.trackingState == Keys.STATE_TRACKING_ACTIVE) stopTracking()

        removeGpsLocationListener()
        removeNetworkLocationListener()
    }

    private fun notifyClientsStarted(started: Boolean) {
        GlobalBus.post(TrackingStatus(started))
    }

    private val notification: Notification
        get() = notificationHelper.createNotification(
            manager.trackingState,
            manager.track.length,
            manager.track.duration
        )

    private fun showNotification() {
        notificationManager.notify(Keys.TRACKER_SERVICE_NOTIFICATION_ID, notification)
    }

    private fun startPassiveManager() {
        if (GpsPreferences.usePassiveLocations) {
            if (passiveLocationListener == null) {
                passiveLocationListener =
                    GeneralLocationListener(this, GeneralLocationListener.PASSIVE)
            }
            locationManager.requestLocationUpdates(
                LocationManager.PASSIVE_PROVIDER,
                1000,
                0f,
                passiveLocationListener!!
            )
        }
    }

    private fun updateGpsStatus() {
        manager.isGpsEnabled =
            if (locationManager.allProviders.contains(LocationManager.GPS_PROVIDER)) {
                locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            } else {
                false
            }
    }

    private fun updateNetworkStatus() {
        manager.isNetworkEnabled =
            if (locationManager.allProviders.contains(LocationManager.NETWORK_PROVIDER)) {
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
            } else {
                false
            }
    }

    private fun addGpsLocationListener() {
        if (!gpsLocationListenerRegistered) {
            if (manager.isGpsEnabled && GpsPreferences.useSatelliteLocations) {
                if (isAllGranted(Permission.ACCESS_FINE_LOCATION)) {
                    locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        1000,
                        0f,
                        gpsLocationListener
                    )
                    locationManager.registerGnssStatusCallback(gpsLocationListener, handler)
                    manager.setUsingGps(true)
                    gpsLocationListenerRegistered = true
                }
            }
        }
    }

    private fun addNetworkLocationListener() {
        if (!networkLocationListenerRegistered) {
            if (manager.isNetworkEnabled && (GpsPreferences.useNetworkLocations || !manager.isGpsEnabled)) {
                if (isAllGranted(Permission.ACCESS_FINE_LOCATION)) {
                    manager.setUsingGps(false)
                    locationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        1000,
                        0f,
                        networkLocationListener
                    )
                    networkLocationListenerRegistered = true
                }
            }
        }
    }

    private fun removeGpsLocationListener() {
        if (isAllGranted(Permission.ACCESS_FINE_LOCATION) && gpsLocationListenerRegistered) {
            locationManager.removeUpdates(gpsLocationListener)
            locationManager.unregisterGnssStatusCallback(gpsLocationListener)
            gpsLocationListenerRegistered = false
        }
    }

    private fun removeNetworkLocationListener() {
        if (isAllGranted(Permission.ACCESS_FINE_LOCATION) && networkLocationListenerRegistered) {
            locationManager.removeUpdates(networkLocationListener)
            networkLocationListenerRegistered = false
        }
    }

    private fun checkGpsManagers() {
        if (manager.isGpsEnabled && GpsPreferences.useSatelliteLocations) {
            startAbsoluteTimer()
        }
        if (manager.isNetworkEnabled && (GpsPreferences.useNetworkLocations || !manager.isGpsEnabled)) {
            startAbsoluteTimer()
        }
        if (!manager.isNetworkEnabled && !manager.isGpsEnabled) {
            manager.setUsingGps(false)
            stopTracking()
            GlobalBus.post(LocationServicesUnavailable())
            return
        }
        if (!GpsPreferences.useNetworkLocations && !GpsPreferences.useSatelliteLocations && !GpsPreferences.usePassiveLocations) {
            manager.setUsingGps(false)
            stopTracking()
            return
        }
    }

    private fun startAbsoluteTimer() {
        if (GpsPreferences.absoluteTimeoutForAcquiringPosition >= 1) {
            handler.postDelayed(
                stopManagerRunnable,
                (GpsPreferences.absoluteTimeoutForAcquiringPosition * 1000).toLong()
            )
        }
    }

    private val stopManagerRunnable = Runnable { stopManagerAndResetAlarm() }

    private fun stopAbsoluteTimer() {
        handler.removeCallbacks(stopManagerRunnable)
    }

    fun onProviderDisabled(provider: String) {
        when (provider) {
            LocationManager.GPS_PROVIDER -> updateGpsStatus()
            LocationManager.NETWORK_PROVIDER -> updateNetworkStatus()
        }
        if (manager.isServiceStarted) {
            checkGpsManagers()
        }
    }

    fun onProviderEnabled(provider: String) {
        when (provider) {
            LocationManager.GPS_PROVIDER -> updateGpsStatus()
            LocationManager.NETWORK_PROVIDER -> updateNetworkStatus()
        }
        if (manager.isServiceStarted) {
            checkGpsManagers()
        }
    }

    fun onLocationChanged(loc: Location) {
        var loc = loc
        val isPassiveLocation = loc.extras.getBoolean(GeneralLocationListener.PASSIVE)
        val currentTimeStamp = System.currentTimeMillis()

        if (!isPassiveLocation && (currentTimeStamp - manager.latestTimeStamp < GpsPreferences.minimumInterval * 1000 ||
                    !isFromValidListener(loc))
        ) {
            return
        }

        //Check if a ridiculous distance has been travelled since previous point - could be a bad GPS jump
        if (manager.currentLocationInfo != null) {
            val distanceTravelled = LocationHelper.calculateDistance(
                manager.currentLocationInfo,
                loc
            )
            val timeDifference =
                (abs(loc.time - manager.currentLocationInfo!!.time).toInt() / 1000).toLong()
            if (timeDifference > 0 && distanceTravelled / timeDifference > 357) { //357 m/s ~=  1285 km/h
                return
            }
        }

        // Don't do anything until the user-defined accuracy is reached
        // even for annotations
        if (GpsPreferences.minimumAccuracy > 0) {
            if (!loc.hasAccuracy() || loc.accuracy == 0f) {
                return
            }
            if (GpsPreferences.minimumAccuracy < abs(loc.accuracy)) {
                if (manager.firstRetryTimeStamp == 0L) {
                    manager.firstRetryTimeStamp = System.currentTimeMillis()
                }
                if (currentTimeStamp - manager.firstRetryTimeStamp <= GpsPreferences.detectingRetryPeriod * 1000) {
                    //return and keep trying
                    return
                } else {
                    // Give up for now
                    stopManagerAndResetAlarm()

                    //reset timestamp for next time.
                    manager.firstRetryTimeStamp = 0
                    return
                }
            }
        }

        if (!isPassiveLocation && GpsPreferences.minimumDistance > 0 && manager.hasValidLocation()) {
            val distanceTraveled = LocationHelper.calculateDistance(
                manager.currentLocationInfo,
                loc
            )
            if (GpsPreferences.minimumDistance > distanceTraveled) {
                stopManagerAndResetAlarm()
                return
            }
        }

        loc = LocationHelper.getLocationWithAdjustedAltitude(loc)
        loc = LocationHelper.getLocationAdjustedForGPSWeekRollover(loc)
        manager.latestTimeStamp = System.currentTimeMillis()
        manager.firstRetryTimeStamp = 0
        manager.currentLocationInfo = loc

        if (manager.isServiceStarted) {
            var numberOfWayPoints: Int = manager.track.wayPoints.size

            // CASE: Second location - check if first location was plausible & remove implausible location
            if (numberOfWayPoints == 1 && !LocationHelper.isFirstLocationPlausible(
                    loc,
                    manager.track
                )
            ) {
                manager.previousLocationInfo = null
                numberOfWayPoints = 0
                manager.track.wayPoints.removeAt(0)
            }
            // CASE: Third location or second location (if first was plausible)
            else if (numberOfWayPoints > 1) {
                manager.previousLocationInfo =
                    manager.track.wayPoints[numberOfWayPoints - 1].toLocation()
            }

            // Step 2: Update duration
            updateDuration()

            // Step 3: Add waypoint, if recent and accurate and different enough
            val shouldBeAdded: Boolean = (LocationHelper.isRecentEnough(loc) &&
                    LocationHelper.isAccurateEnough(
                        loc,
                        Keys.DEFAULT_THRESHOLD_LOCATION_ACCURACY
                    ) &&
                    LocationHelper.isDifferentEnough(manager.previousLocationInfo, loc))
            if (shouldBeAdded) {
                // Step 3.1: Update distance (do not update if resumed -> we do not want to add values calculated during a recording pause)
                if (!resumed) {
                    manager.track.length = manager.track.length + LocationHelper.calculateDistance(
                        manager.previousLocationInfo,
                        loc
                    )
                }
                // Step 3.2: Update altitude values
                val altitude: Double = loc.altitude
                if (altitude != 0.0) {
                    if (numberOfWayPoints == 0) {
                        manager.track.maxAltitude = altitude
                        manager.track.minAltitude = altitude
                    } else {
                        if (altitude > manager.track.maxAltitude)
                            manager.track.maxAltitude = altitude
                        if (altitude < manager.track.minAltitude)
                            manager.track.minAltitude = altitude
                    }
                }
                // Step 3.3: Toggle stop over status, if necessary
                if (manager.track.wayPoints.size < 0) {
                    manager.track.wayPoints[manager.track.wayPoints.size - 1].isStopOver =
                        if (manager.previousLocationInfo == null) false
                        else loc.time - manager.previousLocationInfo!!.time > Keys.STOP_OVER_THRESHOLD
                }

                // Step 3.4: Add current location as point to center on for later display
                manager.track.latitude = loc.latitude
                manager.track.longitude = loc.longitude

                // Step 3.5: Add location as new waypoint
                manager.track.wayPoints.add(
                    WayPoint(
                        location = loc,
                        distanceToStartingPoint = manager.track.length
                    )
                )

                // reset resumed flag, if necessary
                if (resumed) {
                    resumed = false
                }

                // save a temp track
                val now: Date = GregorianCalendar.getInstance().time
                if (now.time - lastSave.time > Keys.SAVE_TEMP_TRACK_INTERVAL) {
                    lastSave = now
                    saveTrack()
                }
            }

            showNotification()
        }

        stopManagerAndResetAlarm()
        GlobalBus.post(LocationUpdate(loc))
    }

    private val periodicTrackUpdate: Runnable = object : Runnable {
        override fun run() {
            updateDuration()
            showNotification()
            handler.postDelayed(this, Keys.ADD_WAYPOINT_TO_TRACK_INTERVAL)
        }
    }

    private fun isFromValidListener(loc: Location): Boolean {
        if (!GpsPreferences.useSatelliteLocations && !GpsPreferences.useNetworkLocations) {
            return true
        }
        if (!GpsPreferences.useNetworkLocations) {
            return loc.provider.equals(LocationManager.GPS_PROVIDER, ignoreCase = true)
        }
        return if (!GpsPreferences.useSatelliteLocations) {
            !loc.provider.equals(LocationManager.GPS_PROVIDER, ignoreCase = true)
        } else true
    }

    private fun updateDuration() {
        val now: Date = GregorianCalendar.getInstance().time
        val difference: Long = now.time - manager.track.recordingStop.time
        manager.track.duration = manager.track.duration + difference
        manager.track.recordingStop = now
    }

    fun stopManagerAndResetAlarm() {
        stopAbsoluteTimer()

        val i = Intent(this, GpsService::class.java)
        i.putExtra(IntentConstants.GET_NEXT_POINT, true)
        val pi = PendingIntent.getService(this, 0, i, 0)
        alarmManager.cancel(pi)
        if (powerManager.isDeviceIdleMode && !powerManager.isIgnoringBatteryOptimizations(this.packageName)) {
            // Only invoked once per 15 minutes in doze mode
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + GpsPreferences.minimumInterval * 1000,
                pi
            )
        } else {
            alarmManager[AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + GpsPreferences.minimumInterval * 1000] =
                pi
        }
    }

    fun setSatelliteInfo(count: Int) {
        manager.setVisibleSatelliteCount(count)
        GlobalBus.post(SatellitesVisible(count))
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }

    override fun onSensorChanged(sensorEvent: SensorEvent?) {
        var steps = 0f
        if (sensorEvent != null) {
            if (stepCountOffset == 0f) {
                // store steps previously recorded by the system
                stepCountOffset =
                    (sensorEvent.values[0] - 1) - manager.track.stepCount // subtract any steps recorded during this session in case the app was killed
            }
            // calculate step count - subtract steps previously recorded
            steps = sensorEvent.values[0] - stepCountOffset
        }
        // update step count in track
        manager.track.stepCount = steps
    }

    fun resumeTracking() {
        // load temp track - returns an empty track if not available
        manager.track = Track.readFromFile(FileHelper.getTempTrackFileUri(this))
        // try to mark last waypoint as stopover
        if (manager.track.wayPoints.size > 0) {
            val lastWayPointIndex = manager.track.wayPoints.size - 1
            manager.track.wayPoints[lastWayPointIndex].isStopOver = true
        }
        // set resumed flag
        resumed = true
        // calculate length of recording break
        manager.track.recordingPaused =
            manager.track.recordingPaused + Track.calculateDurationOfPause(manager.track.recordingStop)
        startTracking(newTrack = false)
    }

    fun startTracking(newTrack: Boolean = true) {
        addGpsLocationListener()
        addNetworkLocationListener()
        if (newTrack) {
            manager.track = Track()
            manager.track.recordingStart = GregorianCalendar.getInstance().time
            manager.track.recordingStop = manager.track.recordingStart
            manager.track.name = DateTimeHelper.convertToReadableDate(manager.track.recordingStart)
            stepCountOffset = 0f
        }
        saveTrackingState(Keys.STATE_TRACKING_ACTIVE)
        // start recording steps and location fixes
        val stepCounterAvailable = sensorManager.registerListener(
            this,
            sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER),
            SensorManager.SENSOR_DELAY_UI
        )
        if (!stepCounterAvailable) {
            manager.track.stepCount = -1f
        }

        // ...
        startForegroundSafe()

        notifyClientsStarted(true)
        startPassiveManager()
        checkGpsManagers()

        manager.isServiceStarted = true

        handler.postDelayed(periodicTrackUpdate, 0)
    }

    private fun startForegroundSafe() {
        try {
            startForeground(
                Keys.TRACKER_SERVICE_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    fun stopTracking() {
        // save temp track
        manager.track.recordingStop = GregorianCalendar.getInstance().time
        saveTrack()
        saveTrackingState(Keys.STATE_TRACKING_STOPPED)
        // stop recording steps and location fixes
        sensorManager.unregisterListener(this)
        handler.removeCallbacks(periodicTrackUpdate)

        // ...
        manager.previousLocationInfo = null
        manager.isServiceStarted = false
        manager.latestTimeStamp = 0
        stopAbsoluteTimer()

        if (alarmIntent != null) {
            val sender =
                PendingIntent.getBroadcast(this, 0, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT)
            alarmManager.cancel(sender)
        }

        manager.currentLocationInfo = null
        stopForeground(true)

        val i = Intent(this, GpsService::class.java)
        i.putExtra(IntentConstants.GET_NEXT_POINT, true)
        val pi = PendingIntent.getService(this, 0, i, PendingIntent.FLAG_IMMUTABLE)
        alarmManager.cancel(pi)

        if (GpsPreferences.usePassiveLocations) {
            locationManager.removeUpdates(passiveLocationListener!!)
        }
        notifyClientsStarted(false)
    }

    fun clearTrack() {
        manager.track = Track()
        FileHelper.deleteTempFile(this)
        saveTrackingState(Keys.STATE_TRACKING_IDLE)
    }

    private fun saveTrackingState(state: Int) {
        manager.trackingState = state
        PreferencesHelper.saveTrackingState(manager.trackingState)
    }

    fun saveTrack() {
        CoroutineScope(Dispatchers.IO).launch {
            Track.saveTempTrackSuspended(
                this@GpsService,
                manager.track
            )
        }
    }
}
