package com.appsbyayush.paintspace.ui.bottomsheets

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.appsbyayush.paintspace.baseactivity.TermsActivity
import com.appsbyayush.paintspace.databinding.BottomSheetSignUpBinding
import com.appsbyayush.paintspace.ui.bottomsheets.base.BaseBottomSheetDialogFragment

class SignupBottomSheetFragment(
    private val clickEvent: SignupBottomSheetClickEvent
): BaseBottomSheetDialogFragment() {
    private var _binding: BottomSheetSignUpBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = BottomSheetSignUpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupButtons()
    }

    private fun setupButtons() {
        binding.btnSignIn.setOnClickListener {
            clickEvent.onLoginButtonClicked()
            dismissAfterDelay()
        }

        binding.txtPrivacyPolicy.setOnClickListener {
            Intent(context, TermsActivity::class.java).also {
                startActivity(it)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    interface SignupBottomSheetClickEvent {
        fun onLoginButtonClicked()
    }
}