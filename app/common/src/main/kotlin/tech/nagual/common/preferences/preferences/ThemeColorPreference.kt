package tech.nagual.common.preferences.preferences

import android.app.Dialog
import android.content.Context
import android.widget.GridView
import androidx.annotation.ColorInt
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import tech.nagual.common.R
import tech.nagual.app.application
import me.zhanghai.android.files.compat.getColorCompat
import tech.nagual.common.databinding.ColorPickerDialogBinding
import tech.nagual.common.preferences.preferences.colopicker.ColorPaletteAdapter
import tech.nagual.theme.custom.ThemeColor
import me.zhanghai.android.files.util.layoutInflater

class ThemeColorPreference(
    key: String
) : BaseColorPreference(key) {
    private lateinit var _stringValue: String
    var stringValue: String
        get() = _stringValue
        set(value) {
            _stringValue = value
            commitString(value)
            requestRebind()
        }

    // We can't use lateinit for Int.
    private var initialValue: Int? = null
    override var value: Int
        // Deliberately only bind for the initial value, because we are going to restart the
        // activity upon change and we want to let the activity animation have the correct visual
        // appearance.
        @ColorInt
        get() {
            var initialValue = initialValue
            if (initialValue == null) {
                initialValue = entryValues[stringValue.toInt()]
                this.initialValue = initialValue
            }
            return initialValue
        }
        set(value) {
            stringValue = entryValues.indexOf(value).toString()
        }

    override val defaultValue = 0

    override val entryValues: IntArray =
        ThemeColor.values().map { application.getColorCompat(it.resourceId) }
            .toIntArray()

    override fun onAttach() {
        stringValue = getString() ?: defaultValue.toString()
    }

    private lateinit var paletteGrid: GridView
    private lateinit var colors: IntArray
    private var checkedColor = 0
    private var defaultColor = 0

    private lateinit var binding: ColorPickerDialogBinding

    override fun createDialog(context: Context): Dialog {
        colors = entryValues
        checkedColor = value
        defaultColor = application.getColorCompat(ThemeColor.values()[defaultValue].resourceId)

        binding = ColorPickerDialogBinding.inflate(context.layoutInflater)
        paletteGrid = ViewCompat.requireViewById(binding.root, R.id.palette)
        paletteGrid.adapter = ColorPaletteAdapter(colors)
        val checkedPosition = colors.indexOf(checkedColor)
        if (checkedPosition != -1) {
            paletteGrid.setItemChecked(checkedPosition, true)
        }
        val dialog = Config.dialogBuilderFactory(context).apply {
            setView(binding.root)
            if (defaultColor in colors) {
                setNeutralButton(R.string.default_, null)
            }
            setPositiveButton(android.R.string.ok) { dialog, _ ->
                val checkedPosition = paletteGrid.checkedItemPosition
                if (checkedPosition == -1) {
                    return@setPositiveButton
                }
                val checkedColor = colors[checkedPosition]
                value = checkedColor
            }
            setNegativeButton(android.R.string.cancel, null)
        }.create()

        if (defaultColor in colors) {
            // Override the listener here so that we won't close the dialog.
            dialog.setOnShowListener {
                dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                    paletteGrid.setItemChecked(colors.indexOf(defaultColor), true)
                }
            }
        }

        return dialog
    }
}
