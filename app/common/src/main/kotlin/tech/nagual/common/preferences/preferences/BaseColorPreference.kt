package tech.nagual.common.preferences.preferences

import android.graphics.drawable.GradientDrawable
import android.view.View
import androidx.annotation.ColorInt
import tech.nagual.common.R
import tech.nagual.common.preferences.PreferencesAdapter
import me.zhanghai.android.files.util.getFloatByAttr
import kotlin.math.roundToInt

abstract class BaseColorPreference(
    key: String
) : DialogPreference(key) {
    override fun getWidgetLayoutResource() = R.layout.color_preference_widget

    override fun bindViews(holder: PreferencesAdapter.ViewHolder) {
        super.bindViews(holder)
        val swatchView: View? = holder.widget?.findViewById(R.id.swatch)
        if (swatchView != null) {
            val swatchDrawable = swatchView.background as GradientDrawable
            swatchDrawable.setColor(value)
            var alpha = 0xFF
            if (!enabled) {
                val disabledAlpha =
                    holder.widget.context.getFloatByAttr(android.R.attr.disabledAlpha)
                alpha = (disabledAlpha * alpha).roundToInt()
            }
            swatchDrawable.alpha = alpha
        }
    }

    @get:ColorInt
    abstract var value: Int

    @get:ColorInt
    abstract val defaultValue: Int

    abstract val entryValues: IntArray
}
