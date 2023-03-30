package tech.nagual.phoenix.tools.organizer.photoeditor.models

import android.graphics.Color

data class PaintOptions(
    var color: Int = Color.BLACK,
    var strokeWidth: Float = 5f
) {
    fun getColorToExport() = "#${Integer.toHexString(color).substring(2)}"
}
