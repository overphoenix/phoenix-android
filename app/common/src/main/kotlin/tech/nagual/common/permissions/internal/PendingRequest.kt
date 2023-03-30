package tech.nagual.common.permissions.internal

import tech.nagual.common.permissions.Callback
import tech.nagual.common.permissions.Permission

internal data class PendingRequest(
    val permissions: Set<Permission>,
    var requestCode: Int,
    val callbacks: MutableList<Callback>
) {
    override fun hashCode(): Int {
        return permissions.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return other != null &&
                other is PendingRequest &&
                this.permissions.equalsPermissions(other.permissions)
    }
}
