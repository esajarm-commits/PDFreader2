import androidx.appcompat.app.AlertDialog
package com.pdfreader.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var textEmpty: TextView
    private lateinit var adapter: PDFAdapter
    
    private val pdfFiles = mutableListOf<PDFFile>()
    
    private val pickPDF = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream = contentResolver.openInputStream(uri)
                val fileName = "document_${System.currentTimeMillis()}.pdf"
                val file = File(filesDir, fileName)
                
                inputStream?.use { input ->
                    FileOutputStream(file).use { output ->
                        input.copyTo(output)
                    }
                }
                
                val pdfFile = PDFFile(
                    name = fileName,
                    path = file.absolutePath
                )
                
                pdfFiles.add(pdfFile)
                adapter.submitList(pdfFiles)
                updateEmptyView()
                Toast.makeText(this, "PDF aggiunto!", Toast.LENGTH_SHORT).show()
                
            } catch (e: Exception) {
                Toast.makeText(this, "Errore: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        recyclerView = findViewById(R.id.recyclerView)
        fabAdd = findViewById(R.id.fabAdd)
val btnWhiteboard = findViewById<android.widget.Button>(R.id.btnWhiteboard)
btnWhiteboard.setOnClickListener {
    startActivity(Intent(this, WhiteboardActivity::class.java))
}
        textEmpty = findViewById(R.id.textEmpty)
        
        setupRecyclerView()
            private fun setupFab() {
        fabAdd.setOnClickListener {
            showAddDialog()
        }
    }
    
    private fun showAddDialog() {
        val options = arrayOf(
            "📄 Aggiungi PDF esistente",
            "🎨 Crea nuovo con template"
        )
        
        AlertDialog.Builder(this)
            .setTitle("Cosa vuoi fare?")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> pickPDF.launch(arrayOf("application/pdf"))
                    1 -> showTemplatePickerForNewPdf()
                }
            }
            .show()
    }
    
    private fun showTemplatePickerForNewPdf() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_template_picker, null)
        val recyclerTemplates = dialogView.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recyclerTemplates)
        
        val templates = TemplateManager.loadAllTemplates(this)
        
        if (templates.isEmpty()) {
            Toast.makeText(this, "Nessun template trovato", Toast.LENGTH_LONG).show()
            return
        }
        
        recyclerTemplates.layoutManager = androidx.recyclerview.widget.GridLayoutManager(this, 3)
        
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setNegativeButton("Annulla", null)
            .create()
        
        val adapter = TemplateAdapter(templates) { template ->
            dialog.dismiss()
            createNewPdfFromTemplate(template)
        }
        recyclerTemplates.adapter = adapter
        
        dialog.show()
    }
    
    private fun createNewPdfFromTemplate(template: TemplateManager.Template) {
        // Chiedi il nome del nuovo PDF
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_text, null)
        val editText = dialogView.findViewById<android.widget.EditText>(R.id.editTextInput)
        editText.hint = "Nome del nuovo PDF"
        
        AlertDialog.Builder(this)
            .setTitle("Crea nuovo PDF")
            .setView(dialogView)
            .setPositiveButton("Crea") { _, _ ->
                val pdfName = editText.text.toString()
                if (pdfName.isEmpty()) {
                    Toast.makeText(this, "Inserisci un nome", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                // Apri la lavagna con il template scelto
                val intent = Intent(this, WhiteboardActivity::class.java)
                intent.putExtra("PDF_PATH", "")  // Nessun PDF di partenza
                intent.putExtra("PDF_NAME", pdfName)
                intent.putExtra("TEMPLATE_FILE", template.fileName)
                intent.putExtra("INSERT_AFTER", 1)
                intent.putExtra("IS_NEW_PDF", true)
                startActivity(intent)
            }
            .setNegativeButton("Annulla", null)
            .show()
    }

