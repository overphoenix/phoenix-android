package tech.nagual.phoenix.tools.gps

import YesNoDialog
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.api.IMapController
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.ItemizedIconOverlay
import org.osmdroid.views.overlay.OverlayItem
import org.osmdroid.views.overlay.TilesOverlay
import org.osmdroid.views.overlay.compass.CompassOverlay
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider
import org.osmdroid.views.overlay.simplefastpoint.SimpleFastPointOverlay
import tech.nagual.app.BaseNavigationFragment
import tech.nagual.common.databinding.GenericAppBarLayoutBinding
import tech.nagual.common.flowbus.EventsReceiver
import tech.nagual.common.flowbus.bindLifecycle
import tech.nagual.common.flowbus.subscribe
import tech.nagual.common.permissions.Permission
import tech.nagual.common.permissions.askForPermissions
import tech.nagual.common.permissions.isAllDenied
import tech.nagual.theme.custom.CustomThemeHelper
import me.zhanghai.android.files.ui.OverlayToolbarActionMode
import tech.nagual.common.ui.speeddial.SpeedDialView
import me.zhanghai.android.files.util.getColor
import me.zhanghai.android.files.util.getResourceIdByAttr
import tech.nagual.phoenix.R
import tech.nagual.phoenix.databinding.GpsMapFragmentBinding
import tech.nagual.phoenix.tools.gps.common.ServiceEvents
import tech.nagual.phoenix.tools.gps.data.Track
import tech.nagual.phoenix.tools.gps.helpers.*
import tech.nagual.phoenix.tools.organizer.utils.navigateSafely
import tech.nagual.phoenix.tools.organizer.utils.viewBinding

class MapFragment : BaseNavigationFragment(R.layout.gps_map_fragment),
    YesNoDialog.YesNoDialogListener,
    MapOverlayHelper.MarkerListener {
    private val binding by viewBinding(GpsMapFragmentBinding::bind)

    private val receiver = EventsReceiver()

    private val mapView: MapView
        get() = binding.map
    private val fabStartButton: FloatingActionButton
        get() = binding.fabStart
    private val speedDialView: tech.nagual.common.ui.speeddial.SpeedDialView
        get() = binding.speedDialView
    val currentLocationButton: FloatingActionButton
        get() = binding.fabLocation

    private val handler: Handler = Handler(Looper.getMainLooper())
    private lateinit var gpsService: GpsService
    private lateinit var markerListener: MapOverlayHelper.MarkerListener
    var userInteraction: Boolean = false
    private lateinit var currentPositionOverlay: ItemizedIconOverlay<OverlayItem>
    private var currentTrackOverlay: SimpleFastPointOverlay? = null
    private var currentTrackSpecialMarkerOverlay: ItemizedIconOverlay<OverlayItem>? = null
    private lateinit var locationErrorBar: Snackbar
    private lateinit var controller: IMapController
    private var zoomLevel: Double = PreferencesHelper.loadZoomLevel()


    private val gpsManager = GpsManager.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        gpsManager.trackingState = PreferencesHelper.loadTrackingState()

        receiver
            .bindLifecycle(this)
            .subscribe { event: ServiceEvents.LocationServicesUnavailable ->
//                toggleLocationErrorBar()
            }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val appBarBinding = GenericAppBarLayoutBinding.bind(binding.root)
        appBarLayout = appBarBinding.appBarLayout
        toolbar = appBarBinding.toolbar
        toolbarTitle = getString(R.string.gps_title)
        super.onViewCreated(view, savedInstanceState)
        overlayActionMode = OverlayToolbarActionMode(appBarBinding.overlayToolbar)

        markerListener = this
        locationErrorBar = Snackbar.make(mapView, String(), Snackbar.LENGTH_INDEFINITE)
        controller = mapView.controller

        mapView.isTilesScaledToDpi = true
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        mapView.zoomController.setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER)
        controller.setZoom(zoomLevel)

        // set dark map tiles, if necessary
        if (CustomThemeHelper.isDarkModeOn(requireContext()))
            mapView.overlayManager.tilesOverlay.setColorFilter(TilesOverlay.INVERT_COLORS)

        // add compass to map
        val compassOverlay =
            CompassOverlay(context, InternalCompassOrientationProvider(context), mapView)
        compassOverlay.enableCompass()
        compassOverlay.setCompassCenter(
            36f,
            36f
        )
        mapView.overlays.add(compassOverlay)

        currentPositionOverlay = MapOverlayHelper(markerListener).createMyLocationOverlay(
            requireContext(),
            gpsManager.currentBestLocation,
            gpsManager.trackingState
        )
        mapView.overlays.add(currentPositionOverlay)
        centerMap(gpsManager.currentBestLocation)

        currentTrackOverlay = null
        currentTrackSpecialMarkerOverlay = null

        updateFabAndSpeedDialButtons(gpsManager.trackingState)

        addInteractionListener()

        currentLocationButton.setOnClickListener {
            centerMap(gpsManager.currentBestLocation, animated = true)
        }

        fabStartButton.setOnClickListener {
            when (gpsManager.trackingState) {
                Keys.STATE_TRACKING_IDLE -> startTrackingWithPermission()
                Keys.STATE_TRACKING_ACTIVE -> {
                    gpsService.stopTracking()
                }
            }
        }
        speedDialView.inflate(R.menu.gps_speed_dial)
        speedDialView.setOnActionSelectedListener {
            when (it.id) {
                R.id.action_save -> saveTrack()
                R.id.action_clear -> gpsService.clearTrack()
                R.id.action_resume -> startTrackingWithPermission(true)
            }
            speedDialView.close()
            true
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.gps_map_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.gps_action_dashboard -> findNavController().navigateSafely(R.id.gps_dashboard_fragment)
            R.id.gps_action_track_list -> findNavController().navigateSafely(R.id.gps_tracklist_fragment)
            R.id.gps_action_settings -> findNavController().navigateSafely(R.id.gps_settings_fragment)
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onStart() {
        super.onStart()
        if (isAllDenied(Permission.ACCESS_FINE_LOCATION))
            askForPermissions(Permission.ACCESS_FINE_LOCATION) { result ->
                val isGranted: Boolean = result.isAllGranted(Permission.ACCESS_FINE_LOCATION)
                if (isGranted) {
                    gpsManager.unbindFromService(connection)
                    gpsManager.bindToService(connection)
                } else {
                    gpsManager.unbindFromService(connection)
                }
                toggleLocationErrorBar()
            }

        gpsManager.bindToService(connection)
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
//        if (bound) {
//            trackerService.addGpsLocationListener()
//            trackerService.addNetworkLocationListener()
//        }
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
        saveState(gpsManager.currentBestLocation)
    }

    override fun onStop() {
        super.onStop()
        if (gpsManager.isBoundToService) {
            gpsManager.unbindFromService(connection)
            handleServiceUnbind()
        }
    }

    /* Overrides onYesNoDialog from YesNoDialogListener */
    override fun onYesNoDialog(
        type: Int,
        dialogResult: Boolean,
        payload: Int,
        payloadString: String
    ) {
        super.onYesNoDialog(type, dialogResult, payload, payloadString)
        when (type) {
            Keys.DIALOG_EMPTY_RECORDING -> {
                when (dialogResult) {
                    // user tapped resume
                    true -> {
                        gpsService.resumeTracking()
                    }
                }
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun addInteractionListener() {
        mapView.setOnTouchListener { _, event ->
            userInteraction = true
            false
        }
    }

    fun centerMap(location: Location, animated: Boolean = false) {
        val position = GeoPoint(location.latitude, location.longitude)
        when (animated) {
            true -> controller.animateTo(position)
            false -> controller.setCenter(position)
        }
        userInteraction = false
    }

    fun saveState(currentBestLocation: Location) {
        PreferencesHelper.saveCurrentBestLocation(currentBestLocation)
        PreferencesHelper.saveZoomLevel(mapView.zoomLevelDouble)
        // reset user interaction state
        userInteraction = false
    }

    fun markCurrentPosition(location: Location, trackingState: Int = Keys.STATE_TRACKING_IDLE) {
        mapView.overlays.remove(currentPositionOverlay)
        currentPositionOverlay = MapOverlayHelper(markerListener).createMyLocationOverlay(
            requireContext(),
            location,
            trackingState
        )
        mapView.overlays.add(currentPositionOverlay)
    }

    fun overlayCurrentTrack(track: Track, trackingState: Int) {
        if (currentTrackOverlay != null) {
            mapView.overlays.remove(currentTrackOverlay)
        }
        if (currentTrackSpecialMarkerOverlay != null) {
            mapView.overlays.remove(currentTrackSpecialMarkerOverlay)
        }
        if (track.wayPoints.isNotEmpty()) {
            val mapOverlayHelper = MapOverlayHelper(markerListener)
            currentTrackOverlay =
                mapOverlayHelper.createTrackOverlay(requireContext(), track, trackingState)
            currentTrackSpecialMarkerOverlay =
                mapOverlayHelper.createSpecialMakersTrackOverlay(
                    requireContext(),
                    track,
                    trackingState
                )
            mapView.overlays.add(currentTrackSpecialMarkerOverlay)
            mapView.overlays.add(currentTrackOverlay)
        }
    }

    fun updateFabAndSpeedDialButtons(trackingState: Int) {
        when (trackingState) {
            Keys.STATE_TRACKING_IDLE -> {
                fabStartButton.isVisible = true
                fabStartButton.backgroundTintList =
                    ColorStateList.valueOf(getColor(getResourceIdByAttr(R.attr.colorPrimary)))
                speedDialView.isVisible = false
            }
            Keys.STATE_TRACKING_ACTIVE -> {
                fabStartButton.isVisible = true
                fabStartButton.backgroundTintList =
                    ColorStateList.valueOf(getColor(R.color.material_red_500))
                speedDialView.isVisible = false
            }
            Keys.STATE_TRACKING_STOPPED -> {
                fabStartButton.isVisible = false
                speedDialView.isVisible = true
            }
        }
    }

    fun toggleLocationErrorBar() {
        if (isAllDenied(Permission.ACCESS_FINE_LOCATION)) {
            locationErrorBar.setText(R.string.gps_snackbar_message_location_permission_denied)
            if (!locationErrorBar.isShown) locationErrorBar.show()
        } else if (!gpsManager.isGpsEnabled && !gpsManager.isNetworkEnabled) {
            locationErrorBar.setText(R.string.gps_snackbar_message_location_offline)
            if (!locationErrorBar.isShown) locationErrorBar.show()
        } else {
            if (locationErrorBar.isShown) locationErrorBar.dismiss()
        }
    }

    override fun onMarkerTapped(latitude: Double, longitude: Double) {
        super.onMarkerTapped(latitude, longitude)
        if (gpsManager.isBoundToService) {
            gpsManager.track =
                Track.toggleStarred(activity as Context, gpsManager.track, latitude, longitude)
            overlayCurrentTrack(gpsManager.track, gpsManager.trackingState)
        }
    }

    private fun startTrackingWithPermission(resume: Boolean = false) {
        if (isAllDenied(Permission.ACTIVITY_RECOGNITION)) {
            askForPermissions(Permission.ACTIVITY_RECOGNITION) { result ->
                startTracking(resume)
            }
        } else {
            startTracking(resume)
        }
    }

    private fun startTracking(resume: Boolean) {
        if (!gpsManager.isServiceStarted) {
            // start service via intent so that it keeps running after unbind
            gpsManager.startService()
        }

        if (resume)
            gpsService.resumeTracking()
        else
            gpsService.startTracking()
    }

    private fun handleServiceUnbind() {
        if (gpsManager.isBoundToService) {
            gpsManager.isBoundToService = false
            // unregister listener for changes in shared preferences
            PreferencesHelper.unregisterPreferenceChangeListener(sharedPreferenceChangeListener)
            // stop receiving location updates
            handler.removeCallbacks(periodicLocationRequestRunnable)
        }
    }

    private fun saveTrack() {
        if (gpsManager.track.wayPoints.isEmpty()) {
            YesNoDialog(this as YesNoDialog.YesNoDialogListener).show(
                activity as Context,
                type = Keys.DIALOG_EMPTY_RECORDING,
                title = R.string.gps_dialog_error_empty_recording_title,
                message = R.string.gps_dialog_error_empty_recording_message,
                yesButton = R.string.gps_dialog_error_empty_recording_action_resume
            )
        } else {
            CoroutineScope(Dispatchers.IO).launch {
                // step 1: create and store filenames for json and gpx files
                gpsManager.track.trackUriString =
                    FileHelper.getTrackFileUri(gpsManager.track).toString()
                gpsManager.track.gpxUriString =
                    FileHelper.getGpxFileUri(gpsManager.track).toString()
                // step 2: save track
                Track.saveTrackSuspended(gpsManager.track, saveGpxToo = true)
                // step 3: save tracklist - suspended
                FileHelper.addTrackAndSaveTracklistSuspended(gpsManager.track)
                // step 3: clear track
                gpsService.clearTrack()
                // step 4: open track in TrackFragement
                withContext(Dispatchers.Main) {
                    openTrack(gpsManager.track)
                }
            }
        }
    }

    private fun openTrack(track: Track) {
        val bundle = Bundle()
        bundle.putString(Keys.ARG_TRACK_TITLE, track.name)
        bundle.putString(Keys.ARG_TRACK_FILE_URI, track.trackUriString)
        bundle.putString(Keys.ARG_GPX_FILE_URI, track.gpxUriString)
        bundle.putLong(Keys.ARG_TRACK_ID, track.id)
        findNavController().navigate(R.id.gps_track_fragment, bundle)
    }

    private val sharedPreferenceChangeListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            when (key) {
                Keys.PREF_TRACKING_STATE -> {
                    if (activity != null) {
                        gpsManager.trackingState = PreferencesHelper.loadTrackingState()
                        updateFabAndSpeedDialButtons(gpsManager.trackingState)
                    }
                }
            }
        }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            gpsManager.isBoundToService = true
            // get reference to tracker service
            val binder = service as GpsService.GpsServiceBinder
            gpsService = binder.service
            // get state of tracking and update button if necessary
            updateFabAndSpeedDialButtons(gpsManager.trackingState)
            // register listener for changes in shared preferences
            PreferencesHelper.registerPreferenceChangeListener(sharedPreferenceChangeListener)
            // start listening for location updates
            handler.removeCallbacks(periodicLocationRequestRunnable)
            handler.postDelayed(periodicLocationRequestRunnable, 0)
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            // service has crashed, or was killed by the system
            handleServiceUnbind()
        }
    }

    private val periodicLocationRequestRunnable: Runnable = object : Runnable {
        override fun run() {
            markCurrentPosition(gpsManager.currentBestLocation, gpsManager.trackingState)
            overlayCurrentTrack(gpsManager.track, gpsManager.trackingState)
            if (!userInteraction) {
                centerMap(gpsManager.currentBestLocation, true)
            }
            toggleLocationErrorBar()
            // use the handler to start runnable again after specified delay
            handler.postDelayed(this, Keys.REQUEST_CURRENT_LOCATION_INTERVAL)
        }
    }
}
