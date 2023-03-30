package tech.nagual.phoenix.tools.organizer.camera.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.camera.core.AspectRatio
import tech.nagual.app.application
import tech.nagual.common.R
import tech.nagual.phoenix.tools.organizer.camera.CamConfig
import tech.nagual.phoenix.tools.organizer.camera.ui.activities.CameraActivity
import tech.nagual.phoenix.tools.organizer.camera.ui.activities.CameraActivity.Companion.camConfig
import tech.nagual.common.extensions.adjustAlpha

class CustomGrid @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private val paint: Paint = Paint()
    private val circlePaint: Paint = Paint()
    private val pointPaint: Paint = Paint()
    private lateinit var mActivity: CameraActivity

    fun setMainActivity(mActivity: CameraActivity) {
        this.mActivity = mActivity
    }

    init {
        paint.isAntiAlias = true
        paint.strokeWidth = 1f
        paint.style = Paint.Style.STROKE
        paint.color = Color.argb(128, 255, 255, 255)

        circlePaint.isAntiAlias = true
        circlePaint.strokeWidth = 1f
        circlePaint.style = Paint.Style.FILL
        circlePaint.color = Color.argb(32, 255, 255, 255)

        pointPaint.isAntiAlias = true
        pointPaint.strokeWidth = 1f
        pointPaint.style = Paint.Style.FILL
        pointPaint.color = application.getColor(R.color.material_red_500).adjustAlpha(0.8f)
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

        } else if (camConfig.gridType == CamConfig.GridType.SIGHT) {
            val seed = 4f
            val radius: Float = width.coerceAtMost(previewHeight) / 8f
            val x1 = width / seed * 2f
            val x2 = width.toFloat() / seed * 2f
            val y1 =previewHeight / seed * 2f

            canvas.drawLine(
                x1,
                0f,
                x1,
                y1 - radius,
                paint
            )
            canvas.drawLine(
                x1,
                y1 + radius,
                x1,
                previewHeight.toFloat(),
                paint
            )

            canvas.drawLine(
                0f, y1,
                x2 - radius, y1, paint
            )
            canvas.drawLine(
                x2 + radius, y1,
                width.toFloat(), y1, paint
            )

            canvas.drawCircle(x1, y1, radius, circlePaint)
            canvas.drawCircle(x1, y1, radius / 12f, pointPaint)
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