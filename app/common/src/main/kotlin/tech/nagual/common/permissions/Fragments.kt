@file:Suppress("unused")

package tech.nagual.common.permissions

import android.content.Intent
import android.net.Uri
import android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
import androidx.annotation.CheckResult
import androidx.fragment.app.Fragment
import tech.nagual.common.permissions.internal.Assent.Companion.ensureFragment
import tech.nagual.common.permissions.rationale.RationaleHandler
import tech.nagual.common.permissions.rationale.RealShouldShowRationale
import tech.nagual.common.permissions.rationale.ShouldShowRationale

/** @return `true` if ALL given [permissions] have been granted. */
@CheckResult
fun Fragment.isAllGranted(vararg permissions: Permission) =
    activity?.isAllGranted(*permissions) ?: error("Fragment Activity is null: $this")

/** @return `true` if ALL given [permissions] have been granted. */
@CheckResult
fun Fragment.isAllDenied(vararg permissions: Permission) =
    activity?.isAllDenied(*permissions) ?: error("Fragment Activity is null: $this")

/**
 * Performs a permission request, asking for all given [permissions], and
 * invoking the [callback] with the result.
 */
fun Fragment.askForPermissions(
    vararg permissions: Permission,
    requestCode: Int = 60,
    rationaleHandler: RationaleHandler? = null,
    callback: Callback
) {
    val activity = activity ?: error("Fragment not attached: $this")
    val prefs: Prefs = RealPrefs(activity)
    val shouldShowRationale: ShouldShowRationale = RealShouldShowRationale(activity, prefs)
    startPermissionRequest(
        ensure = { fragment -> ensureFragment(fragment) },
        permissions = permissions,
        requestCode = requestCode,
        shouldShowRationale = shouldShowRationale,
        rationaleHandler = rationaleHandler?.withOwner(this),
        callback = callback
    )
}

/**
 * Like [askForPermissions], but only executes the [execute] callback if all given
 * [permissions] are granted.
 */
fun Fragment.runWithPermissions(
    vararg permissions: Permission,
    requestCode: Int = 80,
    rationaleHandler: RationaleHandler? = null,
    execute: Callback
) {
    askForPermissions(
        *permissions,
        requestCode = requestCode,
        rationaleHandler = rationaleHandler?.withOwner(this)
    ) {
        if (it.isAllGranted(*permissions)) {
            execute.invoke(it)
        }
    }
}

/**
 * Launches app settings for the the current app. Useful when permissions are permanently
 * denied.
 */
fun Fragment.showSystemAppDetailsPage() {
    val context = requireNotNull(context) { "Fragment context is null, is it attached? $this" }
    startActivity(Intent(ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.parse("package:${context.packageName}")
    })
}
