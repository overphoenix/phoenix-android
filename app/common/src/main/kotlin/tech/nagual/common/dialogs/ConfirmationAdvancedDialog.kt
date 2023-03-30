package tech.nagual.common.dialogs

import android.app.Activity
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import tech.nagual.common.R
import tech.nagual.common.databinding.DialogMessageBinding
import tech.nagual.common.extensions.setupDialogStuff

// similar fo ConfirmationDialog, but has a callback for negative button too
class ConfirmationAdvancedDialog(
    activity: Activity,
    message: String = "",
    messageId: Int = R.string.proceed_with_deletion,
    positive: Int = R.string.yes,
    negative: Int,
    val callback: (result: Boolean) -> Unit
) {
    var dialog: AlertDialog

    private val binding: DialogMessageBinding =
        DialogMessageBinding.inflate(activity.layoutInflater)

    init {
        val view = binding.root
        binding.message.text =
            message.ifEmpty { activity.resources.getString(messageId) }

        val builder = MaterialAlertDialogBuilder(activity)
            .setPositiveButton(positive) { dialog, which -> positivePressed() }
            .setOnCancelListener { negativePressed() }

        if (negative != 0) {
            builder.setNegativeButton(negative) { dialog, which -> negativePressed() }
        }

        dialog = builder.create().apply {
            activity.setupDialogStuff(view, this)
        }
    }

    private fun positivePressed() {
        dialog.dismiss()
        callback(true)
    }

    private fun negativePressed() {
        dialog.dismiss()
        callback(false)
    }
}
