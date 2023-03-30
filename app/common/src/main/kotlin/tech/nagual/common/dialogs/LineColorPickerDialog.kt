package tech.nagual.common.dialogs

import android.view.Menu
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import tech.nagual.common.R
import tech.nagual.common.databinding.DialogLineColorPickerBinding
import tech.nagual.common.extensions.beGoneIf
import tech.nagual.common.extensions.beVisibleIf
import tech.nagual.common.extensions.setupDialogStuff
import tech.nagual.common.activities.BaseSimpleActivity
import tech.nagual.common.extensions.*
import tech.nagual.common.extensions.*
import tech.nagual.common.interfaces.LineColorPickerListener
import java.util.*

class LineColorPickerDialog(
    val activity: tech.nagual.common.activities.BaseSimpleActivity,
    val color: Int,
    val isPrimaryColorPicker: Boolean,
    val primaryColors: Int = R.array.md_primary_colors,
    val appIconIDs: ArrayList<Int>? = null,
    val menu: Menu? = null,
    val callback: (wasPositivePressed: Boolean, color: Int) -> Unit
) {

    private val PRIMARY_COLORS_COUNT = 19
    private val DEFAULT_PRIMARY_COLOR_INDEX = 14
    private val DEFAULT_SECONDARY_COLOR_INDEX = 6
    private val DEFAULT_COLOR_VALUE = activity.resources.getColor(R.color.color_primary)

    private var wasDimmedBackgroundRemoved = false
    private var dialog: AlertDialog? = null
    private val binding: DialogLineColorPickerBinding =
        DialogLineColorPickerBinding.inflate(activity.layoutInflater)
    private var view: View = binding.root

    init {
        view.apply {
            binding.hexCode.text = color.toHex()
            binding.hexCode.setOnLongClickListener {
                activity.copyToClipboard(binding.hexCode.value.substring(1))
                true
            }

            binding.lineColorPickerIcon.beGoneIf(isPrimaryColorPicker)
            val indexes = getColorIndexes(color)

            val primaryColorIndex = indexes.first
            primaryColorChanged(primaryColorIndex)
            binding.primaryLineColorPicker.updateColors(getColors(primaryColors), primaryColorIndex)
            binding.primaryLineColorPicker.listener = object : LineColorPickerListener {
                override fun colorChanged(index: Int, color: Int) {
                    val secondaryColors = getColorsForIndex(index)
                    binding.secondaryLineColorPicker.updateColors(secondaryColors)

                    val newColor =
                        if (isPrimaryColorPicker) binding.secondaryLineColorPicker.getCurrentColor() else color
                    colorUpdated(newColor)

                    if (!isPrimaryColorPicker) {
                        primaryColorChanged(index)
                    }
                }
            }

            binding.secondaryLineColorPicker.beVisibleIf(isPrimaryColorPicker)
            binding.secondaryLineColorPicker.updateColors(
                getColorsForIndex(primaryColorIndex),
                indexes.second
            )
            binding.secondaryLineColorPicker.listener = object : LineColorPickerListener {
                override fun colorChanged(index: Int, color: Int) {
                    colorUpdated(color)
                }
            }
        }

        dialog = MaterialAlertDialogBuilder(activity)
            .setPositiveButton(R.string.ok) { dialog, which -> dialogConfirmed() }
            .setNegativeButton(R.string.cancel) { dialog, which -> dialogDismissed() }
            .setOnCancelListener { dialogDismissed() }
            .create().apply {
                activity.setupDialogStuff(view, this)
            }
    }

    fun getSpecificColor() = binding.secondaryLineColorPicker.getCurrentColor()

    private fun colorUpdated(color: Int) {
        binding.hexCode.text = color.toHex()
        if (isPrimaryColorPicker) {
            if (!wasDimmedBackgroundRemoved) {
                dialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                wasDimmedBackgroundRemoved = true
            }
        }
    }

    private fun getColorIndexes(color: Int): Pair<Int, Int> {
        if (color == DEFAULT_COLOR_VALUE) {
            return getDefaultColorPair()
        }

        for (i in 0 until PRIMARY_COLORS_COUNT) {
            getColorsForIndex(i).indexOfFirst { color == it }.apply {
                if (this != -1) {
                    return Pair(i, this)
                }
            }
        }

        return getDefaultColorPair()
    }

    private fun primaryColorChanged(index: Int) {
        binding.lineColorPickerIcon.setImageResource(appIconIDs?.getOrNull(index) ?: 0)
    }

    private fun getDefaultColorPair() =
        Pair(DEFAULT_PRIMARY_COLOR_INDEX, DEFAULT_SECONDARY_COLOR_INDEX)

    private fun dialogDismissed() {
        callback(false, 0)
    }

    private fun dialogConfirmed() {
        val targetView =
            if (isPrimaryColorPicker) binding.secondaryLineColorPicker else binding.primaryLineColorPicker
        val color = targetView.getCurrentColor()
        callback(true, color)
    }

    private fun getColorsForIndex(index: Int) = when (index) {
        0 -> getColors(R.array.md_reds)
        1 -> getColors(R.array.md_pinks)
        2 -> getColors(R.array.md_purples)
        3 -> getColors(R.array.md_deep_purples)
        4 -> getColors(R.array.md_indigos)
        5 -> getColors(R.array.md_blues)
        6 -> getColors(R.array.md_light_blues)
        7 -> getColors(R.array.md_cyans)
        8 -> getColors(R.array.md_teals)
        9 -> getColors(R.array.md_greens)
        10 -> getColors(R.array.md_light_greens)
        11 -> getColors(R.array.md_limes)
        12 -> getColors(R.array.md_yellows)
        13 -> getColors(R.array.md_ambers)
        14 -> getColors(R.array.md_oranges)
        15 -> getColors(R.array.md_deep_oranges)
        16 -> getColors(R.array.md_browns)
        17 -> getColors(R.array.md_blue_greys)
        18 -> getColors(R.array.md_greys)
        else -> throw RuntimeException("Invalid color id $index")
    }

    private fun getColors(id: Int) = activity.resources.getIntArray(id).toCollection(ArrayList())
}
