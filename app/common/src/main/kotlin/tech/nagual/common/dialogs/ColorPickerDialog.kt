package tech.nagual.common.dialogs

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Color
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import tech.nagual.common.R
import tech.nagual.common.databinding.DialogColorPickerBinding
import tech.nagual.common.extensions.beVisible
import tech.nagual.common.extensions.onGlobalLayout
import tech.nagual.common.extensions.setupDialogStuff
import tech.nagual.common.extensions.*
import tech.nagual.common.extensions.*
import tech.nagual.common.views.ColorPickerSquare
import java.util.*

private const val RECENT_COLORS_NUMBER = 5

// forked from https://github.com/yukuku/ambilwarna
@SuppressLint("ClickableViewAccessibility")
class ColorPickerDialog(
    val activity: Activity,
    color: Int,
    val removeDimmedBackground: Boolean = false,
    showUseDefaultButton: Boolean = false,
    val currentColorCallback: ((color: Int) -> Unit)? = null,
    val callback: (wasPositivePressed: Boolean, color: Int) -> Unit
) {
    var viewHue: View
    var viewSatVal: ColorPickerSquare
    var viewCursor: ImageView
    var viewNewColor: ImageView
    var viewTarget: ImageView
    var newHexField: EditText
    var viewContainer: ViewGroup
    private val baseConfig = activity.baseConfig
    private val currentColorHsv = FloatArray(3)
    private val backgroundColor = activity.resources.getColor(R.color.default_background_color)
    private var isHueBeingDragged = false
    private var wasDimmedBackgroundRemoved = false
    private var dialog: AlertDialog? = null

    private val binding: DialogColorPickerBinding =
        DialogColorPickerBinding.inflate(activity.layoutInflater)

    init {
        Color.colorToHSV(color, currentColorHsv)

        val view = binding.root.apply {
            viewHue = binding.colorPickerHue
            viewSatVal = binding.colorPickerSquare
            viewCursor = binding.colorPickerHueCursor

            viewNewColor = binding.colorPickerNewColor
            viewTarget = binding.colorPickerCursor
            viewContainer = binding.colorPickerHolder
            newHexField = binding.colorPickerNewHex

            viewSatVal.setHue(getHue())

            viewNewColor.setFillWithStroke(getColor(), backgroundColor)
            binding.colorPickerOldColor.setFillWithStroke(color, backgroundColor)

            val hexCode = getHexCode(color)
            binding.colorPickerOldHex.text = "#$hexCode"
            binding.colorPickerOldHex.setOnLongClickListener {
                activity.copyToClipboard(hexCode)
                true
            }
            newHexField.setText(hexCode)
            setupRecentColors()
        }

        viewHue.setOnTouchListener(OnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                isHueBeingDragged = true
            }

            if (event.action == MotionEvent.ACTION_MOVE || event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_UP) {
                var y = event.y
                if (y < 0f)
                    y = 0f

                if (y > viewHue.measuredHeight) {
                    y =
                        viewHue.measuredHeight - 0.001f // to avoid jumping the cursor from bottom to top.
                }
                var hue = 360f - 360f / viewHue.measuredHeight * y
                if (hue == 360f)
                    hue = 0f

                currentColorHsv[0] = hue
                updateHue()
                newHexField.setText(getHexCode(getColor()))

                if (event.action == MotionEvent.ACTION_UP) {
                    isHueBeingDragged = false
                }
                return@OnTouchListener true
            }
            false
        })

        viewSatVal.setOnTouchListener(OnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_MOVE || event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_UP) {
                var x = event.x
                var y = event.y

                if (x < 0f)
                    x = 0f
                if (x > viewSatVal.measuredWidth)
                    x = viewSatVal.measuredWidth.toFloat()
                if (y < 0f)
                    y = 0f
                if (y > viewSatVal.measuredHeight)
                    y = viewSatVal.measuredHeight.toFloat()

                currentColorHsv[1] = 1f / viewSatVal.measuredWidth * x
                currentColorHsv[2] = 1f - 1f / viewSatVal.measuredHeight * y

                moveColorPicker()
                viewNewColor.setFillWithStroke(getColor(), backgroundColor)
                newHexField.setText(getHexCode(getColor()))
                return@OnTouchListener true
            }
            false
        })

        newHexField.onTextChangeListener {
            if (it.length == 6 && !isHueBeingDragged) {
                try {
                    val newColor = Color.parseColor("#$it")
                    Color.colorToHSV(newColor, currentColorHsv)
                    updateHue()
                    moveColorPicker()
                } catch (ignored: Exception) {
                }
            }
        }

        val textColor = Color.GRAY
        val builder = MaterialAlertDialogBuilder(activity)
            .setPositiveButton(R.string.ok) { dialog, which -> confirmNewColor() }
            .setNegativeButton(R.string.cancel) { dialog, which -> dialogDismissed() }
            .setOnCancelListener { dialogDismissed() }

        if (showUseDefaultButton) {
            builder.setNeutralButton(R.string.use_default) { dialog, which -> useDefault() }
        }

        dialog = builder.create().apply {
            activity.setupDialogStuff(view, this) {
                binding.colorPickerArrow.applyColorFilter(textColor)
                binding.colorPickerHexArrow.applyColorFilter(textColor)
                viewCursor.applyColorFilter(textColor)
            }
        }

        view.onGlobalLayout {
            moveHuePicker()
            moveColorPicker()
        }
    }

    private fun View.setupRecentColors() {
        val recentColors = baseConfig.colorPickerRecentColors
        if (recentColors.isNotEmpty()) {
            binding.recentColors.beVisible()
            val squareSize = context.resources.getDimensionPixelSize(R.dimen.colorpicker_hue_width)
            recentColors.take(RECENT_COLORS_NUMBER).reversed().forEach { recentColor ->
                val recentColorView = ImageView(context)
                recentColorView.id = View.generateViewId()
                recentColorView.layoutParams = ViewGroup.LayoutParams(squareSize, squareSize)
                recentColorView.setFillWithStroke(recentColor, backgroundColor)
                recentColorView.setOnClickListener { newHexField.setText(getHexCode(recentColor)) }
                binding.recentColors.addView(recentColorView)
                binding.recentColorsFlow.addView(recentColorView)
            }
        }
    }

    private fun dialogDismissed() {
        callback(false, 0)
    }

    private fun confirmNewColor() {
        val hexValue = newHexField.value
        val newColor = if (hexValue.length == 6) {
            Color.parseColor("#$hexValue")
        } else {
            getColor()
        }
        addRecentColor(newColor)

        callback(true, newColor)
    }

    private fun useDefault() {
        val defaultColor = -1
        addRecentColor(defaultColor)

        callback(true, defaultColor)
    }

    private fun addRecentColor(color: Int) {
        var recentColors = baseConfig.colorPickerRecentColors

        recentColors.remove(color)
        if (recentColors.size >= RECENT_COLORS_NUMBER) {
            val numberOfColorsToDrop = recentColors.size - RECENT_COLORS_NUMBER + 1
            recentColors = LinkedList(recentColors.dropLast(numberOfColorsToDrop))
        }
        recentColors.addFirst(color)

        baseConfig.colorPickerRecentColors = recentColors
    }

    private fun getHexCode(color: Int) = color.toHex().substring(1)

    private fun updateHue() {
        viewSatVal.setHue(getHue())
        moveHuePicker()
        viewNewColor.setFillWithStroke(getColor(), backgroundColor)
        if (removeDimmedBackground && !wasDimmedBackgroundRemoved) {
            dialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            wasDimmedBackgroundRemoved = true
        }

        currentColorCallback?.invoke(getColor())
    }

    private fun moveHuePicker() {
        var y = viewHue.measuredHeight - getHue() * viewHue.measuredHeight / 360f
        if (y == viewHue.measuredHeight.toFloat())
            y = 0f

        viewCursor.x = (viewHue.left - viewCursor.width).toFloat()
        viewCursor.y = viewHue.top + y - viewCursor.height / 2
    }

    private fun moveColorPicker() {
        val x = getSat() * viewSatVal.measuredWidth
        val y = (1f - getVal()) * viewSatVal.measuredHeight
        viewTarget.x = viewSatVal.left + x - viewTarget.width / 2
        viewTarget.y = viewSatVal.top + y - viewTarget.height / 2
    }

    private fun getColor() = Color.HSVToColor(currentColorHsv)
    private fun getHue() = currentColorHsv[0]
    private fun getSat() = currentColorHsv[1]
    private fun getVal() = currentColorHsv[2]
}
