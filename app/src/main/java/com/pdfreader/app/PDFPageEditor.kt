package com.pdfreader.app

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.FileOutputStream

object PDFPageEditor {
    
    // Crea una pagina con stile (bianca, righe, quadri, punti)
    fun createStyledPage(
        pageWidth: Int,
        pageHeight: Int,
        pageNumber: Int,
        style: String,
        outputStream: FileOutputStream
    ) {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas
        
        canvas.drawColor(Color.WHITE)
        
        val paint = Paint().apply {
            color = Color.parseColor("#CCCCCC")
            strokeWidth = 1f
            isAntiAlias = true
        }
        
        when (style) {
            "lined" -> {
                val spacing = 40f
                var y = 60f
                while (y < pageHeight - 60) {
                    canvas.drawLine(40f, y, pageWidth - 40f, y, paint)
                    y += spacing
                }
            }
            "grid" -> {
                val spacing = 40f
                var x = 40f
                while (x < pageWidth - 40) {
                    canvas.drawLine(x, 40f, x, pageHeight - 40f, paint)
                    x += spacing
                }
                var y = 40f
                while (y < pageHeight - 40) {
                    canvas.drawLine(40f, y, pageWidth - 40f, y, paint)
                    y += spacing
                }
            }
            "dotted" -> {
                val spacing = 40f
                val dotPaint = Paint().apply {
                    color = Color.parseColor("#999999")
                    style = Paint.Style.FILL
                    isAntiAlias = true
                }
                var y = 60f
                while (y < pageHeight - 60) {
                    var x = 60f
                    while (x < pageWidth - 60) {
                        canvas.drawCircle(x, y, 3f, dotPaint)
                        x += spacing
                    }
                    y += spacing
                }
            }
        }
        
        document.finishPage(page)
        document.writeTo(outputStream)
        document.close()
    }
    
    // Aggiunge una pagina al PDF usando solo PdfDocument nativo
    // NOTA: questa funzione è un placeholder. L'aggiunta reale di pagine a un PDF
    // esistente richiede una libreria esterna (PDFBox, iText, DroidPDF, ecc.)
    fun addPageToPDF(
        originalPdf: File,
        outputFile: File,
        insertAfterPage: Int,
        style: String,
        pageWidth: Int,
        pageHeight: Int
    ): Boolean {
        return try {
            // Per ora creiamo solo la pagina stilizzata
            // L'unione con il PDF originale richiede una libreria esterna
            val tempPage = File.createTempFile("styled_page", ".pdf")
            val tempStream = FileOutputStream(tempPage)
            createStyledPage(pageWidth, pageHeight, 1, style, tempStream)
            tempStream.close()
            
            // Copia il file temporaneo come output (provvisorio)
            tempPage.copyTo(outputFile, overwrite = true)
            tempPage.delete()
            
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
