package tech.nagual.phoenix.tools.gps.tracklist

import YesNoDialog
import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import tech.nagual.app.BaseNavigationFragment
import me.zhanghai.android.files.ui.OverlayToolbarActionMode
import me.zhanghai.android.files.util.fadeToVisibilityUnsafe
import tech.nagual.phoenix.R
import tech.nagual.common.databinding.GenericAppBarLayoutBinding
import tech.nagual.phoenix.databinding.GpsTrackListFragmentBinding
import tech.nagual.phoenix.tools.gps.Keys
import tech.nagual.phoenix.tools.gps.data.Track
import tech.nagual.phoenix.tools.gps.helpers.LengthUnitHelper
import tech.nagual.phoenix.tools.gps.helpers.UiHelper
import tech.nagual.phoenix.tools.organizer.utils.viewBinding

class TrackListFragment : BaseNavigationFragment(R.layout.gps_track_list_fragment),
    TrackListAdapter.TrackListAdapterListener,
    YesNoDialog.YesNoDialogListener {
    private val binding by viewBinding(GpsTrackListFragmentBinding::bind)

    private lateinit var tracklistAdapter: TrackListAdapter
    private lateinit var trackElementList: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tracklistAdapter = TrackListAdapter(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val appBarBinding = GenericAppBarLayoutBinding.bind(binding.root)
        appBarLayout = appBarBinding.appBarLayout
        toolbar = appBarBinding.toolbar
        toolbarTitle = getString(R.string.gps_tracks)
        super.onViewCreated(view, savedInstanceState)
        overlayActionMode = OverlayToolbarActionMode(appBarBinding.overlayToolbar)

        trackElementList = binding.trackElementList

        // set up recycler view
        trackElementList.layoutManager = CustomLinearLayoutManager(activity as Context)
        trackElementList.itemAnimator = DefaultItemAnimator()
        trackElementList.adapter = tracklistAdapter

        // enable swipe to delete
        val swipeHandler = object : UiHelper.SwipeToDeleteCallback(activity as Context) {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // ask user
                val adapterPosition: Int =
                    viewHolder.adapterPosition // first position in list is reserved for statistics
                val dialogMessage: String =
                    "${getString(R.string.trackbook_dialog_yes_no_message_delete_recording)}\n\n- ${
                        tracklistAdapter.getTrackName(adapterPosition)
                    }"
                YesNoDialog(this@TrackListFragment as YesNoDialog.YesNoDialogListener).show(
                    context = activity as Context,
                    type = Keys.DIALOG_DELETE_TRACK,
                    messageString = dialogMessage,
                    yesButton = R.string.trackbook_dialog_yes_no_positive_button_delete_recording,
                    payload = adapterPosition
                )
            }
        }
        val itemTouchHelper = ItemTouchHelper(swipeHandler)
        itemTouchHelper.attachToRecyclerView(binding.trackElementList)

        toolbar.subtitle = this.getString(
            R.string.gps_total_distance,
            LengthUnitHelper.convertDistanceToString(
                tracklistAdapter.trackList.totalDistance,
                false
            )
        )
        toggleOnboardingLayout()
    }

    override fun onTrackElementTapped(trackListElement: Track) {
        val bundle: Bundle = bundleOf(
            Keys.ARG_TRACK_TITLE to trackListElement.name,
            Keys.ARG_TRACK_FILE_URI to trackListElement.trackUriString,
            Keys.ARG_GPX_FILE_URI to trackListElement.gpxUriString,
            Keys.ARG_TRACK_ID to trackListElement.id
        )
        findNavController().navigate(R.id.gps_track_fragment, bundle)
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
                        toggleOnboardingLayout()
                        tracklistAdapter.removeTrackAtPosition(activity as Context, payload)
                    }
                    // user tapped cancel
                    false -> {
                        tracklistAdapter.notifyItemChanged(payload)
                    }
                }
            }
        }
    }

    private fun toggleOnboardingLayout() {
        binding.emptyView.fadeToVisibilityUnsafe(tracklistAdapter.itemCount == 0)
        when (tracklistAdapter.isEmpty()) {
            true -> {
                trackElementList.visibility = View.GONE
            }
            false -> {
                trackElementList.visibility = View.VISIBLE
            }
        }
    }

    inner class CustomLinearLayoutManager(context: Context) :
        LinearLayoutManager(context, VERTICAL, false) {

        override fun supportsPredictiveItemAnimations(): Boolean {
            return true
        }

        override fun onLayoutCompleted(state: RecyclerView.State?) {
            super.onLayoutCompleted(state)
            // handle delete request from TrackFragment - after layout calculations are complete
            val deleteTrackId: Long = arguments?.getLong(Keys.ARG_TRACK_ID, -1L) ?: -1L
            arguments?.putLong(Keys.ARG_TRACK_ID, -1L)
            if (deleteTrackId != -1L) {
                CoroutineScope(Dispatchers.Main).launch {
                    tracklistAdapter.removeTrackById(
                        deleteTrackId
                    )
                    toggleOnboardingLayout()
                }
            }
        }
    }
}
