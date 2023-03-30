package tech.nagual.common.permissions.internal

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.annotation.VisibleForTesting.PRIVATE
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity

internal class Assent {
    internal val requestQueue = Queue<PendingRequest>()
    internal var currentPendingRequest: PendingRequest? = null
    internal var permissionFragment: PermissionFragment? = null

    companion object {
        private var instance: Assent? = null

        @VisibleForTesting(otherwise = PRIVATE)
        internal var fragmentCreator: () -> PermissionFragment = {
            PermissionFragment()
        }

        @VisibleForTesting(otherwise = PRIVATE)
        internal const val TAG_ACTIVITY = "[assent_permission_fragment/activity]"

        @VisibleForTesting(otherwise = PRIVATE)
        internal const val TAG_FRAGMENT = "[assent_permission_fragment/fragment]"

        fun get(): Assent {
            return instance ?: Assent().also {
                instance = it
            }
        }

        fun ensureFragment(context: Context): PermissionFragment = with(get()) {
            require(context is FragmentActivity) {
                "Unable to ensure the permission Fragment on Context $context"
            }

            permissionFragment = if (permissionFragment == null) {
                fragmentCreator().apply {
                    log("Created new PermissionFragment for Context")
                    context.transact { add(this@apply, TAG_ACTIVITY) }
                }
            } else {
                log("Re-using PermissionFragment for Context")
                permissionFragment
            }
            return permissionFragment ?: error("impossible!")
        }

        fun ensureFragment(context: Fragment): PermissionFragment = with(get()) {
            permissionFragment = if (permissionFragment == null) {
                fragmentCreator().apply {
                    log("Created new PermissionFragment for parent Fragment")
                    context.transact { add(this@apply, TAG_FRAGMENT) }
                }
            } else {
                log("Re-using PermissionFragment for parent Fragment")
                permissionFragment
            }
            return permissionFragment ?: error("impossible!")
        }

        fun forgetFragment() = with(get()) {
            log("forgetFragment()")
            permissionFragment?.detach()
            permissionFragment = null
        }
    }
}
