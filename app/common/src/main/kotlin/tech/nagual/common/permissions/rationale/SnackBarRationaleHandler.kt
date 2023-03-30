@file:Suppress("unused")

package tech.nagual.common.permissions.rationale

import android.app.Activity
import android.view.View
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import tech.nagual.common.permissions.Permission
import tech.nagual.common.permissions.askForPermissions

internal class SnackBarRationaleHandler(
    private val root: View,
    context: Activity,
    requester: Requester
) : RationaleHandler(context, requester) {

    override fun showRationale(
        permission: Permission,
        message: CharSequence,
        confirm: ConfirmCallback
    ) {
        val dismissListener = object : Snackbar.Callback() {
            override fun onDismissed(
                transientBottomBar: Snackbar?,
                event: Int
            ) = confirm(isConfirmed = false)
        }
        Snackbar.make(root, message, Snackbar.LENGTH_INDEFINITE)
            .apply {
                setAction(android.R.string.ok) {
                    removeCallback(dismissListener)
                    confirm(isConfirmed = true)
                }
                addCallback(dismissListener)
                show()
            }
    }

    override fun onDestroy() = Unit
}

fun Fragment.createSnackBarRationale(
    root: View,
    block: RationaleHandler.() -> Unit
): RationaleHandler {
    return SnackBarRationaleHandler(
        root = root,
        context = activity ?: error("Fragment not attached"),
        requester = ::askForPermissions
    ).apply(block)
}

fun Activity.createSnackBarRationale(
    root: View,
    block: RationaleHandler.() -> Unit
): RationaleHandler {
    return SnackBarRationaleHandler(
        root = root,
        context = this,
        requester = ::askForPermissions
    ).apply(block)
}
