package tech.nagual.common.permissions.internal

import android.content.Context
import androidx.fragment.app.Fragment
import tech.nagual.common.permissions.AssentResult
import tech.nagual.common.permissions.RealPrefs
import tech.nagual.common.permissions.internal.Assent.Companion.ensureFragment
import tech.nagual.common.permissions.internal.Assent.Companion.forgetFragment
import tech.nagual.common.permissions.internal.Assent.Companion.get
import tech.nagual.common.permissions.rationale.RealShouldShowRationale

class PermissionFragment : Fragment() {

    override fun onAttach(context: Context) {
        super.onAttach(context)
        log("onAttach(%s)", context)
    }

    override fun onDetach() {
        log("onDetach()")
        super.onDetach()
    }

    internal fun perform(request: PendingRequest) {
        log("perform(%s)", request)
        requestPermissions(request.permissions.allValues(), request.requestCode)
    }

    internal fun detach() {
        if (parentFragment != null) {
            log("Detaching PermissionFragment from parent Fragment %s", parentFragment)
            parentFragment?.transact {
                detach(this@PermissionFragment)
                remove(this@PermissionFragment)
            }
        } else if (activity != null) {
            log("Detaching PermissionFragment from Activity %s", activity)
            activity?.transact {
                detach(this@PermissionFragment)
                remove(this@PermissionFragment)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        onPermissionsResponse(
            permissions = permissions,
            grantResults = grantResults
        )
    }
}

internal fun Fragment.onPermissionsResponse(
    permissions: Array<out String>,
    grantResults: IntArray
) {
    val activity = activity ?: error("Fragment is not attached: $this")
    val prefs = RealPrefs(activity)
    val shouldShowRationale = RealShouldShowRationale(activity, prefs)
    val result = AssentResult(
        permissions = permissions.toPermissions(),
        grantResults = grantResults,
        shouldShowRationale = shouldShowRationale
    )
    log("onPermissionsResponse(): %s", result)

    val currentRequest: PendingRequest? = get().currentPendingRequest
    if (currentRequest == null) {
        warn("onPermissionsResponse() called but there's no current pending request.")
        return
    }

    if (currentRequest.permissions.equalsStrings(permissions)) {
        currentRequest.callbacks.invokeAll(result)
        get().currentPendingRequest = null
    } else {
        warn(
            "onPermissionsResponse() called with a result " +
                    "that doesn't match the current pending request."
        )
        return
    }

    if (get().requestQueue.isNotEmpty()) {
        // Execute the next request in the queue
        val nextRequest: PendingRequest = get().requestQueue.pop()
            .also { get().currentPendingRequest = it }
        log("Executing next request in the queue: %s", nextRequest)
        ensureFragment(this@onPermissionsResponse).perform(nextRequest)
    } else {
        // No more requests to execute, we can destroy the Fragment
        log("Nothing more in the queue to execute, forgetting the PermissionFragment.")
        forgetFragment()
    }
}
