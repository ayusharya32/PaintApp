package com.appsbyayush.paintspace.ui.bottomsheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.appsbyayush.paintspace.databinding.BottomSheetDrawingSettingsBinding
import com.appsbyayush.paintspace.ui.bottomsheets.base.BaseBottomSheetDialogFragment

class DrawingSettingsBottomSheetFragment(
    private val clickEvent: DrawingSettingsBottomSheetClickEvent
): BaseBottomSheetDialogFragment() {
    private var _binding: BottomSheetDrawingSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = BottomSheetDrawingSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupButtons()
    }

    private fun setupButtons() {
        binding.btnRenameDrawing.setOnClickListener {
            clickEvent.onBtnRenameClick()
            dismissAfterDelay()
        }

        binding.btnTrashDrawing.setOnClickListener {
            clickEvent.onBtnMoveToTrashClick()
            dismissAfterDelay()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    interface DrawingSettingsBottomSheetClickEvent {
        fun onBtnRenameClick()
        fun onBtnMoveToTrashClick()
    }
}