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
    
    private var isPenMode = true
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
        btnPen.setOnClickListener {
            isPenMode = true
            drawingView.enableDrawing(true)
            drawingView.setEraserMode(false)
            btnPen.setBackgroundColor(Color.parseColor("#4CAF50"))
            btnEraser.setBackgroundColor(Color.parseColor("#9E9E9E"))
            btnMove.setBackgroundColor(Color.parseColor("#9E9E9E"))
            Toast.makeText(this, "✏️ Penna - disegna con un dito", Toast.LENGTH_SHORT).show()
        }
        
        btnEraser.setOnClickListener {
            showEraserSizeDialog()
        }
        
        btnMove.setOnClickListener {
            isPenMode = false
            drawingView.enableDrawing(false)
            drawingView.setEraserMode(false)
            btnMove.setBackgroundColor(Color.parseColor("#FF5722"))
            btnPen.setBackgroundColor(Color.parseColor("#9E9E9E"))
            btnEraser.setBackgroundColor(Color.parseColor("#9E9E9E"))
            Toast.makeText(this, "✋ Muovi - trascina con un dito", Toast.LENGTH_SHORT).show()
        }
        
        btnText.setOnClickListener {
            showAddTextDialog()
        }
        
        btnColor.setOnClickListener {
            showColorPicker()
        }
        
        btnCenter.setOnClickListener {
            drawingView.centerView()
            updateZoomLabel()
            Toast.makeText(this, "🎯 Vista centrata", Toast.LENGTH_SHORT).show()
        }
        
        btnUndo.setOnClickListener {
            drawingView.undo()
            Toast.makeText(this, "↩️ Annullato", Toast.LENGTH_SHORT).show()
        }
        
        btnClear.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Cancellare tutto?")
                .setMessage("Vuoi cancellare tutta la lavagna?")
                .setPositiveButton("Sì") { _, _ ->
                    drawingView.clearAll()
                    textOverlayView.clearAll()
                    Toast.makeText(this, "🗑️ Lavagna cancellata", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("No", null)
                .show()
        }
        
        btnSave.setOnClickListener {
            saveWhiteboard()
        }
        
        drawingView.post(object : Runnable {
            override fun run() {
                updateZoomLabel()
                drawingView.postDelayed(this, 150)
            }
        })
    }
    
    private fun showEraserSizeDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_eraser_size, null)
        val seekBar = dialogView.findViewById<SeekBar>(R.id.seekBarEraserSize)
        val textValue = dialogView.findViewById<TextView>(R.id.textEraserValue)
        
        seekBar.progress = eraserSize.toInt() - 20  // min 20
        textValue.text = "${eraserSize.toInt()} px"
        
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val size = (progress + 20).toFloat()
                textValue.text = "${size.toInt()} px"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        AlertDialog.Builder(this)
            .setTitle("🧽 Dimensione Gomma")
            .setView(dialogView)
            .setPositiveButton("Attiva") { _, _ ->
                eraserSize = (seekBar.progress + 20).toFloat()
                drawingView.setEraserSize(eraserSize)
                drawingView.setEraserMode(true)
                drawingView.enableDrawing(true)
                isPenMode = false
                btnEraser.setBackgroundColor(Color.parseColor("#FF9800"))
                btnPen.setBackgroundColor(Color.parseColor("#9E9E9E"))
                btnMove.setBackgroundColor(Color.parseColor("#9E9E9E"))
                Toast.makeText(this, "🧽 Gomma attiva - ${eraserSize.toInt()}px", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Annulla", null)
            .show()
    }
    
    private fun updateZoomLabel() {
        val scale = drawingView.getScaleFactor()
        textZoomLevel.text = "${(scale * 100).toInt()}%"
        btnCenter.text = "🎯 ${(scale * 100).toInt()}%"
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
                    val x = 100f
                    val y = 200f + (textOverlayView.textItems.size * 100f)
                    textOverlayView.addText(text, x, y, currentColor, textSize)
                    Toast.makeText(this, "✅ Testo aggiunto", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Annulla", null)
            .show()
    }
    
    private fun showColorPicker() {
        val colorNames = arrayOf(
            "Nero", "Rosso", "Blu", "Verde", "Giallo",
            "Arancione", "Viola", "Ciano", "Verde Chiaro",
            "Grigio", "Marrone"
        )
        
        AlertDialog.Builder(this)
            .setTitle("Scegli colore")
            .setItems(colorNames) { _, which ->
                currentColor = colorPalette[which]
                drawingView.setDrawingColor(currentColor)
                Toast.makeText(this, "Colore: ${colorNames[which]}", Toast.LENGTH_SHORT).show()
            }
            .show()
    }
    
    private fun saveWhiteboard() {
        try {
            val bitmap = Bitmap.createBitmap(
                drawingView.width,
                drawingView.height,
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.WHITE)
            drawingView.draw(canvas)
            textOverlayView.draw(canvas)
            
            val fileName = "whiteboard_${System.currentTimeMillis()}.png"
            val file = File(filesDir, fileName)
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            
            Toast.makeText(this, "💾 Lavagna salvata: $fileName", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Errore: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
