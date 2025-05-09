package com.appsbyayush.paintspace.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.appsbyayush.paintspace.databinding.ItemGraphicElementHorizontalBinding
import com.appsbyayush.paintspace.models.UserGraphicElement
import com.appsbyayush.paintspace.utils.enums.GraphicElementType
import com.bumptech.glide.Glide

class UserGraphicElementAdapter(
    private val clickEvent: UserGraphicElementItemClickEvent
): RecyclerView.Adapter<UserGraphicElementAdapter.UserGraphicElementViewHolder>() {
    companion object {
        private const val TAG = "UserGraphicElementyy"
    }

    var elementsList = listOf<UserGraphicElement>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserGraphicElementViewHolder {
        val binding = ItemGraphicElementHorizontalBinding.inflate(LayoutInflater.from(parent.context),
            parent, false)
        return UserGraphicElementViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserGraphicElementViewHolder, position: Int) {
        val item = elementsList[position]
        holder.bind(item)
    }

    override fun getItemCount() = elementsList.size

    inner class UserGraphicElementViewHolder(private val binding: ItemGraphicElementHorizontalBinding)
        : RecyclerView.ViewHolder(binding.root) {

            init {
                binding.root.setOnClickListener {
                    if(bindingAdapterPosition != RecyclerView.NO_POSITION) {
                        clickEvent.onItemClick(elementsList[bindingAdapterPosition])
                    }
                }
            }

            fun bind(item: UserGraphicElement) {
                Log.d(TAG, "bind: Called")

                Glide.with(binding.root)
                    .load(item.elementUrl)
                    .centerInside()
                    .into(binding.imgItem)
            }
        }

    interface UserGraphicElementItemClickEvent {
        fun onItemClick(userGraphicElement: UserGraphicElement)
    }
}