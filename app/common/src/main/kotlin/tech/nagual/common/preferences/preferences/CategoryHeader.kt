package tech.nagual.common.preferences.preferences

import android.annotation.SuppressLint
import tech.nagual.common.preferences.Preference

class CategoryHeader(key: String) : Preference(key) {
    @SuppressLint("ResourceType")
    override fun getWidgetLayoutResource() = RESOURCE_CONST

    internal companion object {
        internal const val RESOURCE_CONST = -2
    }
}