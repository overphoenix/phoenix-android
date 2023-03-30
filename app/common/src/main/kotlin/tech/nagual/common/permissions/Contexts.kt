package tech.nagual.common.permissions

import android.content.Context
import android.content.pm.PackageManager.PERMISSION_DENIED
import android.content.pm.PackageManager.PERMISSION_GRANTED
import androidx.annotation.CheckResult
import androidx.core.content.ContextCompat
import tech.nagual.common.permissions.internal.Assent.Companion.get
import tech.nagual.common.permissions.internal.PendingRequest
import tech.nagual.common.permissions.internal.PermissionFragment
import tech.nagual.common.permissions.internal.equalsPermissions
import tech.nagual.common.permissions.internal.log
import tech.nagual.common.permissions.rationale.RationaleHandler
import tech.nagual.common.permissions.rationale.ShouldShowRationale

/** @return `true` if ALL given [permissions] have been granted. */
@CheckResult
fun Context.isAllGranted(vararg permissions: Permission): Boolean {
    return permissions.all {
        ContextCompat.checkSelfPermission(
            this, it.value
        ) == PERMISSION_GRANTED
    }
}

/** @return `true` if ALL given [permissions] have been granted. */
@CheckResult
fun Context.isAllDenied(vararg permissions: Permission): Boolean {
    return permissions.all {
        ContextCompat.checkSelfPermission(
            this, it.value
        ) == PERMISSION_DENIED
    }
}

internal fun <T : Any> T.startPermissionRequest(
    ensure: (T) -> PermissionFragment,
    permissions: Array<out Permission>,
    requestCode: Int = 20,
    shouldShowRationale: ShouldShowRationale,
    rationaleHandler: RationaleHandler? = null,
    callback: Callback
) {
    log("startPermissionRequest(%s)", permissions.joinToString())
    // This invalidates the `shouldShowRationale` cache to help detect permanently denied early.
    permissions.forEach { shouldShowRationale.check(it) }

    if (rationaleHandler != null) {
        rationaleHandler.requestPermissions(permissions, requestCode, callback)
        return
    }

    val currentRequest: PendingRequest? = get().currentPendingRequest
    if (currentRequest != null &&
        currentRequest.permissions.equalsPermissions(*permissions)
    ) {
        // Request matches permissions, append a callback
        log(
            "Callback appended to existing matching request for %s",
            permissions.joinToString()
        )
        currentRequest.callbacks.add(callback)
        return
    }

    // Create a new pending request since none exist for these permissions
    val newPendingRequest = PendingRequest(
        permissions = permissions.toSet(),
        requestCode = requestCode,
        callbacks = mutableListOf(callback)
    )

    if (currentRequest == null) {
        // There is no active request so we can execute immediately
        get().currentPendingRequest = newPendingRequest
        log("New request, performing now")
        ensure(this).perform(newPendingRequest)
    } else {
        // There is an active request, append this new one to the queue
        if (currentRequest.requestCode == requestCode) {
            newPendingRequest.requestCode = requestCode + 1
        }
        log("New request queued for when the current is complete")
        get().requestQueue += newPendingRequest
    }
}
