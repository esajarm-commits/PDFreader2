package com.pdfreader.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View

class DrawingView(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    
    // Struttura per memorizzare un tratto
    private data class Stroke(
        val path: Path,
        val baseWidth: Float,   // Larghezza in pixel SCHERMO (non mondo)
        val color: Int,
        val isEraser: Boolean
    )
    
    // Colori
    private var penColor = Color.BLACK
    private var penWidth = 5f         // in pixel schermo
    private var eraserWidth = 60f     // in pixel schermo
    
    // Stato
    private var isDrawingEnabled = true
    private var isEraserMode = false
    private var isScaling = false
    
    // Path corrente
    private var currentPath = Path()
    private var currentBaseWidth = 5f
    private var currentColor = Color.BLACK
    private var currentIsEraser = false
    
    // Tutti i tratti
    private val strokes = mutableListOf<Stroke>()
    
    // Griglia di sfondo
    private val gridSize = 250f
    private val gridPaint = Paint().apply {
        color = Color.parseColor("#EEEEEE")
        strokeWidth = 1f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }
    private val majorGridPaint = Paint().apply {
        color = Color.parseColor("#D5D5D5")
        strokeWidth = 2f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }
    
    // Matrice
    private val matrix = Matrix()
    private val inverseMatrix = Matrix()
    private var scaleFactor = 1f
    private val minScale = 0.05f
    private val maxScale = 20f
    
    // Touch
    private val lastFocus = PointF()
    private var lastX = 0f
    private var lastY = 0f
    private var isTwoFingerPanning = false
    
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
            if (!currentPath.isEmpty) {
                currentPath = Path()
            }
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
        
        // 1. Griglia
        drawGrid(canvas)
        
        // 2. Tutti i tratti salvati
        strokes.forEach { stroke ->
            val paint = Paint().apply {
                isAntiAlias = true
                style = Paint.Style.STROKE
                strokeJoin = Paint.Join.ROUND
                strokeCap = Paint.Cap.ROUND
                color = if (stroke.isEraser) Color.WHITE else stroke.color
                // Importante: dividi per scaleFactor per mantenere spessore visivo costante
                strokeWidth = stroke.baseWidth / scaleFactor
            }
            canvas.drawPath(stroke.path, paint)
        }
        
        // 3. Tratto corrente
        if (!currentPath.isEmpty) {
            val paint = Paint().apply {
                isAntiAlias = true
                style = Paint.Style.STROKE
                strokeJoin = Paint.Join.ROUND
                strokeCap = Paint.Cap.ROUND
                color = if (currentIsEraser) Color.WHITE else currentColor
                strokeWidth = currentBaseWidth / scaleFactor
            }
            canvas.drawPath(currentPath, paint)
        }
        
        canvas.restore()
    }
    
    private fun drawGrid(canvas: Canvas) {
        inverseMatrix.reset()
        matrix.invert(inverseMatrix)
        val visibleRect = android.graphics.RectF(0f, 0f, width.toFloat(), height.toFloat())
        inverseMatrix.mapRect(visibleRect)
        
        var step = gridSize
        while (step * scaleFactor < 30f) {
            step *= 4
        }
        
        var x = (Math.floor((visibleRect.left / step).toDouble()) * step).toFloat()
        while (x < visibleRect.right) {
            val isMajor = (x / step).toInt() % 5 == 0
            canvas.drawLine(x, visibleRect.top, x, visibleRect.bottom,
                if (isMajor) majorGridPaint else gridPaint)
            x += step
        }
        
        var y = (Math.floor((visibleRect.top / step).toDouble()) * step).toFloat()
        while (y < visibleRect.bottom) {
            val isMajor = (y / step).toInt() % 5 == 0
            canvas.drawLine(visibleRect.left, y, visibleRect.right, y,
                if (isMajor) majorGridPaint else gridPaint)
            y += step
        }
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        if (isScaling) return true
        
        val pointerCount = event.pointerCount
        
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastX = event.x
                lastY = event.y
                if (isDrawingEnabled) {
                    startPath(event.x, event.y)
                }
                return true
            }
            
            MotionEvent.ACTION_POINTER_DOWN -> {
                isTwoFingerPanning = true
                if (!currentPath.isEmpty) {
                    currentPath = Path()
                    invalidate()
                }
                if (pointerCount >= 2) {
                    val fx = (event.getX(0) + event.getX(1)) / 2f
                    val fy = (event.getY(0) + event.getY(1)) / 2f
                    lastFocus.set(fx, fy)
                    lastX = fx
                    lastY = fy
                }
                return true
            }
            
            MotionEvent.ACTION_MOVE -> {
                if (isTwoFingerPanning && pointerCount >= 2) {
                    val fx = (event.getX(0) + event.getX(1)) / 2f
                    val fy = (event.getY(0) + event.getY(1)) / 2f
                    val dx = fx - lastX
                    val dy = fy - lastY
                    matrix.postTranslate(dx, dy)
                    invalidate()
                    lastX = fx
                    lastY = fy
                    return true
                }
                
                if (pointerCount == 1 && isDrawingEnabled && !currentPath.isEmpty) {
                    continuePath(event.x, event.y)
                }
                return true
            }
            
            MotionEvent.ACTION_POINTER_UP -> {
                val remainingIndex = if (event.actionIndex == 0) 1 else 0
                if (remainingIndex < event.pointerCount) {
                    lastX = event.getX(remainingIndex)
                    lastY = event.getY(remainingIndex)
                }
                isTwoFingerPanning = false
                return true
            }
            
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isDrawingEnabled && !currentPath.isEmpty) {
                    endPath()
                }
                isTwoFingerPanning = false
                return true
            }
        }
        return super.onTouchEvent(event)
    }
    
    private fun startPath(screenX: Float, screenY: Float) {
        val worldPoint = screenToWorld(screenX, screenY)
        currentPath = Path()
        currentPath.moveTo(worldPoint[0], worldPoint[1])
        
        currentIsEraser = isEraserMode
        currentColor = penColor
        currentBaseWidth = if (isEraserMode) eraserWidth else penWidth
        
        lastX = screenX
        lastY = screenY
        invalidate()
    }
    
    private fun continuePath(screenX: Float, screenY: Float) {
        val worldPoint = screenToWorld(screenX, screenY)
        currentPath.lineTo(worldPoint[0], worldPoint[1])
        invalidate()
    }
    
    private fun endPath() {
        if (!currentPath.isEmpty) {
            strokes.add(Stroke(
                path = Path(currentPath),
                baseWidth = currentBaseWidth,
                color = currentColor,
                isEraser = currentIsEraser
            ))
            currentPath = Path()
            invalidate()
        }
    }
    
    private fun screenToWorld(screenX: Float, screenY: Float): FloatArray {
        inverseMatrix.reset()
        matrix.invert(inverseMatrix)
        val points = floatArrayOf(screenX, screenY)
        inverseMatrix.mapPoints(points)
        return points
    }
    
    // === API PUBBLICHE ===
    
    fun setDrawingColor(color: Int) {
        penColor = color
    }
    
    fun setStrokeWidth(width: Float) {
        penWidth = width
    }
    
    fun setEraserSize(size: Float) {
        eraserWidth = size
    }
    
    fun setEraserMode(enabled: Boolean) {
        isEraserMode = enabled
        if (!enabled && !currentPath.isEmpty) {
            endPath()
        }
    }
    
    fun enableDrawing(enable: Boolean) {
        isDrawingEnabled = enable
        if (!enable && !currentPath.isEmpty) {
            endPath()
        }
    }
    
    fun undo() {
        if (strokes.isNotEmpty()) {
            strokes.removeAt(strokes.size - 1)
            invalidate()
        }
    }
    
    fun clearAll() {
        strokes.clear()
        currentPath = Path()
        invalidate()
    }
    
    fun centerView() {
        matrix.reset()
        scaleFactor = 1f
        invalidate()
    }
    
    fun getScaleFactor(): Float = scaleFactor
}
