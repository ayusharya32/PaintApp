package com.appsbyayush.paintspace.ui.dialogs

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import com.appsbyayush.paintspace.R
import com.appsbyayush.paintspace.databinding.DialogSaveDrawingBinding
import com.appsbyayush.paintspace.ui.dialogs.base.BaseDialogFragment

class SaveDrawingDialog(
    private val newDrawing: Boolean,
    private val clickEvent: SaveDrawingClickEvent
): BaseDialogFragment(R.layout.dialog_save_drawing) {
    private var _binding: DialogSaveDrawingBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = DialogSaveDrawingBinding.bind(view)

        setupButtons()
        setupFragmentViews()
    }

    private fun setupButtons() {
        binding.apply {
            btnSave.setOnClickListener {
                clickEvent.onSaveBtnClicked()
                dismiss()
            }

            btnSaveAsDraft.setOnClickListener {
                clickEvent.onSaveAsDraftBtnClicked()
                dismiss()
            }

            btnDiscardChanges.setOnClickListener {
                clickEvent.onDiscardChangesBtnClicked()
                dismiss()
            }
        }
    }

    private fun setupFragmentViews() {
        binding.apply {
            btnSaveAsDraft.isVisible = newDrawing
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    interface SaveDrawingClickEvent {
        fun onSaveBtnClicked()
        fun onSaveAsDraftBtnClicked()
        fun onDiscardChangesBtnClicked()
    }
}