package tech.nagual.common.preferences.preferences

import tech.nagual.common.R

class CheckBoxPreference(key: String) : TwoStatePreference(key) {
    override fun getWidgetLayoutResource() = R.layout.map_preference_widget_checkbox
}