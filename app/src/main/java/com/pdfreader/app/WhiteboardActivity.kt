package com.pdfreader.app

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import java.io.File
import java.io.FileOutputStream

class WhiteboardActivity : AppCompatActivity() {
    
    private lateinit var toolbar: Toolbar
    private lateinit var drawingView: DrawingView
    private lateinit var textOverlayView: TextOverlayView
    private lateinit var textZoomLevel: TextView
    private lateinit var btnPen: Button
    private lateinit var btnEraser: Button
    private lateinit var btnMove: Button
    private lateinit var btnText: Button
    private lateinit var btnColor: Button
    private lateinit var btnCenter: Button
    private lateinit var btnUndo: Button
    private lateinit var btnClear: Button
    private lateinit var btnSave: Button
    
    private var currentColor = Color.BLACK
    private var strokeWidth = 5f
    private var eraserSize = 60f
    private var textSize = 40f
    
    private val colorPalette = intArrayOf(
        Color.BLACK, Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW,
        Color.parseColor("#FF5722"), Color.parseColor("#9C27B0"),
        Color.parseColor("#00BCD4"), Color.parseColor("#4CAF50"),
        Color.GRAY, Color.parseColor("#795548")
    )
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_whiteboard)
        
        toolbar = findViewById(R.id.toolbar)
        drawingView = findViewById(R.id.drawingView)
        textOverlayView = findViewById(R.id.textOverlayView)
        textZoomLevel = findViewById(R.id.textZoomLevel)
        btnPen = findViewById(R.id.btnPen)
        btnEraser = findViewById(R.id.btnEraser)
        btnMove = findViewById(R.id.btnMove)
        btnText = findViewById(R.id.btnText)
        btnColor = findViewById(R.id.btnColor)
        btnCenter = findViewById(R.id.btnCenter)
        btnUndo = findViewById(R.id.btnUndo)
        btnClear = findViewById(R.id.btnClear)
        btnSave = findViewById(R.id.btnSave)
        
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }
        
        setupButtons()
        updateZoomLabel()
    }
    
    private fun setupButtons() {
        btnPen.setOnClickListener { activatePen() }
        btnPen.setOnLongClickListener { showPenSizeDialog(); true }
        
        btnEraser.setOnClickListener { activateEraser() }
        btnEraser.setOnLongClickListener { showEraserSizeDialog(); true }
        
        btnMove.setOnClickListener {
            drawingView.enableDrawing(false)
            drawingView.setEraserMode(false)
            btnMove.setBackgroundColor(Color.parseColor("#FF5722"))
            btnPen.setBackgroundColor(Color.parseColor("#9E9E9E"))
            btnEraser.setBackgroundColor(Color.parseColor("#9E9E9E"))
            Toast.makeText(this, "✋ Muovi", Toast.LENGTH_SHORT).show()
        }
        
        btnText.setOnClickListener { showAddTextDialog() }
        btnColor.setOnClickListener { showColorPicker() }
        
        btnCenter.setOnClickListener {
            drawingView.centerView()
            updateZoomLabel()
        }
        
        btnUndo.setOnClickListener {
            drawingView.undo()
            Toast.makeText(this, "↩️ Annullato", Toast.LENGTH_SHORT).show()
        }
        
        btnClear.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Cancellare tutto?")
                .setPositiveButton("Sì") { _, _ ->
                    drawingView.clearAll()
                    textOverlayView.clearAll()
                }
                .setNegativeButton("No", null)
                .show()
        }
        
        btnSave.setOnClickListener { saveWhiteboard() }
        
        drawingView.post(object : Runnable {
            override fun run() {
                updateZoomLabel()
                drawingView.postDelayed(this, 150)
            }
        })
    }
    
    private fun activatePen() {
        drawingView.enableDrawing(true)
        drawingView.setEraserMode(false)
        drawingView.setStrokeWidth(strokeWidth)
        btnPen.setBackgroundColor(Color.parseColor("#4CAF50"))
        btnEraser.setBackgroundColor(Color.parseColor("#9E9E9E"))
        btnMove.setBackgroundColor(Color.parseColor("#9E9E9E"))
    }
    
    private fun activateEraser() {
        drawingView.enableDrawing(true)
        drawingView.setEraserMode(true)
        drawingView.setEraserSize(eraserSize)
        btnEraser.setBackgroundColor(Color.parseColor("#FF9800"))
        btnPen.setBackgroundColor(Color.parseColor("#9E9E9E"))
        btnMove.setBackgroundColor(Color.parseColor("#9E9E9E"))
    }
    
    private fun showPenSizeDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_eraser_size, null)
        val seekBar = dialogView.findViewById<SeekBar>(R.id.seekBarEraserSize)
        val textValue = dialogView.findViewById<TextView>(R.id.textEraserValue)
        val titleText = dialogView.findViewById<TextView>(R.id.textDialogTitle)
        
        titleText.text = "Dimensione Penna"
        seekBar.max = 50
        seekBar.progress = Math.max(0, Math.min(50, strokeWidth.toInt() - 1))
        textValue.text = "${strokeWidth.toInt()} px"
        
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                textValue.text = "${progress + 1} px"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("Applica") { _, _ ->
                strokeWidth = (seekBar.progress + 1).toFloat()
                drawingView.setStrokeWidth(strokeWidth)
                activatePen()
            }
            .setNegativeButton("Annulla", null)
            .show()
    }
    
    private fun showEraserSizeDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_eraser_size, null)
        val seekBar = dialogView.findViewById<SeekBar>(R.id.seekBarEraserSize)
        val textValue = dialogView.findViewById<TextView>(R.id.textEraserValue)
        val titleText = dialogView.findViewById<TextView>(R.id.textDialogTitle)
        
        titleText.text = "Dimensione Gomma"
        seekBar.max = 200
        seekBar.progress = Math.max(0, Math.min(200, eraserSize.toInt() - 20))
        textValue.text = "${eraserSize.toInt()} px"
        
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                textValue.text = "${progress + 20} px"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("Applica") { _, _ ->
                eraserSize = (seekBar.progress + 20).toFloat()
                drawingView.setEraserSize(eraserSize)
                activateEraser()
            }
            .setNegativeButton("Annulla", null)
            .show()
    }
    
    private fun updateZoomLabel() {
        val scale = drawingView.getScaleFactor()
        textZoomLevel.text = "${(scale * 100).toInt()}%"
    }
    
    private fun showAddTextDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_text, null)
        val editText = dialogView.findViewById<EditText>(R.id.editTextInput)
        
        AlertDialog.Builder(this)
            .setTitle("Aggiungi Testo")
            .setView(dialogView)
            .setPositiveButton("Aggiungi") { _, _ ->
                val text = editText.text.toString()
                if (text.isNotEmpty()) {
                    textOverlayView.addText(text, 100f, 200f, currentColor, textSize)
                }
            }
            .setNegativeButton("Annulla", null)
            .show()
    }
    
    private fun showColorPicker() {
        val colorNames = arrayOf("Nero", "Rosso", "Blu", "Verde", "Giallo",
            "Arancione", "Viola", "Ciano", "Verde Chiaro", "Grigio", "Marrone")
        
        AlertDialog.Builder(this)
            .setTitle("Scegli colore")
            .setItems(colorNames) { _, which ->
                currentColor = colorPalette[which]
                drawingView.setDrawingColor(currentColor)
            }
            .show()
    }
    
    private fun saveWhiteboard() {
        try {
            val bitmap = Bitmap.createBitmap(drawingView.width, drawingView.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.WHITE)
            drawingView.draw(canvas)
            textOverlayView.draw(canvas)
            
            val file = File(filesDir, "whiteboard_${System.currentTimeMillis()}.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            Toast.makeText(this, "💾 Salvata!", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Errore: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
