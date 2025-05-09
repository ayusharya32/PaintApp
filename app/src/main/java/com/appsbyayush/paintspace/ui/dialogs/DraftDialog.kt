package com.appsbyayush.paintspace.ui.dialogs

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import com.appsbyayush.paintspace.R
import com.appsbyayush.paintspace.databinding.DialogDraftBinding
import com.appsbyayush.paintspace.databinding.DialogLoadingBinding
import com.appsbyayush.paintspace.ui.dialogs.base.BaseDialogFragment

class DraftDialog(
    private val newDrawing: Boolean,
    private val clickEvent: DraftDialogClickEvent
): BaseDialogFragment(R.layout.dialog_draft) {
    private var _binding: DialogDraftBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = DialogDraftBinding.bind(view)

        setupButtons()
        setupFragmentViews()
    }

    private fun setupButtons() {
        binding.apply {
            btnOpenDraft.setOnClickListener {
                clickEvent.onBtnOpenDraftClick()
                dismiss()
            }

            btnNewDrawing.setOnClickListener {
                clickEvent.onBtnNewDrawingClick()
                dismiss()
            }

            btnDiscardAndContinue.setOnClickListener {
                clickEvent.onBtnDiscardAndContinueClick()
                dismiss()
            }
        }
    }

    private fun setupFragmentViews() {
        binding.apply {
            btnNewDrawing.isVisible = newDrawing
            btnDiscardAndContinue.isVisible = !newDrawing

            val textStringId = if(newDrawing) R.string.unsaved_drawing_new else R.string.unsaved_drawing_old
            txtDescription.text = getText(textStringId)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    interface DraftDialogClickEvent {
        fun onBtnOpenDraftClick()
        fun onBtnNewDrawingClick()
        fun onBtnDiscardAndContinueClick()
    }
}