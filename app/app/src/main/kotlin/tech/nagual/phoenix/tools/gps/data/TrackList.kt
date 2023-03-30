package tech.nagual.phoenix.tools.gps.data

import android.os.Parcelable
import androidx.annotation.Keep
import com.google.gson.annotations.Expose
import kotlinx.parcelize.Parcelize
import java.util.*

@Keep
@Parcelize
data class TrackList(
    @Expose val tracks: MutableList<Track> = mutableListOf(),
    @Expose var modificationDate: Date = Date(),
    @Expose var totalDistance: Double = 0.0,
    @Expose var totalDuration: Long = 0L,
    @Expose var totalRecordingPaused: Long = 0L,
    @Expose var totalStepCount: Float = 0f
) : Parcelable {

    fun getTrack(trackId: Long): Track? = tracks.find { it.id == trackId }
}
