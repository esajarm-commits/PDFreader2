package com.pdfreader.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NotePageDao {
    @Insert
    suspend fun insert(page: NotePage): Long

    @Update
    suspend fun update(page: NotePage)

    @Delete
    suspend fun delete(page: NotePage)

    @Query("SELECT * FROM note_pages WHERE pdfPath = :pdfPath ORDER BY insertAfterPage, createdAt")
    fun getPagesForPdf(pdfPath: String): Flow<List<NotePage>>

    @Query("SELECT * FROM note_pages WHERE id = :id")
    suspend fun getPageById(id: Long): NotePage?

    @Query("SELECT * FROM note_pages ORDER BY updatedAt DESC")
    fun getAllPages(): Flow<List<NotePage>>
}
