package tech.nagual.phoenix.tools.organizer.camera.ui.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore.EXTRA_OUTPUT
import android.view.View
import tech.nagual.common.R

class VideoCaptureActivity : CaptureActivity() {

    private var savedUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        captureButton.setImageResource(R.drawable.camera_recording)

        captureButton.setOnClickListener OnClickListener@{
            if (videoCapturer.isRecording) {
                videoCapturer.stopRecording()
            } else {
                videoCapturer.startRecording()
            }
        }
        camConfig.isTakingPicture = false

        confirmButton.setOnClickListener {
            confirmVideo()
        }

    }

    fun afterRecording(savedUri: Uri?) {

        this.savedUri = savedUri

        bitmap = previewView.bitmap!!

        cancelButtonView.visibility = View.VISIBLE

        showPreview()
    }

    override fun showPreview() {
        super.showPreview()
        thirdOption.visibility = View.VISIBLE
    }

    private fun confirmVideo() {
        if (savedUri == null) {
            setResult(RESULT_CANCELED)
        } else {
            val resultIntent = Intent()
            resultIntent.data = savedUri
            resultIntent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            resultIntent.putExtra(
                EXTRA_OUTPUT,
                savedUri
            )
            setResult(RESULT_OK, resultIntent)
        }
        finish()
    }

    override fun hidePreview() {
        super.hidePreview()
        thirdOption.visibility = View.INVISIBLE
    }
}