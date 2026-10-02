package com.pdfreader.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TemplateAdapter(
    private val templates: List<TemplateManager.Template>,
    private val onTemplateClick: (TemplateManager.Template) -> Unit
) : RecyclerView.Adapter<TemplateAdapter.TemplateViewHolder>() {
    
    class TemplateViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val image: ImageView = itemView.findViewById(R.id.imageTemplate)
        val name: TextView = itemView.findViewById(R.id.textTemplateName)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TemplateViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_template, parent, false)
        return TemplateViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: TemplateViewHolder, position: Int) {
        val template = templates[position]
        holder.image.setImageBitmap(template.bitmap)
        holder.name.text = template.displayName
        holder.itemView.setOnClickListener {
            onTemplateClick(template)
        }
    }
    
    override fun getItemCount(): Int = templates.size
}
