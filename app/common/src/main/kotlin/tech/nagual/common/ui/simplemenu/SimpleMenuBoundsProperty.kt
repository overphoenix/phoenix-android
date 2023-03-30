package tech.nagual.common.ui.simplemenu

import android.graphics.Rect
import android.util.Property

internal class SimpleMenuBoundsProperty(name: String?) : Property<PropertyHolder, Rect>(
    Rect::class.java, name
) {
    companion object {
        var BOUNDS: Property<PropertyHolder, Rect>? = null

        init {
            BOUNDS = SimpleMenuBoundsProperty("bounds")
        }
    }

    override fun get(holder: PropertyHolder): Rect? {
        return holder.bounds
    }

    override fun set(holder: PropertyHolder, value: Rect?) {
        holder.bounds = value
    }
}