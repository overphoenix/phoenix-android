package tech.nagual.phoenix.organizers

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import me.zhanghai.android.files.util.*
import tech.nagual.common.R
import tech.nagual.phoenix.databinding.OrganizerEditDialogBinding
import tech.nagual.phoenix.tools.organizer.data.model.Organizer
import tech.nagual.phoenix.tools.organizer.data.repo.OrganizerRepository
import javax.inject.Inject

@AndroidEntryPoint
class EditOrganizerDialogFragment : AppCompatDialogFragment() {
    private val args by args<Args>()

    @Inject
    lateinit var organizerRepository: OrganizerRepository

    private lateinit var binding: OrganizerEditDialogBinding

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialogBuilder = MaterialAlertDialogBuilder(requireContext(), theme)
        dialogBuilder.setTitle(if (args.create) R.string.organizer_create_new else R.string.organizer_edit_existing)
            .apply {
                binding = OrganizerEditDialogBinding.inflate(context.layoutInflater)
                val organizer = args.organizer
                binding.nameLayout.placeholderText = getString(R.string.organizer_new_placeholder)
                if (savedInstanceState == null) {
                    binding.nameEdit.setTextWithSelection(
                        organizer.name
                    )
                    binding.descriptionEdit.setTextWithSelection(
                        organizer.description
                    )
                }
                setView(binding.root)
            }
            .setPositiveButton(android.R.string.ok) { _, _ -> save() }
            .setNegativeButton(android.R.string.cancel) { dialog, _ -> dialog.cancel() }

        if (!args.create) {
            dialogBuilder.setNeutralButton(R.string.remove) { _, _ -> remove() }
        }

        return dialogBuilder.create()
            .apply {
                window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
            }
    }

    private fun save() {
        val name =
            if (binding.nameEdit.text.isNullOrEmpty()) binding.nameLayout.placeholderText.toString()
            else binding.nameEdit.text.toString()
        val description = binding.descriptionEdit.text.toString()
        val organizer = args.organizer.copy(
            name = name,
            description = description
        )
        lifecycleScope.launch(Dispatchers.IO) {
            if (args.create) {
                organizerRepository.insert(organizer)
            } else {
                organizerRepository.update(organizer)
            }
        }

        finish()
    }

    private fun remove() {
        lifecycleScope.launch(Dispatchers.IO) {
            organizerRepository.deleteAndFixOrdinals(args.organizer)
        }
        finish()
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)

        finish()
    }

    @Parcelize
    class Args(val organizer: Organizer, val create: Boolean = false) : ParcelableArgs
}
