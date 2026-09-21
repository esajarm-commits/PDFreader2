package com.pdfreader.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class DrawingView(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    
    private var drawPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        color = Color.BLACK
        strokeWidth = 6f
    }
    
    private var path = Path()
    private var paths = mutableListOf<Pair<Path, Paint>>()
    private var isDrawingEnabled = true
    
    init {
        setLayerType(View.LAYER_TYPE_SOFTWARE, null)
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        paths.forEach { (savedPath, savedPaint) ->
            canvas.drawPath(savedPath, savedPaint)
        }
        canvas.drawPath(path, drawPaint)
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isDrawingEnabled) return false
        
        val x = event.x
        val y = event.y
        
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                path = Path()
                path.moveTo(x, y)
                invalidate()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                path.lineTo(x, y)
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (!path.isEmpty) {
                    val savedPaint = Paint(drawPaint)
                    paths.add(Path(path) to savedPaint)
                    path = Path()
                    invalidate()
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }
    
    fun setDrawingColor(color: Int) {
        drawPaint.color = color
    }
    
    fun setStrokeWidth(width: Float) {
        drawPaint.strokeWidth = width
    }
    
    fun enableDrawing(enable: Boolean) {
        isDrawingEnabled = enable
    }
    
    fun undo() {
        if (paths.isNotEmpty()) {
            paths.removeAt(paths.size - 1)
            invalidate()
        }
    }
    
    fun clearAll() {
        paths.clear()
        invalidate()
    }
}
