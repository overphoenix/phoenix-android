@file:Suppress("unused")

package tech.nagual.common.permissions

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
import tech.nagual.common.permissions.internal.Assent.Companion.ensureFragment
import tech.nagual.common.permissions.rationale.RationaleHandler
import tech.nagual.common.permissions.rationale.RealShouldShowRationale
import tech.nagual.common.permissions.rationale.ShouldShowRationale

typealias Callback = (result: AssentResult) -> Unit

/**
 * Performs a permission request, asking for all given [permissions], and
 * invoking the [callback] with the result.
 */
fun Activity.askForPermissions(
    vararg permissions: Permission,
    requestCode: Int = 20,
    rationaleHandler: RationaleHandler? = null,
    callback: Callback
) {
    val prefs: Prefs = RealPrefs(this)
    val shouldShowRationale: ShouldShowRationale = RealShouldShowRationale(this, prefs)
    startPermissionRequest(
        ensure = { activity -> ensureFragment(activity) },
        permissions = permissions,
        requestCode = requestCode,
        shouldShowRationale = shouldShowRationale,
        rationaleHandler = rationaleHandler,
        callback = callback
    )
}

/**
 * Like [askForPermissions], but only executes the [execute] callback if all given
 * [permissions] are granted.
 */
fun Activity.runWithPermissions(
    vararg permissions: Permission,
    requestCode: Int = 40,
    rationaleHandler: RationaleHandler? = null,
    execute: Callback
) {
    askForPermissions(
        *permissions,
        requestCode = requestCode,
        rationaleHandler = rationaleHandler
    ) {
        if (it.isAllGranted(*permissions)) {
            execute.invoke(it)
        }
    }
}

/**
 * Launches app settings for the current app. Useful when permissions are permanently
 * denied.
 */
fun Activity.showSystemAppDetailsPage() {
    startActivity(Intent(ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.parse("package:$packageName")
    })
}
