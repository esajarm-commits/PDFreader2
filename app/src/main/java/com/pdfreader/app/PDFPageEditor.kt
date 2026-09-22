package com.pdfreader.app

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.FileOutputStream

object PDFPageEditor {
    
    fun createPageWithStyle(
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
        
        // Sfondo bianco
        canvas.drawColor(Color.WHITE)
        
        val paint = Paint().apply {
            color = Color.parseColor("#CCCCCC")
            strokeWidth = 1f
            isAntiAlias = true
        }
        
        when (style) {
            "lined" -> {
                // Righe orizzontali ogni 40px
                val spacing = 40f
                var y = 60f
                while (y < pageHeight - 60) {
                    canvas.drawLine(40f, y, pageWidth - 40f, y, paint)
                    y += spacing
                }
            }
            "grid" -> {
                // Griglia di quadrati 40x40
                val spacing = 40f
                // Linee verticali
                var x = 40f
                while (x < pageWidth - 40) {
                    canvas.drawLine(x, 40f, x, pageHeight - 40f, paint)
                    x += spacing
                }
                // Linee orizzontali
                var y = 40f
                while (y < pageHeight - 40) {
                    canvas.drawLine(40f, y, pageWidth - 40f, y, paint)
                    y += spacing
                }
            }
            "dotted" -> {
                // Punti ogni 40px
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
            // "blank" -> niente da disegnare
        }
        
        document.finishPage(page)
        document.writeTo(outputStream)
        document.close()
    }
    
    fun addPageToPDF(
        originalPdf: File,
        outputFile: File,
        insertAfterPage: Int,
        style: String,
        pageWidth: Int,
        pageHeight: Int
    ): Boolean {
        return try {
            // Usa DroidPDF per inserire la pagina
            val pdf = com.droidpdf.PdfDocument(
                com.droidpdf.PdfReader(originalPdf.inputStream()),
                FileOutputStream(outputFile)
            )
            
            // Crea la pagina con stile
            val tempFile = File.createTempFile("temp_page", ".pdf")
            val tempStream = FileOutputStream(tempFile)
            createPageWithStyle(pageWidth, pageHeight, 1, style, tempStream)
            tempStream.close()
            
            // Inserisci la pagina dopo quella specificata
            val tempPdf = com.droidpdf.PdfDocument(
                com.droidpdf.PdfReader(tempFile.inputStream()),
                FileOutputStream(File.createTempFile("dummy", ".pdf"))
            )
            
            pdf.insertPage(insertAfterPage, tempPdf.getPage(0))
            
            pdf.close()
            tempFile.delete()
            
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
