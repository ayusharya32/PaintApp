package com.appsbyayush.paintspace.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.RecyclerView
import com.appsbyayush.paintspace.R
import com.appsbyayush.paintspace.databinding.ItemGradientColorBinding
import com.appsbyayush.paintspace.models.GradientColor

class GradientColorAdapter(
    private val clickEvent: GradientColorItemClickEvent
): RecyclerView.Adapter<GradientColorAdapter.GradientColorViewHolder>() {

    var gradientColorList = listOf<GradientColor>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GradientColorViewHolder {
        val binding = ItemGradientColorBinding.inflate(LayoutInflater.from(parent.context),
            parent, false)
        return GradientColorViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GradientColorViewHolder, position: Int) {
        val currentItem = gradientColorList[position]
        holder.bind(currentItem)
    }

    override fun getItemCount() = gradientColorList.size

    inner class GradientColorViewHolder(private val binding: ItemGradientColorBinding)
        : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                if(bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    clickEvent.onItemClick(gradientColorList[bindingAdapterPosition])
                }
            }

            binding.root.setOnLongClickListener {
                if(bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    clickEvent.onItemLongClick(gradientColorList[bindingAdapterPosition])
                }

                true
            }

            binding.imgEnabled.setOnClickListener {
                if(bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    clickEvent.onCheckboxClick(gradientColorList[bindingAdapterPosition])
                }
            }
        }

        fun bind(colorItem: GradientColor) {
            binding.apply {
                val color = Color.parseColor(colorItem.colorHexCodeString)
                imgColor.setBackgroundColor(color)
                txtColorHexCode.text = colorItem.colorHexCodeString
                txtColorPosition.text = colorItem.colorPosition.toString()

                val imgEnabledDrawable = if(colorItem.enabled) {
                    AppCompatResources.getDrawable(root.context, R.drawable.ic_checked_checkbox)
                } else {
                    AppCompatResources.getDrawable(root.context, R.drawable.ic_unchecked_checkbox)
                }
                imgEnabled.setImageDrawable(imgEnabledDrawable)
            }
        }
    }

    interface GradientColorItemClickEvent {
        fun onItemClick(gradientColor: GradientColor)
        fun onItemLongClick(gradientColor: GradientColor)
        fun onCheckboxClick(gradientColor: GradientColor)
    }
}