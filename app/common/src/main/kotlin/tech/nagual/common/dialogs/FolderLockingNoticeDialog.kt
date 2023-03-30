package tech.nagual.common.dialogs

import android.app.Activity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import tech.nagual.common.R
import tech.nagual.common.databinding.DialogTextviewBinding
import tech.nagual.common.extensions.setupDialogStuff
import tech.nagual.common.extensions.baseConfig

class FolderLockingNoticeDialog(val activity: Activity, val callback: () -> Unit) {
    private val binding: DialogTextviewBinding =
        DialogTextviewBinding.inflate(activity.layoutInflater)

    init {

        val view = binding.root
        view.apply {
            binding.textView.text = activity.getString(R.string.lock_folder_notice)
        }

        MaterialAlertDialogBuilder(activity)
            .setPositiveButton(R.string.ok) { dialog, which -> dialogConfirmed() }
            .setNegativeButton(R.string.cancel, null)
            .create().apply {
                activity.setupDialogStuff(view, this)
            }
    }

    private fun dialogConfirmed() {
        activity.baseConfig.wasFolderLockingNoticeShown = true
        callback()
    }
}
