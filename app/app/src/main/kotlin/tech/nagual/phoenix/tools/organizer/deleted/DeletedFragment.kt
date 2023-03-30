package tech.nagual.phoenix.tools.organizer.deleted

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import me.zhanghai.android.files.ui.OverlayToolbarActionMode
import tech.nagual.phoenix.R
import tech.nagual.common.databinding.GenericAppBarLayoutBinding
import tech.nagual.phoenix.databinding.OrganizerDeletedFragmentBinding
import tech.nagual.phoenix.databinding.OrganizerNoteItemBinding
import tech.nagual.phoenix.tools.organizer.common.AbstractNotesFragment
import tech.nagual.phoenix.tools.organizer.common.AbstractNotesViewModel
import tech.nagual.phoenix.tools.organizer.common.BaseDialog
import tech.nagual.phoenix.tools.organizer.utils.navigateSafely
import tech.nagual.phoenix.tools.organizer.utils.viewBinding

class DeletedFragment : AbstractNotesFragment(R.layout.organizer_deleted_fragment) {
    private val binding by viewBinding(OrganizerDeletedFragmentBinding::bind)

    override val currentDestinationId: Int = R.id.organizer_deleted_fragment
    override val model: DeletedViewModel by viewModels()

    override val recyclerView: RecyclerView
        get() = binding.recyclerDeleted
    override val swipeRefreshLayout: SwipeRefreshLayout
        get() = binding.layoutSwipeRefresh
    override val snackbarLayout: View
        get() = binding.layoutCoordinator
    override val emptyIndicator: TextView
        get() = binding.emptyView
    override val overlayMenuRes = R.menu.deleted_selected_notes

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val appBarBinding = GenericAppBarLayoutBinding.bind(binding.root)
        appBarLayout = appBarBinding.appBarLayout
        toolbar = appBarBinding.toolbar
        toolbarTitle = getString(R.string.notes_deleted_title)
        super.onViewCreated(view, savedInstanceState)
        overlayActionMode = OverlayToolbarActionMode(appBarBinding.overlayToolbar)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.deleted, menu)
        mainMenu = menu
        setHiddenNotesItemActionText()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_empty_bin -> showEmptyBinDialog()
            R.id.action_show_hidden_notes -> toggleHiddenNotes()
            R.id.action_select_all -> selectAllNotes()
            R.id.action_search -> findNavController().navigateSafely(DeletedFragmentDirections.actionDeletedToSearch())
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDataChanged(data: AbstractNotesViewModel.Data) {
        super.onDataChanged(data)

        val days = data.noteDeletionTimeInDays

        emptyIndicator.text =
            if (days != 0L) getString(
                R.string.indicator_deleted_empty,
                days
            ) else getString(R.string.indicator_bin_disabled)
    }

    override fun onNoteClick(noteId: Long, position: Int, viewBinding: OrganizerNoteItemBinding) {
        applyNavToEditorAnimation(position)
        findNavController().navigateSafely(
            DeletedFragmentDirections.actionDeletedToEditor("editor_$noteId")
                .setNoteId(noteId),
            FragmentNavigatorExtras(viewBinding.root to "editor_$noteId")
        )
    }

    override fun onNoteLongClick(
        noteId: Long,
        position: Int,
        viewBinding: OrganizerNoteItemBinding
    ): Boolean {
        showMenuForNote(position)
        return true
    }

    private fun showEmptyBinDialog() {
        BaseDialog.build(requireContext()) {
            setTitle(R.string.empty_bin_warning_title)
            setMessage(R.string.empty_bin_warning_text)
            setPositiveButton(R.string.yes) { di, _ ->
                model.permanentlyDeleteNotesInBin()
                di.dismiss()
            }
            setNegativeButton(R.string.no) { di, _ -> di.dismiss() }
        }
            .show()
    }
}
