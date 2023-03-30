package tech.nagual.common.preferences.preferences

import android.graphics.Typeface
import android.transition.ChangeBounds
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import tech.nagual.common.preferences.Preference
import tech.nagual.common.preferences.PreferencesAdapter
import tech.nagual.common.R

class ExpandableTextPreference(key: String) : Preference(key) {
    private var expanded = false

    @StringRes
    var textRes: Int = -1
    var text: CharSequence? = null

    var monospace = true

    override fun getWidgetLayoutResource() = R.layout.map_preference_widget_expand_arrow

    override fun bindViews(holder: PreferencesAdapter.ViewHolder) {
        super.bindViews(holder)
        val widget = holder.widget as CheckBox
        val inflater = LayoutInflater.from(widget.context)
        val tv: TextView = (widget.tag ?: run {
            inflater.inflate(R.layout.map_preference_expand_text, holder.root)
                .findViewById<TextView>(android.R.id.message)
        }) as TextView
        widget.tag = tv
        tv.apply {
            if (textRes != -1) setText(textRes) else text = this@ExpandableTextPreference.text
            typeface = if (monospace) Typeface.MONOSPACE else Typeface.SANS_SERIF
            with(context.obtainStyledAttributes(intArrayOf(R.attr.expandableTextBackgroundColor))) {
                setBackgroundColor(
                    getColor(
                        0,
                        ContextCompat.getColor(
                            context,
                            R.color.expandableTextBackgroundColorDefault
                        )
                    )
                )
                recycle()
            }
            isEnabled = enabled
        }
        refreshArrowState(widget)
        refreshTextExpandState(tv)
    }

    override fun onClick(holder: PreferencesAdapter.ViewHolder) {
        expanded = !expanded
        refreshArrowState(holder.widget as CheckBox)
        refreshTextExpandState(holder.widget.tag as TextView)
    }

    private fun refreshArrowState(widget: CheckBox) {
        widget.isChecked = expanded
    }

    private fun refreshTextExpandState(text: TextView) {
        TransitionManager.beginDelayedTransition(text.parent as ViewGroup, ChangeBounds())
        text.isVisible = expanded
    }
}