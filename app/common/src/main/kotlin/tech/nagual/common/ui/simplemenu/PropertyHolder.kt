package tech.nagual.common.ui.simplemenu

import android.graphics.Rect
import android.view.View

/**
 * Holder class holds background drawable and content view.
 */
internal class PropertyHolder(private val background: CustomBoundsDrawable, val contentView: View) {
    var bounds: Rect?
        get() = background.bounds
        set(value) {
            background.setCustomBounds(value!!)
            contentView.invalidateOutline()
        }
}