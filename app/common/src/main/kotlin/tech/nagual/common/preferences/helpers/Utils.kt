package tech.nagual.common.preferences.helpers

import android.widget.SeekBar

const val KEY_ROOT_SCREEN = "root"

const val DEFAULT_RES_ID = -1

internal fun SeekBar.onSeek(callback: (Int, Boolean) -> Unit) {
    setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
        @Suppress("EmptyFunctionBlock")
        override fun onStartTrackingTouch(seekBar: SeekBar) {
        }

        override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
            if (fromUser) callback(progress, false)
        }

        override fun onStopTrackingTouch(seekBar: SeekBar) {
            callback(seekBar.progress, true)
        }
    })
}