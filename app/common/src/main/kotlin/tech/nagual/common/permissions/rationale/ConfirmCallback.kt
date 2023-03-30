package tech.nagual.common.permissions.rationale

import tech.nagual.common.permissions.internal.warn

/** @author Aidan Follestad (@afollestad) */
class ConfirmCallback(
    private var action: ((isConfirmed: Boolean) -> Unit)?
) {
    operator fun invoke(isConfirmed: Boolean) {
        if (action == null) {
            warn("Confirm callback invoked more than once, ignored after first invocation.")
        }
        action?.invoke(isConfirmed)
        action = null
    }
}
