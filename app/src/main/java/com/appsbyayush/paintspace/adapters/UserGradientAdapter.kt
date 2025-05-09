package com.appsbyayush.paintspace.adapters

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.appsbyayush.paintspace.databinding.ItemGradientHorizontalBinding
import com.appsbyayush.paintspace.models.UserGradient
import com.appsbyayush.paintspace.utils.Constants
import com.appsbyayush.paintspace.utils.enums.GradientType
import com.bumptech.glide.Glide

class UserGradientAdapter(
    private val clickEvent: UserGradientItemClickEvent
): RecyclerView.Adapter<UserGradientAdapter.UserGradientViewHolder>() {

    var userGradientList = listOf<UserGradient>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserGradientViewHolder {
        val binding = ItemGradientHorizontalBinding.inflate(LayoutInflater.from(parent.context),
            parent, false)
        return UserGradientViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserGradientViewHolder, position: Int) {
        val currentItem = userGradientList[position]
        holder.bind(currentItem)
    }

    override fun getItemCount() = userGradientList.size

    inner class UserGradientViewHolder(private val binding: ItemGradientHorizontalBinding)
        : RecyclerView.ViewHolder(binding.root) {

            init {
                binding.root.setOnClickListener {
                    if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                        clickEvent.onItemClick(userGradientList[bindingAdapterPosition])
                    }
                }
            }

        fun bind(userGradient: UserGradient) {
            val gradientDrawable = getGradientDrawable(userGradient)

            Glide.with(binding.root)
                .load(gradientDrawable)
                .into(binding.imgItem)
        }

        private fun getGradientDrawable(userGradient: UserGradient): GradientDrawable {
            val gradientColors = userGradient.colors.map { colorHexString ->
                Color.parseColor(colorHexString)
            }.toIntArray()

            val drawable = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                gradientColors
            )

            when(userGradient.gradientType) {
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

    interface UserGradientItemClickEvent {
        fun onItemClick(userGradient: UserGradient)
    }
}