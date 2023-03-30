package tech.nagual.common.permissions

import android.content.pm.PackageManager
import tech.nagual.common.permissions.GrantResult.*
import tech.nagual.common.permissions.rationale.ShouldShowRationale

enum class GrantResult {
    GRANTED,
    DENIED,
    PERMANENTLY_DENIED
}

internal fun Int.asGrantResult(
    forPermission: Permission,
    shouldShowRationale: ShouldShowRationale
): GrantResult {
    if (shouldShowRationale.isPermanentlyDenied(forPermission)) {
        return PERMANENTLY_DENIED
    }
    return when (this) {
        PackageManager.PERMISSION_GRANTED -> GRANTED
        else -> DENIED
    }
}

internal fun IntArray.mapGrantResults(
    permissions: Set<Permission>,
    shouldShowRationale: ShouldShowRationale
): List<GrantResult> {
    return mapIndexed { index, grantResult ->
        val permission: Permission = permissions.elementAt(index)
        grantResult.asGrantResult(permission, shouldShowRationale)
    }
}
