package com.appsbyayush.paintspace.ui.dialogs

import android.os.Bundle
import android.view.View
import com.appsbyayush.paintspace.R
import com.appsbyayush.paintspace.databinding.DialogLoadingBinding
import com.appsbyayush.paintspace.databinding.DialogRenameDrawingBinding
import com.appsbyayush.paintspace.ui.dialogs.base.BaseDialogFragment

class RenameDrawingDialog(
    private val drawingName: String,
    private val clickEvent: RenameDrawingDialogClickEvent
): BaseDialogFragment(R.layout.dialog_rename_drawing) {
    private var _binding: DialogRenameDrawingBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = DialogRenameDrawingBinding.bind(view)

        binding.apply {
            editTextDrawingName.setText(drawingName)
        }

        setupButtons()
    }

    private fun setupButtons() {
        binding.apply {
            btnSubmit.setOnClickListener {
                clickEvent.onBtnSubmitClick(editTextDrawingName.text.toString())
                dismiss()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    interface RenameDrawingDialogClickEvent {
        fun onBtnSubmitClick(updatedName: String)
    }
}