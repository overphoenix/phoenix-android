package tech.nagual.phoenix.tools.gps.helpers

import android.content.Context
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.location.Location
import android.os.Vibrator
import android.widget.Toast
import androidx.core.content.ContextCompat
import org.osmdroid.api.IGeoPoint
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.ItemizedIconOverlay
import org.osmdroid.views.overlay.OverlayItem
import org.osmdroid.views.overlay.simplefastpoint.LabelledGeoPoint
import org.osmdroid.views.overlay.simplefastpoint.SimpleFastPointOverlay
import org.osmdroid.views.overlay.simplefastpoint.SimpleFastPointOverlayOptions
import org.osmdroid.views.overlay.simplefastpoint.SimplePointTheme
import me.zhanghai.android.files.util.getColorByAttr
import tech.nagual.common.R
import tech.nagual.phoenix.tools.gps.Keys
import tech.nagual.phoenix.tools.gps.data.Track
import tech.nagual.phoenix.tools.gps.data.WayPoint
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

class MapOverlayHelper(private var markerListener: MarkerListener) {

    /* Interface used to communicate back to activity/fragment */
    interface MarkerListener {
        fun onMarkerTapped(latitude: Double, longitude: Double) {
        }
    }
    
    fun createMyLocationOverlay(
        context: Context,
        location: Location,
        trackingState: Int
    ): ItemizedIconOverlay<OverlayItem> {

        val overlayItems: ArrayList<OverlayItem> = arrayListOf()
        val locationIsOld: Boolean = LocationHelper.isOldLocation(location)

        val newMarker: Drawable
        when (trackingState) {
            Keys.STATE_TRACKING_ACTIVE -> {
                newMarker = when (locationIsOld) {
                    true -> ContextCompat.getDrawable(
                        context,
                        R.drawable.gps_marker_location_icon_red_grey_24dp
                    )!!
                    false -> ContextCompat.getDrawable(
                        context,
                        R.drawable.gps_marker_location_icon_red_24dp
                    )!!
                }
            }
            else -> {
                newMarker = when (locationIsOld) {
                    true -> ContextCompat.getDrawable(
                        context,
                        R.drawable.gps_marker_location_icon_blue_grey_24dp
                    )!!
                    false -> ContextCompat.getDrawable(
                        context,
                        R.drawable.gps_marker_location_icon_blue_24dp
                    )!!
                }
            }
        }

        // add marker to list of overlay items
        val overlayItem: OverlayItem = createOverlayItem(
            context,
            location.latitude,
            location.longitude,
            location.accuracy,
            location.provider,
            location.time
        )
        overlayItem.setMarker(newMarker)
        overlayItems.add(overlayItem)

        // create and return overlay for current position
        return createOverlay(context, overlayItems, enableStarring = false)
    }

    /* Creates icon overlay for track */
    fun createTrackOverlay(
        context: Context,
        track: Track,
        trackingState: Int
    ): SimpleFastPointOverlay {
        // get marker color
        val color =
            if (trackingState == Keys.STATE_TRACKING_ACTIVE) context.getColor(R.color.material_red_600)
            else context.getColorByAttr(R.attr.colorPrimary)
        // gather points for overlay
        val points: MutableList<IGeoPoint> = mutableListOf()
        track.wayPoints.forEach { wayPoint ->
            val label: String =
                "${context.getString(R.string.trackbook_marker_description_time)}: ${
                    SimpleDateFormat.getTimeInstance(
                        SimpleDateFormat.MEDIUM,
                        Locale.getDefault()
                    ).format(wayPoint.time)
                } | ${context.getString(R.string.trackbook_marker_description_accuracy)}: ${
                    DecimalFormat(
                        "#0.00"
                    ).format(wayPoint.accuracy)
                } (${wayPoint.provider})"
            // only add normal points
            if (!wayPoint.starred && !wayPoint.isStopOver) {
                points.add(
                    LabelledGeoPoint(
                        wayPoint.latitude,
                        wayPoint.longitude,
                        wayPoint.altitude,
                        label
                    )
                )
            }
        }
        val pointTheme: SimplePointTheme = SimplePointTheme(points, false)
        // set styling for overlay
        val style: Paint = Paint()
        style.style = Paint.Style.FILL
        style.color = color
        style.flags = Paint.ANTI_ALIAS_FLAG
        val scalingFactor: Float = UiHelper.getDensityScalingFactor(context)
        val overlayOptions: SimpleFastPointOverlayOptions =
            SimpleFastPointOverlayOptions.getDefaultStyle()
                .setAlgorithm(SimpleFastPointOverlayOptions.RenderingAlgorithm.MAXIMUM_OPTIMIZATION)
                .setSymbol(SimpleFastPointOverlayOptions.Shape.CIRCLE)
                .setPointStyle(style)
                .setRadius(6F * scalingFactor) // radius is set in px - scaling factor makes that display density independent (= dp)
                .setIsClickable(true)
//                .setCellSize(15) // Sets the grid cell size used for indexing, in pixels. Larger cells result in faster rendering speed, but worse fidelity. Default is 10 pixels, for large datasets (>10k points), use 15.
        // create and return overlay
        val overlay: SimpleFastPointOverlay = SimpleFastPointOverlay(pointTheme, overlayOptions)
        overlay.setOnClickListener(object : SimpleFastPointOverlay.OnClickListener {
            override fun onClick(points: SimpleFastPointOverlay.PointAdapter?, point: Int?) {
                if (points != null && point != null) {
                    val markerPoint: IGeoPoint = points.get(point)
                    markerListener.onMarkerTapped(markerPoint.latitude, markerPoint.longitude)
                }
            }
        })
        return overlay
    }

    /* Creates overlay containing start, stop, stopover and starred markers for track */
    fun createSpecialMakersTrackOverlay(
        context: Context,
        track: Track,
        trackingState: Int,
        displayStartEndMarker: Boolean = false
    ): ItemizedIconOverlay<OverlayItem> {
        val overlayItems: ArrayList<OverlayItem> = ArrayList<OverlayItem>()
        val trackingActive: Boolean = trackingState == Keys.STATE_TRACKING_ACTIVE
        val maxIndex: Int = track.wayPoints.size - 1

        track.wayPoints.forEachIndexed { index: Int, wayPoint: WayPoint ->
            var overlayItem: OverlayItem? = null
            if (!trackingActive && index == 0 && displayStartEndMarker && wayPoint.starred) {
                overlayItem = createOverlayItem(
                    context,
                    wayPoint.latitude,
                    wayPoint.longitude,
                    wayPoint.accuracy,
                    wayPoint.provider,
                    wayPoint.time
                )
                overlayItem.setMarker(
                    ContextCompat.getDrawable(
                        context,
                        R.drawable.trackbook_ic_marker_track_start_starred_blue_48dp
                    )!!
                )
            } else if (!trackingActive && index == 0 && displayStartEndMarker && !wayPoint.starred) {
                overlayItem = createOverlayItem(
                    context,
                    wayPoint.latitude,
                    wayPoint.longitude,
                    wayPoint.accuracy,
                    wayPoint.provider,
                    wayPoint.time
                )
                overlayItem.setMarker(
                    ContextCompat.getDrawable(
                        context,
                        R.drawable.trackbook_ic_marker_track_start_blue_48dp
                    )!!
                )
            } else if (!trackingActive && index == maxIndex && displayStartEndMarker && wayPoint.starred) {
                overlayItem = createOverlayItem(
                    context,
                    wayPoint.latitude,
                    wayPoint.longitude,
                    wayPoint.accuracy,
                    wayPoint.provider,
                    wayPoint.time
                )
                overlayItem.setMarker(
                    ContextCompat.getDrawable(
                        context,
                        R.drawable.trackbook_ic_marker_track_end_starred_blue_48dp
                    )!!
                )
            } else if (!trackingActive && index == maxIndex && displayStartEndMarker && !wayPoint.starred) {
                overlayItem = createOverlayItem(
                    context,
                    wayPoint.latitude,
                    wayPoint.longitude,
                    wayPoint.accuracy,
                    wayPoint.provider,
                    wayPoint.time
                )
                overlayItem.setMarker(
                    ContextCompat.getDrawable(
                        context,
                        R.drawable.trackbook_ic_marker_track_end_blue_48dp
                    )!!
                )
            } else if (!trackingActive && wayPoint.starred) {
                overlayItem = createOverlayItem(
                    context,
                    wayPoint.latitude,
                    wayPoint.longitude,
                    wayPoint.accuracy,
                    wayPoint.provider,
                    wayPoint.time
                )
                overlayItem.setMarker(
                    ContextCompat.getDrawable(
                        context,
                        R.drawable.trackbook_ic_star_blue_24dp
                    )!!
                )
            } else if (trackingActive && wayPoint.starred) {
                overlayItem = createOverlayItem(
                    context,
                    wayPoint.latitude,
                    wayPoint.longitude,
                    wayPoint.accuracy,
                    wayPoint.provider,
                    wayPoint.time
                )
                overlayItem.setMarker(
                    ContextCompat.getDrawable(
                        context,
                        R.drawable.trackbook_ic_star_red_24dp
                    )!!
                )
            } else if (wayPoint.isStopOver) {
                overlayItem = createOverlayItem(
                    context,
                    wayPoint.latitude,
                    wayPoint.longitude,
                    wayPoint.accuracy,
                    wayPoint.provider,
                    wayPoint.time
                )
                overlayItem.setMarker(
                    ContextCompat.getDrawable(
                        context,
                        R.drawable.trackbook_ic_marker_track_location_grey_24dp
                    )!!
                )
            }
            // add overlay item, if it was created
            if (overlayItem != null) overlayItems.add(overlayItem)
        }
        // create and return overlay for current position
        return createOverlay(context, overlayItems, enableStarring = true)
    }

    private fun createOverlayItem(
        context: Context,
        latitude: Double,
        longitude: Double,
        accuracy: Float,
        provider: String,
        time: Long
    ): OverlayItem {
        val title = "${context.getString(R.string.trackbook_marker_description_time)}: ${
            SimpleDateFormat.getTimeInstance(
                SimpleDateFormat.MEDIUM,
                Locale.getDefault()
            ).format(time)
        }"
        //val description: String = "${context.getString(R.string.marker_description_accuracy)}: ${DecimalFormat("#0.00").format(accuracy)} (${provider})"
        val description =
            "${context.getString(R.string.trackbook_marker_description_time)}: ${
                SimpleDateFormat.getTimeInstance(
                    SimpleDateFormat.MEDIUM,
                    Locale.getDefault()
                ).format(time)
            } | ${context.getString(R.string.trackbook_marker_description_accuracy)}: ${
                DecimalFormat(
                    "#0.00"
                ).format(accuracy)
            } (${provider})"
        val position = GeoPoint(latitude, longitude)
        val item = OverlayItem(title, description, position)
        item.markerHotspot = OverlayItem.HotspotPlace.CENTER
        return item
    }

    private fun createOverlay(
        context: Context,
        overlayItems: ArrayList<OverlayItem>,
        enableStarring: Boolean
    ): ItemizedIconOverlay<OverlayItem> =
        ItemizedIconOverlay(
            context,
            overlayItems,
            object : ItemizedIconOverlay.OnItemGestureListener<OverlayItem> {
                override fun onItemSingleTapUp(index: Int, item: OverlayItem): Boolean {
                    return if (enableStarring) {
                        markerListener.onMarkerTapped(item.point.latitude, item.point.longitude)
                        true
                    } else {
                        false
                    }
                }

                override fun onItemLongPress(index: Int, item: OverlayItem): Boolean {
                    val v = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                    v.vibrate(50)
                    Toast.makeText(context, item.snippet, Toast.LENGTH_LONG).show()
                    return true
                }
            })
}
