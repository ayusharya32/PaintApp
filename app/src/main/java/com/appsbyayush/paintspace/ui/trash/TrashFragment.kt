package com.appsbyayush.paintspace.ui.trash

import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.appsbyayush.paintspace.R
import com.appsbyayush.paintspace.adapters.TrashDrawingAdapter
import com.appsbyayush.paintspace.databinding.FragmentTrashBinding
import com.appsbyayush.paintspace.models.Drawing
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class TrashFragment: Fragment(R.layout.fragment_trash) {
    companion object {
        private const val TAG = "TrashFragmentyy"
    }
    
    private var _binding: FragmentTrashBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TrashViewModel by viewModels()
    
    private lateinit var trashDrawingAdapter: TrashDrawingAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentTrashBinding.bind(view)
        
        setupDrawingRecyclerView()
        setupButtons()

        setupTrashDrawingsCollector()
        setupUIEventCollector()

        viewModel.onFragmentStarted()
    }

    private fun setupDrawingRecyclerView() {
        trashDrawingAdapter = TrashDrawingAdapter(object: TrashDrawingAdapter.TrashDrawingItemClickEvent {
            override fun onItemClick(drawing: Drawing) {
                showRestoreDialog(drawing)
            }
        })

        binding.rvTrashDrawings.apply {
            layoutManager = GridLayoutManager(context, 2)
            adapter = trashDrawingAdapter
        }
    }

    private fun setupButtons() {
        binding.btnToolbarBack.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun setupTrashDrawingsCollector() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.trashDrawingsFlow.collect { trashDrawings ->

                    binding.llEmptyTrash.isVisible = trashDrawings.isEmpty()
                    binding.txtEmptyTrash.text = "Trash Empty"

                    binding.rvTrashDrawings.isVisible = trashDrawings.isNotEmpty()

                    if(trashDrawings.isNotEmpty()) {
                        trashDrawingAdapter.submitList(trashDrawings)
//                        drawingAdapter.notifyDataSetChanged()
                    }
                }
            }
        }
    }

    private fun showRestoreDialog(drawing: Drawing) {
        val dialog = MaterialAlertDialogBuilder(requireContext()).apply {
            setMessage("To view this drawing, you need to restore it")
            setPositiveButton("Restore Drawing") { dialog, _ ->
                viewModel.restoreDrawing(drawing)
                dialog.dismiss()
            }
            setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
        }.create()

        dialog.show()

        dialog.getButton(DialogInterface.BUTTON_POSITIVE).isAllCaps = false
        dialog.getButton(DialogInterface.BUTTON_NEGATIVE).isAllCaps = false
    }

    private fun setupUIEventCollector() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    binding.progressLoading.isVisible = event is TrashViewModel.Event.Loading

                    when(event) {
                        is TrashViewModel.Event.DrawingRestoredSuccess -> {
                            Toast.makeText(context, "Drawing Restored",
                                Toast.LENGTH_SHORT).show()
                        }

                        is TrashViewModel.Event.ErrorOccurred -> {
                            Toast.makeText(context, event.exception.message, Toast.LENGTH_SHORT).show()
                            viewModel.onEventOccurred()
                        }

                        else -> {}
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}