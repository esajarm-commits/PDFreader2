package com.pdfreader.app

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import java.io.File
import java.io.FileInputStream
import java.util.Base64

class PDFViewerActivity : AppCompatActivity() {
    
    private lateinit var toolbar: Toolbar
    private lateinit var webView: WebView
    private lateinit var btnAddPage: Button
    private var pdfPath: String = ""
    private var pdfName: String = ""
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pdfviewer_webview)
        
        toolbar = findViewById(R.id.toolbar)
        webView = findViewById(R.id.webView)
        btnAddPage = findViewById(R.id.btnAddPage)
        
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        pdfPath = intent.getStringExtra("PDF_PATH") ?: ""
        pdfName = intent.getStringExtra("PDF_NAME") ?: "PDF"
        
        toolbar.title = pdfName
        
        if (pdfPath.isNotEmpty()) {
            loadPDF()
        } else {
            Toast.makeText(this, "Nessun PDF selezionato", Toast.LENGTH_LONG).show()
            finish()
        }
        
        btnAddPage.setOnClickListener {
            showAddPageDialog()
        }
    }
    
    private fun loadPDF() {
        try {
            val file = File(pdfPath)
            if (!file.exists()) {
                Toast.makeText(this, "File non trovato", Toast.LENGTH_LONG).show()
                finish()
                return
            }
            
            val inputStream = FileInputStream(file)
            val bytes = inputStream.readBytes()
            inputStream.close()
            val base64 = Base64.getEncoder().encodeToString(bytes)
            
            webView.settings.javaScriptEnabled = true
            webView.settings.loadWithOverviewMode = true
            webView.settings.useWideViewPort = true
            webView.settings.builtInZoomControls = true
            webView.settings.displayZoomControls = false
            webView.settings.allowFileAccess = true
            webView.settings.domStorageEnabled = true
            
            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    Toast.makeText(this@PDFViewerActivity, "PDF caricato!", Toast.LENGTH_SHORT).show()
                }
            }
            
            val html = buildHtml(base64)
            webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
            
        } catch (e: Exception) {
            Toast.makeText(this, "Errore: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }
    
    private fun buildHtml(base64: String): String {
        return "<!DOCTYPE html>" +
            "<html><head>" +
            "<meta charset=\"UTF-8\">" +
            "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=3.0, user-scalable=yes\">" +
            "<title>PDF Viewer</title>" +
            "<script src=\"https://cdnjs.cloudflare.com/ajax/libs/pdf.js/3.11.174/pdf.min.js\"></script>" +
            "<style>" +
            "* { margin: 0; padding: 0; box-sizing: border-box; }" +
            "body { background: #f5f0eb; font-family: sans-serif; }" +
            "#container { display: flex; flex-direction: column; align-items: center; padding: 10px; min-height: 100vh; }" +
            "canvas { max-width: 100% !important; height: auto !important; box-shadow: 0 2px 10px rgba(0,0,0,0.1); background: white; margin-bottom: 10px; }" +
            "#controls { position: fixed; bottom: 30px; left: 50%; transform: translateX(-50%); background: rgba(44,62,80,0.92); padding: 12px 24px; border-radius: 50px; display: flex; gap: 20px; align-items: center; color: white; z-index: 1000; }" +
            "#controls button { background: none; border: none; color: white; font-size: 22px; cursor: pointer; padding: 8px 16px; border-radius: 30px; min-width: 48px; min-height: 48px; }" +
            "#pageNum { font-size: 15px; min-width: 70px; text-align: center; }" +
            ".loading { text-align: center; padding: 60px 20px; font-size: 18px; color: #666; }" +
            "</style></head><body>" +
            "<div id=\"container\"><div id=\"loading\" class=\"loading\">Caricamento PDF...</div></div>" +
            "<div id=\"controls\">" +
            "<button id=\"prevBtn\">&#9664;</button>" +
            "<span id=\"pageNum\">0 / 0</span>" +
            "<button id=\"nextBtn\">&#9654;</button>" +
            "</div>" +
            "<script>" +
            "var pdfjsLib = window['pdfjs-dist/build/pdf'];" +
            "pdfjsLib.GlobalWorkerOptions.workerSrc = 'https://cdnjs.cloudflare.com/ajax/libs/pdf.js/3.11.174/pdf.worker.min.js';" +
            "var pdfDoc = null, pageNum = 1, scale = 1.2;" +
            "var container = document.getElementById('container');" +
            "var loading = document.getElementById('loading');" +
            "function renderPage(num) {" +
            "  pdfDoc.getPage(num).then(function(page) {" +
            "    var viewport = page.getViewport({scale: scale});" +
            "    var canvas = document.createElement('canvas');" +
            "    var ctx = canvas.getContext('2d');" +
            "    canvas.height = viewport.height;" +
            "    canvas.width = viewport.width;" +
            "    canvas.style.width = '100%';" +
            "    container.innerHTML = '';" +
            "    container.appendChild(canvas);" +
            "    page.render({canvasContext: ctx, viewport: viewport});" +
            "    document.getElementById('pageNum').textContent = num + ' / ' + pdfDoc.numPages;" +
            "  });" +
            "}" +
            "document.getElementById('prevBtn').onclick = function() {" +
            "  if (pageNum > 1) { pageNum--; renderPage(pageNum); }" +
            "};" +
            "document.getElementById('nextBtn').onclick = function() {" +
            "  if (pageNum < pdfDoc.numPages) { pageNum++; renderPage(pageNum); }" +
            "};" +
            "var pdfData = atob('" + base64 + "');" +
            "var pdfBytes = new Uint8Array(pdfData.length);" +
            "for (var i = 0; i < pdfData.length; i++) pdfBytes[i] = pdfData.charCodeAt(i);" +
            "pdfjsLib.getDocument({data: pdfBytes}).promise.then(function(doc) {" +
            "  pdfDoc = doc;" +
            "  loading.style.display = 'none';" +
            "  document.getElementById('pageNum').textContent = '1 / ' + pdfDoc.numPages;" +
            "  renderPage(1);" +
            "}).catch(function(e) {" +
            "  loading.textContent = 'Errore: ' + e.message;" +
            "  document.getElementById('controls').style.display = 'none';" +
            "});" +
            "</script></body></html>"
    }
    
    private fun showAddPageDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_page, null)
        val radioGroup = dialogView.findViewById<RadioGroup>(R.id.radioGroupPageType)
        val editPageNumber = dialogView.findViewById<EditText>(R.id.editPageNumber)
        
        AlertDialog.Builder(this)
            .setTitle("Aggiungi Pagina")
            .setView(dialogView)
            .setPositiveButton("Aggiungi") { _, _ ->
                val pageNumber = editPageNumber.text.toString().toIntOrNull() ?: 1
                
                val style = when (radioGroup.checkedRadioButtonId) {
                    R.id.radioLined -> "lined"
                    R.id.radioGrid -> "grid"
                    R.id.radioDotted -> "dotted"
                    else -> "blank"
                }
                
                addPageToPDF(pageNumber, style)
            }
            .setNegativeButton("Annulla", null)
            .show()
    }
    
    private fun addPageToPDF(afterPage: Int, style: String) {
        try {
            val originalFile = File(pdfPath)
            val outputFile = File(filesDir, "modified_${System.currentTimeMillis()}.pdf")
            
            // Mostra un messaggio di attesa
            Toast.makeText(this, "Aggiunta pagina in corso...", Toast.LENGTH_SHORT).show()
            
            // Esegui in un thread separato (PDFBox è lento)
            Thread {
                val success = PDFPageEditor.addPageToPDF(
                    originalPdf = originalFile,
                    outputFile = outputFile,
                    insertAfterPage = afterPage,
                    style = style
                )
                
                runOnUiThread {
                    if (success) {
                        Toast.makeText(this, "Pagina aggiunta!", Toast.LENGTH_SHORT).show()
                        pdfPath = outputFile.absolutePath
                        loadPDF()
                    } else {
                        Toast.makeText(this, "Errore nell'aggiunta", Toast.LENGTH_LONG).show()
                    }
                }
            }.start()
            
        } catch (e: Exception) {
            Toast.makeText(this, "Errore: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
