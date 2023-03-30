@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package tech.nagual.common.permissions

import androidx.annotation.CheckResult
import tech.nagual.common.permissions.GrantResult.*
import tech.nagual.common.permissions.rationale.ShouldShowRationale

/**
 * Wraps a result for a permission request, which provides utility
 * methods and is sent through callbacks.
 *
 * @author Aidan Follestad (afollestad)
 */
class AssentResult(
    internal val resultsMap: Map<Permission, GrantResult>
) {
    internal constructor(
        permissions: Set<Permission>,
        grantResults: List<GrantResult>
    ) : this(
        permissions
            .mapIndexed { index, permission ->
                Pair(permission, grantResults[index])
            }
            .toMap()
    )

    internal constructor(
        permissions: Set<Permission>,
        grantResults: IntArray,
        shouldShowRationale: ShouldShowRationale
    ) : this(permissions, grantResults.mapGrantResults(permissions, shouldShowRationale))

    /** @return the [GrantResult] for a given [permission]. */
    @CheckResult
    operator fun get(permission: Permission): GrantResult =
        resultsMap[permission] ?: error("No GrantResult for permission $permission")

    /** @return `true` if this result contains the given permission. */
    @CheckResult
    fun containsPermissions(permission: Permission): Boolean =
        resultsMap.containsKey(permission)

    /** @return a list of all granted permissions. */
    @CheckResult
    fun granted(): Set<Permission> {
        return resultsMap.filterValues { it == GRANTED }
            .keys.toSet()
    }

    /** @return a list of all denied permissions, which also includes [permanentlyDenied]. */
    @CheckResult
    fun denied(): Set<Permission> {
        return resultsMap.filterValues { it == DENIED || it == PERMANENTLY_DENIED }
            .keys.toSet()
    }

    /** @return a list of all permanently denied permissions. */
    @CheckResult
    fun permanentlyDenied(): Set<Permission> {
        return resultsMap.filterValues { it == PERMANENTLY_DENIED }
            .keys.toSet()
    }

    /** @return `true` if all given [permissions] were granted. */
    @CheckResult
    fun isAllGranted(vararg permissions: Permission): Boolean {
        return permissions.asSequence()
            .map { permission ->
                resultsMap[permission] ?: error("Permission $permission not in result map.")
            }
            .all { result -> result == GRANTED }
    }

    /** @return `true` if all given [permissions] were denied. */
    @CheckResult
    fun isAllDenied(vararg permissions: Permission): Boolean {
        return permissions.asSequence()
            .map { permission ->
                resultsMap[permission] ?: error("Permission $permission not in result map.")
            }
            .all { result -> result != GRANTED }
    }

    override fun hashCode(): Int = resultsMap.hashCode()

    override fun equals(other: Any?): Boolean {
        return other is AssentResult && other.resultsMap == resultsMap
    }

    override fun toString(): String {
        return resultsMap.entries.joinToString(separator = ", ") { "${it.key} -> ${it.value}" }
    }
}

internal operator fun AssentResult?.plus(other: AssentResult): AssentResult {
    if (this == null) return other
    return AssentResult(resultsMap + other.resultsMap)
}
