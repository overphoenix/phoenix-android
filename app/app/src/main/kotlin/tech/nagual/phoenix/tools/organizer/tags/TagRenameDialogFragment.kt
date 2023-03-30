package tech.nagual.phoenix.tools.organizer.tags

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
import tech.nagual.phoenix.databinding.OrganizerTagEditDialogBinding
import tech.nagual.phoenix.tools.organizer.OrganizersManager
import tech.nagual.phoenix.tools.organizer.data.model.Tag
import tech.nagual.phoenix.tools.organizer.data.repo.TagRepository
import javax.inject.Inject

@AndroidEntryPoint
class TagRenameDialogFragment : BaseMaterialDialogFragment() {
    private lateinit var tag: Tag
    private val args by args<Args>()

    private lateinit var binding: OrganizerTagEditDialogBinding

    @Inject
    lateinit var tagRepository: TagRepository

    override fun getTitle(): String {
        return getString(if (args.tag == null) R.string.tag_create_title else R.string.tag_rename_title)
    }

    override fun createView(savedInstanceState: Bundle?): View {
        binding = OrganizerTagEditDialogBinding.inflate(requireContext().layoutInflater)
        binding.nameLayout.placeholderText = getString(R.string.organizer_default_tag_name)
        if (savedInstanceState == null && args.tag != null) {
            tag = args.tag!!
            binding.nameEdit.setTextWithSelection(
                tag.name
            )
        }

        return binding.root
    }

    override fun submit() {
        val name =
            if (binding.nameEdit.text.isNullOrEmpty()) binding.nameLayout.placeholderText.toString()
            else binding.nameEdit.text.toString()

        when {
            this::tag.isInitialized -> {
                lifecycleScope.launch(Dispatchers.IO) {
                    val exists = tagExistsByName(name, ignoreId = tag.id)
                    if (!exists) {
                        val tag = tag.copy(name = name)
                        updateTag(tag)
                        return@launch dismiss()
                    }

                    showExistsToast(name)
                }
            }
            else -> {
                lifecycleScope.launch(Dispatchers.IO) {
                    val exists = tagExistsByName(name)
                    if (!exists) {
                        val tag = Tag(
                            name = name,
                            organizerId = OrganizersManager.activeOrganizer.id
                        )
                        insertTag(tag)
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
                    getString(R.string.tag_already_exists, name),
                    Toast.LENGTH_SHORT
                )
                .show()
        }
    }

    private fun insertTag(tag: Tag) {
        lifecycleScope.launch(Dispatchers.IO) {
            tagRepository.insert(tag)
        }
    }

    private fun updateTag(tag: Tag) {
        lifecycleScope.launch(Dispatchers.IO) {
            tagRepository.update(tag)
        }
    }

    private suspend fun tagExistsByName(name: String, ignoreId: Long? = null): Boolean {
        val tag = tagRepository.getByName(name).first()
        return tag != null && (if (ignoreId != null) tag.id != ignoreId else true)
    }

    companion object {
        fun show(tag: Tag?, fragment: Fragment) {
            TagRenameDialogFragment().putArgs(Args(tag)).show(fragment)
        }
    }

    @Parcelize
    class Args(val tag: Tag?) : ParcelableArgs
}
