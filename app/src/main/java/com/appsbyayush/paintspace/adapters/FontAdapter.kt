package com.appsbyayush.paintspace.adapters

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.appsbyayush.paintspace.R
import com.appsbyayush.paintspace.databinding.ItemFontBinding
import com.appsbyayush.paintspace.models.FontItem
import com.bumptech.glide.Glide
import com.google.android.material.floatingactionbutton.FloatingActionButton

class FontAdapter(
    private val clickEvent: FontItemClickEvent
): RecyclerView.Adapter<FontAdapter.FontViewHolder>() {

    var fontsList = listOf<FontItem>()
    var currentSelectedFont: FontItem? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FontViewHolder {
        val binding = ItemFontBinding.inflate(LayoutInflater.from(parent.context),
            parent, false)
        return FontViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FontViewHolder, position: Int) {
        val currentItem = fontsList[position]
        holder.bind(currentItem)
    }

    override fun getItemCount() = fontsList.size

    inner class FontViewHolder(private val binding: ItemFontBinding): RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                if(bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    clickEvent.onItemClick(fontsList[bindingAdapterPosition])
                }
            }
        }

        fun bind(fontItem: FontItem) {
            binding.apply {
                Glide.with(root)
                    .load(fontItem.imgUrl)
                    .into(imgFont)

                if(currentSelectedFont != null && currentSelectedFont!!.id == fontItem.id) {
                    root.setCardBackgroundColor(ContextCompat.getColor(root.context, R.color.teal_700))
                    imgFont.imageTintList = ColorStateList.valueOf(Color.WHITE)
                } else {
                    root.setCardBackgroundColor(Color.WHITE)
                    imgFont.imageTintList = ColorStateList.valueOf(Color.BLACK)
                }
            }
        }
    }

    interface FontItemClickEvent {
        fun onItemClick(fontItem: FontItem)
    }
}