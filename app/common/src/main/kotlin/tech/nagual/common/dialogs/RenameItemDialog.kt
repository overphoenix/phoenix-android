package tech.nagual.common.dialogs

import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import tech.nagual.common.R
import tech.nagual.common.activities.BaseSimpleActivity
import tech.nagual.common.extensions.*
import tech.nagual.common.extensions.beGone
import tech.nagual.common.extensions.setupDialogStuff
import tech.nagual.common.extensions.toast
import java.util.*
import tech.nagual.common.databinding.DialogRenameItemBinding
import tech.nagual.common.extensions.*

class RenameItemDialog(val activity: tech.nagual.common.activities.BaseSimpleActivity, val path: String, val callback: (newPath: String) -> Unit) {
    init {
        var ignoreClicks = false
        val fullName = path.getFilenameFromPath()
        val dotAt = fullName.lastIndexOf(".")
        var name = fullName
        val binding = DialogRenameItemBinding.inflate(activity.layoutInflater)
        val view = binding.root
        view.apply {
            if (dotAt > 0 && !activity.getIsPathDirectory(path)) {
                name = fullName.substring(0, dotAt)
                val extension = fullName.substring(dotAt + 1)
                binding.renameItemExtension.setText(extension)
            } else {
                binding.renameItemExtensionHint.beGone()
            }

            binding.renameItemName.setText(name)
        }

        MaterialAlertDialogBuilder(activity)
            .setPositiveButton(R.string.ok, null)
            .setNegativeButton(R.string.cancel, null)
            .create().apply {
                activity.setupDialogStuff(view, this, R.string.rename) {
                    showKeyboard(binding.renameItemName)
                    getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        if (ignoreClicks) {
                            return@setOnClickListener
                        }

                        var newName = binding.renameItemName.value
                        val newExtension = binding.renameItemExtension.value

                        if (newName.isEmpty()) {
                            activity.toast(R.string.empty_name)
                            return@setOnClickListener
                        }

                        if (!newName.isAValidFilename()) {
                            activity.toast(R.string.invalid_name)
                            return@setOnClickListener
                        }

                        val updatedPaths = ArrayList<String>()
                        updatedPaths.add(path)
                        if (!newExtension.isEmpty()) {
                            newName += ".$newExtension"
                        }

                        if (!activity.getDoesFilePathExist(path)) {
                            activity.toast(String.format(activity.getString(R.string.source_file_doesnt_exist), path))
                            return@setOnClickListener
                        }

                        val newPath = "${path.getParentPath()}/$newName"

                        if (path == newPath) {
                            activity.toast(R.string.name_taken)
                            return@setOnClickListener
                        }

                        if (!path.equals(newPath, ignoreCase = true) && activity.getDoesFilePathExist(newPath)) {
                            activity.toast(R.string.name_taken)
                            return@setOnClickListener
                        }

                        updatedPaths.add(newPath)
                        ignoreClicks = true
                        activity.renameFile(path, newPath, false) { success, useAndroid30Way ->
                            ignoreClicks = false
                            if (success) {
                                callback(newPath)
                                dismiss()
                            } else {
                                activity.toast(R.string.unknown_error_occurred)
                            }
                        }
                    }
                }
            }
    }
}
