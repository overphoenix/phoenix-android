package tech.nagual.phoenix.tools.gps.dashboard

import android.content.Context
import android.graphics.drawable.NinePatchDrawable
import android.location.LocationManager
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.h6ah4i.android.widget.advrecyclerview.animator.DraggableItemAnimator
import com.h6ah4i.android.widget.advrecyclerview.draggable.RecyclerViewDragDropManager
import com.h6ah4i.android.widget.advrecyclerview.utils.WrapperAdapterUtils
import tech.nagual.app.BaseNavigationFragment
import tech.nagual.common.databinding.GenericAppBarLayoutBinding
import tech.nagual.common.flowbus.EventsReceiver
import tech.nagual.common.flowbus.bindLifecycle
import tech.nagual.common.flowbus.subscribe
import me.zhanghai.android.files.ui.OverlayToolbarActionMode
import me.zhanghai.android.files.util.fadeToVisibilityUnsafe
import me.zhanghai.android.files.util.getDrawable
import tech.nagual.phoenix.R
import tech.nagual.phoenix.databinding.GpsDashboardFragmentBinding
import tech.nagual.phoenix.tools.gps.GpsManager
import tech.nagual.phoenix.tools.gps.common.ServiceEvents
import tech.nagual.phoenix.tools.gps.helpers.LocationHelper
import tech.nagual.phoenix.tools.organizer.utils.viewBinding
import java.text.DecimalFormat
import kotlin.math.roundToInt

class DashboardFragment : BaseNavigationFragment(R.layout.gps_dashboard_fragment),
    GpsIndicatorsAdapter.Listener {
    private val binding by viewBinding(GpsDashboardFragmentBinding::bind)

    private val receiver = EventsReceiver()

    private lateinit var adapter: GpsIndicatorsAdapter
    private lateinit var dragDropManager: RecyclerViewDragDropManager
    private lateinit var wrappedAdapter: RecyclerView.Adapter<*>

    private val gpsManager = GpsManager.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        clearLocationIndicators()

        receiver
            .bindLifecycle(this)
            .subscribe { event: ServiceEvents.LocationUpdate ->
                updateAll(event)
            }
            .subscribe { event: ServiceEvents.SatellitesVisible ->
                updateSatellites(event.satelliteCount)
            }
            .subscribe { event: ServiceEvents.TrackingStatus ->
                if (event.loggingStarted) {
                    clearLocationIndicators()
                } else {
                    updateSatellites(-1)
                }
            }
            .subscribe { event: ServiceEvents.LocationServicesUnavailable ->
                clearLocationIndicators()
            }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val appBarBinding = GenericAppBarLayoutBinding.bind(binding.root)
        appBarLayout = appBarBinding.appBarLayout
        toolbar = appBarBinding.toolbar
        toolbarTitle = getString(R.string.gps_dashboard)
        super.onViewCreated(view, savedInstanceState)
        overlayActionMode = OverlayToolbarActionMode(appBarBinding.overlayToolbar)

        liftAppBarOnScrollFor(binding.recyclerView)

        binding.recyclerView.layoutManager = LinearLayoutManager(
            activity, RecyclerView.VERTICAL, false
        )
        adapter = GpsIndicatorsAdapter(this)
        dragDropManager = RecyclerViewDragDropManager().apply {
            setDraggingItemShadowDrawable(
                getDrawable(R.drawable.ms9_composite_shadow_z2) as NinePatchDrawable
            )
        }

        wrappedAdapter = dragDropManager.createWrappedAdapter(adapter)
        binding.recyclerView.adapter = wrappedAdapter
        binding.recyclerView.itemAnimator = DraggableItemAnimator()
        dragDropManager.attachRecyclerView(binding.recyclerView)

        if (!gpsManager.isServiceStarted)
            clearLocationIndicators()
        GpsManager.INDICATORS.observe(viewLifecycleOwner) { onIndicatorListChanged(it) }
    }

    override fun onPause() {
        super.onPause()

        dragDropManager.cancelDrag()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        dragDropManager.release()
        WrapperAdapterUtils.releaseAll(wrappedAdapter)
    }

    private fun onIndicatorListChanged(indicators: List<GpsIndicator>) {
        binding.emptyView.fadeToVisibilityUnsafe(indicators.isEmpty())
        adapter.replace(indicators)
    }

    override fun clickIndicator(indicator: GpsIndicator) {

    }

    override fun moveIndicator(fromPosition: Int, toPosition: Int) {
        GpsIndicators.move(fromPosition, toPosition)
    }

    private fun clearLocationIndicators() {
        updateLocation("")
        updateAccuracy("")
        updateAltitude("")
        updateSpeed("")
        updateDirection("")
        updateDuration("")
        updateWaypoints("")
        updateDistance("")
        updateSatellites(-1)
    }

    private fun updateLocation(value: String) {
        GpsIndicators.replace(
            GpsManager.indicatorLocation.copy(
                value = value
            )
        )
    }

    private fun updateAccuracy(value: String) {
        GpsIndicators.replace(
            GpsManager.indicatorAccuracy.copy(
                value = value
            )
        )
    }

    private fun updateAltitude(value: String) {
        GpsIndicators.replace(
            GpsManager.indicatorAltitude.copy(
                value = value
            )
        )
    }

    private fun updateSpeed(value: String) {
        GpsIndicators.replace(
            GpsManager.indicatorSpeed.copy(
                value = value
            )
        )
    }

    private fun updateDirection(value: String) {
        GpsIndicators.replace(
            GpsManager.indicatorDirection.copy(
                value = value
            )
        )
    }

    private fun updateDuration(value: String) {
        GpsIndicators.replace(
            GpsManager.indicatorDuration.copy(
                value = value
            )
        )
    }

    private fun updateWaypoints(value: String) {
        GpsIndicators.replace(
            GpsManager.indicatorWaypoints.copy(
                value = value
            )
        )
    }

    private fun updateDistance(value: String) {
        GpsIndicators.replace(
            GpsManager.indicatorDistance.copy(
                value = value
            )
        )
    }

    fun updateSatellites(count: Int) {
        var value = if (count > -1) count.toString() else ""

        GpsIndicators.replace(
            GpsManager.indicatorSatellites.copy(
                value = value
            )
        )
    }

    private fun updateAll(event: ServiceEvents.LocationUpdate) {
        val locationInfo = event.location
        updateLocation(
            LocationHelper.getFormattedLatLon(locationInfo)
        )

        if (locationInfo.hasAccuracy()) {
            val accuracy = locationInfo.accuracy
            updateAccuracy(
                getDistanceDisplay(
                    requireContext(),
                    accuracy.toDouble(),
                    false,
                    true
                )
            )
        }

        if (locationInfo.hasAltitude()) {
            updateAltitude(
                getDistanceDisplay(
                    requireContext(),
                    locationInfo.altitude,
                    false,
                    false
                )
            )
        }

        if (locationInfo.hasSpeed()) {
            updateSpeed(
                getSpeedDisplay(
                    requireContext(),
                    locationInfo.speed.toDouble(),
                    false
                )
            )
        }

        if (locationInfo.hasBearing()) {
            updateDirection("${locationInfo.bearing.roundToInt()}${getString(R.string.gps_degree_symbol)}")
        }

        updateDuration(
            getTimeDisplay(
                requireContext(),
                gpsManager.track.duration
            )
        )

        val distanceValue = gpsManager.track.length
        updateWaypoints(gpsManager.track.wayPoints.size.toString())

        updateDistance(
            getDistanceDisplay(
                requireContext(),
                distanceValue,
                imperial = false,
                autoscale = true
            )
        )

        val providerName = locationInfo.provider
        if (!providerName.equals(LocationManager.GPS_PROVIDER, ignoreCase = true)) {
            updateSatellites(-1)
        }
    }

    private fun getSpeedDisplay(context: Context, metersPerSecond: Double, imperial: Boolean): String {
        val df = DecimalFormat("#.###")
        var result = df.format(metersPerSecond) + context.getString(R.string.gps_meters_per_second)
        if (imperial) {
            result =
                df.format(metersPerSecond * 2.23693629) + context.getString(R.string.gps_miles_per_hour)
        } else if (metersPerSecond >= 0.28) {
            result =
                df.format(metersPerSecond * 3.6) + context.getString(R.string.gps_kilometers_per_hour)
        }
        return result
    }

    private fun getDistanceDisplay(
        context: Context,
        meters: Double,
        imperial: Boolean,
        autoscale: Boolean
    ): String {
        val df = DecimalFormat("#.###")
        var result = df.format(meters) + context.getString(R.string.gps_meters)
        if (imperial) {
            result = if (!autoscale || meters <= 804) {
                df.format(meters * 3.2808399) + context.getString(R.string.gps_feet)
            } else {
                df.format(meters / 1609.344) + context.getString(R.string.gps_miles)
            }
        } else if (autoscale && meters >= 1000) {
            result = df.format(meters / 1000) + context.getString(R.string.gps_kilometers)
        }
        return result
    }

    private fun getTimeDisplay(context: Context, milliseconds: Long): String {
        val ms = milliseconds.toDouble()
        val df = DecimalFormat("#.##")
        var result = df.format(ms / 1000) + context.getString(R.string.gps_seconds)
        if (ms > 3600000) {
            result = df.format(ms / 3600000) + context.getString(R.string.gps_hours)
        } else if (ms > 60000) {
            result = df.format(ms / 60000) + context.getString(R.string.gps_minutes)
        }
        return result
    }
}
