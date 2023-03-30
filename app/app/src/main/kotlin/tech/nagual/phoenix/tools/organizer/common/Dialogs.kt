package tech.nagual.phoenix.tools.organizer.common

import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import tech.nagual.phoenix.R
import tech.nagual.phoenix.tools.organizer.data.model.Note
import tech.nagual.phoenix.tools.organizer.utils.navigateSafely

fun BaseFragment.showMoveToFolderDialog(vararg notes: Note) {
    lifecycleScope.launch {
        val folders = activityModel.folderRepository.getAll().first()
        var selected = 0
        val notebooksMap: MutableMap<Long?, String> =
            mutableMapOf(null to requireContext().getString(R.string.organizer_without_folder))

        // If notes are in the same folder (or if it's just a single note)
        // we will display the selected folder
        val notesInSameNotebook = notes.all { it.folderId == notes[0].folderId }
        folders.forEachIndexed { index, folder ->
            notebooksMap[folder.id] = folder.name
            if (notesInSameNotebook && notes[0].folderId == folder.id) selected = index + 1
        }

        val dialog = BaseDialog.build(requireContext()) {
            if (!notesInSameNotebook) setItems(notebooksMap.values.toTypedArray()) { dialog, which ->
                activityModel.moveNotes(notebooksMap.keys.toTypedArray()[which], *notes)
                dialog.dismiss()
            }
            else setSingleChoiceItems(
                notebooksMap.values.toTypedArray(),
                selected
            ) { dialog, which ->
                activityModel.moveNotes(notebooksMap.keys.elementAt(which), *notes)
                dialog.dismiss()
            }
            setTitle(requireContext().getString(R.string.move_to_action))
            setNeutralButton(requireContext().getString(R.string.organizer_manage_folders)) { dialog, which ->
                findNavController().navigateSafely(R.id.organizer_manage_notebooks_fragment)
            }
            setPositiveButton(requireContext().getString(R.string.ok)) { dialog, which -> }
        }

        dialog.show()
    }
}
