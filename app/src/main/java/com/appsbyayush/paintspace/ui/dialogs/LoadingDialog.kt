package com.appsbyayush.paintspace.ui.dialogs

import android.os.Bundle
import android.view.View
import com.appsbyayush.paintspace.R
import com.appsbyayush.paintspace.databinding.DialogLoadingBinding
import com.appsbyayush.paintspace.ui.dialogs.base.BaseDialogFragment

class LoadingDialog(
    private val loadingText: String
): BaseDialogFragment(R.layout.dialog_loading) {
    private var _binding: DialogLoadingBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = DialogLoadingBinding.bind(view)

        binding.txtLoading.text = loadingText
        isCancelable = false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}