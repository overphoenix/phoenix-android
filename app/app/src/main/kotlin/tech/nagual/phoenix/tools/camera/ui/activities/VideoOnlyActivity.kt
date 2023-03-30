package tech.nagual.phoenix.tools.camera.ui.activities

import android.os.Bundle
import tech.nagual.common.R

class VideoOnlyActivity : CameraActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        captureButton.setImageResource(R.drawable.camera_recording)

        tabLayout.alpha = 0f
        tabLayout.isClickable = false
        tabLayout.isEnabled = false
//        (tabLayout.layoutParams as ViewGroup.MarginLayoutParams).let {
//            it.setMargins(it.leftMargin, it.height, it.rightMargin, it.bottomMargin)
//            it.height = 0
//        }
//
//        (previewView.layoutParams as ViewGroup.MarginLayoutParams).let {
//            it.setMargins(it.leftMargin, it.topMargin, it.rightMargin, 0)
//        }
    }

}