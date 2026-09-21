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
import kotlin.math.sqrt

class DrawingView(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    
    // Paint per il disegno
    private var drawPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        color = Color.BLACK
        strokeWidth = 4f
    }
    
    // Percorsi disegnati (in coordinate mondo)
    private var currentPath = Path()
    private var paths = mutableListOf<Pair<Path, Paint>>()
    
    // Griglia di sfondo
    private val gridPaint = Paint().apply {
        color = Color.parseColor("#E8E8E8")
        strokeWidth = 1f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }
    private val majorGridPaint = Paint().apply {
        color = Color.parseColor("#D0D0D0")
        strokeWidth = 1.5f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }
    private val gridSize = 80f // dimensione griglia in coordinate mondo
    
    // Matrice di trasformazione (pan + zoom)
    private val matrix = Matrix()
    private val inverseMatrix = Matrix()
    
    // Stato
    private var scaleFactor = 1f
    private val minScale = 0.1f
    private val maxScale = 10f
    private var isDrawingEnabled = true
    private var isScaling = false
    
    // Touch tracking
    private val lastTouch = PointF()
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var lastDistance = 0f
    private var isPanning = false
    private var panStartX = 0f
    private var panStartY = 0f
    
    // Scale detector
    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val scale = detector.scaleFactor
            val newScale = scaleFactor * scale
            if (newScale in minScale..maxScale) {
                scaleFactor = newScale
                // Zoom centrato sul focus (punto tra le due dita)
                matrix.postScale(scale, scale, detector.focusX, detector.focusY)
                invalidate()
            }
            return true
        }
        
        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            isScaling = true
            isPanning = false
            return true
        }
        
        override fun onScaleEnd(detector: ScaleGestureDetector) {
            isScaling = false
        }
    })
    
    init {
        setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        // Inizializza la matrice identità
        matrix.reset()
    }
    
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        canvas.save()
        canvas.concat(matrix)
        
        // Disegna la griglia di sfondo (in coordinate mondo)
        drawGrid(canvas)
        
        // Disegna tutti i percorsi
        paths.forEach { (savedPath, savedPaint) ->
            canvas.drawPath(savedPath, savedPaint)
        }
        
        // Disegna il percorso corrente
        canvas.drawPath(currentPath, drawPaint)
        
        canvas.restore()
    }
    
    private fun drawGrid(canvas: Canvas) {
        // Calcola l'area visibile in coordinate mondo
        inverseMatrix.reset()
        matrix.invert(inverseMatrix)
        val visibleRect = android.graphics.RectF(0f, 0f, width.toFloat(), height.toFloat())
        inverseMatrix.mapRect(visibleRect)
        
        // Limita il numero di linee per performance
        val left = (visibleRect.left / gridSize).toInt() * gridSize
        val top = (visibleRect.top / gridSize).toInt() * gridSize
        val right = visibleRect.right
        val bottom = visibleRect.bottom
        
        var x = left
        while (x < right) {
            val isMajor = (x / gridSize).toInt() % 5 == 0
            gridPaint.color = if (isMajor) Color.parseColor("#D0D0D0") else Color.parseColor("#E8E8E8")
            canvas.drawLine(x, visibleRect.top, x, visibleRect.bottom, if (isMajor) majorGridPaint else gridPaint)
            x += gridSize
        }
        
        var y = top
        while (y < bottom) {
            val isMajor = (y / gridSize).toInt() % 5 == 0
            gridPaint.color = if (isMajor) Color.parseColor("#D0D0D0") else Color.parseColor("#E8E8E8")
            canvas.drawLine(visibleRect.left, y, visibleRect.right, y, if (isMajor) majorGridPaint else gridPaint)
            y += gridSize
        }
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Gestione zoom con pinch
        scaleDetector.onTouchEvent(event)
        
        // Se stiamo zoomando, non disegnare
        if (isScaling) return true
        
        val pointerCount = event.pointerCount
        
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (isDrawingEnabled) {
                    // Inizia a disegnare
                    startPath(event.x, event.y)
                } else {
                    // Inizia pan con un dito
                    isPanning = true
                    lastTouchX = event.x
                    lastTouchY = event.y
                }
                return true
            }
            
            MotionEvent.ACTION_POINTER_DOWN -> {
                // Secondo dito appoggiato - annulla il disegno e inizia zoom
                if (isDrawingEnabled && currentPath.isEmpty.not() == false) {
                    // Niente, il disegno continua
                }
                isPanning = false
                // Il pinch è gestito dal scaleDetector
                return true
            }
            
            MotionEvent.ACTION_MOVE -> {
                if (pointerCount >= 2) {
                    // Due o più dita: pinch zoom + pan
                    if (event.historySize > 0) {
                        val focusX = (event.getX(0) + event.getX(1)) / 2f
                        val focusY = (event.getY(0) + event.getY(1)) / 2f
                        
                        val dx = focusX - lastTouch.x
                        val dy = focusY - lastTouch.y
                        matrix.postTranslate(dx, dy)
                        invalidate()
                        
                        lastTouch.set(focusX, focusY)
                    }
                    return true
                }
                
                if (isDrawingEnabled && !currentPath.isEmpty) {
                    // Continuare a disegnare
                    continuePath(event.x, event.y)
                } else if (isPanning) {
                    // Pan con un dito (in modalità select)
                    val dx = event.x - lastTouchX
                    val dy = event.y - lastTouchY
                    matrix.postTranslate(dx, dy)
                    invalidate()
                    lastTouchX = event.x
                    lastTouchY = event.y
                }
                return true
            }
            
            MotionEvent.ACTION_POINTER_UP -> {
                // Un dito sollevato, resetta il tracking per il pan
                val remainingIndex = if (event.actionIndex == 0) 1 else 0
                if (remainingIndex < event.pointerCount) {
                    lastTouchX = event.getX(remainingIndex)
                    lastTouchY = event.getY(remainingIndex)
                    lastTouch.set(lastTouchX, lastTouchY)
                }
                return true
            }
            
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isDrawingEnabled && !currentPath.isEmpty) {
                    endPath()
                }
                isPanning = false
                isScaling = false
                return true
            }
        }
        return super.onTouchEvent(event)
    }
    
    private fun startPath(screenX: Float, screenY: Float) {
        val worldPoint = screenToWorld(screenX, screenY)
        currentPath = Path()
        currentPath.moveTo(worldPoint[0], worldPoint[1])
        lastTouch.set(screenX, screenY)
        invalidate()
    }
    
    private fun continuePath(screenX: Float, screenY: Float) {
        val worldPoint = screenToWorld(screenX, screenY)
        currentPath.lineTo(worldPoint[0], worldPoint[1])
        invalidate()
    }
    
    private fun endPath() {
        if (!currentPath.isEmpty) {
            // Applica lo spessore inversamente proporzionale allo zoom per mantenere consistenza visiva
            val savedPaint = Paint(drawPaint)
            savedPaint.strokeWidth = drawPaint.strokeWidth / scaleFactor
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
    
    fun enableDrawing(enable: Boolean) {
        isDrawingEnabled = enable
        if (!enable) {
            // Se disabilitato, completa il path corrente
            if (!currentPath.isEmpty) {
                endPath()
            }
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
    
    fun resetView() {
        matrix.reset()
        scaleFactor = 1f
        invalidate()
    }
    
    fun centerView() {
        matrix.reset()
        scaleFactor = 1f
        invalidate()
    }
    
    fun getScaleFactor(): Float = scaleFactor
}
