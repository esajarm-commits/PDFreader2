package com.pdfreader.app

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
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
    private lateinit var btnText: Button
    private lateinit var btnColor: Button
    private lateinit var btnZoomReset: Button
    private lateinit var btnUndo: Button
    private lateinit var btnClear: Button
    private lateinit var btnSave: Button
    
    private var isPenMode = true
    private var currentColor = Color.BLACK
    private var strokeWidth = 6f
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
        btnText = findViewById(R.id.btnText)
        btnColor = findViewById(R.id.btnColor)
        btnZoomReset = findViewById(R.id.btnZoomReset)
        btnUndo = findViewById(R.id.btnUndo)
        btnClear = findViewById(R.id.btnClear)
        btnSave = findViewById(R.id.btnSave)
        
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }
        
        setupButtons()
    }
    
    private fun setupButtons() {
        btnPen.setOnClickListener {
            isPenMode = true
            drawingView.enableDrawing(true)
            btnPen.setBackgroundColor(Color.parseColor("#4CAF50"))
            btnText.setBackgroundColor(Color.parseColor("#9E9E9E"))
            Toast.makeText(this, "✏️ Penna attiva - disegna con un dito", Toast.LENGTH_SHORT).show()
        }
        
        btnText.setOnClickListener {
            isPenMode = false
            drawingView.enableDrawing(false)
            btnText.setBackgroundColor(Color.parseColor("#2196F3"))
            btnPen.setBackgroundColor(Color.parseColor("#9E9E9E"))
            showAddTextDialog()
        }
        
        btnColor.setOnClickListener {
            showColorPicker()
        }
        
        btnZoomReset.setOnClickListener {
            drawingView.resetZoom()
            updateZoomLabel()
            Toast.makeText(this, "🔍 Zoom resettato", Toast.LENGTH_SHORT).show()
        }
        
        btnUndo.setOnClickListener {
            if (isPenMode) {
                drawingView.undo()
            } else {
                textOverlayView.undo()
            }
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
        
        // Aggiorna l'indicatore di zoom periodicamente
        drawingView.post(object : Runnable {
            override fun run() {
                updateZoomLabel()
                drawingView.postDelayed(this, 200)
            }
        })
    }
    
    private fun updateZoomLabel() {
        val scale = drawingView.getScaleFactor()
        textZoomLevel.text = "${(scale * 100).toInt()}%"
        btnZoomReset.text = "🔍 ${(scale * 100).toInt()}%"
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
                    val y = 200f + (textOverlayView.textItems.size * 80f)
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
