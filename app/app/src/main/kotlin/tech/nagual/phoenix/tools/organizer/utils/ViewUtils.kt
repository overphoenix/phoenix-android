package tech.nagual.phoenix.tools.organizer.utils

import android.text.Layout
import android.view.View
import android.view.ViewTreeObserver
import android.widget.TextView
import androidx.core.view.postDelayed
import androidx.drawerlayout.widget.DrawerLayout
import tech.nagual.common.extensions.showKeyboard
import tech.nagual.phoenix.tools.organizer.common.ExtendedEditText


fun View.requestFocusAndKeyboard() {
    postDelayed(100) {
        if (this is ExtendedEditText) {
            requestFocusAndMoveCaret()
        } else {
            requestFocus()
        }

        if (hasWindowFocus()) return@postDelayed showKeyboard()

        viewTreeObserver.addOnWindowFocusChangeListener(
            object : ViewTreeObserver.OnWindowFocusChangeListener {
                override fun onWindowFocusChanged(hasFocus: Boolean) {
                    if (hasFocus) {
                        this@requestFocusAndKeyboard.showKeyboard()
                        viewTreeObserver.removeOnWindowFocusChangeListener(this)
                    }
                }
            }
        )
    }
}

fun TextView.ellipsize() {
    viewTreeObserver.addOnGlobalLayoutListener(object :
        ViewTreeObserver.OnGlobalLayoutListener {
        override fun onGlobalLayout() {
            viewTreeObserver.removeOnGlobalLayoutListener(this)
            val maxLines: Int = maxLines
            if (layout != null) {
                val layout: Layout = layout
                if (layout.lineCount > maxLines) {
                    val end: Int = layout.getLineEnd(maxLines - 1)
                    setText(text.subSequence(0, end - 3), TextView.BufferType.SPANNABLE)
                    append("...")
                }
            }
        }
    }
    )
}

inline fun DrawerLayout.closeAndThen(crossinline block: () -> Unit) {
    addDrawerListener(object : DrawerLayout.DrawerListener {
        override fun onDrawerSlide(drawerView: View, slideOffset: Float) {}

        override fun onDrawerOpened(drawerView: View) {}

        override fun onDrawerClosed(drawerView: View) {
            removeDrawerListener(this)
            block()
        }

        override fun onDrawerStateChanged(newState: Int) {}
    })

    close()
}
