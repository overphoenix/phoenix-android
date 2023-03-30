package tech.nagual.phoenix.tools.organizer.camera.ui.activities

import android.content.Intent
import android.graphics.*
import android.graphics.Bitmap.CompressFormat
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.widget.ImageButton
import android.widget.ImageView
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import tech.nagual.phoenix.R
import tech.nagual.phoenix.tools.organizer.camera.CamConfig
import tech.nagual.phoenix.tools.organizer.components.MediaStorageManager
import tech.nagual.common.extensions.adjustAlpha
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import javax.inject.Inject

@AndroidEntryPoint
open class MultiCaptureActivity : CameraActivity() {

    @Inject
    lateinit var mediaStorageManager: MediaStorageManager

    var outputUris: ArrayList<Uri> = arrayListOf()

    lateinit var bitmap: Bitmap

    private lateinit var retakeIcon: ImageView
    private lateinit var addIcon: ImageView

    private lateinit var flipCameraContent: ImageView
    lateinit var confirmButton: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        retakeIcon = findViewById(R.id.retake_icon)
        addIcon = findViewById(R.id.add_icon)
        flipCameraContent = findViewById(R.id.flip_camera_icon_content)

        confirmButton = findViewById(R.id.confirm_button)

        // Disable capture button for a while (to avoid picture capture)
        captureButton.isEnabled = false
        captureButton.alpha = 0f

        // Enable the capture button after a while
        Handler(Looper.getMainLooper()).postDelayed({

            captureButton.animate()
                .alpha(1f)
                .setDuration(300)
                .withEndAction {
                    captureButton.isEnabled = true
                }

        }, 2000)

        // Remove the modes tab layout as we do not want the user to be able to switch to
        // another custom mode in this state
        tabLayout.visibility = View.INVISIBLE

        // Remove the margin so that that the previewView can take some more space
        (previewView.layoutParams as MarginLayoutParams).let {
            it.setMargins(it.leftMargin, it.topMargin, it.rightMargin, 0)
        }

        // Bring the three buttons a bit down in the UI
        (threeButtons.layoutParams as MarginLayoutParams).let {
            it.setMargins(it.leftMargin, it.topMargin, it.rightMargin, 0)
        }

        // Change the drawable to cancel mode
        cancelButtonView.setImageResource(R.drawable.camera_cancel)

        // Overwrite the existing listener to just close the existing activity
        // (in this case)
        cancelButtonView.setOnClickListener {
            finish()
        }

        thirdOption.visibility = View.INVISIBLE

        captureButton.setOnClickListener {
            if (timerDuration == 0) {
                takePicture()
            } else {
                if (cdTimer.isRunning) {
                    cdTimer.cancelTimer()
                } else {
                    cdTimer.startTimer()
                }
            }
        }

        retakeIcon.setOnClickListener {
            outputUris.clear()
            hidePreview()
        }

        confirmButton.setOnClickListener {
            lifecycleScope.launch {
                confirmImage()
            }
        }

        addIcon.setOnClickListener {
            lifecycleScope.launch {
                handleNewPhoto { result ->
                    if (result) {
                        hidePreview()
                    }
                }
            }
        }
    }

    private fun postProcessPhoto(bitmap: Bitmap): Bitmap {
        if (camConfig.gridType == CamConfig.GridType.SIGHT) {
            val fixedBitmap = bitmap.copy(bitmap.config, true);
            val canvas = Canvas(fixedBitmap)
            val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG)
            circlePaint.strokeWidth = 1f
            circlePaint.style = Paint.Style.FILL
            circlePaint.color = Color.argb(32, 255, 255, 255)

            val pointPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            pointPaint.strokeWidth = 1f
            pointPaint.style = Paint.Style.FILL
            pointPaint.color =
                tech.nagual.app.application.getColor(R.color.material_red_500)
                    .adjustAlpha(0.8f)

            val radius: Float = bitmap.width.coerceAtMost(bitmap.height) / 8f
            val x1 = bitmap.width / 2f
            val y1 = bitmap.height / 2f

            canvas.drawCircle(x1, y1, radius, circlePaint)
            canvas.drawCircle(x1, y1, radius / 12f, pointPaint)

            return fixedBitmap
        }

        return bitmap
    }

    fun takePicture() {
        camConfig.isTakingPicture = true
        camConfig.imageCapture?.takePicture(
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    super.onCaptureSuccess(image)
                    bitmap = imageProxyToBitmap(image, image.imageInfo.rotationDegrees.toFloat())
                    bitmap = postProcessPhoto(bitmap)
                    showPreview()
                    camConfig.isTakingPicture = false
                    image.close()
                }

                override fun onError(exception: ImageCaptureException) {
                    super.onError(exception)
                    exception.printStackTrace()

                    finishActivity(RESULT_CANCELED)
                }
            }
        )
    }

    open fun showPreview() {
        camConfig.cameraProvider?.unbindAll()

        mainOverlay.setImageBitmap(bitmap)
        mainOverlay.visibility = View.VISIBLE

        settingsIcon.visibility = View.INVISIBLE

        flipCameraContent.visibility = View.INVISIBLE
        retakeIcon.visibility = View.VISIBLE

        thirdOption.visibility = View.VISIBLE

        captureButton.visibility = View.INVISIBLE
        confirmButton.visibility = View.VISIBLE

        previewView.visibility = View.INVISIBLE
    }

    open fun hidePreview() {
        camConfig.startCamera(true)

        settingsIcon.visibility = View.VISIBLE

        flipCameraContent.visibility = View.VISIBLE
        retakeIcon.visibility = View.INVISIBLE

        thirdOption.visibility = View.INVISIBLE

        captureButton.visibility = View.VISIBLE
        confirmButton.visibility = View.INVISIBLE

        previewView.visibility = View.VISIBLE
    }

    private suspend fun confirmImage() {
        val resultIntent = Intent("inline-data")

        handleNewPhoto { result ->
            if (result) {
                resultIntent.putParcelableArrayListExtra("photos", outputUris)
                setResult(RESULT_OK, resultIntent)
            } else {
                setResult(RESULT_CANCELED)
            }
        }

        finish()
    }

    private suspend fun handleNewPhoto(callback: (Boolean) -> Unit) {
        val out =
            mediaStorageManager.createMediaFile(type = MediaStorageManager.MediaType.IMAGE)
        if (out != null) {
            val outputUri = out.first
            val bos = ByteArrayOutputStream()

            val cf: CompressFormat =
                if (outputUri.path?.endsWith(".png") == true) {
                    CompressFormat.PNG
                } else {
                    CompressFormat.JPEG
                }

            bitmap.compress(cf, 100, bos)
            val bitmapData: ByteArray = bos.toByteArray()

            val oStream =
                contentResolver.openOutputStream(outputUri)

            if (oStream != null) {
                oStream.write(bitmapData)
                oStream.close()

                outputUris.add(outputUri)

                callback(true)
            } else {
                callback(false)
            }
        }
    }

    private fun imageProxyToBitmap(image: ImageProxy, rotation: Float): Bitmap {
        val planeProxy = image.planes[0]
        val buffer: ByteBuffer = planeProxy.buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size).rotate(rotation)
    }

    private fun resizeImage(image: Bitmap): Bitmap {

        val width = image.width
        val height = image.height

        val scaleWidth = width / 10
        val scaleHeight = height / 10

        if (image.byteCount <= 1000000)
            return image

        return Bitmap.createScaledBitmap(image, scaleWidth, scaleHeight, false)
    }

    private fun Bitmap.rotate(degrees: Float): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
    }
}