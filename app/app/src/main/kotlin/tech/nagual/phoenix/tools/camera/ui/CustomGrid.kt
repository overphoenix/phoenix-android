package tech.nagual.phoenix.tools.camera.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.camera.core.AspectRatio
import tech.nagual.phoenix.tools.camera.CamConfig
import tech.nagual.phoenix.tools.camera.ui.activities.CameraActivity
import tech.nagual.phoenix.tools.camera.ui.activities.CameraActivity.Companion.camConfig

class CustomGrid @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private val paint: Paint = Paint()
    private lateinit var mActivity: CameraActivity

    fun setMainActivity(mActivity: CameraActivity) {
        this.mActivity = mActivity
    }

    init {
        paint.isAntiAlias = true
        paint.strokeWidth = 1f
        paint.style = Paint.Style.STROKE
        paint.color = Color.argb(255, 255, 255, 255)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (camConfig.gridType == CamConfig.GridType.NONE) {
            return
        }

        val previewHeight = if (camConfig.aspectRatio == AspectRatio.RATIO_16_9) {
            mActivity.previewView.width * 16 / 9
        } else {
            mActivity.previewView.width * 4 / 3
        }

        if (camConfig.gridType == CamConfig.GridType.GOLDEN_RATIO) {

            val cx = width / 2f
            val cy = previewHeight / 2f

            val dxH = width / 8f
            val dyH = previewHeight / 8f

            canvas.drawLine(cx - dxH, 0f, cx - dxH, previewHeight.toFloat(), paint)
            canvas.drawLine(cx + dxH, 0f, cx + dxH, previewHeight.toFloat(), paint)
            canvas.drawLine(0f, cy - dyH, width.toFloat(), cy - dyH, paint)
            canvas.drawLine(0f, cy + dyH, width.toFloat(), cy + dyH, paint)

        } else {

            val seed = if (camConfig.gridType == CamConfig.GridType.THREE_BY_THREE) {
                3f
            } else {
                4f
            }

            canvas.drawLine(
                width / seed * 2f,
                0f,
                width / seed * 2f,
                previewHeight.toFloat(),
                paint
            )
            canvas.drawLine(width / seed, 0f, width / seed, previewHeight.toFloat(), paint)
            canvas.drawLine(
                0f, previewHeight / seed * 2f,
                width.toFloat(), previewHeight / seed * 2f, paint
            )
            canvas.drawLine(0f, previewHeight / seed, width.toFloat(), previewHeight / seed, paint)

            if (seed == 4f) {
                canvas.drawLine(
                    width / seed * 3f,
                    0f,
                    width / seed * 3f,
                    previewHeight.toFloat(),
                    paint
                )
                canvas.drawLine(
                    0f, previewHeight / seed * 3f,
                    width.toFloat(), previewHeight / seed * 3f, paint
                )
            }
        }
    }
}