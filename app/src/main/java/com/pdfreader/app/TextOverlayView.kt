package com.pdfreader.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

data class TextItem(
    var text: String,
    var x: Float,
    var y: Float,
    var color: Int = Color.BLACK,
    var textSize: Float = 40f
)

class TextOverlayView(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    
    val textItems = mutableListOf<TextItem>()
    
    private val textPaint = Paint().apply {
        isAntiAlias = true
        color = Color.BLACK
        textSize = 40f
    }
    
    var transformMatrix: Matrix? = null
    
    private var draggingItem: TextItem? = null
    private var lastX = 0f
    private var lastY = 0f
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.save()
        transformMatrix?.let { canvas.concat(it) }
        textItems.forEach { item ->
            textPaint.color = item.color
            textPaint.textSize = item.textSize
            canvas.drawText(item.text, item.x, item.y, textPaint)
        }
        canvas.restore()
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                draggingItem = textItems.findLast { item ->
                    val textWidth = textPaint.measureText(item.text)
                    x >= item.x && x <= item.x + textWidth &&
                    y >= item.y - item.textSize && y <= item.y + 10
                }
                if (draggingItem != null) {
                    lastX = x
                    lastY = y
                    return true
                }
            }
            MotionEvent.ACTION_MOVE -> {
                draggingItem?.let { item ->
                    item.x += x - lastX
                    item.y += y - lastY
                    lastX = x
                    lastY = y
                    invalidate()
                    return true
                }
            }
            MotionEvent.ACTION_UP -> {
                draggingItem = null
            }
        }
        return draggingItem != null
    }
    
    fun addText(text: String, x: Float, y: Float, color: Int, size: Float) {
        textItems.add(TextItem(text, x, y, color, size))
        invalidate()
    }
    
    fun undo() {
        if (textItems.isNotEmpty()) {
            textItems.removeAt(textItems.size - 1)
            invalidate()
        }
    }
    
    fun clearAll() {
        textItems.clear()
        invalidate()
    }
}
