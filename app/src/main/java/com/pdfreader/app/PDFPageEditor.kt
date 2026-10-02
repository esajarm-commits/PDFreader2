package com.pdfreader.app

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import java.io.File
import java.io.FileOutputStream

object PDFPageEditor {
    
    // Crea un'immagine bitmap da un template PNG
    fun createStyledBitmap(
        width: Int,
        height: Int,
        context: android.content.Context,
        templateFileName: String
    ): Bitmap {
        // Se c'è un template PNG, usalo come sfondo
        if (templateFileName.isNotEmpty()) {
            try {
                val inputStream = context.assets.open("templates/$templateFileName")
                val templateBitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                inputStream.close()
                
                if (templateBitmap != null) {
                    // Scala il template alla dimensione richiesta
                    val scaled = Bitmap.createScaledBitmap(templateBitmap, width, height, true)
                    return scaled
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        // Altrimenti crea una pagina bianca
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        return bitmap
    }
    
    // Salva una pagina come immagine PNG
    fun savePageAsImage(
        bitmap: Bitmap,
        outputFile: File
    ): Boolean {
        return try {
            FileOutputStream(outputFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
