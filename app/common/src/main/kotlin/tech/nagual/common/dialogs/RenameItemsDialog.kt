package tech.nagual.common.dialogs

import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import tech.nagual.common.R
import tech.nagual.common.databinding.DialogRenameItemsBinding
import tech.nagual.common.extensions.*
import tech.nagual.common.activities.BaseSimpleActivity
import tech.nagual.common.extensions.*
import tech.nagual.common.extensions.showKeyboard

// used at renaming folders
class RenameItemsDialog(
    val activity: tech.nagual.common.activities.BaseSimpleActivity,
    val paths: ArrayList<String>,
    val callback: () -> Unit
) {
    init {
        var ignoreClicks = false
        val binding = DialogRenameItemsBinding.inflate(activity.layoutInflater)
        val view = binding.root

        MaterialAlertDialogBuilder(activity)
            .setPositiveButton(R.string.ok, null)
            .setNegativeButton(R.string.cancel, null)
            .create().apply {
                activity.setupDialogStuff(view, this, R.string.rename) {
                    showKeyboard(binding.renameItemsValue)
                    getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        if (ignoreClicks) {
                            return@setOnClickListener
                        }

                        val valueToAdd = binding.renameItemsValue.text.toString()
                        val append =
                            binding.renameItemsRadioGroup.checkedRadioButtonId == binding.renameItemsRadioAppend.id

                        if (valueToAdd.isEmpty()) {
                            callback()
                            dismiss()
                            return@setOnClickListener
                        }

                        if (!valueToAdd.isAValidFilename()) {
                            activity.toast(R.string.invalid_name)
                            return@setOnClickListener
                        }

                        val validPaths = paths.filter { activity.getDoesFilePathExist(it) }
                        val sdFilePath = validPaths.firstOrNull { activity.isPathOnSD(it) }
                            ?: validPaths.firstOrNull()
                        if (sdFilePath == null) {
                            activity.toast(R.string.unknown_error_occurred)
                            dismiss()
                            return@setOnClickListener
                        }

                        activity.handleSAFDialog(sdFilePath) {
                            if (!it) {
                                return@handleSAFDialog
                            }

                            ignoreClicks = true
                            var pathsCnt = validPaths.size
                            for (path in validPaths) {
                                val fullName = path.getFilenameFromPath()
                                var dotAt = fullName.lastIndexOf(".")
                                if (dotAt == -1) {
                                    dotAt = fullName.length
                                }

                                val name = fullName.substring(0, dotAt)
                                val extension =
                                    if (fullName.contains(".")) ".${fullName.getFilenameExtension()}" else ""

                                val newName = if (append) {
                                    "$name$valueToAdd$extension"
                                } else {
                                    "$valueToAdd$fullName"
                                }

                                val newPath = "${path.getParentPath()}/$newName"

                                if (activity.getDoesFilePathExist(newPath)) {
                                    continue
                                }

                                activity.renameFile(
                                    path,
                                    newPath,
                                    true
                                ) { success, useAndroid30Way ->
                                    if (success) {
                                        pathsCnt--
                                        if (pathsCnt == 0) {
                                            callback()
                                            dismiss()
                                        }
                                    } else {
                                        ignoreClicks = false
                                        activity.toast(R.string.unknown_error_occurred)
                                        dismiss()
                                    }
                                }
                            }
                        }
                    }
                }
            }
    }
}
