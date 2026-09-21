package com.pdfreader.app

import android.content.Context
import android.graphics.BlendMode
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.os.Build
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
    
    // Paint per la gomma (usa CLEAR per cancellare)
    private var eraserPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        color = Color.BLACK
        strokeWidth = 40f
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            blendMode = BlendMode.CLEAR
        } else {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        }
    }
    
    // Percorsi disegnati (con Paint associato)
    private var currentPath = Path()
    private var currentPaint: Paint = drawPaint
    private var paths = mutableListOf<Pair<Path, Paint>>()
    
    // Griglia di sfondo - MOLTO PIÙ GRANDE
    private val gridSize = 250f  // era 80f
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
    
    // Scale detector (pinch)
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
            // Cancella il path corrente quando inizia lo zoom
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
        
        // 1. Disegna la griglia di sfondo (in coordinate mondo)
        drawGrid(canvas)
        
        // 2. Salva un layer per i disegni (così la gomma non cancella la griglia)
        val layerId = canvas.saveLayer(null, null)
        
        // 3. Disegna tutti i percorsi
        paths.forEach { (savedPath, savedPaint) ->
            canvas.drawPath(savedPath, savedPaint)
        }
        canvas.drawPath(currentPath, currentPaint)
        
        // 4. Ripristina il layer
        canvas.restoreToCount(layerId)
        
        canvas.restore()
    }
    
    private fun drawGrid(canvas: Canvas) {
        inverseMatrix.reset()
        matrix.invert(inverseMatrix)
        val visibleRect = android.graphics.RectF(0f, 0f, width.toFloat(), height.toFloat())
        inverseMatrix.mapRect(visibleRect)
        
        // Calcola il range di linee da disegnare
        val startX = (Math.floor((visibleRect.left / gridSize).toDouble()) * gridSize).toFloat()
        val startY = (Math.floor((visibleRect.top / gridSize).toDouble()) * gridSize).toFloat()
        
        // Calcola il passo in base allo zoom per evitare troppe linee
        val effectiveScale = scaleFactor
        var step = gridSize
        // Se zoom out troppo, aumenta lo step per non disegnare troppe linee
        while (step * effectiveScale < 30f) {
            step *= 4
        }
        
        // Linee verticali
        var x = (Math.floor((visibleRect.left / step).toDouble()) * step).toFloat()
        while (x < visibleRect.right) {
            val isMajor = (x / step).toInt() % 5 == 0
            canvas.drawLine(
                x, visibleRect.top, x, visibleRect.bottom,
                if (isMajor) majorGridPaint else gridPaint
            )
            x += step
        }
        
        // Linee orizzontali
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
        // Sempre al scale detector per pinch
        scaleDetector.onTouchEvent(event)
        
        if (isScaling) return true
        
        val pointerCount = event.pointerCount
        
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastX = event.x
                lastY = event.y
                if (isDrawingEnabled) {
                    startPath(event.x, event.y)
                } else {
                    isTwoFingerPanning = false
                }
                return true
            }
            
            MotionEvent.ACTION_POINTER_DOWN -> {
                // Secondo dito appoggiato
                isTwoFingerPanning = true
                // Cancella il path corrente (l'utente vuole muoversi, non disegnare)
                if (!currentPath.isEmpty) {
                    currentPath = Path()
                    invalidate()
                }
                // Imposta il focus al centro tra le due dita
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
                    // Pan con due dita (funziona in ogni modalità!)
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
                
                // Disegno con un dito
                if (pointerCount == 1 && isDrawingEnabled && !currentPath.isEmpty) {
                    continuePath(event.x, event.y)
                }
                return true
            }
            
            MotionEvent.ACTION_POINTER_UP -> {
                // Un dito sollevato - rimane un dito
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
        // Applica spessore inverso allo zoom (per avere dimensione apparente costante)
        currentPaint.strokeWidth = if (isEraserMode) {
            eraserPaint.strokeWidth / scaleFactor
        } else {
            drawPaint.strokeWidth / scaleFactor
        }
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
