package com.pdfreader.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class PDFAdapter(
    private val onItemClick: (PDFFile) -> Unit
) : RecyclerView.Adapter<PDFAdapter.PDFViewHolder>() {
    
    private var items: List<PDFFile> = emptyList()
    
    fun submitList(list: List<PDFFile>) {
        items = list
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PDFViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pdf, parent, false)
        return PDFViewHolder(view, onItemClick)
    }
    
    override fun onBindViewHolder(holder: PDFViewHolder, position: Int) {
        holder.bind(items[position])
    }
    
    override fun getItemCount(): Int = items.size
    
    class PDFViewHolder(
        itemView: View,
        private val onItemClick: (PDFFile) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        
        private val textName: TextView = itemView.findViewById(R.id.textName)
        private val textDate: TextView = itemView.findViewById(R.id.textDate)
        
        fun bind(pdf: PDFFile) {
            textName.text = pdf.name
            
            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            textDate.text = dateFormat.format(Date(pdf.lastOpened))
            
            itemView.setOnClickListener {
                onItemClick(pdf)
            }
        }
    }
}
