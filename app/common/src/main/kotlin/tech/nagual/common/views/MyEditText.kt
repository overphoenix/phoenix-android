package tech.nagual.common.views

import android.content.Context
import android.util.AttributeSet
import android.widget.EditText
import tech.nagual.common.extensions.applyColorFilter

class MyEditText : EditText {
    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    )

    fun setColors(/*textColor: Int,*/ accentColor: Int, backgroundColor: Int) {
        background?.mutate()?.applyColorFilter(accentColor)

        setLinkTextColor(accentColor)
    }
}
