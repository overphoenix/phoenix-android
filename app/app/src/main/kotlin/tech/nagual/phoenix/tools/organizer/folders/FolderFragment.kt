package tech.nagual.phoenix.tools.organizer.folders

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDirections
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import kotlinx.coroutines.launch
import tech.nagual.phoenix.R
import tech.nagual.phoenix.tools.organizer.MainFragment

class FolderFragment : MainFragment() {
    override val currentDestinationId: Int = R.id.organizer_folder_fragment
    private val args: FolderFragmentArgs by navArgs()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        folderId = args.folderId.takeIf { it >= 0L || it == R.id.default_notebook.toLong() }
        super.onViewCreated(view, savedInstanceState)
        updateToolbarTitle(args.folderName)
    }

    override fun actionToEditor(
        transitionName: String,
        noteId: Long
    ): NavDirections =
        FolderFragmentDirections.actionNotebookToEditor(
            transitionName
        )
            .setNoteId(noteId)
            .setNewNoteFolderId(folderId.takeUnless { it == R.id.default_notebook.toLong() }
                ?: 0L)
            .setNewNoteViewType(defaultNoteViewType)

    override fun actionToSearch(searchQuery: String) =
        FolderFragmentDirections.actionNotebookToSearch().setSearchQuery(searchQuery)

    override fun onResume() {
        super.onResume()

        // Check if folder exists in database. If it doesn't then go back
        lifecycleScope.launch {
            if (!model.notebookExists(args.folderId) && args.folderId != R.id.default_notebook.toLong()) {
                findNavController().navigateUp()
            }
        }
    }
}
