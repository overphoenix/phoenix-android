package tech.nagual.phoenix.tools.organizer.archive

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
import dagger.hilt.android.AndroidEntryPoint
import me.zhanghai.android.files.ui.OverlayToolbarActionMode
import tech.nagual.phoenix.R
import tech.nagual.common.databinding.GenericAppBarLayoutBinding
import tech.nagual.phoenix.databinding.OrganizerArchiveFragmentBinding
import tech.nagual.phoenix.databinding.OrganizerNoteItemBinding
import tech.nagual.phoenix.tools.organizer.common.AbstractNotesFragment
import tech.nagual.phoenix.tools.organizer.utils.navigateSafely
import tech.nagual.phoenix.tools.organizer.utils.viewBinding

@AndroidEntryPoint
class ArchiveFragment : AbstractNotesFragment(R.layout.organizer_archive_fragment) {
    private val binding by viewBinding(OrganizerArchiveFragmentBinding::bind)

    override val currentDestinationId: Int = R.id.ogranizer_archive_fragment
    override val model: ArchiveViewModel by viewModels()

    override val recyclerView: RecyclerView
        get() = binding.recyclerArchive
    override val swipeRefreshLayout: SwipeRefreshLayout
        get() = binding.layoutSwipeRefresh
    override val snackbarLayout: View
        get() = binding.layoutCoordinator
    override val emptyIndicator: TextView
        get() = binding.emptyView
    override val overlayMenuRes: Int = R.menu.archive_selected_notes

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val appBarBinding = GenericAppBarLayoutBinding.bind(binding.root)
        appBarLayout = appBarBinding.appBarLayout
        toolbar = appBarBinding.toolbar
        toolbarTitle = getString(R.string.notes_archive_title)
        super.onViewCreated(view, savedInstanceState)
        overlayActionMode = OverlayToolbarActionMode(appBarBinding.overlayToolbar)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.archive, menu)
        mainMenu = menu
        setHiddenNotesItemActionText()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_search -> findNavController().navigateSafely(ArchiveFragmentDirections.actionArchiveToSearch())
            R.id.action_show_hidden_notes -> toggleHiddenNotes()
            R.id.action_select_all -> selectAllNotes()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onNoteClick(noteId: Long, position: Int, viewBinding: OrganizerNoteItemBinding) {
        applyNavToEditorAnimation(position)
        findNavController().navigateSafely(
            ArchiveFragmentDirections.actionArchiveToEditor(
                "editor_$noteId"
            )
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
}
