@file:Suppress("unused")

package tech.nagual.common.permissions.rationale

import android.app.Activity
import androidx.annotation.CheckResult
import androidx.annotation.StringRes
import androidx.lifecycle.Lifecycle.Event.ON_DESTROY
import tech.nagual.common.permissions.*
import tech.nagual.common.permissions.GrantResult.DENIED
import tech.nagual.common.permissions.GrantResult.PERMANENTLY_DENIED
import tech.nagual.common.permissions.internal.log
import tech.nagual.common.permissions.internal.maybeObserveLifecycle
import kotlin.properties.Delegates.notNull

typealias Requester = (Array<out Permission>, Int, RationaleHandler?, Callback) -> Unit

abstract class RationaleHandler(
    private val context: Activity,
    private val requester: Requester,
    shouldShowRationale: ShouldShowRationale? = null
) {
    private val messages = mutableMapOf<Permission, CharSequence>()
    private var requestCode: Int by notNull()
    private var callback: Callback by notNull()
    private var remainingRationalePermissions: MutableSet<Permission> by notNull()
    private val prefs: Prefs = RealPrefs(context)
    private var showRationale: ShouldShowRationale =
        shouldShowRationale ?: RealShouldShowRationale(context, prefs)

    private var simpleAssentResult: AssentResult? = null
    private var rationaleAssentResult: AssentResult? = null
    private var owner: Any = context

    @CheckResult
    internal fun withOwner(owner: Any) = apply { this.owner = owner }

    fun onPermission(
        permission: Permission,
        @StringRes message: Int
    ) = onPermission(permission, context.getText(message))

    fun onPermission(
        permission: Permission,
        message: CharSequence
    ) {
        messages[permission] = message
    }

    fun requestPermissions(
        permissions: Array<out Permission>,
        requestCode: Int,
        finalCallback: Callback
    ) {
        this.requestCode = requestCode
        this.callback = finalCallback

        remainingRationalePermissions = permissions
            .filter {
                showRationale.check(it) ||
                        showRationale.isPermanentlyDenied(it)
            }
            .toMutableSet()
        val simplePermissions = permissions.filterNot { showRationale.check(it) }

        log(
            "Found %d permissions that DO require a rationale: %s",
            remainingRationalePermissions.size, remainingRationalePermissions.joinToString()
        )
        if (simplePermissions.isEmpty()) {
            log("No simple permissions to request")
            requestRationalePermissions()
            return
        }

        requester(simplePermissions.toTypedArray(), requestCode, null) {
            simpleAssentResult = it
            requestRationalePermissions()
        }
    }

    abstract fun showRationale(
        permission: Permission,
        message: CharSequence,
        confirm: ConfirmCallback
    )

    abstract fun onDestroy()

    private fun requestRationalePermissions() {
        val nextInQueue = remainingRationalePermissions.firstOrNull() ?: return finish()
        log("Showing rationale for permission %s", nextInQueue)
        owner.maybeObserveLifecycle(ON_DESTROY) { onDestroy() }

        if (showRationale.isPermanentlyDenied(nextInQueue)) {
            onPermanentlyDeniedDetected(nextInQueue)
            return
        }

        showRationale(nextInQueue, getMessageFor(nextInQueue),
            ConfirmCallback { confirmed ->
                if (confirmed) {
                    onUserConfirmedRationale(nextInQueue)
                } else {
                    onUserDeniedRationale(nextInQueue)
                }
            }
        )
    }

    private fun onUserConfirmedRationale(permission: Permission) {
        log("Got rationale confirm signal for permission %s", permission)
        requester(arrayOf(permission), requestCode, null) {
            rationaleAssentResult += it
            remainingRationalePermissions.remove(permission)
            requestRationalePermissions()
        }
    }

    private fun onUserDeniedRationale(permission: Permission) {
        log("Got rationale deny signal for permission %s", permission)
        rationaleAssentResult += AssentResult(mapOf(permission to DENIED))
        remainingRationalePermissions.remove(permission)
        requestRationalePermissions()
    }

    private fun onPermanentlyDeniedDetected(permission: Permission) {
        log("Permission %s is permanently denied.", permission)
        rationaleAssentResult += AssentResult(mapOf(permission to PERMANENTLY_DENIED))
        remainingRationalePermissions.remove(permission)
        requestRationalePermissions()
    }

    private fun finish() {
        log("finish()")
        val simpleResult = simpleAssentResult
        val rationaleResult = rationaleAssentResult
        when {
            simpleResult != null && rationaleResult != null -> {
                callback(simpleResult + rationaleResult)
            }
            simpleResult != null -> callback(simpleResult)
            rationaleResult != null -> callback(rationaleResult)
        }
    }

    private fun getMessageFor(permission: Permission): CharSequence {
        return messages[permission] ?: error("No message provided for $permission")
    }
}
