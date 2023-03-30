package tech.nagual.common.ui

import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.core.view.get
import androidx.core.view.size
import tech.nagual.common.databinding.GenericReadonlyTextItemBinding
import me.zhanghai.android.files.util.layoutInflater

class ViewBuilder(private val linearLayout: LinearLayout) {
    private var itemCount = 0

    fun addItemView(
        hint: String,
        text: String,
        onClickListener: ((View) -> Unit)? = null
    ): TextView {
        val itemBinding = if (itemCount < linearLayout.size) {
            linearLayout[itemCount].tag as GenericReadonlyTextItemBinding
        } else {
            GenericReadonlyTextItemBinding.inflate(
                linearLayout.context.layoutInflater, linearLayout, true
            ).also { it.root.tag = it }
        }
        itemBinding.textInputLayout.hint = hint
        itemBinding.textInputLayout.setDropDown(onClickListener != null)
        itemBinding.text.setText(text)
        itemBinding.text.setTextIsSelectable(onClickListener == null)
        itemBinding.text.setOnClickListener(
            onClickListener?.let { View.OnClickListener(it) }
        )
        ++itemCount
        return itemBinding.text
    }

    fun addItemView(
        @StringRes hintRes: Int,
        text: String,
        onClickListener: ((View) -> Unit)? = null
    ): TextView = addItemView(linearLayout.context.getString(hintRes), text, onClickListener)

    fun build() {
        for (index in linearLayout.size - 1 downTo itemCount) {
            linearLayout.removeViewAt(index)
        }
    }
}