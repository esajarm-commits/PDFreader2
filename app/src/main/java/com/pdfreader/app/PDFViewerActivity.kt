package com.pdfreader.app

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.pdfreader.app.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.ByteArrayOutputStream

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
            loadPDFWithNotes()
        } else {
            Toast.makeText(this, "Nessun PDF selezionato", Toast.LENGTH_LONG).show()
            finish()
        }
        
        btnAddPage.setOnClickListener {
            showAddPageDialog()
        }
    }
    
    private fun loadPDFWithNotes() {
        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getInstance(this@PDFViewerActivity)
            val notePages = db.notePageDao().getPagesForPdf(pdfPath)
            
            // Raccogli le pagine in una lista
            val pagesList = mutableListOf<Any>()
            notePages.collect { pages ->
                runOnUiThread {
                    loadPDFWithNotesList(pages)
                }
            }
        }
    }
    
    private fun loadPDFWithNotesList(notePages: List<com.pdfreader.app.data.NotePage>) {
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
            val pdfBase64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            
            // Prepara le immagini delle note
            val notesJson = buildNotesJson(notePages)
            
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
            
            val html = buildHtml(pdfBase64, notesJson)
            webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
            
        } catch (e: Exception) {
            Toast.makeText(this, "Errore: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun buildNotesJson(notePages: List<com.pdfreader.app.data.NotePage>): String {
        val sb = StringBuilder("[")
        notePages.forEachIndexed { index, page ->
            if (index > 0) sb.append(",")
            
            // Leggi l'immagine e convertila in base64
            val imageFile = File(page.imagePath)
            val imageBase64 = if (imageFile.exists()) {
                val bytes = imageFile.readBytes()
                Base64.encodeToString(bytes, Base64.NO_WRAP)
            } else ""
            
            sb.append("{")
            sb.append("\"id\":${page.id},")
            sb.append("\"name\":\"${page.pageName.replace("\"", "\\\"")}\",")
            sb.append("\"after\":${page.insertAfterPage},")
            sb.append("\"image\":\"$imageBase64\"")
            sb.append("}")
        }
        sb.append("]")
        return sb.toString()
    }
    
    private fun buildHtml(pdfBase64: String, notesJson: String): String {
        return "<!DOCTYPE html>" +
            "<html><head>" +
            "<meta charset=\"UTF-8\">" +
            "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=3.0, user-scalable=yes\">" +
            "<title>PDF Viewer</title>" +
            "<script src=\"https://cdnjs.cloudflare.com/ajax/libs/pdf.js/3.11.174/pdf.min.js\"></script>" +
            "<style>" +
            "* { margin: 0; padding: 0; box-sizing: border-box; }" +
            "body { background: #f5f0eb; font-family: sans-serif; padding-bottom: 100px; }" +
            "#container { display: flex; flex-direction: column; align-items: center; padding: 10px; }" +
            ".page-wrapper { position: relative; margin-bottom: 20px; width: 100%; max-width: 100%; }" +
            ".page-label { text-align: center; padding: 6px; font-size: 13px; color: #666; background: #FFF3E0; border-radius: 8px 8px 0 0; }" +
            ".page-label.note { background: #E3F2FD; color: #1565C0; font-weight: bold; }" +
            "canvas, .note-image { max-width: 100% !important; height: auto !important; box-shadow: 0 2px 10px rgba(0,0,0,0.1); background: white; display: block; }" +
            ".note-image { border: 2px solid #2196F3; }" +
            "#controls { position: fixed; bottom: 0; left: 0; right: 0; background: rgba(44,62,80,0.95); padding: 12px 24px; display: flex; justify-content: center; gap: 20px; align-items: center; color: white; z-index: 1000; }" +
            "#controls button { background: none; border: none; color: white; font-size: 22px; cursor: pointer; padding: 8px 16px; border-radius: 30px; min-width: 48px; }" +
            ".loading { text-align: center; padding: 60px 20px; font-size: 18px; color: #666; }" +
            "</style></head><body>" +
            "<div id=\"container\"><div class=\"loading\">Caricamento PDF...</div></div>" +
            "<div id=\"controls\">" +
            "<button onclick=\"window.scrollTo({top:0,behavior:'smooth'})\">&#9650;</button>" +
            "<span id=\"pageCount\">-</span>" +
            "<button onclick=\"window.scrollTo({top:document.body.scrollHeight,behavior:'smooth'})\">&#9660;</button>" +
            "</div>" +
            "<script>" +
            "var pdfjsLib = window['pdfjs-dist/build/pdf'];" +
            "pdfjsLib.GlobalWorkerOptions.workerSrc = 'https://cdnjs.cloudflare.com/ajax/libs/pdf.js/3.11.174/pdf.worker.min.js';" +
            "var notes = " + notesJson + ";" +
            "var container = document.getElementById('container');" +
            "var scale = 1.5;" +
            "" +
            "function renderAllPages() {" +
            "  var pdfData = atob('" + pdfBase64 + "');" +
            "  var pdfBytes = new Uint8Array(pdfData.length);" +
            "  for (var i = 0; i < pdfData.length; i++) pdfBytes[i] = pdfData.charCodeAt(i);" +
            "  pdfjsLib.getDocument({data: pdfBytes}).promise.then(function(pdfDoc) {" +
            "    container.innerHTML = '';" +
            "    var totalPdfPages = pdfDoc.numPages;" +
            "    var notesByAfter = {};" +
            "    notes.forEach(function(n) {" +
            "      if (!notesByAfter[n.after]) notesByAfter[n.after] = [];" +
            "      notesByAfter[n.after].push(n);" +
            "    });" +
            "    " +
            "    function renderPage(pdfPageNum, callback) {" +
            "      pdfDoc.getPage(pdfPageNum).then(function(page) {" +
            "        var viewport = page.getViewport({scale: scale});" +
            "        var wrapper = document.createElement('div');" +
            "        wrapper.className = 'page-wrapper';" +
            "        var label = document.createElement('div');" +
            "        label.className = 'page-label';" +
            "        label.textContent = 'PDF - Pagina ' + pdfPageNum + ' di ' + totalPdfPages;" +
            "        wrapper.appendChild(label);" +
            "        var canvas = document.createElement('canvas');" +
            "        var ctx = canvas.getContext('2d');" +
            "        canvas.height = viewport.height;" +
            "        canvas.width = viewport.width;" +
            "        wrapper.appendChild(canvas);" +
            "        container.appendChild(wrapper);" +
            "        page.render({canvasContext: ctx, viewport: viewport}).promise.then(function() {" +
            "          callback();" +
            "        });" +
            "      });" +
            "    }" +
            "    " +
            "    function addNotePage(note, callback) {" +
            "      var wrapper = document.createElement('div');" +
            "      wrapper.className = 'page-wrapper';" +
            "      var label = document.createElement('div');" +
            "      label.className = 'page-label note';" +
            "      label.textContent = 'NOTA: ' + note.name;" +
            "      wrapper.appendChild(label);" +
            "      if (note.image) {" +
            "        var img = document.createElement('img');" +
            "        img.className = 'note-image';" +
            "        img.src = 'data:image/png;base64,' + note.image;" +
            "        wrapper.appendChild(img);" +
            "      } else {" +
            "        var empty = document.createElement('div');" +
            "        empty.style.cssText = 'height:400px;background:white;display:flex;align-items:center;justify-content:center;color:#999;';" +
            "        empty.textContent = 'Pagina vuota';" +
            "        wrapper.appendChild(empty);" +
            "      }" +
            "      container.appendChild(wrapper);" +
            "      callback();" +
            "    }" +
            "    " +
            "    function processAll(pageNum) {" +
            "      if (pageNum > totalPdfPages) {" +
            "        if (notesByAfter[totalPdfPages + 1]) {" +
            "          notesByAfter[totalPdfPages + 1].forEach(function(note) { addNotePage(note, function(){}); });" +
            "        }" +
            "        document.getElementById('pageCount').textContent = 'Fatto';" +
            "        return;" +
            "      }" +
            "      renderPage(pageNum, function() {" +
            "        var notesForThis = notesByAfter[pageNum] || [];" +
            "        var idx = 0;" +
            "        function nextNote() {" +
            "          if (idx >= notesForThis.length) {" +
            "            processAll(pageNum + 1);" +
            "            return;" +
            "          }" +
            "          addNotePage(notesForThis[idx], function() {" +
            "            idx++;" +
            "            nextNote();" +
            "          });" +
            "        }" +
            "        nextNote();" +
            "      });" +
            "    }" +
            "    " +
            "    processAll(1);" +
            "  }).catch(function(e) {" +
            "    container.innerHTML = '<div style=\"padding:40px;color:#c00;\">Errore: ' + e.message + '</div>';" +
            "  });" +
            "}" +
            "" +
            "renderAllPages();" +
            "</script></body></html>"
    }
    
    private fun showAddPageDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_page, null)
        val radioGroup = dialogView.findViewById<RadioGroup>(R.id.radioGroupPageType)
        val editPageNumber = dialogView.findViewById<EditText>(R.id.editPageNumber)
        
        AlertDialog.Builder(this)
            .setTitle("Aggiungi Pagina")
            .setView(dialogView)
            .setPositiveButton("Crea") { _, _ ->
                val pageNumber = editPageNumber.text.toString().toIntOrNull() ?: 1
                
                val style = when (radioGroup.checkedRadioButtonId) {
                    R.id.radioLined -> "lined"
                    R.id.radioGrid -> "grid"
                    R.id.radioDotted -> "dotted"
                    else -> "blank"
                }
                
                // Apri la lavagna con le info della pagina
                val intent = Intent(this, WhiteboardActivity::class.java)
                intent.putExtra("PDF_PATH", pdfPath)
                intent.putExtra("PDF_NAME", pdfName)
                intent.putExtra("STYLE", style)
                intent.putExtra("INSERT_AFTER", pageNumber)
                startActivity(intent)
            }
            .setNegativeButton("Annulla", null)
            .show()
    }
    
    override fun onResume() {
        super.onResume()
        // Ricarica il PDF quando si torna dalla lavagna
        if (pdfPath.isNotEmpty()) {
            loadPDFWithNotes()
        }
    }
}
