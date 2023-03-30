package tech.nagual.phoenix.tools.painter.dialogs

import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import tech.nagual.common.extensions.*
import tech.nagual.phoenix.databinding.PainterDialogSaveImageBinding
import tech.nagual.phoenix.R
import tech.nagual.common.activities.BaseSimpleActivity
import tech.nagual.common.dialogs.ConfirmationDialog
import tech.nagual.common.dialogs.FilePickerDialog
import tech.nagual.common.extensions.*
import tech.nagual.common.extensions.internalStoragePath
import tech.nagual.common.helpers.PAINT_JPG
import tech.nagual.common.helpers.PAINT_PNG
import tech.nagual.common.helpers.PAINT_SVG
import java.io.File

class SaveImageDialog(
    val activity: tech.nagual.common.activities.BaseSimpleActivity,
    val defaultPath: String,
    val defaultFilename: String,
    val defaultExtension: String,
    val hidePath: Boolean,
    callback: (fullPath: String, filename: String, extension: String) -> Unit
) {
    private lateinit var binding: PainterDialogSaveImageBinding

    private val SIMPLE_DRAW = "Simple Draw"

    init {
        val initialFilename = getInitialFilename()
        var folder =
            if (defaultPath.isEmpty()) "${activity.internalStoragePath}/$SIMPLE_DRAW" else defaultPath
        val binding = PainterDialogSaveImageBinding.inflate(activity.layoutInflater)
        val view = binding.root
//        val view = activity.layoutInflater.inflate(R.layout.paint_dialog_save_image, null).apply {
        binding.saveImageFilename.setText(initialFilename)
        binding.saveImageRadioGroup.check(
            when (defaultExtension) {
                PAINT_JPG -> R.id.save_image_radio_jpg
                PAINT_SVG -> R.id.save_image_radio_svg
                else -> R.id.save_image_radio_png
            }
        )

        if (hidePath) {
            binding.saveImagePathLabel.beGone()
            binding.saveImagePath.beGone()
        } else {
            binding.saveImagePath.text = activity.humanizePath(folder)
            binding.saveImagePath.setOnClickListener {
                FilePickerDialog(activity, folder, false, showFAB = true) {
                    binding.saveImagePath.text = activity.humanizePath(it)
                    folder = it
                }
            }
        }
//        }

        MaterialAlertDialogBuilder(activity)
            .setPositiveButton(R.string.ok, null)
            .setNegativeButton(R.string.cancel, null)
            .create().apply {
                activity.setupDialogStuff(view, this, R.string.save_as) {
                    showKeyboard(binding.saveImageFilename)
                    getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val filename = binding.saveImageFilename.value
                        if (filename.isEmpty()) {
                            activity.toast(R.string.filename_cannot_be_empty)
                            return@setOnClickListener
                        }

                        val extension = when (binding.saveImageRadioGroup.checkedRadioButtonId) {
                            R.id.save_image_radio_png -> PAINT_PNG
                            R.id.save_image_radio_svg -> PAINT_SVG
                            else -> PAINT_JPG
                        }

                        val newPath = "${folder.trimEnd('/')}/$filename.$extension"
                        if (!newPath.getFilenameFromPath().isAValidFilename()) {
                            activity.toast(R.string.filename_invalid_characters)
                            return@setOnClickListener
                        }

                        if (!hidePath && File(newPath).exists()) {
                            val title = String.format(
                                activity.getString(R.string.file_already_exists_overwrite),
                                newPath.getFilenameFromPath()
                            )
                            ConfirmationDialog(activity, title) {
                                callback(newPath, filename, extension)
                                dismiss()
                            }
                        } else {
                            callback(newPath, filename, extension)
                            dismiss()
                        }
                    }
                }
            }
    }

    private fun getInitialFilename(): String {
        val newFilename = "image_${activity.getCurrentFormattedDateTime()}"
        return if (defaultFilename.isEmpty()) newFilename else defaultFilename
    }
}
