package tech.nagual.common.permissions.rationale

import android.app.Activity
import android.content.pm.PackageManager.PERMISSION_GRANTED
import androidx.annotation.CheckResult
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import tech.nagual.common.permissions.Permission
import tech.nagual.common.permissions.Prefs

interface ShouldShowRationale {
    fun check(permission: Permission): Boolean

    @CheckResult
    fun isPermanentlyDenied(permission: Permission): Boolean
}

internal class RealShouldShowRationale(
    private val activity: Activity,
    private val prefs: Prefs
) : ShouldShowRationale {

    override fun check(permission: Permission): Boolean {
        return ActivityCompat.shouldShowRequestPermissionRationale(activity, permission.value)
            .also { shouldShow ->
                if (shouldShow) {
                    prefs.set(permission.key(), shouldShow)
                }
            }
    }

    /**
     * Android provides a utility method, `shouldShowRequestPermissionRationale()`, that returns:
     *   - `true` if the user has previously denied the request...
     *   - `false` if a user has denied a permission and selected the "Don't ask again" option in
     *      the permission request dialog...
     *   - `false` if a device policy prohibits the permission.
     */
    override fun isPermanentlyDenied(permission: Permission): Boolean {
        val showRationaleWasTrue: Boolean = prefs[permission.key()] ?: false
        // Using point 2 in the kdoc here...
        return showRationaleWasTrue && !permission.isGranted() && !check(permission)
    }

    /**
     * Provides a sanity check to avoid falsely returning true in [isPermanentlyDenied]. See
     * [https://github.com/afollestad/assent/issues/16].
     */
    private fun Permission.isGranted(): Boolean =
        ContextCompat.checkSelfPermission(activity, value) == PERMISSION_GRANTED

    private fun Permission.key() = "${KEY_SHOULD_SHOW_RATIONALE}_$value"
}

private const val KEY_SHOULD_SHOW_RATIONALE = "show_rationale_"
