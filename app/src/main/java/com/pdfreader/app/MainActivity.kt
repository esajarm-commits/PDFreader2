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
        setupFab()
        checkPermissions()
        loadExistingPDFs()
    }
    
    private fun setupRecyclerView() {
        adapter = PDFAdapter { pdf ->
            val intent = Intent(this, PDFViewerActivity::class.java)
            intent.putExtra("PDF_PATH", pdf.path)
            intent.putExtra("PDF_NAME", pdf.name)
            startActivity(intent)
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }
    
    private fun setupFab() {
        fabAdd.setOnClickListener {
            pickPDF.launch(arrayOf("application/pdf"))
        }
    }
    
    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_MEDIA_IMAGES
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(
                    arrayOf(
                        Manifest.permission.READ_MEDIA_IMAGES,
                        Manifest.permission.READ_MEDIA_VIDEO,
                        Manifest.permission.READ_MEDIA_AUDIO
                    ),
                    1001
                )
            }
        }
    }
    
    private fun loadExistingPDFs() {
        val filesDir = filesDir
        val files = filesDir.listFiles { file -> file.extension == "pdf" }
        files?.forEach { file ->
            val pdfFile = PDFFile(
                name = file.name,
                path = file.absolutePath
            )
            pdfFiles.add(pdfFile)
        }
        adapter.submitList(pdfFiles)
        updateEmptyView()
    }
    
    private fun updateEmptyView() {
        if (pdfFiles.isEmpty()) {
            textEmpty.visibility = android.view.View.VISIBLE
            recyclerView.visibility = android.view.View.GONE
        } else {
            textEmpty.visibility = android.view.View.GONE
            recyclerView.visibility = android.view.View.VISIBLE
        }
    }
}

data class PDFFile(
    val name: String,
    val path: String,
    val lastOpened: Long = System.currentTimeMillis()
)
