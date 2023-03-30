package tech.nagual.phoenix.tools.gps.data

import android.content.Context
import android.net.Uri
import android.os.Parcelable
import android.widget.Toast
import androidx.annotation.Keep
import androidx.core.net.toFile
import androidx.core.net.toUri
import com.google.gson.Gson
import com.google.gson.annotations.Expose
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import tech.nagual.common.R
import tech.nagual.phoenix.tools.gps.Keys
import tech.nagual.phoenix.tools.gps.helpers.FileHelper
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Keep
@Parcelize
data class Track(
    @Expose val wayPoints: MutableList<WayPoint> = mutableListOf(),
    @Expose var length: Double = 0.0,
    @Expose var duration: Long = 0L,
    @Expose var recordingPaused: Long = 0L,
    @Expose var stepCount: Float = 0f,
    @Expose var recordingStart: Date = GregorianCalendar.getInstance().time,
    @Expose var recordingStop: Date = recordingStart,
    @Expose var maxAltitude: Double = 0.0,
    @Expose var minAltitude: Double = 0.0,
    @Expose var trackUriString: String = String(),
    @Expose var gpxUriString: String = String(),
    @Expose var latitude: Double = Keys.DEFAULT_LATITUDE,
    @Expose var longitude: Double = Keys.DEFAULT_LONGITUDE,
    @Expose var zoomLevel: Double = Keys.DEFAULT_ZOOM_LEVEL,
    @Expose var name: String = String(),
    @Expose var starred: Boolean = false
) : Parcelable {
    val id: Long
        get() = recordingStart.time

    companion object {
        fun readFromFile(fileUri: Uri): Track {
            val json: String = FileHelper.readTextFile(fileUri)
            var track = Track()
            when (json.isNotEmpty()) {
                // convert JSON and return as track
                true -> try {
                    track = GsonUtil.getCustomGson().fromJson(json, Track::class.java)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            return track
        }

        private fun toJsonString(track: Track): String {
            val gson: Gson = GsonUtil.getCustomGson()
            var json = String()
            try {
                json = gson.toJson(track)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return json
        }

        private fun saveTempTrack(context: Context, track: Track) {
            val json: String = toJsonString(track)
            if (json.isNotBlank()) {
                FileHelper.writeTextFile(json, FileHelper.getTempTrackFileUri(context))
            }
        }

        fun saveTrack(track: Track, saveGpxToo: Boolean) {
            val jsonString: String = toJsonString(track)
            if (jsonString.isNotBlank()) {
                // write track file
                FileHelper.writeTextFile(jsonString, track.trackUriString.toUri())
            }
            if (saveGpxToo) {
                val gpxString: String = createGpxString(track)
                if (gpxString.isNotBlank()) {
                    // write GPX file
                    FileHelper.writeTextFile(gpxString, track.gpxUriString.toUri())
                }
            }
        }

        private fun renameTrack(track: Track, newName: String) {
            val tracklist: TrackList = FileHelper.readTrackList()
            var trackUriString = String()
            tracklist.tracks.forEach { tracklistElement ->
                if (tracklistElement.id == track.id) {
                    tracklistElement.name = newName
                    trackUriString = tracklistElement.trackUriString
                }
            }
            if (trackUriString.isNotEmpty()) {
                FileHelper.saveTracklist(tracklist, GregorianCalendar.getInstance().time)
                track.name = newName
                saveTrack(track, saveGpxToo = true)
            }
        }

        private fun deleteTrack(position: Int, tracklist: TrackList): TrackList {
            val track: Track = tracklist.tracks[position]
            // delete track files
            track.trackUriString.toUri().toFile().delete()
            track.gpxUriString.toUri().toFile().delete()
            // subtract track length from total distance
            tracklist.totalDistance -= track.length
            // remove track element from list
            tracklist.tracks.removeIf {
                it.id == track.id
            }
            FileHelper.saveTracklist(tracklist, GregorianCalendar.getInstance().time)
            return tracklist
        }

        suspend fun saveTrackSuspended(track: Track, saveGpxToo: Boolean) {
            return suspendCoroutine { cont ->
                cont.resume(saveTrack(track, saveGpxToo))
            }
        }

        suspend fun saveTempTrackSuspended(context: Context, track: Track) {
            return suspendCoroutine { cont ->
                cont.resume(saveTempTrack(context, track))
            }
        }

        suspend fun renameTrackSuspended(track: Track, newName: String) {
            return suspendCoroutine { cont ->
                cont.resume(renameTrack(track, newName))
            }
        }

        suspend fun deleteTrackSuspended(position: Int, trackList: TrackList): TrackList {
            return suspendCoroutine { cont ->
                cont.resume(deleteTrack(position, trackList))
            }
        }

        /* Calculates time passed since last stop of recording */
        fun calculateDurationOfPause(recordingStop: Date): Long =
            GregorianCalendar.getInstance().time.time - recordingStop.time


        /* Creates GPX string for given track */
        fun createGpxString(track: Track): String {
            var gpxString: String

            // add header
            gpxString = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\" ?>\n" +
                    "<gpx version=\"1.1\" creator=\"Trackbook App (Android)\"\n" +
                    "     xmlns=\"http://www.topografix.com/GPX/1/1\"\n" +
                    "     xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                    "     xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd\">\n"

            // add name
            gpxString += createGpxName(track)

            // add POIs
            gpxString += createGpxPois(track)

            // add track
            gpxString += createGpxTrk(track)

            // add closing tag
            gpxString += "</gpx>\n"

            return gpxString
        }


        /* Creates name for GPX file */
        private fun createGpxName(track: Track): String {
            val gpxName = StringBuilder("")
            gpxName.append("\t<metadata>\n")
            gpxName.append("\t\t<name>")
            gpxName.append("Trackbook Recording: ${track.name}")
            gpxName.append("</name>\n")
            gpxName.append("\t</metadata>\n")
            return gpxName.toString()
        }


        /* Creates GPX formatted points of interest */
        private fun createGpxPois(track: Track): String {
            val gpxPois = StringBuilder("")
            val poiList: List<WayPoint> = track.wayPoints.filter { it.starred }
            poiList.forEach { poi ->
                gpxPois.append("\t<wpt lat=\"")
                gpxPois.append(poi.latitude)
                gpxPois.append("\" lon=\"")
                gpxPois.append(poi.longitude)
                gpxPois.append("\">\n")

                // add name to waypoint
                gpxPois.append("\t\t<name>")
                gpxPois.append("Point of interest")
                gpxPois.append("</name>\n")

                // add altitude
                gpxPois.append("\t\t<ele>")
                gpxPois.append(poi.altitude)
                gpxPois.append("</ele>\n")

                // add closing tag
                gpxPois.append("\t</wpt>\n")
            }
            return gpxPois.toString()
        }


        /* Creates GPX formatted track */
        private fun createGpxTrk(track: Track): String {
            val gpxTrack = StringBuilder("")
            val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            dateFormat.timeZone = TimeZone.getTimeZone("UTC")

            // add opening track tag
            gpxTrack.append("\t<trk>\n")

            // add name to track
            gpxTrack.append("\t\t<name>")
            gpxTrack.append("Track")
            gpxTrack.append("</name>\n")

            // add opening track segment tag
            gpxTrack.append("\t\t<trkseg>\n")

            // add route point
            track.wayPoints.forEach { wayPoint ->
                // add longitude and latitude
                gpxTrack.append("\t\t\t<trkpt lat=\"")
                gpxTrack.append(wayPoint.latitude)
                gpxTrack.append("\" lon=\"")
                gpxTrack.append(wayPoint.longitude)
                gpxTrack.append("\">\n")

                // add altitude
                gpxTrack.append("\t\t\t\t<ele>")
                gpxTrack.append(wayPoint.altitude)
                gpxTrack.append("</ele>\n")

                // add time
                gpxTrack.append("\t\t\t\t<time>")
                gpxTrack.append(dateFormat.format(Date(wayPoint.time)))
                gpxTrack.append("</time>\n")

                // add closing tag
                gpxTrack.append("\t\t\t</trkpt>\n")
            }

            // add closing track segment tag
            gpxTrack.append("\t\t</trkseg>\n")

            // add closing track tag
            gpxTrack.append("\t</trk>\n")

            return gpxTrack.toString()
        }


        /* Toggles starred flag for given position */
        fun toggleStarred(
            context: Context,
            track: Track,
            latitude: Double,
            longitude: Double
        ): Track {
            track.wayPoints.forEach { waypoint ->
                if (waypoint.latitude == latitude && waypoint.longitude == longitude) {
                    waypoint.starred = !waypoint.starred
                    when (waypoint.starred) {
                        true -> Toast.makeText(
                            context,
                            R.string.trackbook_toast_message_poi_added,
                            Toast.LENGTH_LONG
                        ).show()
                        false -> Toast.makeText(
                            context,
                            R.string.trackbook_toast_message_poi_removed,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
            return track
        }

        /* Calculates total distance, duration and pause */
        fun calculateAndSaveTrackTotals(tracklist: TrackList) {
            CoroutineScope(Dispatchers.IO).launch {
                var totalDistanceAll = 0.0
//            var totalDurationAll: Long = 0L
//            var totalRecordingPausedAll: Long = 0L
//            var totalStepCountAll: Float = 0f
                tracklist.tracks.forEach { tracklistElement ->
                    val track: Track =
                        readFromFile(tracklistElement.trackUriString.toUri())
                    totalDistanceAll += track.length
//                totalDurationAll += track.duration
//                totalRecordingPausedAll += track.recordingPaused
//                totalStepCountAll += track.stepCount
                }
                tracklist.totalDistance = totalDistanceAll
//            tracklist.totalDurationAll = totalDurationAll
//            tracklist.totalRecordingPausedAll = totalRecordingPausedAll
//            tracklist.totalStepCountAll = totalStepCountAll
                FileHelper.saveTracklistSuspended(
                    tracklist,
                    GregorianCalendar.getInstance().time
                )
            }
        }
    }
}
