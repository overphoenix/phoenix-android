package tech.nagual.phoenix.tools.organizer.common

import android.graphics.Rect
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.activity.addCallback
import androidx.annotation.CallSuper
import androidx.annotation.LayoutRes
import androidx.core.view.doOnLayout
import androidx.fragment.app.clearFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.transition.MaterialElevationScale
import dagger.hilt.android.AndroidEntryPoint
import io.noties.markwon.Markwon
import kotlinx.coroutines.launch
import tech.nagual.common.ui.BottomSheet
import me.zhanghai.android.files.ui.ToolbarActionMode
import me.zhanghai.android.files.util.fadeToVisibilityUnsafe
import tech.nagual.phoenix.R
import tech.nagual.phoenix.databinding.OrganizerNoteItemBinding
import tech.nagual.phoenix.tools.organizer.common.recycler.NoteRecyclerAdapter
import tech.nagual.phoenix.tools.organizer.common.recycler.NoteRecyclerListener
import tech.nagual.phoenix.tools.organizer.common.recycler.onBackPressedHandler
import tech.nagual.phoenix.tools.organizer.data.model.Note
import tech.nagual.phoenix.tools.organizer.preferences.LayoutMode
import tech.nagual.phoenix.tools.organizer.utils.collect
import tech.nagual.phoenix.tools.organizer.utils.launch
import tech.nagual.phoenix.tools.organizer.utils.shareNote
import java.util.concurrent.TimeUnit
import javax.inject.Inject

private typealias Data = AbstractNotesViewModel.Data

@AndroidEntryPoint
abstract class AbstractNotesFragment(@LayoutRes resId: Int) : BaseFragment(resId) {
    abstract val currentDestinationId: Int
    abstract val recyclerView: RecyclerView
    abstract val model: AbstractNotesViewModel
    abstract val emptyIndicator: TextView
    abstract val swipeRefreshLayout: SwipeRefreshLayout

    open val isSelectionEnabled = true
    open val overlayMenuRes: Int = 0

    open val snackbarLayout: View? = null
    open val snackbarAnchor: View? = null

    protected lateinit var recyclerAdapter: NoteRecyclerAdapter
    protected val inSelectionMode get() = recyclerAdapter.selectedItemIds.isNotEmpty()
    protected var mainMenu: Menu? = null

    protected var data = Data()

    private var bottomSheet: BottomSheet? = null
    private var snackbar: Snackbar? = null
    private var showHiddenNotes: Boolean
        get() = activityModel.showHiddenNotes
        set(value) {
            activityModel.showHiddenNotes = value
            recyclerAdapter.showHiddenNotes = value
        }

    @Inject
    lateinit var markwon: Markwon

    // Bug:
    //
    // When clicking on a note but then quickly pressing the back button before the animation takes place
    // makes the toolbar unusable. This is because the editor fragment gets created and calls setupToolbar() which
    // makes the editor toolbar active. However clicking the back button before the animation means that the current
    // fragment is not destroyed yet and thus will not be recreated which means that this fragment's setupToolbar() function
    // will not be called.
    //
    // The fix:
    //
    // This fix adds an onDestinationChangedListener to the NavController, which sets the toolbar up
    // if it's navigating back to the same fragment. This listener gets removed when the view gets destroyed.
    // For this to work however, each fragment that wants to inherit from this fragment must override currentDestinationId.
    private var isListenerSet = false
    private val destinationChangedListener = object : NavController.OnDestinationChangedListener {
        override fun onDestinationChanged(
            controller: NavController,
            destination: NavDestination,
            arguments: Bundle?
        ) {
            if (destination.id == currentDestinationId) {
                setupToolbar()
                controller.removeOnDestinationChangedListener(this)
                isListenerSet = false
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        activity?.onBackPressedDispatcher?.addCallback(viewLifecycleOwner) {
            onBackPressedHandler(
                recyclerAdapter
            )
        }

        setFragmentResultListener(FRAGMENT_MESSAGE) { _, bundle ->
            val message = bundle.getString(FRAGMENT_MESSAGE) ?: return@setFragmentResultListener
            onMessageAvailable(message)
            clearFragmentResult(FRAGMENT_MESSAGE)
        }

        // Setup recycler view
        val listener = object : NoteRecyclerListener {
            override fun onItemClick(position: Int, viewBinding: OrganizerNoteItemBinding) {
                val noteId = recyclerAdapter.getItemAtPosition(position).id

                if (isSelectionEnabled && inSelectionMode) {
                    toggleNoteSelected(noteId)
                } else {
                    this@AbstractNotesFragment.onNoteClick(noteId, position, viewBinding)
                }
            }

            override fun onLongClick(
                position: Int,
                viewBinding: OrganizerNoteItemBinding
            ): Boolean {
                val noteId = recyclerAdapter.getItemAtPosition(position).id

                return if (isSelectionEnabled && inSelectionMode) {
                    false
                } else this@AbstractNotesFragment.onNoteLongClick(noteId, position, viewBinding)
            }
        }

        recyclerAdapter = NoteRecyclerAdapter(
            listener = listener,
            markwon = markwon,
        ).apply {
            stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
            showHiddenNotes = this@AbstractNotesFragment.showHiddenNotes

            setOnListChangedListener {
                emptyIndicator.fadeToVisibilityUnsafe(it.isEmpty())

                recyclerView.doOnLayout {
                    startPostponedEnterTransition()
                }
            }

            if (isSelectionEnabled) {
                enableSelection(this@AbstractNotesFragment, ::onSelectionChanged)
            }
        }

        // Configure the recycler view
        recyclerView.apply {
            adapter = recyclerAdapter

            val padding = resources.getDimensionPixelSize(R.dimen.recycler_padding) / 2
            setPadding(
                recyclerView.paddingLeft + padding,
                recyclerView.paddingTop + padding,
                recyclerView.paddingRight + padding,
                recyclerView.paddingBottom + padding,
            )

            clipToPadding = false
            clipChildren = false

            addItemDecoration(object : RecyclerView.ItemDecoration() {
                override fun getItemOffsets(
                    outRect: Rect,
                    view: View,
                    parent: RecyclerView,
                    state: RecyclerView.State
                ) {
                    outRect.set(padding, padding, padding, padding)
                }
            })
        }

        liftAppBarOnScrollFor(recyclerView)

        // Set up an observer to change the notes list whenever they update
//        model.data.collect(viewLifecycleOwner, ::onDataChanged)
        model.data.collect(viewLifecycleOwner, ::onDataChanged)

        swipeRefreshLayout.setOnRefreshListener {
            viewLifecycleOwner.lifecycleScope.launch {
                swipeRefreshLayout.isRefreshing = false
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                swipeRefreshLayout.isRefreshing = false

                activityModel
                    .discardEmptyNotesAsync()
                    .await()

                swipeRefreshLayout.isRefreshing = false
            }
        }

        postponeEnterTransition(1500L, TimeUnit.MILLISECONDS)
    }

    override fun onDestroyView() {
        // Bug fix. See the the comments at the declaration of destinationChangedListener for more info.
        isListenerSet = false
        findNavController().removeOnDestinationChangedListener(destinationChangedListener)

        recyclerAdapter.listener = null

        mainMenu = null

        snackbar?.dismiss()
        snackbar?.anchorView = null

        snackbar = null
        super.onDestroyView()
    }

    override fun onPause() {
        super.onPause()
        bottomSheet?.dismiss()
    }

    @CallSuper
    open fun onDataChanged(data: Data) {
        this.data = data

        // Submit the list to the adapter
        onNotesChanged(data.notes)

        // Update recycler layout and note order
        recyclerView.layoutManager = when (data.layoutMode) {
            LayoutMode.LIST -> LinearLayoutManager(requireContext())
            LayoutMode.GRID -> StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        }
        onLayoutModeChanged()
        onSortMethodChanged()
    }

    open fun onNotesChanged(notes: List<Note>) {
        recyclerAdapter.submitList(notes)
    }

    open fun onLayoutModeChanged() {}

    open fun onSortMethodChanged() {}

    open fun onNoteClick(noteId: Long, position: Int, viewBinding: OrganizerNoteItemBinding) {}

    open fun onNoteLongClick(noteId: Long, position: Int, viewBinding: OrganizerNoteItemBinding) =
        false

    @CallSuper
    open fun onSelectionChanged(selectedIds: List<Long>) {
        if (selectedIds.isEmpty()) {
            if (overlayActionMode.isActive) {
                overlayActionMode.finish()
            }
            return
        }
        overlayActionMode.title = getString(R.string.list_select_title_format, selectedIds.size)
        overlayActionMode.setMenuResource(overlayMenuRes)

        if (!overlayActionMode.isActive) {
            appBarLayout.setExpanded(true)
            overlayActionMode.start(object : ToolbarActionMode.Callback {
                override fun onToolbarActionModeStarted(toolbarActionMode: ToolbarActionMode) {}

                override fun onToolbarActionModeItemClicked(
                    toolbarActionMode: ToolbarActionMode,
                    item: MenuItem
                ): Boolean = onOverlayActionModeItemClicked(toolbarActionMode, item)

                override fun onToolbarActionModeFinished(toolbarActionMode: ToolbarActionMode) {
                    onOverlayActionModeFinished(toolbarActionMode)
                }
            })
        }
    }

    @CallSuper
    open fun onMessageAvailable(text: CharSequence) {
        snackbar =
            Snackbar.make(snackbarLayout ?: return, text, BaseTransientBottomBar.LENGTH_SHORT)
                .apply {
                    if (snackbarAnchor != null) anchorView = snackbarAnchor

                    // setAnchorView() causes the snackbar to leak the root layout
                    // this seems to be fixing it
                    addCallback(object : BaseTransientBottomBar.BaseCallback<Snackbar>() {
                        override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                            super.onDismissed(transientBottomBar, event)
                            snackbar?.anchorView = null
                            snackbar = null
                        }
                    })
                }
        snackbar?.show()
    }

    private fun onOverlayActionModeItemClicked(
        toolbarActionMode: ToolbarActionMode,
        item: MenuItem
    ): Boolean {
        var clearSelectionAfterAction = true
        val selectedNotes = recyclerAdapter.getSelectedItems().toTypedArray()

        when (item.itemId) {
            R.id.action_pin_selected -> activityModel.pinNotes(*selectedNotes)
            R.id.action_archive_selected -> {
                activityModel.archiveNotes(*selectedNotes)
                sendMessage(
                    if (selectedNotes.size > 1) getString(R.string.indicator_archived_notes) else getString(
                        R.string.indicator_archive_note
                    )
                )
            }
            R.id.action_unarchive -> activityModel.unarchiveNotes(*selectedNotes)
            R.id.action_restore_selected -> activityModel.restoreNotes(*selectedNotes)
            R.id.action_delete_selected -> {
                activityModel.deleteNotes(*selectedNotes)
                lifecycleScope.launch {
                    if (data.noteDeletionTimeInDays == 0L) {
                        sendMessage(getString(R.string.indicator_deleted_notes_permanently))
                    } else {
                        sendMessage(getString(R.string.indicator_moved_notes_to_bin))
                    }
                }
            }
            R.id.action_delete_permanently_selected -> {
                activityModel.deleteNotesPermanently(*selectedNotes)
                sendMessage(
                    if (selectedNotes.size > 1) getString(R.string.indicator_deleted_notes_permanently) else getString(
                        R.string.indicator_deleted_note_permanently
                    )
                )
            }
            R.id.action_hide_selected -> activityModel.hideNotes(*selectedNotes)
//            R.id.action_duplicate_selected -> activityModel.duplicateNotes(*selectedNotes)
            R.id.action_move_selected -> showMoveToFolderDialog(*selectedNotes)
            R.id.action_export_selected -> {
                activityModel.notesToProcess = selectedNotes.toSet()
                exportNotesLauncher.launch()
            }
            R.id.action_select_all -> {
                selectAllNotes()
                clearSelectionAfterAction = false
            }
        }
        if (clearSelectionAfterAction) clearSelection()
        return true
    }

    private fun onOverlayActionModeFinished(toolbarActionMode: ToolbarActionMode) {
        clearSelection()
    }

    fun toggleNoteSelected(id: Long) {
        recyclerAdapter.toggleSelectionForItem(id)
    }

    private fun clearSelection() {
        recyclerAdapter.clearSelection()
    }

    protected fun selectAllNotes() {
        recyclerAdapter.selectAll()
    }

    protected fun toggleLayoutMode() {
        when (data.layoutMode) {
            LayoutMode.LIST -> activityModel.setLayoutMode(LayoutMode.GRID)
            LayoutMode.GRID -> activityModel.setLayoutMode(LayoutMode.LIST)
        }
    }

    protected fun toggleHiddenNotes() {
        showHiddenNotes = !showHiddenNotes
        setHiddenNotesItemActionText()
    }

    fun setHiddenNotesItemActionText() {
        if (hasMenu) mainMenu?.findItem(R.id.action_show_hidden_notes)?.isChecked = showHiddenNotes
    }

    protected fun applyNavToEditorAnimation(position: Int?) {
        // Bug fix. See the the comments at the declaration of destinationChangedListener for more info.
        if (!isListenerSet) {
            findNavController().addOnDestinationChangedListener(destinationChangedListener)
            isListenerSet = true
        }

        // Shared element transition causes the item to overlap with the app bar
        // By scrolling to the item position, the transition will happen inside the RV's bounds
        if (position != null) recyclerView.scrollToPosition(position)

        exitTransition = MaterialElevationScale(false).apply {
            duration = 300L
        }
        reenterTransition = MaterialElevationScale(true).apply {
            duration = 300L
        }
    }

    fun showMenuForNote(position: Int, isSelectionEnabled: Boolean = true) {
        val note = recyclerAdapter.getItemAtPosition(position)
        val isNormal = !note.isDeleted && !note.isArchived

        val title = if (note.title.isNotEmpty()) note.title else "note.id"

        bottomSheet = BottomSheet.show(note.title, parentFragmentManager) {
            action(
                R.string.unpin_title,
                R.drawable.ic_unpin,
                condition = note.isPinned && isNormal
            ) {
                activityModel.pinNotes(note)
            }
            action(R.string.pin, R.drawable.pin_icon_24dp, condition = !note.isPinned && isNormal) {
                activityModel.pinNotes(note)
            }
            action(R.string.action_show, R.drawable.ic_show, condition = note.isHidden) {
                activityModel.showNotes(note)
            }
            action(R.string.hide_action, R.drawable.ic_hidden, condition = !note.isHidden) {
                activityModel.hideNotes(note)
            }
            action(R.string.move_to_action, R.drawable.organizer_move_to_folder_icon_24dp, condition = isNormal) {
                showMoveToFolderDialog(note)
            }
            action(R.string.action_restore, R.drawable.ic_restore, condition = note.isDeleted) {
                activityModel.restoreNotes(note)
            }
            action(
                R.string.action_delete_permanently,
                R.drawable.ic_bin,
                condition = note.isDeleted
            ) {
                activityModel.deleteNotesPermanently(note)
                sendMessage(getString(R.string.indicator_deleted_note_permanently))
            }
            action(
                R.string.archive_action,
                R.drawable.ic_archive_action,
                condition = !note.isArchived && isNormal
            ) {
                activityModel.archiveNotes(note)
                sendMessage(getString(R.string.indicator_archive_note))
            }
            action(
                R.string.action_unarchive,
                R.drawable.ic_unarchive,
                condition = note.isArchived
            ) {
                activityModel.unarchiveNotes(note)
            }
            action(R.string.delete_action, R.drawable.ic_bin, condition = !note.isDeleted) {
                activityModel.deleteNotes(note)
                lifecycleScope.launch {
                    if (data.noteDeletionTimeInDays == 0L) {
                        sendMessage(getString(R.string.indicator_deleted_note_permanently))
                    } else {
                        sendMessage(getString(R.string.indicator_moved_note_to_bin))
                    }
                }
            }
//            action(
//                R.string.disable_markdown_action,
//                R.drawable.ic_markdown,
//                condition = !note.isDeleted && note.isMarkdownEnabled
//            ) {
//                activityModel.disableMarkdown(note)
//            }
//            action(
//                R.string.enable_markdown_action,
//                R.drawable.ic_markdown,
//                condition = !note.isDeleted && !note.isMarkdownEnabled
//            ) {
//                activityModel.enableMarkdown(note)
//            }
//            action(R.string.action_duplicate, R.drawable.ic_duplicate, condition = isNormal) {
//                activityModel.duplicateNotes(note)
//            }
            action(R.string.export_action, R.drawable.ic_export_note) {
                activityModel.notesToProcess = setOf(note)
                exportNotesLauncher.launch()
            }
            action(R.string.share_action, R.drawable.ic_share) {
                shareNote(requireContext(), note)
            }
            action(
                R.string.action_select_more,
                R.drawable.ic_select_more,
                condition = isSelectionEnabled
            ) {
                toggleNoteSelected(note.id)
            }
        }
    }
}
