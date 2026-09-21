package com.pdfreader.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
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
    
    // Per zoom e pan
    private val matrix = Matrix()
    private var scaleFactor = 1f
    private val minScale = 0.5f
    private val maxScale = 5f
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var isScaling = false
    private var isPanning = false
    
    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val scale = detector.scaleFactor
            val newScale = scaleFactor * scale
            if (newScale in minScale..maxScale) {
                scaleFactor = newScale
                matrix.postScale(scale, scale, detector.focusX, detector.focusY)
                invalidate()
            }
            return true
        }
        
        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            isScaling = true
            return true
        }
        
        override fun onScaleEnd(detector: ScaleGestureDetector) {
            isScaling = false
        }
    })
    
    init {
        setLayerType(View.LAYER_TYPE_SOFTWARE, null)
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.save()
        canvas.concat(matrix)
        
        paths.forEach { (savedPath, savedPaint) ->
            canvas.drawPath(savedPath, savedPaint)
        }
        canvas.drawPath(path, drawPaint)
        
        canvas.restore()
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Gestione zoom con due dita (sempre attiva)
        scaleDetector.onTouchEvent(event)
        
        // Se stiamo facendo zoom con due dita, non disegnare
        if (event.pointerCount > 1 || isScaling) {
            isPanning = false
            return true
        }
        
        // Se la penna è disabilitata, permette solo pan con due dita
        if (!isDrawingEnabled) {
            return true
        }
        
        val x = event.x
        val y = event.y
        
        // Trasforma le coordinate tenendo conto di zoom/pan
        val invertedMatrix = Matrix()
        matrix.invert(invertedMatrix)
        val points = floatArrayOf(x, y)
        invertedMatrix.mapPoints(points)
        val drawX = points[0]
        val drawY = points[1]
        
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                path = Path()
                path.moveTo(drawX, drawY)
                lastTouchX = x
                lastTouchY = y
                invalidate()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                path.lineTo(drawX, drawY)
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
    
    fun resetZoom() {
        matrix.reset()
        scaleFactor = 1f
        invalidate()
    }
    
    fun getScaleFactor(): Float {
        return scaleFactor
    }
}
