package com.appsbyayush.paintspace.ui.bottomsheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.appsbyayush.paintspace.databinding.BottomSheetSettingsBinding
import com.appsbyayush.paintspace.ui.bottomsheets.base.BaseBottomSheetDialogFragment

class SettingsBottomSheetFragment(
    private val drawingsSyncing: Boolean = false,
    private val clickEvent: SettingsBottomSheetClickEvent
): BaseBottomSheetDialogFragment() {
    private var _binding: BottomSheetSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = BottomSheetSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnSyncDrawingsText = if(drawingsSyncing) "Syncing drawings..." else "Sync Drawings"

        binding.btnSyncDrawings.apply {
            text = btnSyncDrawingsText
            isEnabled = !drawingsSyncing
        }

        setupButtons()
    }

    private fun setupButtons() {
        binding.btnSyncDrawings.setOnClickListener {
            clickEvent.onBtnSyncDrawingsClick()
            dismissAfterDelay()
        }

        binding.btnMoreSettings.setOnClickListener {
            clickEvent.onBtnMoreSettingsClick()
            dismissAfterDelay()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    interface SettingsBottomSheetClickEvent {
        fun onBtnSyncDrawingsClick()
        fun onBtnMoreSettingsClick()
    }
}