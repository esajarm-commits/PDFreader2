package com.pdfreader.app

import android.content.Context
import android.graphics.Bitmap
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
    
    private var drawPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        color = Color.BLACK
        strokeWidth = 5f
    }
    
    private var eraserPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        color = Color.TRANSPARENT
        strokeWidth = 40f
        xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.CLEAR)
    }
    
    private var currentPath = Path()
    private var currentPaint: Paint = drawPaint
    private var paths = mutableListOf<Pair<Path, Paint>>()
    private var backgroundBitmap: Bitmap? = null
    
    private val matrix = Matrix()
    private val inverseMatrix = Matrix()
    
    private var scaleFactor = 1f
    private val minScale = 0.05f
    private val maxScale = 20f
    private var isDrawingEnabled = true
    private var isEraserMode = false
    private var isScaling = false
    
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
            if (!currentPath.isEmpty) currentPath = Path()
            return true
        }
        override fun onScaleEnd(detector: ScaleGestureDetector) { isScaling = false }
    })
    
        init { setLayerType(View.LAYER_TYPE_HARDWARE, null) }
    
    fun setBackgroundBitmap(bitmap: Bitmap) {
        backgroundBitmap = bitmap
        invalidate()
    }
    
    fun drawBackground(canvas: Canvas) {
        backgroundBitmap?.let { bg -> canvas.drawBitmap(bg, 0f, 0f, null) }
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.save()
        canvas.concat(matrix)
        
        backgroundBitmap?.let { bg -> canvas.drawBitmap(bg, 0f, 0f, null) }
        
        paths.forEach { (savedPath, savedPaint) -> canvas.drawPath(savedPath, savedPaint) }
        canvas.drawPath(currentPath, currentPaint)
        
        canvas.restore()
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        if (isScaling) return true
        
        val pointerCount = event.pointerCount
        
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastX = event.x
                lastY = event.y
                if (isDrawingEnabled) startPath(event.x, event.y)
                return true
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                isTwoFingerPanning = true
                if (!currentPath.isEmpty) { currentPath = Path(); invalidate() }
                if (pointerCount >= 2) {
                    val fx = (event.getX(0) + event.getX(1)) / 2f
                    val fy = (event.getY(0) + event.getY(1)) / 2f
                    lastFocus.set(fx, fy)
                    lastX = fx; lastY = fy
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
                    lastX = fx; lastY = fy
                    return true
                }
                if (pointerCount == 1 && isDrawingEnabled && !currentPath.isEmpty) {
                    continuePath(event.x, event.y)
                }
                return true
            }
         private var eraserPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        color = Color.TRANSPARENT
        strokeWidth = 40f
        xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.CLEAR)
    }       MotionEvent.ACTION_POINTER_UP -> {
                val remainingIndex = if (event.actionIndex == 0) 1 else 0
                if (remainingIndex < event.pointerCount) {
                    lastX = event.getX(remainingIndex)
                    lastY = event.getY(remainingIndex)
                }
                isTwoFingerPanning = false
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isDrawingEnabled && !currentPath.isEmpty) endPath()
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
        currentPaint.strokeWidth = (if (isEraserMode) eraserPaint.strokeWidth else drawPaint.strokeWidth) / scaleFactor
        lastX = screenX; lastY = screenY
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
    
    fun setDrawingColor(color: Int) { drawPaint.color = color }
    fun setStrokeWidth(width: Float) { drawPaint.strokeWidth = width }
    fun setEraserSize(size: Float) { eraserPaint.strokeWidth = size }
    fun setEraserMode(enabled: Boolean) {
        isEraserMode = enabled
        if (!enabled && !currentPath.isEmpty) endPath()
    }
    fun enableDrawing(enable: Boolean) {
        isDrawingEnabled = enable
        if (!enable && !currentPath.isEmpty) endPath()
    }
    fun undo() {
        if (paths.isNotEmpty()) { paths.removeAt(paths.size - 1); invalidate() }
    }
    fun clearAll() { paths.clear(); currentPath = Path(); invalidate() }
    fun centerView() { matrix.reset(); scaleFactor = 1f; invalidate() }
        fun getScaleFactor(): Float = scaleFactor
    
       fun exportPathsToJson(): String {
        val sb = StringBuilder("[")
        paths.forEachIndexed { index, pair ->
            if (index > 0) sb.append(",")
            val path = pair.first
            val paint = pair.second
            sb.append("{")
            sb.append("\"color\":${paint.color},")
            sb.append("\"width\":${paint.strokeWidth},")
            sb.append("\"points\":[")
            val pathMeasure = android.graphics.PathMeasure(path, false)
            val length = pathMeasure.length
            val numPoints = (length / 5).toInt().coerceAtLeast(2)
            for (i in 0..numPoints) {
                val distance = (length * i / numPoints)
                val pos = FloatArray(2)
                val tan = FloatArray(2)
                pathMeasure.getPosTan(distance, pos, tan)
                if (i > 0) sb.append(",")
                sb.append("[${pos[0]},${pos[1]}]")
            }
            sb.append("]}")
        }
        sb.append("]")
        return sb.toString()
    }

            sb.append("]}")
        }
        sb.append("]")
        return sb.toString()
    }
    
    fun importPathsFromJson(json: String) {
        try {
            val array = org.json.JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val color = obj.getInt("color")
                val width = obj.getDouble("width").toFloat()
                val points = obj.getJSONArray("points")
                val path = Path()
                for (j in 0 until points.length()) {
                    val point = points.getJSONArray(j)
                    val x = point.getDouble(0).toFloat()
                    val y = point.getDouble(1).toFloat()
                    if (j == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                val paint = Paint(drawPaint).apply {
                    this.color = color
                    strokeWidth = width
                }
                paths.add(path to paint)
            }
            invalidate()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
