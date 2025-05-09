package com.appsbyayush.paintspace.ui.dialogs

import android.os.Bundle
import android.view.View
import com.appsbyayush.paintspace.R
import com.appsbyayush.paintspace.databinding.DialogStoragePermissionBinding
import com.appsbyayush.paintspace.ui.dialogs.base.BaseDialogFragment

class PermissionDialog(
    private val showSettingsOption: Boolean,
    private val clickEvent: PermissionDialogClickEvent
): BaseDialogFragment(R.layout.dialog_storage_permission) {
    private var _binding: DialogStoragePermissionBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = DialogStoragePermissionBinding.bind(view)

        setupButtons()
        setupFragmentViews()
    }

    private fun setupButtons() {
        binding.apply {
            btnNotNow.setOnClickListener {
                clickEvent.onBtnNotNowClick()
                dismiss()
            }

            btnContinue.setOnClickListener {
                clickEvent.onBtnContinueClick()
                dismiss()
            }
        }
    }

    private fun setupFragmentViews() {
        binding.apply {
            btnContinue.text = if(showSettingsOption) "Settings" else "Continue"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    interface PermissionDialogClickEvent {
        fun onBtnNotNowClick()
        fun onBtnContinueClick()
    }
}