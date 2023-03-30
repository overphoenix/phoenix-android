package tech.nagual.common.permissions.internal

import tech.nagual.common.permissions.AssentResult
import tech.nagual.common.permissions.Callback
import tech.nagual.common.permissions.Permission

internal fun Set<Permission>.containsPermission(
    permission: Permission
) = indexOfFirst { it.value == permission.value } > -1

internal fun Set<Permission>.equalsStrings(strings: Array<out String>): Boolean {
    if (this.size != strings.size) {
        return false
    }
    for (perm in this) {
        if (!strings.contains(perm.value)) {
            return false
        }
    }
    return true
}

internal fun Set<Permission>.equalsPermissions(vararg permissions: Permission) =
    equalsPermissions(permissions.toSet())

internal fun Set<Permission>.equalsPermissions(permissions: Set<Permission>): Boolean {
    if (this.size != permissions.size) {
        return false
    }
    for ((i, perm) in this.withIndex()) {
        if (perm.value != permissions.elementAt(i).value) {
            return false
        }
    }
    return true
}

internal fun Set<Permission>.allValues(): Array<out String> =
    map { it.value }.toTypedArray()

internal fun Array<out String>.toPermissions() =
    map { Permission.parse(it) }.toSet()

internal fun List<Callback>.invokeAll(result: AssentResult) {
    for (callback in this) {
        callback.invoke(result)
    }
}
