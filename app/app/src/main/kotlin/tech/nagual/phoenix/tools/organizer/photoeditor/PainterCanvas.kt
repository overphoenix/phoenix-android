package tech.nagual.phoenix.tools.organizer.photoeditor

import android.app.Activity
import android.content.Context
import android.graphics.*
import android.net.Uri
import android.os.Parcelable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.MotionEvent.INVALID_POINTER_ID
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewConfiguration
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import tech.nagual.app.ensureBackgroundThread
import tech.nagual.common.extensions.toast
import tech.nagual.phoenix.tools.organizer.photoeditor.models.PaintOptions
import tech.nagual.phoenix.tools.organizer.photoeditor.models.PaintParcelable
import tech.nagual.phoenix.tools.organizer.photoeditor.models.PaintPath
import tech.nagual.common.R
import java.util.concurrent.ExecutionException
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min


class PainterCanvas(context: Context, attrs: AttributeSet) : View(context, attrs) {
    private val MIN_ERASER_WIDTH = 20f
    private val mScaledTouchSlop = ViewConfiguration.get(context).scaledTouchSlop

    var mPaths = LinkedHashMap<PaintPath, PaintOptions>()
    var mBackgroundBitmap: Bitmap? = null
    var mListener: Listener? = null

    private var mLastPaths = LinkedHashMap<PaintPath, PaintOptions>()
    private var mUndonePaths = LinkedHashMap<PaintPath, PaintOptions>()
    private var mLastBackgroundBitmap: Bitmap? = null

    private var mPaint = Paint()
    private var mPath = PaintPath()
    private var mPaintOptions = PaintOptions()

    private var mCurX = 0f
    private var mCurY = 0f
    private var mStartX = 0f
    private var mStartY = 0f
    private var mPosX = 0f
    private var mPosY = 0f
    private var mLastTouchX = 0f
    private var mLastTouchY = 0f
    private var mActivePointerId = INVALID_POINTER_ID

    private var mImageWidth = 0
    private var mImageHeight = 0

    private var mCurrBrushSize = 0f
    private var mAllowMovingZooming = true
    private var mWasMultitouch = false
    private var mBackgroundColor = 0
    private var mCenter: PointF? = null

    private var mScaleDetector: ScaleGestureDetector? = null
    private var mScaleFactor = 1f

    private var mLastMotionEvent: MotionEvent? = null
    private var mTouchSloppedBeforeMultitouch: Boolean = false

    init {
        mPaint.apply {
            color = mPaintOptions.color
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
            strokeWidth = mPaintOptions.strokeWidth
            isAntiAlias = true
        }

        mScaleDetector = ScaleGestureDetector(context, ScaleListener())
        pathsUpdated()
    }

    fun undo() {
        if (mPaths.isEmpty() && mLastPaths.isNotEmpty()) {
            mPaths = mLastPaths.clone() as LinkedHashMap<PaintPath, PaintOptions>
            mBackgroundBitmap = mLastBackgroundBitmap
            mLastPaths.clear()
            pathsUpdated()
            invalidate()
            return
        }

        if (mPaths.isEmpty()) {
            return
        }

        val lastPath = mPaths.values.lastOrNull()
        val lastKey = mPaths.keys.lastOrNull()

        mPaths.remove(lastKey)
        if (lastPath != null && lastKey != null) {
            mUndonePaths[lastKey] = lastPath
            mListener?.toggleRedoVisibility(true)
        }
        pathsUpdated()
        invalidate()
    }

    fun redo() {
        if (mUndonePaths.keys.isEmpty()) {
            mListener?.toggleRedoVisibility(false)
            return
        }

        val lastKey = mUndonePaths.keys.last()
        addPath(lastKey, mUndonePaths.values.last())
        mUndonePaths.remove(lastKey)
        if (mUndonePaths.isEmpty()) {
            mListener?.toggleRedoVisibility(false)
        }
        invalidate()
    }

    fun setColor(newColor: Int) {
        mPaintOptions.color = newColor
    }

    fun updateBackgroundColor(newColor: Int) {
        mBackgroundColor = newColor
        setBackgroundColor(newColor)
        mBackgroundBitmap = null
    }

    fun setBrushSize(newBrushSize: Float) {
        mCurrBrushSize = newBrushSize
        mPaintOptions.strokeWidth =
            resources.getDimension(R.dimen.paint_full_brush_size) * (newBrushSize / mScaleFactor / 100f)
    }

    fun setAllowZooming(allowZooming: Boolean) {
        mAllowMovingZooming = allowZooming
    }

    fun getBitmap(): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        draw(canvas)
        return bitmap
    }

    fun drawBitmap(activity: Activity, path: Any) {
        ensureBackgroundThread {
            val size = Point()
            activity.windowManager.defaultDisplay.getSize(size)
            val options = RequestOptions()
                .format(DecodeFormat.PREFER_ARGB_8888)
                .disallowHardwareConfig()
                .fitCenter()

            val imgOptions = BitmapFactory.Options()
            imgOptions.inJustDecodeBounds = true
            BitmapFactory.decodeStream(
                activity.contentResolver.openInputStream(path as Uri),
                null,
                imgOptions
            )
            mImageWidth = imgOptions.outWidth
            mImageHeight = imgOptions.outHeight

            val realWidth = min(size.x, mImageWidth)
            val realHeight = min(size.y, mImageHeight)

            try {
                val builder = Glide.with(context)
                    .asBitmap()
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .load(path)
                    .apply(options)
                    .submit(size.x, size.y)

                mBackgroundBitmap = builder.get()
                activity.runOnUiThread {
                    layoutParams.width = realWidth
                    layoutParams.height = realHeight
                    requestLayout()
                    invalidate()
                }
            } catch (e: ExecutionException) {
                val errorMsg =
                    String.format(activity.getString(R.string.paint_failed_to_load_image), path)
                activity.toast(errorMsg)
            }
        }
    }

    fun addPath(path: PaintPath, options: PaintOptions) {
        mPaths[path] = options
        pathsUpdated()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.save()

        if (mCenter == null) {
            mCenter = PointF(width / 2f, height / 2f)
        }

        canvas.translate(mPosX, mPosY)
        canvas.scale(mScaleFactor, mScaleFactor, mCenter!!.x, mCenter!!.y)

        if (mBackgroundBitmap != null) {
            val left = (width - mBackgroundBitmap!!.width) / 2
            val top = (height - mBackgroundBitmap!!.height) / 2
            canvas.drawBitmap(mBackgroundBitmap!!, left.toFloat(), top.toFloat(), null)
        }

//        canvas.drawText(
//            "${mScaleFactor}, ${mImageWidth}, $mImageHeight, ${mScaleFactor * mImageWidth}, ${mScaleFactor * mImageHeight}",
//            10f,
//            10f,
//            mPaint
//        )

        for ((key, value) in mPaths) {
            changePaint(value)
            canvas.drawPath(key, mPaint)
        }

        changePaint(mPaintOptions)
        canvas.drawPath(mPath, mPaint)
        canvas.restore()
    }

    private fun changePaint(paintOptions: PaintOptions) {
        mPaint.color = paintOptions.color
        mPaint.strokeWidth = paintOptions.strokeWidth
    }

    fun clearCanvas() {
        mLastPaths = mPaths.clone() as LinkedHashMap<PaintPath, PaintOptions>
        mLastBackgroundBitmap = mBackgroundBitmap
        mBackgroundBitmap = null
        mPath.reset()
        mPaths.clear()
        pathsUpdated()
        invalidate()
    }

    private fun actionDown(x: Float, y: Float) {
        mPath.reset()
        mPath.moveTo(x, y)
        mCurX = x
        mCurY = y
    }

    private fun actionMove(x: Float, y: Float) {
        mPath.quadTo(mCurX, mCurY, (x + mCurX) / 2, (y + mCurY) / 2)
        mCurX = x
        mCurY = y
    }

    private fun actionUp() {
        if (!mWasMultitouch) {
            mPath.lineTo(mCurX, mCurY)

            // draw a dot on click
            if (mStartX == mCurX && mStartY == mCurY) {
                mPath.lineTo(mCurX, mCurY + 2)
                mPath.lineTo(mCurX + 1, mCurY + 2)
                mPath.lineTo(mCurX + 1, mCurY)
            }
        }

        mPaths[mPath] = mPaintOptions
        pathsUpdated()
        mPath = PaintPath()
        mPaintOptions = PaintOptions(mPaintOptions.color, mPaintOptions.strokeWidth)
    }

    private fun pathsUpdated() {
        mListener?.toggleUndoVisibility(mPaths.isNotEmpty() || mLastPaths.isNotEmpty())
    }

    fun getDrawingHashCode() =
        mPaths.hashCode().toLong() + (mBackgroundBitmap?.hashCode()?.toLong() ?: 0L)

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (mAllowMovingZooming) {
            mScaleDetector!!.onTouchEvent(event)
        }

        val action = event.actionMasked
        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE) {
            mActivePointerId = event.getPointerId(0)
        }

        val pointerIndex = event.findPointerIndex(mActivePointerId)
        val x: Float
        val y: Float

        try {
            x = event.getX(pointerIndex)
            y = event.getY(pointerIndex)
        } catch (e: Exception) {
            return true
        }

        val scaledWidth = width / mScaleFactor
        val touchPercentageX = x / width
        val compensationX = (scaledWidth / 2) * (1 - mScaleFactor)
        val newValueX = scaledWidth * touchPercentageX - compensationX - (mPosX / mScaleFactor)

        val scaledHeight = height / mScaleFactor
        val touchPercentageY = y / height
        val compensationY = (scaledHeight / 2) * (1 - mScaleFactor)
        val newValueY = scaledHeight * touchPercentageY - compensationY - (mPosY / mScaleFactor)

        when (action) {
            MotionEvent.ACTION_DOWN -> {
                mWasMultitouch = false
                mStartX = x
                mStartY = y
                mLastTouchX = x
                mLastTouchY = y
                actionDown(newValueX, newValueY)
                mUndonePaths.clear()
                mListener?.toggleRedoVisibility(false)
            }
            MotionEvent.ACTION_MOVE -> {
                if (mTouchSloppedBeforeMultitouch) {
                    mPath.reset()
                    mTouchSloppedBeforeMultitouch = false
                }

                if (!mAllowMovingZooming || (!mScaleDetector!!.isInProgress && event.pointerCount == 1 && !mWasMultitouch)) {
                    actionMove(newValueX, newValueY)
                }

                if (mAllowMovingZooming && mWasMultitouch) {
                    val imgLeft = (width - mBackgroundBitmap!!.width) / 2f
                    val imgTop = (height - mBackgroundBitmap!!.height) / 2f

                    mPosX += x - mLastTouchX
//                    if (mPosX > imgLeft) mPosX = imgLeft

                    mPosY += y - mLastTouchY
//                    if (mPosY > imgTop) mPosY = imgTop
                    invalidate()
                }

                mLastTouchX = x
                mLastTouchY = y
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                mActivePointerId = INVALID_POINTER_ID
                actionUp()
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                mWasMultitouch = true
                mTouchSloppedBeforeMultitouch =
                    mLastMotionEvent.isTouchSlop(pointerIndex, mStartX, mStartY)
            }
            MotionEvent.ACTION_POINTER_UP -> {
                val upPointerIndex = event.actionIndex
                val pointerId = event.getPointerId(upPointerIndex)
                if (pointerId == mActivePointerId) {
                    val newPointerIndex = if (upPointerIndex == 0) 1 else 0
                    mLastTouchX = event.getX(newPointerIndex)
                    mLastTouchY = event.getY(newPointerIndex)
                    mActivePointerId = event.getPointerId(newPointerIndex)
                }
            }
        }


        mLastMotionEvent = MotionEvent.obtain(event)

        invalidate()
        return true
    }

    public override fun onSaveInstanceState(): Parcelable {
        val superState = super.onSaveInstanceState()
        val savedState = PaintParcelable(superState!!)
        savedState.paths = mPaths
        return savedState
    }

    public override fun onRestoreInstanceState(state: Parcelable) {
        if (state !is PaintParcelable) {
            super.onRestoreInstanceState(state)
            return
        }

        super.onRestoreInstanceState(state.superState)
        mPaths = state.paths
        pathsUpdated()
    }

    private fun MotionEvent?.isTouchSlop(pointerIndex: Int, startX: Float, startY: Float): Boolean {
        return if (this == null || actionMasked != MotionEvent.ACTION_MOVE) {
            false
        } else {
            try {
                val moveX = abs(getX(pointerIndex) - startX)
                val moveY = abs(getY(pointerIndex) - startY)

                moveX <= mScaledTouchSlop && moveY <= mScaledTouchSlop
            } catch (e: Exception) {
                false
            }
        }
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            mScaleFactor *= detector.scaleFactor
            mScaleFactor = max(0.9f, min(mScaleFactor, 10.0f))
            setBrushSize(mCurrBrushSize)
            invalidate()
            return true
        }
    }

    interface Listener {
        fun toggleUndoVisibility(visible: Boolean)

        fun toggleRedoVisibility(visible: Boolean)
    }
}
