package com.smartsolar.microgrid.member4.qr

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.SystemClock
import android.util.AttributeSet
import android.view.View
import kotlin.math.min

class QrViewfinder @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    var scanning = false
        set(value) { field = value; invalidate() }

    override fun onDraw(canvas: Canvas) {
        // The transparent centre shows the actual PreviewView below; animate only while camera frames are streaming.
        super.onDraw(canvas)
        val side = min(width, height) * .72f
        val left = (width - side) / 2f
        val top = (height - side) / 2f
        val right = left + side
        val bottom = top + side
        paint.style = Paint.Style.FILL
        paint.color = 0x88091422.toInt()
        canvas.drawRect(0f, 0f, width.toFloat(), top, paint)
        canvas.drawRect(0f, bottom, width.toFloat(), height.toFloat(), paint)
        canvas.drawRect(0f, top, left, bottom, paint)
        canvas.drawRect(right, top, width.toFloat(), bottom, paint)
        paint.color = Color.WHITE
        paint.strokeWidth = 3 * resources.displayMetrics.density
        paint.strokeCap = Paint.Cap.ROUND
        val corner = side * .12f
        for ((x, dx) in listOf(left to corner, right to -corner)) {
            for ((y, dy) in listOf(top to corner, bottom to -corner)) {
                canvas.drawLine(x, y, x + dx, y, paint)
                canvas.drawLine(x, y, x, y + dy, paint)
            }
        }
        if (scanning && isShown) {
            paint.color = 0xFF4DE0B0.toInt()
            paint.strokeWidth = resources.displayMetrics.density * 2
            val y = top + side * ((SystemClock.uptimeMillis() % 2400) / 2400f)
            canvas.drawLine(left + 8, y, right - 8, y, paint)
            postInvalidateDelayed(32)
        }
    }
}
