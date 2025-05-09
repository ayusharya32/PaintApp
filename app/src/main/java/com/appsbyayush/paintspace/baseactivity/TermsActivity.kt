package com.appsbyayush.paintspace.baseactivity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.appsbyayush.paintspace.databinding.ActivityTermsBinding

class TermsActivity: AppCompatActivity() {
    companion object {
        private const val TAG = "TermsActivityyy"
    }

    private lateinit var binding: ActivityTermsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTermsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.webView.loadUrl("file:///android_res/raw/privacy.html")
        setupTermsRadioGroup()

        binding.btnToolbarBack.setOnClickListener {
            finish()
        }
    }

    private fun setupTermsRadioGroup() {
        binding.apply {
            radioGroupPolicy.setOnCheckedChangeListener { _, checkedId ->
                if(checkedId == radioBtnPrivacyPolicy.id) {
                    webView.loadUrl("file:///android_res/raw/privacy.html")
                } else {
                    webView.loadUrl("file:///android_res/raw/terms_of_use.html")
                }
            }
        }
    }
}