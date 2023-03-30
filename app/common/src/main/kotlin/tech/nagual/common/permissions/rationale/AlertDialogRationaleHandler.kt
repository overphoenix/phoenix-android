@file:Suppress("unused")

package tech.nagual.common.permissions.rationale

import android.app.Activity
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import tech.nagual.common.permissions.Permission
import tech.nagual.common.permissions.askForPermissions

internal class DialogRationaleHandler(
    private val context: Activity,
    @StringRes private val dialogTitle: Int,
    requester: Requester
) : RationaleHandler(context, requester) {
    private var dialog: AlertDialog? = null

    override fun showRationale(
        permission: Permission,
        message: CharSequence,
        confirm: ConfirmCallback
    ) {
        dialog = MaterialAlertDialogBuilder(context)
            .setTitle(dialogTitle)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                (dialog as AlertDialog).setOnDismissListener(null)
                confirm(isConfirmed = true)
            }
            .setOnDismissListener {
                confirm(isConfirmed = false)
            }
            .show()
    }

    override fun onDestroy() {
        dialog?.dismiss()
        dialog = null
    }
}

fun Fragment.createDialogRationale(
    @StringRes dialogTitle: Int,
    block: RationaleHandler.() -> Unit
): RationaleHandler {
    return DialogRationaleHandler(
        dialogTitle = dialogTitle,
        context = activity ?: error("Fragment not attached"),
        requester = ::askForPermissions
    ).apply(block)
}

fun Activity.createDialogRationale(
    @StringRes dialogTitle: Int,
    block: RationaleHandler.() -> Unit
): RationaleHandler {
    return DialogRationaleHandler(
        dialogTitle = dialogTitle,
        context = this,
        requester = ::askForPermissions
    ).apply(block)
}
