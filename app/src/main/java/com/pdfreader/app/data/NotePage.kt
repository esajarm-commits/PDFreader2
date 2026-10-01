package com.pdfreader.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "note_pages")
data class NotePage(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val pdfPath: String,           // Percorso del PDF a cui appartiene
    val pdfName: String,            // Nome del PDF
    val pageName: String,           // Nome della pagina (es. "Appunti 1")
    val style: String,              // "blank", "lined", "grid", "dotted"
    val insertAfterPage: Int,       // Dopo quale pagina del PDF inserirla (1-based)
    val imagePath: String,          // Percorso dell'immagine PNG salvata
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
