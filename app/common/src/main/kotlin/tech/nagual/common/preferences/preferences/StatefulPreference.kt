package tech.nagual.common.preferences.preferences

import tech.nagual.common.preferences.Preference
import tech.nagual.common.preferences.helpers.DependencyManager

abstract class StatefulPreference(key: String) : Preference(key) {
    internal abstract val state: Boolean

    override fun onAttach() {
        publishState()
    }

    internal fun publishState() = DependencyManager.publishState(this)
}