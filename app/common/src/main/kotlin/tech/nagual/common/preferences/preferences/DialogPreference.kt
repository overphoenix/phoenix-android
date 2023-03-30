package tech.nagual.common.preferences.preferences

import android.app.Dialog
import android.content.Context
import androidx.annotation.CallSuper
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import tech.nagual.common.preferences.Preference
import tech.nagual.common.preferences.PreferencesAdapter

/**
 * DialogPreference is a helper class to display custom [Dialog]s on preference clicks.
 * It is recommended to override this class once and then inflate different dialogs
 * based on their keys in [createDialog].
 */
abstract class DialogPreference(key: String) : Preference(key), LifecycleEventObserver {

    private var dialog: Dialog? = null

    /**
     * This flag tells the preference whether to recreate the dialog after a configuration change
     */
    private var recreateDialog = false

    override fun onClick(holder: PreferencesAdapter.ViewHolder) {
        createAndShowDialog(holder.itemView.context)
    }

    /**
     * Subclasses must create the dialog which will managed by this preference here.
     * However, they should not [show][Dialog.show] it already, that will be done in [onClick].
     *
     * @param context the context to create your Dialog with, has a window attached
     */
    abstract fun createDialog(context: Context): Dialog

    private fun createAndShowDialog(context: Context) {
        (dialog ?: createDialog(context).apply { dialog = this }).show()
    }

    /**
     * Dismiss the currently attached dialog, if any
     */
    fun dismiss() = dialog?.dismiss()

    @CallSuper
    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_CREATE -> {
                if (recreateDialog && source is Context) {
                    recreateDialog = false
                    createAndShowDialog(source)
                }
            }
            Lifecycle.Event.ON_STOP -> @Suppress("DEPRECATION") onStop()
            Lifecycle.Event.ON_DESTROY -> {
                dialog?.apply {
                    recreateDialog = isShowing
                    dismiss()
                }
                dialog = null
            }
            else -> Unit // ignore
        }
    }

    /**
     * Kept for backwards compatibility
     */
    @Deprecated("Override onStateChanged() instead")
    open fun onStop() {
    }
}