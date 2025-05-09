package com.appsbyayush.paintspace.baseactivity

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import com.appsbyayush.paintspace.R
import com.appsbyayush.paintspace.databinding.ActivityDrawingBinding
import com.appsbyayush.paintspace.models.Drawing
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DrawingActivity: AppCompatActivity() {
    companion object {
        const val KEY_CURRENT_DRAWING = "KEY_CURRENT_DRAWING"
        const val KEY_LOAD_DRAFT = "KEY_LOAD_DRAFT"

        private const val TAG = "DrawingActivityy"
    }

    private lateinit var binding: ActivityDrawingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDrawingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val currentDrawing = intent.getParcelableExtra<Drawing>(KEY_CURRENT_DRAWING)
        val loadDraft = intent.getBooleanExtra(KEY_LOAD_DRAFT, false)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
                as NavHostFragment

        val bundleArgs = Bundle().apply {
            putParcelable("currentDrawing", currentDrawing)
            putBoolean("loadDraft", loadDraft)
        }
        navHostFragment.navController.setGraph(R.navigation.drawing_navigation, bundleArgs)
    }
}