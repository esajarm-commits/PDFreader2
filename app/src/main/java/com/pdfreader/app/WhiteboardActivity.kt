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
import com.pdfreader.app.data.AppDatabase
import com.pdfreader.app.data.NotePage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
    
    private var pdfPath: String = ""
    private var pdfName: String = ""
    private var templateFileName: String = ""
    private var insertAfterPage: Int = 1
    private var existingPageId: Long = -1
    private var existingImagePath: String = ""
    private var isNewPdf: Boolean = false
    
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
        
        pdfPath = intent.getStringExtra("PDF_PATH") ?: ""
        pdfName = intent.getStringExtra("PDF_NAME") ?: "PDF"
        templateFileName = intent.getStringExtra("TEMPLATE_FILE") ?: ""
        insertAfterPage = intent.getIntExtra("INSERT_AFTER", 1)
        existingPageId = intent.getLongExtra("PAGE_ID", -1)
        existingImagePath = intent.getStringExtra("IMAGE_PATH") ?: ""
        isNewPdf = intent.getBooleanExtra("IS_NEW_PDF", false)
        
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }
        
        if (templateFileName.isNotEmpty()) {
            loadTemplateBackground(templateFileName)
        } else if (existingImagePath.isNotEmpty()) {
            loadExistingImage(existingImagePath)
        }
        
        setupButtons()
        updateZoomLabel()
    }
    
    private fun loadTemplateBackground(fileName: String) {
        try {
            val inputStream = assets.open("templates/$fileName")
            val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            if (bitmap != null) drawingView.setBackgroundBitmap(bitmap)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun loadExistingImage(path: String) {
        try {
            val bitmap = android.graphics.BitmapFactory.decodeFile(path)
            if (bitmap != null) drawingView.setBackgroundBitmap(bitmap)
        } catch (e: Exception) {
            e.printStackTrace()
        }
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
        }
        
        btnText.setOnClickListener { showAddTextDialog() }
        btnColor.setOnClickListener { showColorPicker() }
        btnCenter.setOnClickListener {
            drawingView.centerView()
            updateZoomLabel()
        }
        btnUndo.setOnClickListener { drawingView.undo() }
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
        btnSave.setOnClickListener { saveAndExit() }
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
    
    private fun saveAndExit() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_text, null)
        val editText = dialogView.findViewById<EditText>(R.id.editTextInput)
        editText.hint = "Nome della pagina"
        
        AlertDialog.Builder(this)
            .setTitle("Salva Pagina")
            .setView(dialogView)
            .setPositiveButton("Salva") { _, _ ->
                val pageName = editText.text.toString()
                if (pageName.isEmpty()) {
                    Toast.makeText(this, "Inserisci un nome", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                savePageToDatabase(pageName)
            }
            .setNegativeButton("Annulla", null)
            .show()
    }
    
    private fun savePageToDatabase(pageName: String) {
        try {
            val bitmap = Bitmap.createBitmap(
                drawingView.width.coerceAtLeast(595),
                drawingView.height.coerceAtLeast(842),
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.WHITE)
            
            if (isNewPdf) {
                // Salva come NUOVO PDF
                val pdfFile = File(filesDir, "${pdfName.replace(" ", "_")}.pdf")
                val pdfDocument = android.graphics.pdf.PdfDocument()
                val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(
                    bitmap.width, bitmap.height, 1
                ).create()
                val page = pdfDocument.startPage(pageInfo)
                page.canvas.drawBitmap(bitmap, 0f, 0f, null)
                pdfDocument.finishPage(page)
                FileOutputStream(pdfFile).use { out -> pdfDocument.writeTo(out) }
                pdfDocument.close()
                runOnUiThread {
                    Toast.makeText(this@WhiteboardActivity, "PDF creato: ${pdfFile.name}", Toast.LENGTH_LONG).show()
                    finish()
                }
                return
            }
            
            // Salva come nota nel database
            val imagesDir = File(filesDir, "note_pages")
            if (!imagesDir.exists()) imagesDir.mkdirs()
            val imageFile = File(imagesDir, "page_${System.currentTimeMillis()}.png")
            FileOutputStream(imageFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            
            val db = AppDatabase.getInstance(this)
            val page = NotePage(
                id = if (existingPageId > 0) existingPageId else 0,
                pdfPath = pdfPath,
                pdfName = pdfName,
                pageName = pageName,
                style = templateFileName,
                insertAfterPage = insertAfterPage,
                imagePath = imageFile.absolutePath
            )
            
            CoroutineScope(Dispatchers.IO).launch {
                if (existingPageId > 0) {
                    db.notePageDao().update(page)
                } else {
                    db.notePageDao().insert(page)
                }
                runOnUiThread {
                    Toast.makeText(this@WhiteboardActivity, "Pagina salvata: $pageName", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Errore: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
