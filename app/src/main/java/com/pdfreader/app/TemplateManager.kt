package com.pdfreader.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.IOException

object TemplateManager {
    
    data class Template(
        val fileName: String,
        val displayName: String,
        val bitmap: Bitmap
    )
    
    fun loadAllTemplates(context: Context): List<Template> {
        val templates = mutableListOf<Template>()
        try {
            val files = context.assets.list("templates") ?: emptyArray()
            for (fileName in files) {
                if (fileName.endsWith(".png", ignoreCase = true) || 
                    fileName.endsWith(".jpg", ignoreCase = true) ||
                    fileName.endsWith(".jpeg", ignoreCase = true)) {
                    try {
                        val inputStream = context.assets.open("templates/$fileName")
                        val bitmap = BitmapFactory.decodeStream(inputStream)
                        inputStream.close()
                        
                        if (bitmap != null) {
                            // Crea un nome leggibile
                            val displayName = fileName
                                .replace(".png", "", ignoreCase = true)
                                .replace(".jpg", "", ignoreCase = true)
                                .replace(".jpeg", "", ignoreCase = true)
                                .replace("_", " ")
                                .replace("-", " ")
                                .split(" ")
                                .filter { it.isNotEmpty() }
                                .joinToString(" ") { 
                                    it.replaceFirstChar { c -> c.uppercase() } 
                                }
                            
                            templates.add(Template(fileName, displayName, bitmap))
                        }
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return templates.sortedBy { it.displayName }
    }
}
