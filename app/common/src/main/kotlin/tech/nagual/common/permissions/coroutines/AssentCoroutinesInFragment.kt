package tech.nagual.common.permissions.coroutines

import androidx.fragment.app.Fragment
import tech.nagual.common.permissions.AssentResult
import tech.nagual.common.permissions.Permission
import tech.nagual.common.permissions.askForPermissions
import tech.nagual.common.permissions.rationale.RationaleHandler
import tech.nagual.common.permissions.runWithPermissions
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Performs a permission request, asking for all given [permissions],
 * and returning the result.
 */
suspend fun Fragment.awaitPermissionsResult(
    vararg permissions: Permission,
    requestCode: Int = 60,
    rationaleHandler: RationaleHandler? = null
): AssentResult {
    checkMainThread()
    return suspendCoroutine { continuation ->
        askForPermissions(
            permissions = *permissions,
            requestCode = requestCode,
            rationaleHandler = rationaleHandler
        ) { result ->
            continuation.resume(result)
        }
    }
}

/**
 * Like [awaitPermissionsResult], but only returns if all given
 * permissions are granted. So be warned, this method will wait
 * indefinitely if permissions are not all granted.
 */
suspend fun Fragment.awaitPermissionsGranted(
    vararg permissions: Permission,
    requestCode: Int = 80,
    rationaleHandler: RationaleHandler? = null
): AssentResult {
    checkMainThread()
    return suspendCoroutine { continuation ->
        runWithPermissions(
            permissions = *permissions,
            requestCode = requestCode,
            rationaleHandler = rationaleHandler
        ) { result ->
            continuation.resume(result)
        }
    }
}
