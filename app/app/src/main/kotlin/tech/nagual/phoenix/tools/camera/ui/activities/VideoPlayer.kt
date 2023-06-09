package tech.nagual.phoenix.tools.camera.ui.activities

import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.MediaController
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import tech.nagual.phoenix.R

class VideoPlayer : AppCompatActivity() {

    private var handler: Handler = Handler(Looper.myLooper()!!)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.camera_video_player)

        supportActionBar?.let {
            it.setBackgroundDrawable(
                ColorDrawable(
                    ContextCompat.getColor(
                        this,
                        R.color.camera_appbar
                    )
                )
            )
            it.setDisplayShowTitleEnabled(false)
            it.setDisplayHomeAsUpEnabled(true)
        }

        if (intent.extras?.containsKey("videoUri") != true) {
            throw Exception("Video Player requires videoUri")
        }

        val uri = intent.extras!!.get("videoUri") as Uri

        val videoView = findViewById<VideoView>(R.id.video_player)

        val mediaController = object : MediaController(this) {
            override fun show() {
                super.show()
                supportActionBar?.show()
            }

            override fun hide() {
                super.hide()
                supportActionBar?.hide()
            }
        }
        mediaController.setAnchorView(videoView)

        videoView.setMediaController(mediaController)
        videoView.setVideoURI(uri)
        videoView.requestFocus()
        videoView.start()

        handler.postDelayed(
            { mediaController.show(0) },
            100
        )
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onResume() {
        super.onResume()
        supportActionBar?.show()
    }
}