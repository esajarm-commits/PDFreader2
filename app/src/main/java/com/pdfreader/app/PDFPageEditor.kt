package com.pdfreader.app

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.droidpdf.core.PdfDocument as DroidPdfDocument
import com.droidpdf.core.PdfReader
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
    
    // Aggiunge una pagina a un PDF esistente
    fun addPageToPDF(
        originalPdf: File,
        outputFile: File,
        insertAfterPage: Int,
        style: String,
        pageWidth: Int,
        pageHeight: Int
    ): Boolean {
        return try {
            // 1. Crea la pagina stilizzata in un file temporaneo
            val tempPage = File.createTempFile("styled_page", ".pdf")
            val tempStream = FileOutputStream(tempPage)
            createStyledPage(pageWidth, pageHeight, 1, style, tempStream)
            tempStream.close()
            
            // 2. Apri il PDF originale con DroidPDF
            val pdf = DroidPdfDocument(
                PdfReader(originalPdf.inputStream()),
                FileOutputStream(outputFile)
            )
            
            // 3. Apri la pagina temporanea e inseriscila
            val tempPdf = DroidPdfDocument(
                PdfReader(tempPage.inputStream()),
                FileOutputStream(File.createTempFile("dummy", ".pdf"))
            )
            
            // Inserisci la pagina dopo quella specificata (indice 0-based)
            val insertIndex = (insertAfterPage - 1).coerceAtLeast(0)
            pdf.insertPage(insertIndex, tempPdf.getPage(0))
            
            pdf.close()
            tempPdf.close()
            tempPage.delete()
            
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
