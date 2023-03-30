package tech.nagual.phoenix.tools.gps

import YesNoDialog
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.core.content.FileProvider
import androidx.core.net.toFile
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.osmdroid.api.IGeoPoint
import org.osmdroid.api.IMapController
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
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
import tech.nagual.theme.custom.CustomThemeHelper
import me.zhanghai.android.files.ui.OverlayToolbarActionMode
import tech.nagual.phoenix.R
import tech.nagual.common.databinding.GenericAppBarLayoutBinding
import tech.nagual.phoenix.databinding.GpsTrackFragmentBinding
import tech.nagual.phoenix.tools.gps.data.Track
import tech.nagual.phoenix.tools.gps.dialogs.RenameTrackDialog
import tech.nagual.phoenix.tools.gps.helpers.FileHelper
import tech.nagual.phoenix.tools.gps.helpers.LogHelper
import tech.nagual.phoenix.tools.gps.helpers.MapOverlayHelper
import tech.nagual.phoenix.tools.organizer.utils.viewBinding

class TrackFragment : BaseNavigationFragment(R.layout.gps_track_fragment),
    RenameTrackDialog.RenameTrackListener, YesNoDialog.YesNoDialogListener,
    MapOverlayHelper.MarkerListener, MapListener {
    private val binding by viewBinding(GpsTrackFragmentBinding::bind)

    private val TAG: String = LogHelper.makeLogTag(TrackFragment::class.java)


    private lateinit var trackFileUriString: String

    //    val shareButton: ImageButton
//        get() = binding.saveButton
//    val deleteButton: ImageButton
//        get() = binding.deleteButton
//    val editButton: ImageButton
//        get() = binding.editButton
//    private val trackNameView: MaterialTextView
//        get() = binding.statisticsTrackNameHeadline
    private val mapView: MapView
        get() = binding.map
    private var trackSpecialMarkersOverlay: ItemizedIconOverlay<OverlayItem>? = null
    private var trackOverlay: SimpleFastPointOverlay? = null
    private val controller: IMapController
        get() = mapView.controller

    lateinit var track: Track

    //private var zoomLevel: Double
//    private lateinit var statisticsSheetBehavior: BottomSheetBehavior<View>
//    private lateinit var statisticsSheet: NestedScrollView
//    private lateinit var statisticsView: View
//    private lateinit var distanceView: MaterialTextView
//    private lateinit var stepsTitleView: MaterialTextView
//    private lateinit var stepsView: MaterialTextView
//    private lateinit var waypointsView: MaterialTextView
//    private lateinit var durationView: MaterialTextView
//    private lateinit var velocityView: MaterialTextView
//    private lateinit var recordingStartView: MaterialTextView
//    private lateinit var recordingStopView: MaterialTextView
//    private lateinit var recordingPausedView: MaterialTextView
//    private lateinit var recordingPausedLabelView: MaterialTextView
//    private lateinit var maxAltitudeView: MaterialTextView
//    private lateinit var minAltitudeView: MaterialTextView
//    private lateinit var positiveElevationView: MaterialTextView
//    private lateinit var negativeElevationView: MaterialTextView
//    private lateinit var elevationDataViews: Group
//    private lateinit var trackManagementViews: Group

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        trackFileUriString = arguments?.getString(Keys.ARG_TRACK_FILE_URI, String()) ?: String()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val appBarBinding = GenericAppBarLayoutBinding.bind(binding.root)
        appBarLayout = appBarBinding.appBarLayout
        toolbar = appBarBinding.toolbar
        toolbarTitle = ""
        super.onViewCreated(view, savedInstanceState)
        overlayActionMode = OverlayToolbarActionMode(appBarBinding.overlayToolbar)

        track = if (this::trackFileUriString.isInitialized && trackFileUriString.isNotBlank()) {
            Track.readFromFile(Uri.parse(trackFileUriString))
        } else {
            Track()
        }

        // basic map setup
        mapView.addMapListener(this)
        mapView.isTilesScaledToDpi = true
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        mapView.zoomController.setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER)
        controller.setCenter(GeoPoint(track.latitude, track.longitude))
        controller.setZoom(track.zoomLevel)

//        // get views for statistics sheet
//        statisticsSheet = binding.statisticsSheet
//        statisticsView = binding.statisticsView
//        distanceView = binding.statisticsDataDistance
//        stepsTitleView = binding.statisticsPSteps
//        stepsView = binding.statisticsDataSteps
//        waypointsView = binding.statisticsDataWaypoints
//        durationView = binding.statisticsDataDuration
//        velocityView = binding.statisticsDataVelocity
//        recordingStartView = binding.statisticsDataRecordingStart
//        recordingStopView = binding.statisticsDataRecordingStop
//        recordingPausedLabelView = binding.statisticsPRecordingPaused
//        recordingPausedView = binding.statisticsDataRecordingPaused
//        maxAltitudeView = binding.statisticsDataMaxAltitude
//        minAltitudeView = binding.statisticsDataMinAltitude
//        positiveElevationView = binding.statisticsDataPositiveElevation
//        negativeElevationView = binding.statisticsDataNegativeElevation
//        elevationDataViews = binding.elevationData
//        trackManagementViews = binding.managementIcons

        // set dark map tiles, if necessary
        if (CustomThemeHelper.isDarkModeOn(context as Activity)) {
            mapView.overlayManager.tilesOverlay.setColorFilter(TilesOverlay.INVERT_COLORS)
        }

        // add compass to map
        val compassOverlay =
            CompassOverlay(context, InternalCompassOrientationProvider(context), mapView)
        compassOverlay.enableCompass()
        compassOverlay.setCompassCenter(36f, 36f)
        mapView.overlays.add(compassOverlay)

        // create map overlay
        val mapOverlayHelper: MapOverlayHelper = MapOverlayHelper(this)
        trackOverlay =
            mapOverlayHelper.createTrackOverlay(requireContext(), track, Keys.STATE_TRACKING_IDLE)
        trackSpecialMarkersOverlay = mapOverlayHelper.createSpecialMakersTrackOverlay(
            requireContext(),
            track,
            Keys.STATE_TRACKING_IDLE,
            displayStartEndMarker = true
        )
        if (track.wayPoints.isNotEmpty()) {
            mapView.overlays.add(trackSpecialMarkersOverlay)
            mapView.overlays.add(trackOverlay)
        }

//        // set up and show statistics sheet
//        statisticsSheetBehavior = BottomSheetBehavior.from<View>(statisticsSheet)
//        statisticsSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
//        statisticsSheetBehavior.addBottomSheetCallback(getStatisticsSheetCallback())
        setupStatisticsViews()

//        // set up share button
//        shareButton.setOnClickListener {
//            openSaveGpxDialog()
//        }
//        layout.shareButton.setOnLongClickListener {
//            val v = (activity as Context).getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
//            v.vibrate(50)
//            shareGpxTrack()
//            return@setOnLongClickListener true
//        }
//        // set up delete button
//        deleteButton.setOnClickListener {
//            val dialogMessage: String =
//                "${getString(R.string.trackbook_dialog_yes_no_message_delete_recording)}\n\n- ${trackNameView.text}"
//            YesNoDialog(this@TrackFragment as YesNoDialog.YesNoDialogListener).show(
//                context = activity as Context,
//                type = Keys.DIALOG_DELETE_TRACK,
//                messageString = dialogMessage,
//                yesButton = R.string.trackbook_dialog_yes_no_positive_button_delete_recording
//            )
//        }
//        // set up rename button
//        editButton.setOnClickListener {
//            RenameTrackDialog(this as RenameTrackDialog.RenameTrackListener).show(
//                activity as Context,
//                trackNameView.text.toString()
//            )
//        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.gps_track_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
//        when (item.itemId) {
//            R.id.action_track_list -> findNavController().navigateSafely(R.id.gps_tracklist_fragment)
//        }
        return super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
        // save zoom level and map center
        saveViewStateToTrack()
    }

    /* Register the ActivityResultLauncher for saving GPX */
    private val requestSaveGpxLauncher =
        registerForActivityResult(StartActivityForResult(), this::requestSaveGpxResult)

    private fun requestSaveGpxResult(result: ActivityResult) {
        // save GPX file to result file location
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val sourceUri: Uri = Uri.parse(track.gpxUriString)
            val targetUri: Uri? = result.data?.data
            if (targetUri != null) {
                // copy file async (= fire & forget - no return value needed)
                CoroutineScope(Dispatchers.IO).launch {
                    FileHelper.saveCopyOfFileSuspended(
                        activity as Context,
                        originalFileUri = sourceUri,
                        targetFileUri = targetUri
                    )
                }
                Toast.makeText(
                    activity as Context,
                    R.string.trackbook_toast_message_save_gpx,
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onRenameTrackDialog(textInput: String) {
        // rename track async (= fire & forget - no return value needed)
        CoroutineScope(Dispatchers.IO).launch {
            Track.renameTrackSuspended(
                track,
                textInput
            )
        }
        // update name in layout
        track.name = textInput
        updateToolbarTitle(textInput)
//        trackNameView.text = textInput
    }

    override fun onYesNoDialog(
        type: Int,
        dialogResult: Boolean,
        payload: Int,
        payloadString: String
    ) {
        when (type) {
            Keys.DIALOG_DELETE_TRACK -> {
                when (dialogResult) {
                    // user tapped remove track
                    true -> {
                        // switch to TracklistFragment and remove track there
                        val bundle: Bundle =
                            bundleOf(Keys.ARG_TRACK_ID to track.id)
                        findNavController().navigate(R.id.gps_tracklist_fragment, bundle)
                    }
                }
            }
        }
    }


    /* Overrides onMarkerTapped from MarkerListener */
    override fun onMarkerTapped(latitude: Double, longitude: Double) {
        super.onMarkerTapped(latitude, longitude)
        // update track display
        track =
            Track.toggleStarred(activity as Context, track, latitude, longitude)
        updateTrackOverlay()
    }

    /* Opens up a file picker to select the save location */
    private fun openSaveGpxDialog() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = Keys.MIME_TYPE_GPX
            putExtra(Intent.EXTRA_TITLE, FileHelper.getGpxFileName(track))
        }
        // file gets saved in the ActivityResult
        try {
            requestSaveGpxLauncher.launch(intent)
        } catch (e: Exception) {
            LogHelper.e(TAG, "Unable to save GPX.")
            Toast.makeText(
                activity as Context,
                R.string.trackbook_toast_message_install_file_helper,
                Toast.LENGTH_LONG
            ).show()
        }
    }


    /* Share track as GPX via share sheet */
    private fun shareGpxTrack() {
        val gpxFile = Uri.parse(track.gpxUriString).toFile()
        val gpxShareUri = FileProvider.getUriForFile(
            this.activity as Context,
            "${requireActivity().applicationContext.packageName}.provider",
            gpxFile
        )
        val shareIntent: Intent = Intent.createChooser(Intent().apply {
            action = Intent.ACTION_SEND
            data = gpxShareUri
            type = Keys.MIME_TYPE_GPX
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            putExtra(Intent.EXTRA_STREAM, gpxShareUri)
        }, null)

        // show share sheet - if file helper is available
        val packageManager: PackageManager? = activity?.packageManager
        if (packageManager != null && shareIntent.resolveActivity(packageManager) != null) {
            startActivity(shareIntent)
        } else {
            Toast.makeText(
                activity,
                R.string.trackbook_toast_message_install_file_helper,
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun updateTrackOverlay() {
        if (trackOverlay != null) {
            mapView.overlays.remove(trackOverlay)
        }
        if (trackSpecialMarkersOverlay != null) {
            mapView.overlays.remove(trackSpecialMarkersOverlay)
        }
        if (track.wayPoints.isNotEmpty()) {
            val mapOverlayHelper: MapOverlayHelper = MapOverlayHelper(this)
            trackOverlay =
                mapOverlayHelper.createTrackOverlay(
                    requireContext(),
                    track,
                    Keys.STATE_TRACKING_IDLE
                )
            trackSpecialMarkersOverlay = mapOverlayHelper.createSpecialMakersTrackOverlay(
                requireContext(),
                track,
                Keys.STATE_TRACKING_IDLE,
                displayStartEndMarker = true
            )
            mapView.overlays.add(trackOverlay)
            mapView.overlays.add(trackSpecialMarkersOverlay)
        }
        // save track
        CoroutineScope(Dispatchers.IO).launch { Track.saveTrackSuspended(track, true) }
    }

    /* Saves zoom level and center of this map */
    fun saveViewStateToTrack() {
        if (track.latitude != 0.0 && track.longitude != 0.0) {
            CoroutineScope(Dispatchers.IO).launch { Track.saveTrackSuspended(track, false) }
        }
    }

    private fun setupStatisticsViews() {
//        // get step count string - hide step count if not available
//        val steps: String
//        if (track.stepCount == -1f) {
//            steps =
//                requireContext().getString(R.string.trackbook_statistics_sheet_p_steps_no_pedometer)
//            stepsTitleView.isGone = true
//            stepsView.isGone = true
//        } else {
//            steps = track.stepCount.roundToInt().toString()
//            stepsTitleView.isVisible = true
//            stepsView.isVisible = true
//        }
//
//        // populate views
        updateToolbarTitle(track.name)
//        trackNameView.text = track.name
//        distanceView.text = LengthUnitHelper.convertDistanceToString(track.length, false)
//        stepsView.text = steps
//        waypointsView.text = track.wayPoints.size.toString()
//        durationView.text = DateTimeHelper.convertToReadableTime(requireContext(), track.duration)
//        velocityView.text = LengthUnitHelper.convertToVelocityString(
//            track.duration,
//            track.recordingPaused,
//            track.length,
//            false
//        )
//        recordingStartView.text = DateTimeHelper.convertToReadableDateAndTime(track.recordingStart)
//        recordingStopView.text = DateTimeHelper.convertToReadableDateAndTime(track.recordingStop)
//        maxAltitudeView.text = LengthUnitHelper.convertDistanceToString(track.maxAltitude, false)
//        minAltitudeView.text = LengthUnitHelper.convertDistanceToString(track.minAltitude, false)
//        positiveElevationView.text =
//            LengthUnitHelper.convertDistanceToString(track.positiveElevation, false)
//        negativeElevationView.text =
//            LengthUnitHelper.convertDistanceToString(track.negativeElevation, false)
//
//        // show / hide recording pause
//        if (track.recordingPaused != 0L) {
//            recordingPausedLabelView.isVisible = true
//            recordingPausedView.isVisible = true
//            recordingPausedView.text =
//                DateTimeHelper.convertToReadableTime(requireContext(), track.recordingPaused)
//        } else {
//            recordingPausedLabelView.isGone = true
//            recordingPausedView.isGone = true
//        }
//
//        // inform user about possible accuracy issues with altitude measurements
//
//        elevationDataViews.referencedIds.forEach { id ->
//            (elevationDataViews.referencedIds[id] as View).setOnClickListener {
//                Toast.makeText(
//                    context,
//                    R.string.trackbook_toast_message_elevation_info,
//                    Toast.LENGTH_LONG
//                ).show()
//            }
//        }
//        // make track name on statistics sheet clickable
//        trackNameView.setOnClickListener {
//            toggleStatisticsSheetVisibility()
//        }
    }

//    /* Shows/hides the statistics sheet */
//    private fun toggleStatisticsSheetVisibility() {
//        when (statisticsSheetBehavior.state) {
//            BottomSheetBehavior.STATE_EXPANDED -> statisticsSheetBehavior.state =
//                BottomSheetBehavior.STATE_COLLAPSED
//            else -> statisticsSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
//        }
//    }
//
//    /* Defines the behavior of the statistics sheet  */
//    private fun getStatisticsSheetCallback(): BottomSheetBehavior.BottomSheetCallback {
//        return object : BottomSheetBehavior.BottomSheetCallback() {
//            override fun onStateChanged(bottomSheet: View, newState: Int) {
//                when (newState) {
//                    BottomSheetBehavior.STATE_EXPANDED -> {
//                        statisticsSheet.background =
//                            requireContext().getDrawable(R.drawable.trackbook_shape_statistics_background_expanded)
//                        trackManagementViews.isVisible = true
//                        shareButton.isGone = true
//                        // bottomSheet.setPadding(0,24,0,0)
//                    }
//                    else -> {
//                        statisticsSheet.background =
//                            requireContext().getDrawable(R.drawable.trackbook_shape_statistics_background_collapsed)
//                        trackManagementViews.isGone = true
//                        shareButton.isVisible = true
//                        // bottomSheet.setPadding(0,0,0,0)
//                    }
//                }
//            }
//
//            override fun onSlide(bottomSheet: View, slideOffset: Float) {
//                if (slideOffset < 0.125f) {
//                    statisticsSheet.background =
//                        requireContext().getDrawable(R.drawable.trackbook_shape_statistics_background_collapsed)
//                    trackManagementViews.isGone = true
//                    shareButton.isVisible = true
//                } else {
//                    statisticsSheet.background =
//                        requireContext().getDrawable(R.drawable.trackbook_shape_statistics_background_expanded)
//                    trackManagementViews.isVisible = true
//                    shareButton.isGone = true
//                }
//            }
//        }
//    }

    override fun onZoom(event: ZoomEvent?): Boolean {
        return if (event == null) {
            false
        } else {
            track.zoomLevel = event.zoomLevel
            true
        }
    }

    override fun onScroll(event: ScrollEvent?): Boolean {
        return if (event == null) {
            false
        } else {
            val center: IGeoPoint = mapView.mapCenter
            track.latitude = center.latitude
            track.longitude = center.longitude
            true
        }
    }
}
