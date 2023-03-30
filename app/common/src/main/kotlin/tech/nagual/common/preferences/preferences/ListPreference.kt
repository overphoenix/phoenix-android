package tech.nagual.common.preferences.preferences

import android.content.Context
import android.text.TextUtils
import android.view.ContextThemeWrapper
import android.view.View
import tech.nagual.common.R
import tech.nagual.common.preferences.Preference
import tech.nagual.common.preferences.PreferencesAdapter
import tech.nagual.common.ui.simplemenu.SimpleMenuPopupWindow

class ListPreference(key: String) : Preference(key), SimpleMenuPopupWindow.OnItemClickListener {
    private var mAnchor: View? = null
    private var mItemView: View? = null
    private var mPopupWindow: SimpleMenuPopupWindow? = null

    lateinit var mEntries: Array<CharSequence>
    lateinit var entryValues: Array<CharSequence>
    private var mValue: String? = null
    private var mValueSet = false
    var defaultValue = "0"
//    private lateinit var context: Context

    override fun onClick(holder: PreferencesAdapter.ViewHolder) {
        if (mEntries.isEmpty()) {
            return
        }
        mPopupWindow!!.setEntries(mEntries)
        mPopupWindow!!.selectedIndex = findIndexOfValue(mValue)
        val container = mItemView!!.parent as View // -> list (RecyclerView)
        mPopupWindow!!.show(mItemView!!, container, mAnchor!!.x.toInt())
    }

    private fun findIndexOfValue(value: String?): Int {
        if (value != null) {
            for (i in entryValues.indices.reversed()) {
                if (TextUtils.equals(entryValues[i].toString(), value)) {
                    return i
                }
            }
        }
        return -1
    }

    fun setEntries(entries: Array<CharSequence>) {
        mEntries = entries
        mPopupWindow?.requestMeasure()
    }

    fun getEntries() = mEntries

    fun setValue(value: String) {
        // Always persist/notify the first time.
        val changed = !TextUtils.equals(mValue, value)
        if (changed || !mValueSet) {
            mValue = value
            mValueSet = true
            commitString(value)
            if (changed) {
                requestRebind()
            }
        }
    }

    override fun onAttach() {
        mValue = getString() ?: defaultValue
        summary = mEntries[mValue!!.toInt()]
    }

    override fun bindViews(holder: PreferencesAdapter.ViewHolder) {
        super.bindViews(holder)

        val context = holder.root.context

        mItemView = holder.itemView
        mAnchor = holder.icon

        val popupContext: Context = ContextThemeWrapper(context, R.style.ThemeOverlay_Preference_SimpleMenuPreference_PopupMenu)
        mPopupWindow = SimpleMenuPopupWindow(
            popupContext,
            null,
            R.styleable.SimpleMenuPreference_android_popupMenuStyle,
            R.style.Widget_Preference_SimpleMenuPreference_PopupMenu
        )
        mPopupWindow!!.onItemClickListener = this
    }

    companion object {
        var isLightFixEnabled = false
    }

    override fun onItemClick(i: Int) {
        val value = entryValues[i].toString()
        setValue(value)
        summary = mEntries[value.toInt()]
    }
}