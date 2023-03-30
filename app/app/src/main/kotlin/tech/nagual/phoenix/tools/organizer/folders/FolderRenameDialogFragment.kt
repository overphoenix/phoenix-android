package tech.nagual.phoenix.tools.organizer.folders

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import tech.nagual.common.ui.simpledialogs.BaseMaterialDialogFragment
import me.zhanghai.android.files.util.*
import tech.nagual.common.R
import tech.nagual.phoenix.databinding.OrganizerFolderEditDialogBinding
import tech.nagual.phoenix.tools.organizer.OrganizersManager
import tech.nagual.phoenix.tools.organizer.data.model.Folder
import tech.nagual.phoenix.tools.organizer.data.repo.FolderRepository
import javax.inject.Inject

@AndroidEntryPoint
class FolderRenameDialogFragment : BaseMaterialDialogFragment() {
    private lateinit var folder: Folder
    private val args by args<Args>()

    private lateinit var binding: OrganizerFolderEditDialogBinding

    @Inject
    lateinit var folderRepository: FolderRepository

    override fun getTitle(): String {
        return getString(if (args.folder == null) R.string.organizer_folder_create_title else R.string.organizer_folder_rename_title)
    }

    override fun createView(savedInstanceState: Bundle?): View {
        binding = OrganizerFolderEditDialogBinding.inflate(requireContext().layoutInflater)
        binding.nameLayout.placeholderText = getString(R.string.organizer_default_folder_name)
        if (savedInstanceState == null && args.folder != null) {
            folder = args.folder!!
            binding.nameEdit.setTextWithSelection(
                folder.name
            )
        }

        return binding.root
    }

    override fun submit() {
        val name =
            if (binding.nameEdit.text.isNullOrEmpty()) binding.nameLayout.placeholderText.toString()
            else binding.nameEdit.text.toString()

        when {
            this::folder.isInitialized -> {
                lifecycleScope.launch(Dispatchers.IO) {
                    val exists = folderExistsByName(name, ignoreId = folder.id)
                    if (!exists) {
                        val folder = folder.copy(name = name)
                        updateFolder(folder)
                        return@launch dismiss()
                    }

                    showExistsToast(name)
                }
            }
            else -> {
                lifecycleScope.launch(Dispatchers.IO) {
                    val exists = folderExistsByName(name)
                    if (!exists) {
                        val folder = Folder(
                            name = name,
                            organizerId = OrganizersManager.activeOrganizer.id
                        )
                        insertFolder(folder)
                        return@launch dismiss()
                    }

                    showExistsToast(name)
                }
            }
        }
    }

    private suspend fun showExistsToast(name: String) {
        withContext(Dispatchers.Main) {
            Toast
                .makeText(
                    requireContext(),
                    getString(R.string.organizer_folder_already_exists, name),
                    Toast.LENGTH_SHORT
                )
                .show()
        }
    }

    private fun insertFolder(folder: Folder) {
        lifecycleScope.launch(Dispatchers.IO) {
            folderRepository.insert(folder)
        }
    }

    private fun updateFolder(folder: Folder) {
        lifecycleScope.launch(Dispatchers.IO) {
            folderRepository.update(folder)
        }
    }

    private suspend fun folderExistsByName(name: String, ignoreId: Long? = null): Boolean {
        val folder = folderRepository.getByName(name).first()
        return folder != null && (if (ignoreId != null) folder.id != ignoreId else true)
    }

    companion object {
        fun show(folder: Folder?, fragment: Fragment) {
            FolderRenameDialogFragment().putArgs(Args(folder)).show(fragment)
        }
    }

    @Parcelize
    class Args(val folder: Folder?) : ParcelableArgs
}
