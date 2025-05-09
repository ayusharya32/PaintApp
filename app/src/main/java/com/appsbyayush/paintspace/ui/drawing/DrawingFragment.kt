package com.appsbyayush.paintspace.ui.drawing

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.appsbyayush.paintspace.R
import com.appsbyayush.paintspace.adapters.FontAdapter
import com.appsbyayush.paintspace.customviews.ColorEditText
import com.appsbyayush.paintspace.customviews.VerticalSeekbar
import com.appsbyayush.paintspace.databinding.FragmentDrawingBinding
import com.appsbyayush.paintspace.models.FontItem
import com.appsbyayush.paintspace.models.UserGradient
import com.appsbyayush.paintspace.models.UserGraphicElement
import com.appsbyayush.paintspace.ui.bottomsheets.gradient.GradientBottomSheetFragment
import com.appsbyayush.paintspace.ui.bottomsheets.pickimage.PickImageBottomSheetFragment
import com.appsbyayush.paintspace.ui.dialogs.LoadingDialog
import com.appsbyayush.paintspace.ui.dialogs.SaveDrawingDialog
import com.appsbyayush.paintspace.utils.CommonMethods
import com.appsbyayush.paintspace.utils.Constants.IMAGE_TYPE_JPEG
import com.appsbyayush.paintspace.utils.Constants.IMAGE_TYPE_JPG
import com.appsbyayush.paintspace.utils.Constants.IMAGE_TYPE_PNG
import com.appsbyayush.paintspace.utils.Constants.IMAGE_TYPE_WEBP
import com.appsbyayush.paintspace.utils.enums.BrushType
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.skydoves.colorpickerview.ColorEnvelope
import com.skydoves.colorpickerview.ColorPickerDialog
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class DrawingFragment: Fragment(R.layout.fragment_drawing) {
    companion object {
        private const val TAG = "DrawingFragmentyy"
        private const val DEFAULT_BRUSH_COLOR = "#1C82AD"
        private const val DEFAULT_BRUSH_WIDTH = 20F

        private const val DEFAULT_TRANSPARENCY = 0

        private const val TEXT_COLOR = "TEXT_COLOR"
        private const val BRUSH_COLOR = "BRUSH_COLOR"
    }

    private var _binding: FragmentDrawingBinding? = null
    private val binding get() = _binding!!

    private lateinit var fontsAdapter: FontAdapter
    private var downloadingFontDialog: LoadingDialog? = null
    private var loadingDrawingDialog: LoadingDialog? = null

    private val viewModel: DrawingViewModel by viewModels()
    private val args: DrawingFragmentArgs by navArgs()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentDrawingBinding.bind(view)

        setupFragmentViews()
        setupFontsAdapter()

        setupButtons()
        setupBrushModeButtons()
        setupSelectionModeButtons()
        setupTextModeButtons()

        setupDrawingView()
        setupSeekbarBrush()
        setupSeekbarTransparency()
        setupPointerCircle()

        setupFontsFlowCollector()
        setupCurrentSelectedItemFlowCollector()
        setupDrawingLoadingFlowCollector()
        setupUIEventCollector()

        viewModel.onFragmentStarted(args.currentDrawing, args.loadDraft)
    }

    private fun setupButtons() {
        binding.apply {
            fabText.setOnClickListener {
                viewModel.apply {
                    updateInternetStatus()
                    currentUiMode = DrawingViewModel.UIMode.MODE_TEXT
                }

                binding.fabColor.setColorFilter(cetText.currentTextColor)
                setupFragmentViews()
            }

            fabImage.setOnClickListener {
                openPickImageBottomSheet()
            }

            fabBrush.setOnClickListener {
                viewModel.currentUiMode = DrawingViewModel.UIMode.MODE_BRUSH
                setupFragmentViews()
            }
            
            fabSelection.setOnClickListener { 
                viewModel.currentUiMode = DrawingViewModel.UIMode.MODE_SELECTION
                setupFragmentViews()
            }

            btnSaveDrawing.setOnClickListener {
                viewModel.onSaveDrawingBtnClicked(binding.drawingView.isEmpty())
            }

            activity?.onBackPressedDispatcher
                ?.addCallback(viewLifecycleOwner, object: OnBackPressedCallback(true) {
                    override fun handleOnBackPressed() {
                        if(drawingView.isEmpty()
                            && viewModel.currentUiMode == DrawingViewModel.UIMode.MODE_IDLE) {
                            isEnabled = false
                            activity?.onBackPressed()

                        } else {
                            onBackButtonClicked()
                        }
                    }
                })
        }

    }

    private fun onBackButtonClicked() {
        when(viewModel.currentUiMode) {
            DrawingViewModel.UIMode.MODE_BRUSH, DrawingViewModel.UIMode.MODE_SELECTION -> {
                setUiModeIdle()
                setupFragmentViews()
            }

            DrawingViewModel.UIMode.MODE_TEXT -> {
                binding.apply {
                    if(cetText.getCurrentTextElement() == null) {
                        setUiModeIdle()
                        setupFragmentViews()

                        return
                    }

                    cetText.getCurrentTextElement()?.let {
                        viewModel.onBackPressedWithCurrentTextElementPresent(it)
                    }
                }
            }

            DrawingViewModel.UIMode.MODE_IDLE -> {
                showSaveDrawingDialog()
            }
        }
    }

    private fun setupFontsAdapter() {
        fontsAdapter = FontAdapter(object: FontAdapter.FontItemClickEvent {
            override fun onItemClick(fontItem: FontItem) {
                viewModel.onFontItemClicked(fontItem)
            }
        })

        binding.rvFonts.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL,
                false)
            adapter = fontsAdapter
            setHasFixedSize(true)
        }
    }

    private fun openPickImageBottomSheet() {
        val pickImageSheet = PickImageBottomSheetFragment(
            object: PickImageBottomSheetFragment.PickImageBottomSheetClickEvent {
                override fun onBtnPickImageFromGalleryClick() {
                    val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                        type = "image/*"
                        putExtra(Intent.EXTRA_MIME_TYPES, 
                            arrayOf(IMAGE_TYPE_JPEG, IMAGE_TYPE_JPG, IMAGE_TYPE_PNG, IMAGE_TYPE_WEBP))
                    }
                    startActivityForResultLauncher.launch(intent)
                }

                override fun onBtnCreateGradientClick() {
                    openGradientBottomSheet()
                }

                override fun onGraphicElementItemClick(userGraphicElement: UserGraphicElement) {
                    viewModel.onGraphicElementClicked(userGraphicElement)
                }
            })

        pickImageSheet.show(childFragmentManager, "Pick Image")
    }

    private fun openGradientBottomSheet() {
        val gradientBottomSheet = GradientBottomSheetFragment(
            object: GradientBottomSheetFragment.GradientBottomSheetClickEvent {
                override fun onCreateBtnClick(userGradient: UserGradient) {
                    viewModel.onGradientCreateBtnClicked(userGradient)
                }
            }
        )

        gradientBottomSheet.show(childFragmentManager, "Create Gradient")
    }

    private fun setupBrushModeButtons() {
        binding.apply {
            fabBrushSimple.setOnClickListener {
                drawingView.setBrushType(BrushType.SIMPLE)
                makeCurrentBrushTypeButtonSelected()
            }

            fabBrushNormal.setOnClickListener {
                drawingView.setBrushType(BrushType.NORMAL)
                makeCurrentBrushTypeButtonSelected()
            }

            fabBrushBlur.setOnClickListener {
                drawingView.setBrushType(BrushType.SOLID)
                makeCurrentBrushTypeButtonSelected()
            }

            fabBrushOutline.setOnClickListener {
                drawingView.setBrushType(BrushType.OUTLINE)
                makeCurrentBrushTypeButtonSelected()
            }

            fabEraser.setOnClickListener {
                drawingView.setBrushType(BrushType.ERASER)
                makeCurrentBrushTypeButtonSelected()
            }

            fabUndo.setOnClickListener {
                drawingView.undoLastOperation()
            }

            fabRedo.setOnClickListener {
                drawingView.redoLastOperation()
            }

            fabColor.setOnClickListener {
                getColorPickerDialog(BRUSH_COLOR).show()
            }

            fabBrushDone.setOnClickListener {
                setUiModeIdle()
                setupFragmentViews()
            }

            fabToggleBrushOptions.setOnClickListener {
                viewModel.showBrushOptions = !viewModel.showBrushOptions
                setupFragmentViews()
            }
        }
    }

    private fun setupSelectionModeButtons() {
        binding.apply {
            fabBringForward.setOnClickListener {
                drawingView.bringSelectedItemForward()
            }

            fabMoveBackward.setOnClickListener {
                drawingView.moveSelectedItemBackward()
            }

            fabBringToFront.setOnClickListener {
                drawingView.bringSelectedItemForward(bringToFront = true)
            }

            fabMoveToBack.setOnClickListener {
                drawingView.moveSelectedItemBackward(sendToBack = true)
            }

            fabToggleSelectOptions.setOnClickListener {
                viewModel.showSelectOptions = !viewModel.showSelectOptions
                setupFragmentViews()
            }

            fabEditText.setOnClickListener {
                if(drawingView.isSelectedItemTextElement()) {
                    viewModel.onEditTextBtnClicked(drawingView.currentSelectedItemFlow.value
                        ?.drawingText?.textElementId)
                }
            }

            fabLockItem.setOnClickListener {
                drawingView.toggleSelectedItemLock()
                setupFragmentViews()
            }

            fabDeleteItem.setOnClickListener {
                drawingView.deleteSelectedItem()
                Snackbar.make(requireView(), "Item Deleted", Snackbar.LENGTH_LONG)
                    .setAction("Undo") {
                        drawingView.undoLastSelectedItemDeletion()
                    }
                    .show()
            }

            fabSelectionDone.setOnClickListener {
                setUiModeIdle()
                setupFragmentViews()
            }
        }
    }

    private fun setupTextModeButtons() {
        binding.apply {
            fabTextColor.setOnClickListener {
                getColorPickerDialog(TEXT_COLOR).show()
            }

            fabTextBold.setOnClickListener {
                cetText.textBold = !cetText.textBold
                setupFragmentViews()
            }

            fabTextItalic.setOnClickListener {
                cetText.textItalic = !cetText.textItalic
                setupFragmentViews()
            }

            fabTextUnderline.setOnClickListener {
                cetText.textUnderline = !cetText.textUnderline
                setupFragmentViews()
            }

            fabTextAlignment.setOnClickListener {
                cetText.changeTextAlignment()
                setupFragmentViews()
            }

            fabTextBackground.setOnClickListener {
                binding.cetText.toggleTextBackground()
                setupFragmentViews()
            }

            fabTextDone.setOnClickListener {
                viewModel.onBtnTextDoneClicked(binding.cetText.text.toString())
            }
        }
    }

    private fun setupFragmentViews() {
        binding.apply {
            makeChangesAccordingToCurrentUiMode()

            llMainMenu.isVisible = viewModel.currentUiMode == DrawingViewModel.UIMode.MODE_IDLE

            // BRUSH
            llBrushMenu1.isVisible = viewModel.currentUiMode == DrawingViewModel.UIMode.MODE_BRUSH
            fabBrushDone.isVisible = viewModel.currentUiMode == DrawingViewModel.UIMode.MODE_BRUSH
            llBrushMenu2.isVisible = viewModel.currentUiMode == DrawingViewModel.UIMode.MODE_BRUSH
                    && viewModel.showBrushOptions
            llBrushTypes.isVisible = viewModel.showBrushOptions

            fabToggleBrushOptions.alpha = if(viewModel.showBrushOptions) 1F else 0.4F
            fabBrushDone.alpha = if(viewModel.showBrushOptions) 1F else 0.4F

            val brushOptionsToggleBtnImage = if(viewModel.showBrushOptions) R.drawable.ic_visibility_off
            else R.drawable.ic_visibility_on
            fabToggleBrushOptions.setImageResource(brushOptionsToggleBtnImage)

            fabColor.setColorFilter(drawingView.getBrushColor())

            // SELECTION
            llSelectMenu1.isVisible = viewModel.currentUiMode == DrawingViewModel.UIMode.MODE_SELECTION
                    && drawingView.getCurrentSelectedItem() != null
            llSelectMenu2.isVisible = viewModel.currentUiMode == DrawingViewModel.UIMode.MODE_SELECTION
                    && drawingView.getCurrentSelectedItem() != null && viewModel.showSelectOptions
            txtNoItemSelected.isVisible = viewModel.currentUiMode == DrawingViewModel.UIMode.MODE_SELECTION
                    && drawingView.getCurrentSelectedItem() == null
            fabSelectionDone.isVisible = viewModel.currentUiMode == DrawingViewModel.UIMode.MODE_SELECTION

            llSelectMenu1Items.isVisible = viewModel.currentUiMode == DrawingViewModel.UIMode.MODE_SELECTION
                    && viewModel.showSelectOptions

            fabToggleSelectOptions.alpha = if(viewModel.showSelectOptions) 1F else 0.4F
            fabSelectionDone.alpha = if(viewModel.showSelectOptions) 1F else 0.4F

            val selectOptionsToggleBtnImage = if(viewModel.showSelectOptions) R.drawable.ic_visibility_off
                else R.drawable.ic_visibility_on
            fabToggleBrushOptions.setImageResource(selectOptionsToggleBtnImage)

            // TEXT
            llTextMenu1.isVisible = viewModel.currentUiMode == DrawingViewModel.UIMode.MODE_TEXT
            cetText.isVisible = viewModel.currentUiMode == DrawingViewModel.UIMode.MODE_TEXT
            viewBgCetText.isVisible = viewModel.currentUiMode == DrawingViewModel.UIMode.MODE_TEXT
            rvFonts.isVisible = viewModel.currentUiMode == DrawingViewModel.UIMode.MODE_TEXT
                    && viewModel.internetAvailableFlow.value
            fabTextDone.isVisible = viewModel.currentUiMode == DrawingViewModel.UIMode.MODE_TEXT
            llOnlineFontsNotAvailable.isVisible = viewModel.currentUiMode == DrawingViewModel.UIMode.MODE_TEXT
                    && !viewModel.internetAvailableFlow.value

            makeCurrentBrushTypeButtonSelected()

            updateEnabledButtonsInSelectionMode()

            makeTextStyleBtnAsSelected()
            updateTextAlignmentBtn()
            updateTextBackgroundBtn()
        }
    }

    private fun makeChangesAccordingToCurrentUiMode() {
        binding.apply {
            drawingView.brushSelected = viewModel.currentUiMode == DrawingViewModel.UIMode.MODE_BRUSH
            drawingView.selectionMode = viewModel.currentUiMode == DrawingViewModel.UIMode.MODE_SELECTION
        }
    }

    private fun makeCurrentBrushTypeButtonSelected() {
        binding.apply {
            setDisabledStyleOnButton(fabBrushSimple)
            setDisabledStyleOnButton(fabBrushNormal)
            setDisabledStyleOnButton(fabBrushBlur)
            setDisabledStyleOnButton(fabBrushOutline)
            setDisabledStyleOnButton(fabEraser)

            when(drawingView.currentBrushType) {
                BrushType.SIMPLE -> setEnabledStyleOnButton(fabBrushSimple)
                BrushType.NORMAL -> setEnabledStyleOnButton(fabBrushNormal)
                BrushType.SOLID -> setEnabledStyleOnButton(fabBrushBlur)
                BrushType.OUTLINE -> setEnabledStyleOnButton(fabBrushOutline)
                BrushType.ERASER -> setEnabledStyleOnButton(fabEraser)
            }
        }
    }

    private fun updateEnabledButtonsInSelectionMode() {
        binding.apply {
            val isItemLocked = drawingView.currentSelectedItemFlow.value?.isLocked ?: false

            if(isItemLocked) setEnabledStyleOnButton(fabLockItem)
                else setDisabledStyleOnButton(fabLockItem)
        }
    }

    private fun makeTextStyleBtnAsSelected() {
        binding.apply {
            if(cetText.textBold) {
                setEnabledStyleOnButton(fabTextBold)
            } else {
                setDisabledStyleOnButton(fabTextBold)
            }

            if(cetText.textItalic) {
                setEnabledStyleOnButton(fabTextItalic)
            } else {
                setDisabledStyleOnButton(fabTextItalic)
            }

            if(cetText.textUnderline) {
                setEnabledStyleOnButton(fabTextUnderline)
            } else {
                setDisabledStyleOnButton(fabTextUnderline)
            }
        }
    }

    private fun updateTextAlignmentBtn() {
        binding.apply {
            val iconDrawableId = when(cetText.currentTextAlignment) {
                ColorEditText.TextAlignment.START -> R.drawable.ic_text_align_left
                ColorEditText.TextAlignment.CENTER -> R.drawable.ic_text_align_center
                ColorEditText.TextAlignment.END -> R.drawable.ic_text_align_right
            }

            val iconDrawable = AppCompatResources.getDrawable(requireContext(), iconDrawableId)
            fabTextAlignment.setImageDrawable(iconDrawable)
        }
    }

    private fun updateTextBackgroundBtn() {
        binding.apply {
            val iconDrawableId = if(cetText.addBackgroundToText) {
                R.drawable.ic_text_with_background
            } else {
                R.drawable.ic_text_without_background
            }

            val iconDrawable = AppCompatResources.getDrawable(requireContext(), iconDrawableId)
            fabTextBackground.setImageDrawable(iconDrawable)
        }
    }

    private fun setDisabledStyleOnButton(btn: FloatingActionButton) {
        btn.apply {
            imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.grey_400))
            backgroundTintList = ColorStateList.valueOf(Color.WHITE)
        }
    }

    private fun setEnabledStyleOnButton(btn: FloatingActionButton) {
        btn.apply {
            imageTintList = ColorStateList.valueOf(Color.WHITE)
            backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.blue_180))
        }
    }

    private fun setupDrawingView() {
        binding.drawingView.apply {
            setBrushSize(DEFAULT_BRUSH_WIDTH)
            setBrushColor(Color.parseColor(DEFAULT_BRUSH_COLOR))
        }
    }

    private fun setUiModeIdle() {
        viewModel.currentUiMode = DrawingViewModel.UIMode.MODE_IDLE
        viewModel.showBrushOptions = true

        binding.drawingView.apply {
            brushSelected = false
            setCurrentSelectedItem(null)
            setBrushType(BrushType.SIMPLE)
            clearDeletedBrushDrawingItems()
        }

        binding.cetText.apply {
            textBold = false
            textItalic = false
            textUnderline = false
            setText("")
            currentTextAlignment = ColorEditText.TextAlignment.CENTER
        }
    }

    private fun setupPointerCircle() {
        binding.apply {
            pointerCircle.layoutParams.height = drawingView.getBrushSize().toInt()
            pointerCircle.layoutParams.width = drawingView.getBrushSize().toInt()
            pointerCircle.requestLayout()
        }
    }

    private fun setupSeekbarBrush() {
        binding.seekBrushSize.apply {
            setProgress(DEFAULT_BRUSH_WIDTH.toInt())
        }

        binding.seekBrushSize.setOnSeekbarChangeListener(object: VerticalSeekbar.OnSeekbarChangeListener {
            override fun onProgressChanged(progress: Int) {
                binding.apply {
                    drawingView.setBrushSize(progress.toFloat())
                    setupPointerCircle()
                }
            }

            override fun onStartTrackingTouch() {
                binding.pointerCircle.visibility = View.VISIBLE
            }

            override fun onStopTrackingTouch() {
                binding.pointerCircle.visibility = View.GONE
            }
        })
    }

    private fun setupSeekbarTransparency() {
        binding.seekElementTransparency.apply {
            setProgress((DEFAULT_TRANSPARENCY * 100).toInt())
        }

        binding.seekElementTransparency.setOnSeekbarChangeListener(
            object: VerticalSeekbar.OnSeekbarChangeListener {
                override fun onProgressChanged(progress: Int) {
                    binding.apply {
                        val transparency = (progress / 100F) * 150
                        drawingView.setSelectedItemTransparency(transparency.toInt())
                    }
                }

                override fun onStartTrackingTouch() {}
                override fun onStopTrackingTouch() {}
        })
    }

    private fun setupFontsFlowCollector() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.fontsFlow.collect { response ->
                    binding.apply {
                        rvFonts.isVisible = viewModel.currentUiMode == DrawingViewModel.UIMode.MODE_TEXT
                                && !response.data.isNullOrEmpty() && viewModel.internetAvailableFlow.value
                    }

                    response.data?.let {
                        fontsAdapter.fontsList = it
                        fontsAdapter.notifyDataSetChanged()
                    }
                }
            }
        }
    }

    private fun setupCurrentSelectedItemFlowCollector() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                binding.drawingView.currentSelectedItemFlow.collect { currentSelectedItem ->
                    val transparencyProgress = if(currentSelectedItem != null) {
                        ((currentSelectedItem.transparency / 150F) * 100).toInt()
                    } else {
                        0
                    }

                    binding.apply {
                        seekElementTransparency.setProgress(transparencyProgress)
                        fabMoveToBack.isEnabled = !drawingView.isSelectedItemAtFirstPosition()
                        fabMoveBackward.isEnabled = !drawingView.isSelectedItemAtFirstPosition()
                        fabBringToFront.isEnabled = !drawingView.isSelectedItemAtLastPosition()
                        fabBringForward.isEnabled = !drawingView.isSelectedItemAtLastPosition()

                        fabEditText.isVisible = drawingView.isSelectedItemTextElement()
                    }

                    setupFragmentViews()
                }
            }
        }
    }

    private fun setupDrawingLoadingFlowCollector() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                binding.drawingView.loadingFlow.collect { loading ->
                    if(loadingDrawingDialog == null) {
                        loadingDrawingDialog = LoadingDialog("Loading Drawing..")
                    }

                    if(loading) {
                        loadingDrawingDialog?.show(childFragmentManager, "Drawing Loading")
                    } else {
                        if(loadingDrawingDialog?.isVisible == true) {
                            loadingDrawingDialog?.dismiss()
                        }
                    }
                }
            }
        }
    }

    private fun setupUIEventCollector() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    when(event) {
                        is DrawingViewModel.Event.LoadCurrentDrawing -> {
                            binding.drawingView.currentDrawing = viewModel.currentDrawing
                            viewModel.onEventOccurred()
                        }

                        is DrawingViewModel.Event.LoadDrawingDraft -> {
                            binding.drawingView.setDrawingItems(event.drawingItems)
                            viewModel.onEventOccurred()
                        }

                        is DrawingViewModel.Event.SelectedImageCompressed -> {
                            binding.drawingView.addDrawingImageItem(event.imageUri)
                            viewModel.currentUiMode = DrawingViewModel.UIMode.MODE_SELECTION
                            setupFragmentViews()
                            viewModel.onEventOccurred()
                        }

                        is DrawingViewModel.Event.DownloadingFont -> {
                            showDownloadingFontDialog()
                            viewModel.onEventOccurred()
                        }

                        is DrawingViewModel.Event.DownloadFontSuccess -> {
                            hideDownloadingFontDialog()
                            updateTextFont(event.fontItem)

                            viewModel.onEventOccurred()
                        }

                        is DrawingViewModel.Event.DownloadFontError -> {
                            hideDownloadingFontDialog()
                            Toast.makeText(context, event.exception.message, Toast.LENGTH_SHORT).show()

                            viewModel.onEventOccurred()
                        }

                        is DrawingViewModel.Event.EmptyTextDone -> {
                            setUiModeIdle()
                            setupFragmentViews()

                            viewModel.onEventOccurred()
                        }

                        is DrawingViewModel.Event.SaveTextImage -> {
                            CommonMethods.hideSoftKeyboard(requireContext(), binding.root)
                            binding.cetText.isCursorVisible = false
                            delay(100)

                            withContext(Dispatchers.IO) {
                                binding.cetText.saveTextImage()
                            }?.also { textElement ->
                                viewModel.onTextImageSaved(textElement)
                                binding.cetText.isCursorVisible = true
                            }

                            viewModel.onEventOccurred()
                        }

                        is DrawingViewModel.Event.UserTextElementSaved -> {
                            binding.drawingView.addTextDrawingItem(event.userTextElement)
                            viewModel.currentUiMode = DrawingViewModel.UIMode.MODE_SELECTION
                            setupFragmentViews()

                            viewModel.onEventOccurred()
                        }

                        is DrawingViewModel.Event.EditUserTextElement -> {
                            binding.apply {
                                drawingView.deleteSelectedItem()
                                cetText.setCurrentTextElement(event.userTextElement)
                            }
                            viewModel.currentUiMode = DrawingViewModel.UIMode.MODE_TEXT
                            setupFragmentViews()

                            viewModel.onEventOccurred()
                        }

                        is DrawingViewModel.Event.UserGraphicElementReceived -> {
                            binding.drawingView.addDrawingImageItem(event.userGraphicElement)
                            viewModel.currentUiMode = DrawingViewModel.UIMode.MODE_SELECTION
                            setupFragmentViews()

                            viewModel.onEventOccurred()
                        }

                        is DrawingViewModel.Event.UserGradientReceived -> {
                            binding.drawingView.addDrawingImageItem(event.userGradient)
                            viewModel.currentUiMode = DrawingViewModel.UIMode.MODE_SELECTION
                            setupFragmentViews()

                            viewModel.onEventOccurred()
                        }

                        is DrawingViewModel.Event.EmptyDrawing -> {
                            Toast.makeText(context, "Empty Drawing Discarded",
                                Toast.LENGTH_SHORT).show()
                            requireActivity().finish()
                        }

                        is DrawingViewModel.Event.ProceedToSaveDrawing -> {
                            showSaveDrawingDialog()
                            viewModel.onEventOccurred()
                        }

                        is DrawingViewModel.Event.SaveDrawingImage -> {
                            val drawingImageUri = withContext(Dispatchers.IO) {
                                binding.drawingView.saveDrawingImage()
                            }

                            if(drawingImageUri == null) {
                                Toast.makeText(context, "Error saving drawing..",
                                    Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.onDrawingImageSaved(drawingImageUri)
                            }
                            viewModel.onEventOccurred()
                        }

                        is DrawingViewModel.Event.DrawingSaved -> {
                            Toast.makeText(context, "Drawing Saved",
                                Toast.LENGTH_LONG).show()

                            requireActivity().finish()
                        }

                        is DrawingViewModel.Event.DrawingSavedAsDraft -> {
                            Toast.makeText(context, "Saved as draft",
                                Toast.LENGTH_LONG).show()

                            requireActivity().finish()
                        }

                        is DrawingViewModel.Event.ChangesDiscarded -> {
                            requireActivity().finish()
                        }

                        is DrawingViewModel.Event.ErrorOccurred -> {
                            Toast.makeText(context, event.exception.message, Toast.LENGTH_SHORT).show()
                            viewModel.onEventOccurred()
                        }

                        is DrawingViewModel.Event.Idle -> {}
                    }
                }
            }
        }
    }

    private fun updateTextFont(fontItem: FontItem) {
        binding.cetText.setFont(fontItem)

        fontsAdapter.currentSelectedFont = fontItem
        fontsAdapter.notifyDataSetChanged()
    }

    private fun showDownloadingFontDialog() {
        if(downloadingFontDialog != null) {
            downloadingFontDialog?.dismiss()
            downloadingFontDialog = null
        }

        downloadingFontDialog = LoadingDialog(loadingText = "Downloading Font..")
        downloadingFontDialog?.show(childFragmentManager, "Downloading Font")
    }

    private fun hideDownloadingFontDialog() {
        if(downloadingFontDialog == null) {
            return
        }

        downloadingFontDialog?.dismiss()
    }

    private fun getColorPickerDialog(colorToUpdate: String) = ColorPickerDialog.Builder(requireContext()).apply {
        setTitle("Choose a color")
        setPositiveButton("Ok", object : ColorEnvelopeListener {
            override fun onColorSelected(envelope: ColorEnvelope?, fromUser: Boolean) {

                val hexCode = envelope?.hexCode
                val color = Color.parseColor("#$hexCode")

                binding.apply {
                    if(colorToUpdate == BRUSH_COLOR) {
                        fabColor.setColorFilter(color)
                        drawingView.setBrushColor(color)

                    } else if(colorToUpdate == TEXT_COLOR) {
                        fabTextColor.setColorFilter(color)
                        cetText.setTextAndBackgroundColor(color)
                    }
                }
            }
        })
        setNegativeButton("Cancel") { dialogInterface, _ ->
            dialogInterface.cancel()
        }

        attachAlphaSlideBar(colorToUpdate == BRUSH_COLOR)
        attachBrightnessSlideBar(true)
        create()
    }

    private fun showSaveDrawingDialog() {
        val saveDrawingDialog = SaveDrawingDialog(
            viewModel.currentDrawing == null,
            object: SaveDrawingDialog.SaveDrawingClickEvent {
                override fun onSaveBtnClicked() {
                    viewModel.saveDrawing()
                }

                override fun onSaveAsDraftBtnClicked() {
                    viewModel.saveDrawingAsDraft(binding.drawingView.getAllDrawingItems())
                }

                override fun onDiscardChangesBtnClicked() {
                    viewModel.discardChanges()
                }
            })

        saveDrawingDialog.show(childFragmentManager, "Save Drawing")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private val startActivityForResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if(result.resultCode == Activity.RESULT_OK) {
                val uri = result.data?.data

                uri?.let { imageUri ->
                    viewModel.onImagePickedFromGallery(imageUri)
                }
            }
        }
}