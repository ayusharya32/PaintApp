package com.appsbyayush.paintspace.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import com.appsbyayush.paintspace.databinding.ItemSimpleSpinnerBinding
import com.appsbyayush.paintspace.databinding.ItemSimpleSpinnerDropdownBinding
import com.appsbyayush.paintspace.models.GraphicElementTypeItem

class GraphicElementTypeAdapter(
    context: Context,
    graphicElementTypeList: List<GraphicElementTypeItem>
): ArrayAdapter<GraphicElementTypeItem>(context, 0, graphicElementTypeList) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val binding: ItemSimpleSpinnerBinding = if(convertView == null) {
            ItemSimpleSpinnerBinding.inflate(LayoutInflater.from(parent.context),
                parent, false)
        } else {
            ItemSimpleSpinnerBinding.bind(convertView)
        }

        val currentItem = getItem(position)

        currentItem?.let { item ->
            binding.apply {
                txtItemName.text = item.name
            }
        }

        return binding.root
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val binding: ItemSimpleSpinnerDropdownBinding = if(convertView == null) {
            ItemSimpleSpinnerDropdownBinding.inflate(LayoutInflater.from(parent.context),
                parent, false)
        } else {
            ItemSimpleSpinnerDropdownBinding.bind(convertView)
        }

        val currentItem = getItem(position)

        currentItem?.let { item ->
            binding.apply {
                txtItemName.text = item.name
            }
        }

        return binding.root
    }
}