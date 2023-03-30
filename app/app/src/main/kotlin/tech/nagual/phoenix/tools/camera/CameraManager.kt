package tech.nagual.phoenix.tools.camera

import android.Manifest
import android.content.pm.PackageManager
import android.location.Criteria
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.annotation.RequiresPermission
import tech.nagual.app.application
import tech.nagual.phoenix.tools.camera.ui.activities.CameraActivity

class CameraManager {

    private var activity: CameraActivity? = null
    private var location: Location? = null

    private var isLocationFetchInProgress = false

    private val locationManager by lazy {
        application.getSystemService(LocationManager::class.java)!!
    }
    private val providerType by lazy {
        locationManager.getBestProvider(Criteria(), true) ?: LocationManager.GPS_PROVIDER
    }
    private val locationListener: LocationListener by lazy {
        object : LocationListener {
            override fun onLocationChanged(changedLocation: Location) {
                location = listOf(location, changedLocation).getAccurateOne()
            }

            override fun onProviderDisabled(provider: String) {
                super.onProviderDisabled(provider)
                if (!isAnyLocationProvideActive()) {
                    activity?.indicateLocationProvidedIsDisabled()
                }
            }

            override fun onLocationChanged(locations: MutableList<Location>) {
                super.onLocationChanged(locations)
                val location = locations.getAccurateOne()
                if (location != null) {
                    this@CameraManager.location = location
                }
            }

        }
    }

    private val activityLifeCycleHelper by lazy {
        ActivityLifeCycleHelper { activity ->
            if (activity is CameraActivity) {
                this.activity = activity
            }
        }
    }

    fun isAnyLocationProvideActive(): Boolean {
        if (!locationManager.isLocationEnabled) return false
        val providers = locationManager.allProviders

        providers.forEach {
            if (locationManager.isProviderEnabled(it)) return true
        }
        return false
    }

    fun List<Location?>.getAccurateOne(): Location? {
        if (isNullOrEmpty()) return null

        var lastBestAccuracy = 0f
        var response: Location? = null
        forEach { location ->
            if (location != null && location.accuracy > lastBestAccuracy) {
                lastBestAccuracy = location.accuracy
                response = location
            }
        }
        return response
    }

    fun init() {
        application.registerActivityLifecycleCallbacks(activityLifeCycleHelper)
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    fun requestLocationUpdates(reAttach: Boolean = false) {
        if (!isLocationEnabled()) {
            activity?.indicateLocationProvidedIsDisabled()
        }
        if (isLocationFetchInProgress) {
            if (!reAttach) return
            dropLocationUpdates()
        }
        isLocationFetchInProgress = true
        if (location == null) {
            val providers = locationManager.allProviders
            val locations = providers.map {
                locationManager.getLastKnownLocation(it)
            }
            val fetchedLocation = locations.getAccurateOne()
            if (fetchedLocation != null) {
                location = fetchedLocation
            }
        }

        locationManager.requestLocationUpdates(
            providerType,
            2000,
            10f,
            locationListener
        )
    }

    fun dropLocationUpdates() {
        isLocationFetchInProgress = false
        locationManager.removeUpdates(locationListener)
    }

    fun getLocation(): Location? = location

    private fun isLocationEnabled(): Boolean = locationManager.isLocationEnabled

    fun shouldAskForLocationPermission() =
        application.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                application.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED

//    override fun onTerminate() {
//        super.onTerminate()
//        unregisterActivityLifecycleCallbacks(activityLifeCycleHelper)
//    }

    companion object {
        private lateinit var INSTANCE: CameraManager
        fun getInstance(): CameraManager {
            if (!this::INSTANCE.isInitialized) {
                synchronized(CameraManager::class.java) {
                    if (!this::INSTANCE.isInitialized) {
                        INSTANCE = CameraManager()
                    }
                }
            }
            return INSTANCE
        }
    }
}