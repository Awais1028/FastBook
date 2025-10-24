package com.example.myapplication.offices // ✅ Correct package

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.FrameLayout
import kotlin.math.max
import kotlin.math.min

class ZoomLayout @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : FrameLayout(context, attrs) {

    private var scaleFactor = 1f
    private var lastX = 0f
    private var lastY = 0f
    private var posX = 0f
    private var posY = 0f
    private val scaleGestureDetector: ScaleGestureDetector

    init {
        scaleGestureDetector = ScaleGestureDetector(context, ScaleListener())
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleGestureDetector.onTouchEvent(event)
        val child = getChildAt(0) ?: return true

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastX = event.x
                lastY = event.y
            }
            MotionEvent.ACTION_MOVE -> {
                if (!scaleGestureDetector.isInProgress) {
                    val dx = event.x - lastX
                    val dy = event.y - lastY
                    posX += dx
                    posY += dy
                    applyTranslation(child)
                }
                lastX = event.x
                lastY = event.y
            }
        }
        return true
    }

    private fun applyTranslation(child: View) {
        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()
        val childWidth = child.width * scaleFactor
        val childHeight = child.height * scaleFactor

        posX = max(min(posX, (childWidth - viewWidth) / 2), -(childWidth - viewWidth) / 2)
        posY = max(min(posY, (childHeight - viewHeight) / 2), -(childHeight - viewHeight) / 2)

        child.translationX = posX
        child.translationY = posY
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            scaleFactor *= detector.scaleFactor
            scaleFactor = max(1.0f, min(scaleFactor, 4.0f)) // Min 1x, Max 4x zoom
            getChildAt(0)?.apply {
                scaleX = scaleFactor
                scaleY = scaleFactor
                applyTranslation(this)
            }
            return true
        }
    }
}