package com.pdfreader.app

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.FileOutputStream

object PDFPageEditor {
    
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
        
        // Paint per linee
        val linePaint = Paint()
        linePaint.color = Color.parseColor("#CCCCCC")
        linePaint.strokeWidth = 1f
        linePaint.isAntiAlias = true
        linePaint.style = Paint.Style.STROKE
        
        // Paint per punti
        val dotPaint = Paint()
        dotPaint.color = Color.parseColor("#999999")
        dotPaint.style = Paint.Style.FILL
        dotPaint.isAntiAlias = true
        
        when (style) {
            "lined" -> {
                val spacing = 40f
                var y = 60f
                while (y < pageHeight - 60) {
                    canvas.drawLine(40f, y, pageWidth - 40f, y, linePaint)
                    y += spacing
                }
            }
            "grid" -> {
                val spacing = 40f
                var x = 40f
                while (x < pageWidth - 40) {
                    canvas.drawLine(x, 40f, x, pageHeight - 40f, linePaint)
                    x += spacing
                }
                var y = 40f
                while (y < pageHeight - 40) {
                    canvas.drawLine(40f, y, pageWidth - 40f, y, linePaint)
                    y += spacing
                }
            }
            "dotted" -> {
                val spacing = 40f
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
    
    fun addPageToPDF(
        originalPdf: File,
        outputFile: File,
        insertAfterPage: Int,
        style: String,
        pageWidth: Int,
        pageHeight: Int
    ): Boolean {
        return try {
            val tempPage = File.createTempFile("styled_page", ".pdf")
            val tempStream = FileOutputStream(tempPage)
            createStyledPage(pageWidth, pageHeight, 1, style, tempStream)
            tempStream.close()
            
            tempPage.copyTo(outputFile, overwrite = true)
            tempPage.delete()
            
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
