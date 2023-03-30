package tech.nagual.common.views

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatCheckBox

class MyAppCompatCheckbox : AppCompatCheckBox {
    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle)

    fun setColors(/*textColor: Int,*/ accentColor: Int, backgroundColor: Int) {
//        setTextColor(textColor)
        val colorStateList = ColorStateList(
            arrayOf(
                intArrayOf(-android.R.attr.state_checked),
                intArrayOf(android.R.attr.state_checked)
            ),
            intArrayOf(0xffffffff.toInt()/*textColor.adjustAlpha(0.8f)*/, accentColor)
        )
        supportButtonTintList = colorStateList
    }
}
