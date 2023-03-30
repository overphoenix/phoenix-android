package tech.nagual.phoenix.tools.camera

import android.media.MediaPlayer
import tech.nagual.phoenix.tools.camera.ui.activities.CameraActivity
import tech.nagual.common.R
import tech.nagual.phoenix.tools.camera.ui.activities.CameraActivity.Companion.camConfig

class TunePlayer(mActivity: CameraActivity) {

    private val shutterPlayer: MediaPlayer = MediaPlayer.create(mActivity, R.raw.camera_image_shot)

    private val fSPlayer: MediaPlayer = MediaPlayer.create(mActivity, R.raw.camera_focus_start)
//    private val fCPlayer: MediaPlayer = MediaPlayer.create(mActivity, R.raw.focus_complete)

    private val tIPlayer: MediaPlayer = MediaPlayer.create(mActivity, R.raw.camera_timer_increment)
    private val tCPlayer: MediaPlayer = MediaPlayer.create(mActivity, R.raw.camera_timer_final_second)

    private val vRecPlayer: MediaPlayer = MediaPlayer.create(mActivity, R.raw.camera_video_start)
    private val vStopPlayer: MediaPlayer = MediaPlayer.create(mActivity, R.raw.camera_video_stop)

    private fun shouldNotPlayTune(): Boolean {
        return !camConfig.enableCameraSounds
    }

    fun playShutterSound() {
        if (shouldNotPlayTune()) return
        shutterPlayer.seekTo(0)
        shutterPlayer.start()
    }

    fun playVRStartSound() {
        if (shouldNotPlayTune()) return
        vRecPlayer.seekTo(0)
        vRecPlayer.start()
        // Wait until the audio is played
        try {
            Thread.sleep(vRecPlayer.duration.toLong())
        } catch (exception : Exception) { }
    }

    fun playVRStopSound() {
        if (shouldNotPlayTune()) return
        vStopPlayer.seekTo(0)
        vStopPlayer.start()
    }

    fun playTimerIncrementSound() {
        if (shouldNotPlayTune()) return
        tIPlayer.seekTo(0)
        tIPlayer.start()
    }

    fun playTimerFinalSSound() {
        if (shouldNotPlayTune()) return
        tCPlayer.seekTo(0)
        tCPlayer.start()
    }

    fun playFocusStartSound() {
        if (shouldNotPlayTune()) return
        fSPlayer.seekTo(0)
        fSPlayer.start()
    }
}