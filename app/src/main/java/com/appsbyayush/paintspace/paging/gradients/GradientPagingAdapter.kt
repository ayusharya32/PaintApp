package com.appsbyayush.paintspace.paging.gradients

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.appsbyayush.paintspace.databinding.ItemGradientBinding
import com.appsbyayush.paintspace.models.Gradient
import com.appsbyayush.paintspace.utils.Constants
import com.appsbyayush.paintspace.utils.enums.GradientType
import com.bumptech.glide.Glide

class GradientPagingAdapter(
    var gradientType: GradientType,
    private val clickEvent: GradientItemClickEvent
) : PagingDataAdapter<Gradient, GradientPagingAdapter.GradientViewHolder>(GradientItemComparator()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GradientViewHolder {
        val binding = ItemGradientBinding.inflate(LayoutInflater.from(parent.context),
            parent, false)
        return GradientViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GradientViewHolder, position: Int) {
        getItem(position)?.let { currentGradientItem ->
            holder.bind(currentGradientItem)
        }
    }

    inner class GradientViewHolder(private val binding: ItemGradientBinding)
        : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    getItem(bindingAdapterPosition)?.let {
                        clickEvent.onItemClick(it)
                    }
                }
            }
        }

        fun bind(gradientItem: Gradient) {
            val gradientDrawable = getGradientDrawable(gradientItem)

            Glide.with(binding.root)
                .load(gradientDrawable)
                .into(binding.imgItem)
        }

        private fun getGradientDrawable(gradient: Gradient): GradientDrawable {
            val gradientColors = gradient.colors.map { colorHexString ->
                Color.parseColor(colorHexString)
            }.toIntArray()

            val drawable = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                gradientColors
            )

            when(gradientType) {
                GradientType.LINEAR -> {
                    drawable.gradientType = GradientDrawable.LINEAR_GRADIENT
                }

                GradientType.RADIAL -> {
                    drawable.gradientType = GradientDrawable.RADIAL_GRADIENT
                    drawable.gradientRadius = Constants.DEFAULT_GRADIENT_RADIUS
                }

                GradientType.SWEEP -> {
                    drawable.gradientType = GradientDrawable.SWEEP_GRADIENT
                }
            }

            return drawable
        }
    }

    class GradientItemComparator : DiffUtil.ItemCallback<Gradient>() {
        override fun areItemsTheSame(oldItem: Gradient, newItem: Gradient): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Gradient, newItem: Gradient): Boolean {
            return oldItem == newItem
        }
    }

    interface GradientItemClickEvent {
        fun onItemClick(gradient: Gradient)
    }
}