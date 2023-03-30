package tech.nagual.common.dialogs

import android.app.Activity
import android.text.Html
import androidx.appcompat.app.AlertDialog
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import tech.nagual.common.R
import tech.nagual.common.databinding.DialogWritePermissionBinding
import tech.nagual.common.databinding.DialogWritePermissionOtgBinding
import tech.nagual.common.extensions.setupDialogStuff
import tech.nagual.common.activities.BaseSimpleActivity
import tech.nagual.common.extensions.humanizePath

class WritePermissionDialog(activity: Activity, val mode: Mode, val callback: () -> Unit) {
    sealed class Mode {
        object Otg : Mode()
        object SdCard : Mode()
        data class OpenDocumentTreeSDK30(val path: String) : Mode()
        object CreateDocumentSDK30 : Mode()
    }

    var dialog: AlertDialog

    init {
        val binding = if (mode == Mode.SdCard)
            DialogWritePermissionBinding.inflate(activity.layoutInflater)
        else
            DialogWritePermissionOtgBinding.inflate(activity.layoutInflater)
        val view = binding.root

        val glide = Glide.with(activity)
        val crossFade = DrawableTransitionOptions.withCrossFade()
        when (mode) {
            Mode.Otg -> {
                (binding as DialogWritePermissionOtgBinding).writePermissionsDialogOtgText.setText(R.string.confirm_usb_storage_access_text)
                glide.load(R.drawable.img_write_storage_otg).transition(crossFade)
                    .into(binding.writePermissionsDialogOtgImage)

            }
            Mode.SdCard -> {
                glide.load(R.drawable.img_write_storage).transition(crossFade)
                    .into((binding as DialogWritePermissionBinding).writePermissionsDialogImage)
                glide.load(R.drawable.img_write_storage_sd).transition(crossFade)
                    .into(binding.writePermissionsDialogImageSd)

            }
            is Mode.OpenDocumentTreeSDK30 -> {
                val humanizedPath = activity.humanizePath(mode.path)
                (binding as DialogWritePermissionOtgBinding).writePermissionsDialogOtgText.text =
                    Html.fromHtml(activity.getString(R.string.confirm_storage_access_android_text_specific, humanizedPath))
                glide.load(R.drawable.img_write_storage_sdk_30).transition(crossFade).into(binding.writePermissionsDialogOtgImage)

                binding.writePermissionsDialogOtgImage.setOnClickListener {
                    dialogConfirmed()
                }
            }
            Mode.CreateDocumentSDK30 -> {
                (binding as DialogWritePermissionOtgBinding).writePermissionsDialogOtgText.text = Html.fromHtml(activity.getString(R.string.confirm_create_doc_for_new_folder_text))
                glide.load(R.drawable.img_write_storage_create_doc_sdk_30).transition(crossFade).into(binding.writePermissionsDialogOtgImage)

                binding.writePermissionsDialogOtgImage.setOnClickListener {
                    dialogConfirmed()
                }
            }
        }

        dialog = MaterialAlertDialogBuilder(activity)
            .setPositiveButton(R.string.ok) { dialog, which -> dialogConfirmed() }
            .setOnCancelListener {
                tech.nagual.common.activities.BaseSimpleActivity.funAfterSAFPermission?.invoke(false)
                tech.nagual.common.activities.BaseSimpleActivity.funAfterSAFPermission = null
            }
            .create().apply {
                activity.setupDialogStuff(view, this, R.string.confirm_storage_access_title)
            }
    }

    private fun dialogConfirmed() {
        dialog.dismiss()
        callback()
    }
}
