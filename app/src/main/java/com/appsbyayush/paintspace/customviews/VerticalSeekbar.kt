package com.appsbyayush.paintspace.customviews

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.appsbyayush.paintspace.R
import com.appsbyayush.paintspace.utils.CommonMethods
import com.appsbyayush.paintspace.utils.enums.SeekbarState
import kotlin.math.abs
import kotlin.math.ceil

class VerticalSeekbar(
    context: Context,
    attrs: AttributeSet
): View(context, attrs) {

    companion object {
        private const val TAG = "VerticalSeekbaryy"
        private const val SEEKBAR_WIDTH = 120
    }

    interface OnSeekbarChangeListener {
        fun onProgressChanged(progress: Int)
        fun onStartTrackingTouch()
        fun onStopTrackingTouch()
    }

    private var seekBarStartX: Float = 0.0F
    private var seekBarStartY: Float = 0.0F
    private var seekBarEndX: Float = 0.0F
    private var seekBarEndY: Float = 0.0F
    private var seekbarHeight: Float = 0.0F

    private lateinit var seekbarContainerRect: Rect

    private var currentState = SeekbarState.INACTIVE
    private var seekbarProgress: Int = 15
        set(value) {
            val progressValue = if(value > 100) 100 else if (value < 0) 0 else value
            field = progressValue
            seekbarChangeListener?.onProgressChanged(progressValue)
        }

    private var touchStartX: Float = 0.0F
    private var touchStartY: Float = 0.0F

    private var seekbarChangeListener: OnSeekbarChangeListener? = null

    fun setOnSeekbarChangeListener(listener: OnSeekbarChangeListener) {
        seekbarChangeListener = listener
    }

    fun getProgress() = seekbarProgress
    fun setProgress(progress: Int) {
        seekbarProgress = progress
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        drawSeekbar(canvas)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(SEEKBAR_WIDTH, heightMeasureSpec)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        setupSeekbarCoordinates()
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        event?.let { motionEvent ->
            when(motionEvent.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    onMotionEventActionDown(motionEvent)
                }

                MotionEvent.ACTION_MOVE -> {
                    onMotionEventActionMove(motionEvent)
                }

                MotionEvent.ACTION_UP -> {
                    onMotionEventActionUp(motionEvent)
                }
            }
        }

        invalidate()
        return true
    }

    override fun performClick(): Boolean {
        return super.performClick()
    }

    private fun setupSeekbarCoordinates() {
        seekBarStartX = width / 2.0F
        seekBarStartY = paddingTop.toFloat()

        seekBarEndX = seekBarStartX
        seekBarEndY = height - paddingBottom.toFloat()

        seekbarHeight = CommonMethods.getDistanceBetweenPoints(seekBarStartX, seekBarEndX,
            seekBarStartY, seekBarEndY)
    }

    private fun drawSeekbar(canvas: Canvas?) {
        canvas?.let { currentCanvas ->
            drawSeekbarContainer(currentCanvas)
//            drawSeekbarCircle(currentCanvas)
        }
    }

    private fun drawSeekbarContainer(canvas: Canvas) {
        val paint = Paint().apply {
            strokeWidth = 5.0F
            color = ContextCompat.getColor(context, R.color.blue_200)
            style = Paint.Style.STROKE
            isAntiAlias = true
            strokeCap = Paint.Cap.ROUND
        }

        when(seekbarProgress) {
            0 -> {
                drawEmptyContainerForSeekbar(canvas, paint)
            }
            in 1..99 -> {
                drawContainerWithProgress(canvas, paint)
            }
            100 -> {
                drawFilledContainerForSeekbar(canvas, paint)
            }
        }

        seekbarContainerRect = Rect((seekBarStartX - getSeekbarWidth()).toInt(), seekBarStartY.toInt(),
            (seekBarEndX + getSeekbarWidth()).toInt(), seekBarEndY.toInt())

//        canvas.drawRect(seekbarContainerRect, Paint().apply {
//            style = Paint.Style.STROKE
//            strokeWidth = 6.0F
//        })
    }

    private fun drawEmptyContainerForSeekbar(canvas: Canvas, paint: Paint) {
        val path = Path()
        path.moveTo(seekBarStartX, seekBarStartY)
        path.lineTo(seekBarStartX + getSeekbarWidth(), seekBarStartY)
        path.lineTo(seekBarEndX, seekBarEndY)
        path.lineTo(seekBarStartX - getSeekbarWidth(), seekBarStartY)
        path.lineTo(seekBarStartX, seekBarStartY)

        canvas.drawPath(path, paint)
    }

    private fun drawContainerWithProgress(canvas: Canvas, paint: Paint) {
        val progressedHeight = (seekbarHeight / 100) * seekbarProgress
        val yCoordinateForCurrentProgress = seekBarEndY - progressedHeight
        val seekbarWidthAtProgressPoint = getSeekbarWidthAtProgressPoint(progressedHeight)

        val progressedPath = Path().apply {
            moveTo(seekBarStartX, yCoordinateForCurrentProgress)
            lineTo(seekBarStartX + seekbarWidthAtProgressPoint, yCoordinateForCurrentProgress)
            lineTo(seekBarEndX, seekBarEndY)
            lineTo(seekBarStartX - seekbarWidthAtProgressPoint, yCoordinateForCurrentProgress)
            lineTo(seekBarStartX, yCoordinateForCurrentProgress)
        }

        val unProgressedPath = Path().apply {
            moveTo(seekBarStartX, seekBarStartY)
            lineTo(seekBarStartX + getSeekbarWidth(), seekBarStartY)
            lineTo(seekBarStartX + seekbarWidthAtProgressPoint, yCoordinateForCurrentProgress)
            lineTo(seekBarStartX - seekbarWidthAtProgressPoint, yCoordinateForCurrentProgress)
            lineTo(seekBarStartX - getSeekbarWidth(), seekBarStartY)
            lineTo(seekBarStartX, seekBarStartY)
        }

        canvas.drawPath(progressedPath, paint.apply { style = Paint.Style.FILL_AND_STROKE })
        canvas.drawPath(unProgressedPath, paint.apply { style = Paint.Style.STROKE })
    }

    private fun drawFilledContainerForSeekbar(canvas: Canvas, paint: Paint) {
        val path = Path()
        path.moveTo(seekBarStartX, seekBarStartY)
        path.lineTo(seekBarStartX + getSeekbarWidth(), seekBarStartY)
        path.lineTo(seekBarEndX, seekBarEndY)
        path.lineTo(seekBarStartX - getSeekbarWidth(), seekBarStartY)
        path.lineTo(seekBarStartX, seekBarStartY)

        canvas.drawPath(path, paint.apply { style = Paint.Style.FILL_AND_STROKE })
    }

    private fun drawSeekbarCircle(canvas: Canvas) {
        val circlePaint = Paint().apply {
            color = Color.GREEN
        }

        val circleRadius = if(currentState == SeekbarState.ACTIVE) 40.0F else 25.0F

        canvas.drawCircle(seekBarEndX, seekBarEndY, circleRadius, circlePaint)
    }

    private fun onMotionEventActionDown(event: MotionEvent) {
        currentState = SeekbarState.ACTIVE
        touchStartX = event.x
        touchStartY = event.y

        if(seekbarChangeListener == null) {
            Log.d(TAG, "onMotionEventActionDown: Seekbar Listener NULL")
        }
        seekbarChangeListener?.onStartTrackingTouch()

//        setProgressAccordingToTouch(event)

//        if(seekbarContainerRect.contains(event.x.toInt(), event.y.toInt())) {
//            Log.d(TAG, "onMotionEventActionDown: Clicked on Seekbar")
//        } else {
//            Log.d(TAG, "onMotionEventActionDown: NOT Clicked on Seekbar")
//        }
    }

    /**
     * abs(verticalMovement) -> Vertical Movement in any direction
     * progressChange -> Based on fast or slow vertical scroll to vary speed of changing progress
     * touchStartX, touchStartY -> Used to determine changes in touch i.e., scroll
     */
    private fun onMotionEventActionMove(event: MotionEvent) {
        val unitProgressHeight = seekbarHeight / 100
        val verticalMovement = touchStartY - event.y

        if(abs(verticalMovement) > 10) {
            val progressChange = if(abs(verticalMovement) > 25) 5 else 1

            if(verticalMovement > 0) {
                seekbarProgress += progressChange
            } else {
                seekbarProgress -= progressChange
            }

            touchStartX = event.x
            touchStartY = event.y
        }
    }

    private fun onMotionEventActionUp(event: MotionEvent) {
        currentState = SeekbarState.INACTIVE
        seekbarChangeListener?.onStopTrackingTouch()
    }
    
    private fun setProgressAccordingToTouch(event: MotionEvent) {
        val touchY = event.y
        
        val progressedHeight = seekBarEndY - touchY
        val calculatedProgress = ceil((progressedHeight / seekbarHeight) * 100)

        seekbarProgress = calculatedProgress.toInt()
    }

    private fun getSeekbarWidth(): Int {
        return if(seekbarHeight > 700) 45 else 32
    }

    /**
     * This function returns width on each side i.e., totalWidth / 2
     */
    private fun getSeekbarWidthAtProgressPoint(progressedHeight: Float): Float {
        return (getSeekbarWidth() * progressedHeight) / seekbarHeight
    }
}