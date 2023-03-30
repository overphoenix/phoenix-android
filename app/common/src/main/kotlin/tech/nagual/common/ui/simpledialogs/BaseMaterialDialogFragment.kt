package tech.nagual.common.ui.simpledialogs

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder

abstract class BaseMaterialDialogFragment : AppCompatDialogFragment() {

    open val positiveName = android.R.string.ok

    open val negativeName = android.R.string.cancel

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        isCancelable = false

        val builder = MaterialAlertDialogBuilder(requireContext(), theme)
            .setTitle(getTitle())
            .setView(createView(savedInstanceState))
            .setCancelable(false)
            .setPositiveButton(positiveName, null)
            .setNegativeButton(negativeName) { dialog, _ -> dialog.dismiss() }
        customizeDialog(builder)
        val dialog = builder.create()
            .apply {
                window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
            }

        dialog.setOnShowListener {
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                submit()
            }
        }

        onAfterDialogCreated(dialog)

        return dialog
    }

    override fun onSaveInstanceState(outState: Bundle) {

    }

    open fun customizeDialog(builder: MaterialAlertDialogBuilder) {}

    open fun onAfterDialogCreated(dialog: Dialog) {}

    protected abstract fun getTitle(): String
    protected abstract fun createView(savedInstanceState: Bundle?): View

    protected abstract fun submit()
}
