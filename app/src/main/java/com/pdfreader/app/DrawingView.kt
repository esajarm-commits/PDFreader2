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
    
    // Paint per il disegno normale
    private var drawPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        color = Color.BLACK
        strokeWidth = 5f
    }
    
    // Paint per la gomma (disegna in bianco come lo sfondo)
    private var eraserPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        color = Color.WHITE
        strokeWidth = 40f
    }
    
    // Percorsi disegnati
    private var currentPath = Path()
    private var currentPaint: Paint = drawPaint
    private var paths = mutableListOf<Pair<Path, Paint>>()
    
    // Griglia di sfondo grande
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
    
    // Matrice di trasformazione
    private val matrix = Matrix()
    private val inverseMatrix = Matrix()
    
    // Stato
    private var scaleFactor = 1f
    private val minScale = 0.05f
    private val maxScale = 20f
    private var isDrawingEnabled = true
    private var isEraserMode = false
    private var isScaling = false
    
    // Touch tracking
    private val lastFocus = PointF()
    private var lastX = 0f
    private var lastY = 0f
    private var isTwoFingerPanning = false
    
    // Scale detector
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
        
        // 1. Disegna la griglia di sfondo
        drawGrid(canvas)
        
        // 2. Disegna tutti i percorsi (inclusa la gomma bianca)
        paths.forEach { (savedPath, savedPaint) ->
            canvas.drawPath(savedPath, savedPaint)
        }
        canvas.drawPath(currentPath, currentPaint)
        
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
            canvas.drawLine(
                x, visibleRect.top, x, visibleRect.bottom,
                if (isMajor) majorGridPaint else gridPaint
            )
            x += step
        }
        
        var y = (Math.floor((visibleRect.top / step).toDouble()) * step).toFloat()
        while (y < visibleRect.bottom) {
            val isMajor = (y / step).toInt() % 5 == 0
            canvas.drawLine(
                visibleRect.left, y, visibleRect.right, y,
                if (isMajor) majorGridPaint else gridPaint
            )
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
                    val focusX = (event.getX(0) + event.getX(1)) / 2f
                    val focusY = (event.getY(0) + event.getY(1)) / 2f
                    lastFocus.set(focusX, focusY)
                    lastX = focusX
                    lastY = focusY
                }
                return true
            }
            
            MotionEvent.ACTION_MOVE -> {
                if (isTwoFingerPanning && pointerCount >= 2) {
                    val focusX = (event.getX(0) + event.getX(1)) / 2f
                    val focusY = (event.getY(0) + event.getY(1)) / 2f
                    
                    val dx = focusX - lastX
                    val dy = focusY - lastY
                    matrix.postTranslate(dx, dy)
                    invalidate()
                    
                    lastX = focusX
                    lastY = focusY
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
        
        currentPaint = if (isEraserMode) eraserPaint else drawPaint
        
        // Applica spessore inversamente proporzionale allo zoom
        // così il tratto ha sempre la stessa dimensione visiva
        val baseWidth = if (isEraserMode) eraserPaint.strokeWidth else drawPaint.strokeWidth
        currentPaint.strokeWidth = baseWidth / scaleFactor
        
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
            val savedPaint = Paint(currentPaint)
            paths.add(Path(currentPath) to savedPaint)
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
        drawPaint.color = color
    }
    
    fun setStrokeWidth(width: Float) {
        drawPaint.strokeWidth = width
    }
    
    fun setEraserSize(size: Float) {
        eraserPaint.strokeWidth = size
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
        if (paths.isNotEmpty()) {
            paths.removeAt(paths.size - 1)
            invalidate()
        }
    }
    
    fun clearAll() {
        paths.clear()
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
