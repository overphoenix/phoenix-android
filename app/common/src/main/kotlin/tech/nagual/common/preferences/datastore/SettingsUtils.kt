package tech.nagual.common.preferences.datastore

import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import tech.nagual.common.R

inline fun <reified T> Fragment.showOneChoicePreferenceDialog(
    titleRes: Int,
    selected: T,
    dismiss: Boolean = true,
    items: Array<String>? = null,
    crossinline onClick: (T) -> Unit,
) where T : Enum<T>, T : EnumPreference {
    val enumValues = enumValues<T>()
    val selectedIndex = enumValues.indexOf(selected)
    val items = items ?: enumValues
        .map {
            if (it is HasNameResource) requireContext().getString(it.nameResource) else ""
        }
        .toTypedArray()

    MaterialAlertDialogBuilder(requireContext())
        .setTitle(requireContext().getString(titleRes))
        .setSingleChoiceItems(items, selectedIndex) { dialogInterface, which ->
            if (dismiss) dialogInterface.dismiss()
            onClick(enumValues[which])
        }
        .setNegativeButton(getString(R.string.cancel)) { dialogInterface, i ->
            dialogInterface.dismiss()
        }
        .show()
}
