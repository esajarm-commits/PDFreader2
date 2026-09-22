package com.pdfreader.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.util.AttributeSet
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
    
    // Rinomina "matrix" in "transformMatrix" per evitare conflitto con View.getMatrix()
    var transformMatrix: Matrix? = null
    
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
