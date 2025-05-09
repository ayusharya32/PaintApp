package com.appsbyayush.paintspace.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.isVisible
import androidx.core.view.marginStart
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.appsbyayush.paintspace.databinding.ItemDrawingBinding
import com.appsbyayush.paintspace.databinding.ItemTrashDrawingBinding
import com.appsbyayush.paintspace.models.Drawing
import com.appsbyayush.paintspace.utils.CommonMethods
import com.appsbyayush.paintspace.utils.Constants
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy

class TrashDrawingAdapter(
    private val clickEvent: TrashDrawingItemClickEvent
): ListAdapter<Drawing, TrashDrawingAdapter.TrashDrawingViewHolder>(DrawingAdapter.DrawingComparator()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrashDrawingViewHolder {
        val binding = ItemTrashDrawingBinding.inflate(LayoutInflater.from(parent.context),
            parent, false)
        return TrashDrawingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TrashDrawingViewHolder, position: Int) {
        val currentDrawing = getItem(position)
        holder.bind(currentDrawing)
    }

    inner class TrashDrawingViewHolder(private val binding: ItemTrashDrawingBinding)
        : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                if(bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    clickEvent.onItemClick(getItem(bindingAdapterPosition))
                }
            }
        }

        fun bind(drawing: Drawing) {
            binding.apply {
                if(drawing.localDrawingImgUri != null) {
                    Glide.with(binding.root)
                        .load(drawing.localDrawingImgUri)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .skipMemoryCache(true)
                        .into(imgDrawing)

                } else {
                    Glide.with(binding.root)
                        .load(drawing.drawingImgUrl)
                        .into(imgDrawing)
                }

                txtDrawingTitle.text = drawing.name
                txtDrawingModified.text = CommonMethods.getFormattedDateTime(
                    date = drawing.modifiedAt,
                    format = Constants.DATE_FORMAT_2
                )
            }
        }
    }

    interface TrashDrawingItemClickEvent {
        fun onItemClick(drawing: Drawing)
    }
}