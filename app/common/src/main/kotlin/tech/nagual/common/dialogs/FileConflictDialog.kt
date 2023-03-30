package tech.nagual.common.dialogs

import android.app.Activity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import tech.nagual.common.R
import tech.nagual.common.R.id.*
import tech.nagual.common.databinding.DialogFileConflictBinding
import tech.nagual.common.extensions.beVisibleIf
import tech.nagual.common.extensions.setupDialogStuff
import tech.nagual.common.helpers.CONFLICT_KEEP_BOTH
import tech.nagual.common.helpers.CONFLICT_MERGE
import tech.nagual.common.helpers.CONFLICT_OVERWRITE
import tech.nagual.common.helpers.CONFLICT_SKIP
import tech.nagual.common.extensions.baseConfig
import tech.nagual.common.models.FileDirItem

class FileConflictDialog(
    val activity: Activity, val fileDirItem: FileDirItem, val showApplyToAllCheckbox: Boolean,
    val callback: (resolution: Int, applyForAll: Boolean) -> Unit
) {
    private val binding: DialogFileConflictBinding =
        DialogFileConflictBinding.inflate(activity.layoutInflater)
    val view = binding.root

    init {
        view.apply {
            val stringBase =
                if (fileDirItem.isDirectory) R.string.folder_already_exists else R.string.file_already_exists
            binding.conflictDialogTitle.text =
                String.format(activity.getString(stringBase), fileDirItem.name)
            binding.conflictDialogApplyToAll.isChecked = activity.baseConfig.lastConflictApplyToAll
            binding.conflictDialogApplyToAll.beVisibleIf(showApplyToAllCheckbox)
            binding.conflictDialogRadioMerge.beVisibleIf(fileDirItem.isDirectory)

            val resolutionButton = when (activity.baseConfig.lastConflictResolution) {
                CONFLICT_OVERWRITE -> binding.conflictDialogRadioOverwrite
                CONFLICT_MERGE -> binding.conflictDialogRadioMerge
                else -> binding.conflictDialogRadioSkip
            }
            resolutionButton.isChecked = true
        }

        MaterialAlertDialogBuilder(activity)
            .setPositiveButton(R.string.ok) { dialog, which -> dialogConfirmed() }
            .setNegativeButton(R.string.cancel, null)
            .create().apply {
                activity.setupDialogStuff(view, this)
            }
    }

    private fun dialogConfirmed() {
        val resolution = when (binding.conflictDialogRadioGroup.checkedRadioButtonId) {
            conflict_dialog_radio_skip -> CONFLICT_SKIP
            conflict_dialog_radio_merge -> CONFLICT_MERGE
            conflict_dialog_radio_keep_both -> CONFLICT_KEEP_BOTH
            else -> CONFLICT_OVERWRITE
        }

        val applyToAll = binding.conflictDialogApplyToAll.isChecked
        activity.baseConfig.apply {
            lastConflictApplyToAll = applyToAll
            lastConflictResolution = resolution
        }

        callback(resolution, applyToAll)
    }
}
