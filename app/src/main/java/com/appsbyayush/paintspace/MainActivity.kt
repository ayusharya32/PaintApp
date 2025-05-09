package com.appsbyayush.paintspace

import android.graphics.BlurMaskFilter
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.SeekBar
import android.widget.Toast
import com.appsbyayush.paintspace.databinding.ActivityMainBinding
import com.skydoves.colorpickerview.ColorEnvelope
import com.skydoves.colorpickerview.ColorPickerDialog
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener
import java.util.*

class MainActivity : AppCompatActivity() {

    companion object {
        const val FILE_NAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        const val PAINTINGS_DIR = "Paintings"
    }

    private lateinit var binding: ActivityMainBinding
    private var currentColor = Color.GRAY

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

//        binding.apply {
//            seekBar.progress = 20
//
//            fabColor.setOnClickListener {
//                getColorPickerDialog().show()
//            }
//
//            fabUndo.setOnClickListener {
////                drawingView.undo()
//            }
//
//            fabUndo.setOnLongClickListener {
////                drawingView.clearView()
//                true
//            }
//
//            fabSave.setOnClickListener {
//                val (filePath, isFileSaved) = drawingView.save()
//
//                if(isFileSaved) {
//                    Toast.makeText(this@MainActivity, "Painting saved successfully at $filePath", Toast.LENGTH_SHORT).show()
//                } else {
//                    Toast.makeText(this@MainActivity, "Error saving painting", Toast.LENGTH_SHORT).show()
//                }
//            }
//
//            fabBrush.setOnClickListener {
//                drawingView.apply {
//                    setBrushColor(currentColor)
//                    fabColor.setColorFilter(currentColor)
//                    setBlurEffect(null)
//                }
//            }
//
//            fabEraser.setOnClickListener {
//                drawingView.setBrushColor(Color.WHITE)
//            }
//
//            fabNormal.setOnClickListener {
//                drawingView.setBlurEffect(BlurMaskFilter.Blur.NORMAL)
//            }
//
//            fabOuter.setOnClickListener {
//                drawingView.setBlurEffect(BlurMaskFilter.Blur.OUTER)
//            }
//
//            fabBlur.setOnClickListener {
//                drawingView.setBlurEffect(BlurMaskFilter.Blur.SOLID)
//            }
//
//            seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
//                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) { /*NO OPERATION*/ }
//
//                override fun onStartTrackingTouch(seekBar: SeekBar?) {/*NO OPERATION*/}
//
//                override fun onStopTrackingTouch(seekBar: SeekBar?) {
//                    seekBar?.progress?.let {
//                        binding.drawingView.setBrushSize(it.toFloat())
//                    }
//                }
//
//            })
//        }
    }

    private fun getColorPickerDialog() = ColorPickerDialog.Builder(this).apply {
        setTitle("Choose a color")
        setPositiveButton("Ok", object : ColorEnvelopeListener {
            override fun onColorSelected(envelope: ColorEnvelope?, fromUser: Boolean) {

                val hexCode = envelope?.hexCode
                val color = Color.parseColor("#$hexCode")

                binding.apply {
                    currentColor = color
                    fabColor.setColorFilter(color)
                    drawingView.setBrushColor(color)
                }
            }
        })
        setNegativeButton("Cancel") { dialogInterface, _ ->
            dialogInterface.cancel()
        }
        attachAlphaSlideBar(true)
        attachBrightnessSlideBar(true)
        create()
    }
}