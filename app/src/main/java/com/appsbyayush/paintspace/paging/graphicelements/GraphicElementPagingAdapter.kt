package com.appsbyayush.paintspace.paging.graphicelements

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.appsbyayush.paintspace.databinding.ItemGraphicElementBinding
import com.appsbyayush.paintspace.models.GraphicElement
import com.appsbyayush.paintspace.utils.enums.GraphicElementType
import com.bumptech.glide.Glide

class GraphicElementPagingAdapter(
    private val clickEvent: GraphicElementItemClickEvent
): PagingDataAdapter<GraphicElement, GraphicElementPagingAdapter.GraphicElementViewHolder>(
    GraphicElementComparator()
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GraphicElementViewHolder {
        val binding = ItemGraphicElementBinding.inflate(
            LayoutInflater.from(parent.context),
            parent, false)
        return GraphicElementViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GraphicElementViewHolder, position: Int) {
        val item = getItem(position)
        item?.let { holder.bind(it) }
    }

    inner class GraphicElementViewHolder(private val binding: ItemGraphicElementBinding)
        : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                if(bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    getItem(bindingAdapterPosition)?.let {
                        clickEvent.onItemClick(it)
                    }
                }
            }
        }

        fun bind(item: GraphicElement) {
            Glide.with(binding.root)
                .load(item.elementUrl)
                .centerInside()
                .into(binding.imgItem)
        }
    }

    class GraphicElementComparator(): DiffUtil.ItemCallback<GraphicElement>() {
        override fun areItemsTheSame(oldItem: GraphicElement, newItem: GraphicElement): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: GraphicElement, newItem: GraphicElement): Boolean {
            return oldItem == newItem
        }
    }

    interface GraphicElementItemClickEvent {
        fun onItemClick(element: GraphicElement)
    }
}