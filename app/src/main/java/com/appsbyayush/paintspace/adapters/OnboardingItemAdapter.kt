package com.appsbyayush.paintspace.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.RecyclerView
import com.appsbyayush.paintspace.databinding.ItemOnboardingBinding
import com.appsbyayush.paintspace.models.OnboardingItem
import com.appsbyayush.paintspace.utils.Constants
import com.bumptech.glide.Glide

class OnboardingItemAdapter: RecyclerView.Adapter<OnboardingItemAdapter.OnboardingItemViewHolder>() {

    val onboardingItems = Constants.ONBOARDING_ITEMS_LIST

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OnboardingItemViewHolder {
        val binding = ItemOnboardingBinding.inflate(LayoutInflater.from(parent.context),
            parent, false)
        return OnboardingItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OnboardingItemViewHolder, position: Int) {
        val currentItem = onboardingItems[position]
        holder.bind(currentItem)
    }

    override fun getItemCount() = onboardingItems.size

    inner class OnboardingItemViewHolder(private val binding: ItemOnboardingBinding)
        : RecyclerView.ViewHolder(binding.root) {

        fun bind(currentItem: OnboardingItem) {
            binding.apply {
                Glide.with(root.context)
                    .load(AppCompatResources.getDrawable(root.context, currentItem.imageResourceId))
                    .fitCenter()
                    .into(imgItem)

                txtDescription.text = currentItem.description
            }
        }
    }
}