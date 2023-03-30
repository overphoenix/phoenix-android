package tech.nagual.phoenix.tools.organizer.utils

import android.content.Context
import tech.nagual.common.R
import tech.nagual.phoenix.tools.organizer.data.model.NoteColor

fun NoteColor.resId(context: Context): Int? {
    val colorId = when (this) {
        NoteColor.Green -> R.color.note_color_green
        NoteColor.Pink -> R.color.note_color_pink
        NoteColor.Blue -> R.color.note_color_blue
        NoteColor.Red -> R.color.note_color_red
        NoteColor.Orange -> R.color.note_color_orange
        NoteColor.Brown -> R.color.note_color_brown
        NoteColor.Purple -> R.color.note_color_purple
        else -> R.color.note_color_default
    }

    return context.getColor(colorId)
}
