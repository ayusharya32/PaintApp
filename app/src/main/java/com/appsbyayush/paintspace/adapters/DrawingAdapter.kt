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
import com.appsbyayush.paintspace.models.Drawing
import com.appsbyayush.paintspace.utils.CommonMethods
import com.appsbyayush.paintspace.utils.Constants
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.signature.ObjectKey

class DrawingAdapter(
    private val clickEvent: DrawingItemClickEvent
): ListAdapter<Drawing, DrawingAdapter.DrawingViewHolder>(DrawingComparator()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DrawingViewHolder {
        val binding = ItemDrawingBinding.inflate(LayoutInflater.from(parent.context),
            parent, false)
        return DrawingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DrawingViewHolder, position: Int) {
        val currentDrawing = getItem(position)
        holder.bind(currentDrawing)
    }

    inner class DrawingViewHolder(private val binding: ItemDrawingBinding): RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                if(bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    clickEvent.onItemClick(getItem(bindingAdapterPosition))
                }
            }

            binding.flBtnShare.setOnClickListener {
                if(bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    clickEvent.onShareBtnClick(getItem(bindingAdapterPosition))
                }
            }

            binding.flBtnCopyToDevice.setOnClickListener {
                if(bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    clickEvent.onCopyToDeviceBtnClick(getItem(bindingAdapterPosition))
                }
            }

            binding.flBtnMoreSettings.setOnClickListener {
                if(bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    clickEvent.onMoreSettingsBtnClick(getItem(bindingAdapterPosition))
                }
            }
        }

        fun bind(drawing: Drawing) {
            binding.apply {
                if(drawing.localDrawingImgUri != null) {
//                    Glide.with(binding.root)
//                        .load(drawing.localDrawingImgUri)
//                        .diskCacheStrategy(DiskCacheStrategy.NONE)
//                        .skipMemoryCache(true)
//                        .into(imgDrawing)

                    Glide.with(binding.root)
                        .load(drawing.localDrawingImgUri)
                        .signature(ObjectKey(drawing.modifiedAt))
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

                llButtons.isVisible = !drawing.isDeleted
            }
        }
    }

    class DrawingComparator: DiffUtil.ItemCallback<Drawing>() {
        override fun areItemsTheSame(oldItem: Drawing, newItem: Drawing): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Drawing, newItem: Drawing): Boolean {
            return oldItem == newItem
        }
    }

    interface DrawingItemClickEvent {
        fun onItemClick(drawing: Drawing)
        fun onShareBtnClick(drawing: Drawing)
        fun onCopyToDeviceBtnClick(drawing: Drawing)
        fun onMoreSettingsBtnClick(drawing: Drawing)
    }
}