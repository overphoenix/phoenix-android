package tech.nagual.common.preferences.preferences

import android.content.Context
import android.graphics.drawable.StateListDrawable
import android.widget.CompoundButton
import tech.nagual.common.preferences.PreferencesAdapter

@Suppress("MemberVisibilityCanBePrivate")
abstract class TwoStatePreference(key: String) : StatefulPreference(key) {
    private var checkedInternal = false
    var checked: Boolean
        get() = checkedInternal
        set(value) {
            checkNotNull(parent) {
                "Setting the checked value before the preference was attached isn't supported. Consider using `defaultValue` instead."
            }
            if (value != checkedInternal) updateState(null, value)
        }

    /**
     * The default value of this preference, when nothing was committed to storage yet
     */
    var defaultValue = false
    var checkedChangeListener: OnCheckedChangeListener? = null

    var summaryOn: CharSequence? = null
    var summaryOnRes: Int = -1

    /**
     * When set to true, dependents are disabled when this preference is checked,
     * and are enabled when it's not
     */
    var disableDependents = false

    override val state: Boolean get() = checkedInternal xor disableDependents

    override fun onAttach() {
        checkedInternal = getBoolean(defaultValue)
        super.onAttach()
    }

    override fun resolveSummary(context: Context) = when {
        checkedInternal && summaryOnRes != -1 -> context.resources.getText(summaryOnRes)
        checkedInternal && summaryOn != null -> summaryOn
        else -> super.resolveSummary(context)
    }

    override fun bindViews(holder: PreferencesAdapter.ViewHolder) {
        super.bindViews(holder)
        updateButton(holder)
    }

    private fun updateState(holder: PreferencesAdapter.ViewHolder?, new: Boolean) {
        if (checkedChangeListener?.onCheckedChanged(this, holder, new) != false) {
            commitBoolean(new)
            checkedInternal = new // Update internal state
            if (holder != null) {
                if (summaryOnRes != -1 || summaryOn != null) {
                    bindViews(holder)
                } else updateButton(holder)
            } else requestRebind()
            publishState()
        }
    }

    private fun updateButton(holder: PreferencesAdapter.ViewHolder) {
        holder.icon?.drawable?.apply {
            if (this is StateListDrawable) {
                setState(
                    if (checkedInternal) intArrayOf(android.R.attr.state_checked) else IntArray(
                        0
                    )
                )
            }
        }
        (holder.widget as CompoundButton).isChecked = checkedInternal
    }

    override fun onClick(holder: PreferencesAdapter.ViewHolder) {
        updateState(holder, !checkedInternal)
    }

    fun interface OnCheckedChangeListener {
        /**
         * Notified when the [checked][TwoStatePreference.checked] state of the connected [TwoStatePreference] changes.
         * This is called before the change gets persisted and can be prevented by returning false.
         *
         * @param holder the [ViewHolder][PreferencesAdapter.ViewHolder] with the views of the Preference instance,
         * or null if the change didn't occur as part of a click event
         * @param checked the new state
         *
         * @return true to commit the new button state to [SharedPreferences][android.content.SharedPreferences]
         */
        fun onCheckedChanged(
            preference: TwoStatePreference,
            holder: PreferencesAdapter.ViewHolder?,
            checked: Boolean
        ): Boolean
    }
}