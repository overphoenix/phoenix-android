package tech.nagual.phoenix.tools.gps.tracklist

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import tech.nagual.phoenix.R
import tech.nagual.phoenix.tools.gps.Keys
import tech.nagual.phoenix.tools.gps.data.Track
import tech.nagual.phoenix.tools.gps.data.TrackList
import tech.nagual.phoenix.tools.gps.helpers.DateTimeHelper
import tech.nagual.phoenix.tools.gps.helpers.FileHelper
import tech.nagual.phoenix.tools.gps.helpers.LengthUnitHelper
import java.util.*

class TrackListAdapter(private val fragment: Fragment) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val context: Context = fragment.activity as Context
    private lateinit var trackListListener: TrackListAdapterListener
    var trackList: TrackList = TrackList()


    interface TrackListAdapterListener {
        fun onTrackElementTapped(trackListElement: Track) {}
        // fun onTrackElementStarred(trackId: Long, starred: Boolean)
    }


    /* Overrides onAttachedToRecyclerView from RecyclerView.Adapter */
    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        // get reference to listener
        trackListListener = fragment as TrackListAdapterListener
        // load tracklist
        trackList = FileHelper.readTrackList()
        trackList.tracks.sortByDescending { tracklistElement -> tracklistElement.recordingStart }
        // calculate total duration and distance, if necessary
        if (trackList.tracks.isNotEmpty() && trackList.totalDuration == 0L) {
            Track.calculateAndSaveTrackTotals(trackList)
        }
    }


    /* Overrides onCreateViewHolder from RecyclerView.Adapter */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        when (viewType) {
            else -> {
                val v = LayoutInflater.from(parent.context)
                    .inflate(R.layout.trackbook_element_track, parent, false)
                return ElementTrackViewHolder(v)
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return Keys.VIEW_TYPE_TRACK
    }


    /* Overrides getItemCount from RecyclerView.Adapter */
    override fun getItemCount(): Int = trackList.tracks.size

    /* Overrides onBindViewHolder from RecyclerView.Adapter */
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            // CASE TRACK ELEMENT
            is ElementTrackViewHolder -> {
                val positionInTracklist: Int = position
                val elementTrackViewHolder: ElementTrackViewHolder =
                    holder as ElementTrackViewHolder
                elementTrackViewHolder.trackNameView.text =
                    trackList.tracks[positionInTracklist].name
                elementTrackViewHolder.trackDataView.text =
                    createTrackDataString(positionInTracklist)
                when (trackList.tracks[positionInTracklist].starred) {
                    true -> elementTrackViewHolder.starButton.setImageDrawable(
                        ContextCompat.getDrawable(
                            context,
                            R.drawable.trackbook_ic_star_filled_24dp
                        )
                    )
                    false -> elementTrackViewHolder.starButton.setImageDrawable(
                        ContextCompat.getDrawable(
                            context,
                            R.drawable.trackbook_ic_star_outline_24dp
                        )
                    )
                }
                elementTrackViewHolder.trackElement.setOnClickListener {
                    trackListListener.onTrackElementTapped(trackList.tracks[positionInTracklist])
                }
                elementTrackViewHolder.starButton.setOnClickListener {
                    toggleStarred(it, positionInTracklist)
                }
            }
        }
    }

    /* Get track name for given position */
    fun getTrackName(positionInRecyclerView: Int): String {
        // first position is always the statistics element
        return trackList.tracks[positionInRecyclerView].name
    }

    /* Removes track and track files for given position - used by TracklistFragment */
    fun removeTrackAtPosition(context: Context, position: Int) {
        CoroutineScope(IO).launch {
            val positionInTracklist = position
            val deferred: Deferred<TrackList> =
                async { Track.deleteTrackSuspended(positionInTracklist, trackList) }
            // wait for result and store in tracklist
            withContext(Main) {
                trackList = deferred.await()
                notifyItemRemoved(position)
                notifyItemChanged(0)
            }
        }
    }

    /* Removes track and track files for given track id - used by TracklistFragment */
    fun removeTrackById(trackId: Long) {
        CoroutineScope(IO).launch {
            // reload tracklist //todo check if necessary
            trackList = FileHelper.readTrackList()
            val positionInTracklist: Int = findPosition(trackId)
            val deferred: Deferred<TrackList> =
                async { Track.deleteTrackSuspended(positionInTracklist, trackList) }
            // wait for result and store in tracklist
            withContext(Main) {
                trackList = deferred.await()
                val positionInRecyclerView: Int =
                    positionInTracklist // position 0 is the statistics element
                notifyItemRemoved(positionInRecyclerView)
            }
        }
    }

    /* Returns if the adapter is empty */
    fun isEmpty(): Boolean {
        return trackList.tracks.size == 0
    }

    /* Finds current position of track element in adapter list */
    private fun findPosition(trackId: Long): Int {
        trackList.tracks.forEachIndexed { index, track ->
            if (track.id == trackId) return index
        }
        return -1
    }

    /* Toggles the starred state of tracklist element - and saves tracklist */
    private fun toggleStarred(view: View, position: Int) {
        val starButton: ImageButton = view as ImageButton
        when (trackList.tracks[position].starred) {
            true -> {
                starButton.setImageDrawable(
                    ContextCompat.getDrawable(
                        context,
                        R.drawable.trackbook_ic_star_outline_24dp
                    )
                )
                trackList.tracks[position].starred = false
            }
            false -> {
                starButton.setImageDrawable(
                    ContextCompat.getDrawable(
                        context,
                        R.drawable.trackbook_ic_star_filled_24dp
                    )
                )
                trackList.tracks[position].starred = true
            }
        }
        CoroutineScope(Dispatchers.IO).launch {
            FileHelper.saveTracklistSuspended(
                trackList,
                GregorianCalendar.getInstance().time
            )
        }
    }

    /* Creates the track data string */
    private fun createTrackDataString(position: Int): String {
        val tracklistElement: Track = trackList.tracks[position]
        val trackDataString: String
        when (tracklistElement.name == DateTimeHelper.convertToReadableDate(tracklistElement.recordingStart)) {
            // CASE: no individual name set - exclude date
            true -> trackDataString = "${
                LengthUnitHelper.convertDistanceToString(
                    tracklistElement.length,
                    false
                )
            } • ${DateTimeHelper.convertToReadableTime(context, tracklistElement.duration)}"
            // CASE: no individual name set - include date
            false -> trackDataString =
                "${DateTimeHelper.convertToReadableDate(tracklistElement.recordingStart)} • ${
                    LengthUnitHelper.convertDistanceToString(
                        tracklistElement.length,
                        false
                    )
                } • ${DateTimeHelper.convertToReadableTime(context, tracklistElement.duration)}"
        }
        return trackDataString
    }

    /*
     * Inner class: DiffUtil.Callback that determines changes in data - improves list performance
     */
    private inner class DiffCallback(val oldList: TrackList, val newList: TrackList) :
        DiffUtil.Callback() {

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = oldList.tracks[oldItemPosition]
            val newItem = newList.tracks[newItemPosition]
            return oldItem.id == newItem.id
        }

        override fun getOldListSize(): Int {
            return oldList.tracks.size
        }

        override fun getNewListSize(): Int {
            return newList.tracks.size
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = oldList.tracks[oldItemPosition]
            val newItem = newList.tracks[newItemPosition]
            return oldItem.id == newItem.id && oldItem.length == newItem.length
        }
    }

    inner class ElementTrackViewHolder(elementTrackLayout: View) :
        RecyclerView.ViewHolder(elementTrackLayout) {
        val trackElement: ConstraintLayout = elementTrackLayout.findViewById(R.id.track_element)
        val trackNameView: TextView = elementTrackLayout.findViewById(R.id.track_name)
        val trackDataView: TextView = elementTrackLayout.findViewById(R.id.track_data)
        val starButton: ImageButton = elementTrackLayout.findViewById(R.id.star_button)
    }
}
