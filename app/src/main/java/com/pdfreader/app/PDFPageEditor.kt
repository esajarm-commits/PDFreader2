package com.pdfreader.app

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject
import java.io.File
import java.io.FileOutputStream

object PDFPageEditor {
    
    // Crea un'immagine bitmap con lo stile richiesto
    fun createStyledBitmap(
        width: Int,
        height: Int,
        style: String
    ): android.graphics.Bitmap {
        val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        
        val linePaint = Paint()
        linePaint.color = Color.parseColor("#CCCCCC")
        linePaint.strokeWidth = 1f
        linePaint.isAntiAlias = true
        linePaint.setStyle(Paint.Style.STROKE)
        
        val dotPaint = Paint()
        dotPaint.color = Color.parseColor("#999999")
        dotPaint.isAntiAlias = true
        dotPaint.setStyle(Paint.Style.FILL)
        
        when (style) {
            "lined" -> {
                val spacing = 40f
                var y = 60f
                while (y < height - 60) {
                    canvas.drawLine(40f, y, width - 40f, y, linePaint)
                    y += spacing
                }
            }
            "grid" -> {
                val spacing = 40f
                var x = 40f
                while (x < width - 40) {
                    canvas.drawLine(x, 40f, x, height - 40f, linePaint)
                    x += spacing
                }
                var y = 40f
                while (y < height - 40) {
                    canvas.drawLine(40f, y, width - 40f, y, linePaint)
                    y += spacing
                }
            }
            "dotted" -> {
                val spacing = 40f
                var y = 60f
                while (y < height - 60) {
                    var x = 60f
                    while (x < width - 60) {
                        canvas.drawCircle(x, y, 3f, dotPaint)
                        x += spacing
                    }
                    y += spacing
                }
            }
        }
        
        return bitmap
    }
    
    // Aggiunge una pagina al PDF usando PDFBox
    fun addPageToPDF(
        originalPdf: File,
        outputFile: File,
        insertAfterPage: Int,
        style: String
    ): Boolean {
        return try {
            // 1. Carica il PDF originale
            val document = PDDocument.load(originalPdf)
            
            // 2. Crea la bitmap con lo stile
            val pageWidth = 595
            val pageHeight = 842
            val bitmap = createStyledBitmap(pageWidth, pageHeight, style)
            
            // 3. Salva la bitmap in un file temporaneo
            val tempImage = File.createTempFile("styled_page", ".png")
            FileOutputStream(tempImage).use { out ->
                bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
            }
            
            // 4. Crea la nuova pagina
            val newPage = PDPage(PDRectangle(pageWidth.toFloat(), pageHeight.toFloat()))
            
            // 5. Inserisci la pagina dopo quella specificata
            val insertIndex = (insertAfterPage - 1).coerceIn(0, document.numberOfPages)
            document.pages.add(insertIndex, newPage)
            
            // 6. Aggiungi l'immagine alla nuova pagina
            val image = PDImageXObject.createFromFile(tempImage.absolutePath, document)
            PDPageContentStream(document, newPage).use { contentStream ->
                contentStream.drawImage(image, 0f, 0f, pageWidth.toFloat(), pageHeight.toFloat())
            }
            
            // 7. Salva il PDF modificato
            document.save(outputFile)
            document.close()
            
            // 8. Pulisci
            tempImage.delete()
            bitmap.recycle()
            
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
