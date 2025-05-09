package com.appsbyayush.paintspace.ui.bottomsheets.gradient

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.paging.LoadState
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.appsbyayush.paintspace.adapters.GradientColorAdapter
import com.appsbyayush.paintspace.adapters.GradientTypeAdapter
import com.appsbyayush.paintspace.adapters.UserGradientAdapter
import com.appsbyayush.paintspace.databinding.BottomSheetGradientBinding
import com.appsbyayush.paintspace.models.Gradient
import com.appsbyayush.paintspace.models.GradientColor
import com.appsbyayush.paintspace.models.UserGradient
import com.appsbyayush.paintspace.paging.gradients.GradientPagingAdapter
import com.appsbyayush.paintspace.ui.bottomsheets.base.BaseBottomSheetDialogFragment
import com.appsbyayush.paintspace.ui.bottomsheets.pickimage.PickImageBottomSheetFragment
import com.appsbyayush.paintspace.utils.Constants
import com.appsbyayush.paintspace.utils.Resource
import com.appsbyayush.paintspace.utils.enums.GradientType
import com.skydoves.colorpickerview.ColorEnvelope
import com.skydoves.colorpickerview.ColorPickerDialog
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class GradientBottomSheetFragment(
    private val clickEvent: GradientBottomSheetClickEvent
): BaseBottomSheetDialogFragment() {
    companion object {
        private const val TAG = "GradientBottomSyyy"
    }

    private var _binding: BottomSheetGradientBinding? = null
    private val binding get() = _binding!!

    private val viewModel: GradientViewModel by viewModels()

    private lateinit var currentGradientColor: GradientColor

    private lateinit var gradientColorAdapter: GradientColorAdapter
    private lateinit var gradientSearchPagingAdapter: GradientPagingAdapter
    private lateinit var userGradientAdapter: UserGradientAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = BottomSheetGradientBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupGradientColorsRecyclerView()
        setupGradientTypeSpinner()
        setupCurrentGradientTypeFlowCollector()
        setupButtons()

        if(viewModel.internetAvailable) {
            setupSearchGradientsRecyclerView()
            setupUserGradientsRecyclerView()
            setupGradientSearch()
            setupSearchGradientsFlowCollector()
            setupSearchGradientsLoadStateCollector()
            setupUserGradientsFlowCollector()
        }

        setupFragmentViews()

        setupUiEventFlowCollector()
        viewModel.onFragmentStarted()
    }

    private fun setupFragmentViews() {
        binding.apply {
            llNoInternet.isVisible = !viewModel.internetAvailable

            if(!viewModel.internetAvailable) {
                editTextSearchGradients.isVisible = false
                rvSearchGradients.isVisible = false
                llGradientSearchResultStates.isVisible = false
                txtUsedGradients.isVisible = false
                rvUserGradients.isVisible = false

                return
            }
        }
    }

    private fun setupGradientTypeSpinner() {
        val gradientTypeArrayAdapter = GradientTypeAdapter(requireContext(),
            Constants.GRADIENT_TYPE_LIST)

        binding.apply {
            spinnerGradientType.adapter = gradientTypeArrayAdapter

            spinnerGradientType.onItemSelectedListener = object: AdapterView.OnItemSelectedListener{
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val selectedGradientType = gradientTypeArrayAdapter.getItem(position)?.gradientType
                        ?: GradientType.LINEAR
                    viewModel.updateCurrentGradientType(selectedGradientType)
                }

                override fun onNothingSelected(parent: AdapterView<*>?) { /* DO NOTHING */}
            }
        }
    }

    private fun setupCurrentGradient() {
        binding.apply {
            imgGradient.setImageDrawable(getCurrentGradientDrawable())
        }
    }

    private fun setupButtons() {
        binding.apply {
            btnRetryGradientSearch.setOnClickListener {
                gradientSearchPagingAdapter.retry()
            }

            btnCreateGradient.setOnClickListener {
                viewModel.onCreateBtnClick(gradientColorAdapter.gradientColorList,
                    getCurrentGradientDrawable())
            }
        }
    }

    private fun setupGradientSearch() {
        binding.editTextSearchGradients.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.onSearchQueryChanged(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupGradientColorsRecyclerView() {
        gradientColorAdapter = GradientColorAdapter(object: GradientColorAdapter.GradientColorItemClickEvent {
            override fun onItemClick(gradientColor: GradientColor) {
                Log.d(TAG, "GradientColorAdapter onItemClick: Called")
                currentGradientColor = gradientColor
                getColorPickerDialog(currentGradientColor.colorHexCodeString).show()
            }

            override fun onItemLongClick(gradientColor: GradientColor) {
                Log.d(TAG, "GradientColorAdapter onItemLongClick: Called")
            }

            override fun onCheckboxClick(gradientColor: GradientColor) {
                Log.d(TAG, "onCheckboxClick: Called")
                gradientColor.enabled = !gradientColor.enabled
                gradientColorAdapter.notifyDataSetChanged()
                setupCurrentGradient()
            }
        })

        gradientColorAdapter.gradientColorList = Constants.DEFAULT_GRADIENT_COLORS_LIST

        binding.rvGradientColors.apply {
            adapter = gradientColorAdapter
            layoutManager = GridLayoutManager(context, 2)
        }
    }

    private fun setupSearchGradientsRecyclerView() {
        gradientSearchPagingAdapter = GradientPagingAdapter(
            viewModel.currentGradientTypeFlow.value,
            object: GradientPagingAdapter.GradientItemClickEvent {
                override fun onItemClick(gradient: Gradient) {
                    setGradientColors(gradient)
                }
            }
        )

        binding.rvSearchGradients.apply {
            layoutManager = GridLayoutManager(context, 2, GridLayoutManager.HORIZONTAL, false)
            adapter = gradientSearchPagingAdapter
        }
    }

    private fun setupUserGradientsRecyclerView() {
        userGradientAdapter = UserGradientAdapter(object: UserGradientAdapter.UserGradientItemClickEvent {
            override fun onItemClick(userGradient: UserGradient) {
                viewModel.selectedUserGradient = userGradient
                setGradientColors(userGradient)
            }
        })

        binding.rvUserGradients.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = userGradientAdapter
        }
    }

    private fun setGradientColors(gradient: Gradient) {
        gradientColorAdapter.gradientColorList.forEachIndexed { index, gradientColor ->
            if(index >= gradient.colors.size) {
                gradientColor.enabled = false
                return@forEachIndexed
            }

            val colorHexString = gradient.colors[index]

            gradientColor.apply {
                colorHexCodeString = colorHexString
                enabled = true
            }
        }

        gradientColorAdapter.notifyDataSetChanged()
        setupCurrentGradient()
    }

    private fun setGradientColors(userGradient: UserGradient) {
        gradientColorAdapter.gradientColorList.forEachIndexed { index, gradientColor ->
            if(index >= userGradient.colors.size) {
                gradientColor.enabled = false
                return@forEachIndexed
            }

            val colorHexString = userGradient.colors[index]

            gradientColor.apply {
                colorHexCodeString = colorHexString
                enabled = true
            }
        }

        gradientColorAdapter.notifyDataSetChanged()
        setupCurrentGradient()
    }

    private fun getCurrentGradientDrawable(): Drawable {
        val enabledColors = gradientColorAdapter.gradientColorList.filter { it.enabled }

        if(enabledColors.size <= 1) {
            return if(enabledColors.size == 1) {
                ColorDrawable(Color.parseColor(enabledColors.first().colorHexCodeString))
            } else {
                ColorDrawable(Color.WHITE)
            }
        }

        val gradientColors = enabledColors.sortedBy { it.colorPosition }
            .map { gradientColor -> Color.parseColor(gradientColor.colorHexCodeString) }
            .toIntArray()

        val gradientDrawable = GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            gradientColors
        )

        when(viewModel.currentGradientTypeFlow.value) {
            GradientType.LINEAR -> {
                gradientDrawable.gradientType = GradientDrawable.LINEAR_GRADIENT
            }

            GradientType.RADIAL -> {
                gradientDrawable.gradientType = GradientDrawable.RADIAL_GRADIENT
                gradientDrawable.gradientRadius = Constants.DEFAULT_GRADIENT_RADIUS
            }

            GradientType.SWEEP -> {
                gradientDrawable.gradientType = GradientDrawable.SWEEP_GRADIENT
            }
        }

        return gradientDrawable
    }

    private fun getColorPickerDialog(currentColorHexCode: String = "")
    = ColorPickerDialog.Builder(requireContext()).apply {
        setTitle("Choose a color")
        setPositiveButton("Ok", object : ColorEnvelopeListener {
            override fun onColorSelected(envelope: ColorEnvelope?, fromUser: Boolean) {

                val hexCode = envelope?.hexCode
                val colorHexCodeString = "#$hexCode"

                currentGradientColor.colorHexCodeString = colorHexCodeString
                gradientColorAdapter.notifyDataSetChanged()

                viewModel.selectedUserGradient = null
                setupCurrentGradient()
            }
        })
        setNegativeButton("Cancel") { dialogInterface, _ ->
            dialogInterface.cancel()
        }

        if(currentColorHexCode.isNotEmpty()) {
            colorPickerView.apply {
                setInitialColor(Color.parseColor(currentColorHexCode))
            }
        }

        attachAlphaSlideBar(true)
        attachBrightnessSlideBar(true)
        create()
    }

    private fun setupCurrentGradientTypeFlowCollector() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.currentGradientTypeFlow.collect { type ->
                    setupCurrentGradient()

                    if(viewModel.internetAvailable) {
                        gradientSearchPagingAdapter.gradientType = type
                        gradientSearchPagingAdapter.notifyDataSetChanged()
                    }
                }
            }
        }
    }

    private fun setupSearchGradientsFlowCollector() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.searchGradientsFlow.collect { pagingData ->
                    gradientSearchPagingAdapter.submitData(pagingData)
                }
            }
        }
    }

    private fun setupSearchGradientsLoadStateCollector() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                gradientSearchPagingAdapter.loadStateFlow.collect { loadStates ->
                    binding.apply {
                        llLoadingGradients.isVisible = loadStates.source.refresh is LoadState.Loading
                                && gradientSearchPagingAdapter.itemCount < 1
                        llRetryLoadingGradients.isVisible = loadStates.source.refresh is LoadState.Error

                        txtNoSearchResults.isVisible = loadStates.source.refresh is LoadState.NotLoading
                                && gradientSearchPagingAdapter.itemCount < 1
                    }
                }
            }
        }
    }

    private fun setupUserGradientsFlowCollector() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.usedGradientsFlow.collect { response ->
                    Log.d(TAG, "setupUserGradientsFlowCollector: ${response}")
                    Log.d(TAG, "setupUserGradientsFlowCollector: ${response.data}")

                    binding.apply {
                        txtUsedGradients.isVisible = response !is Resource.Loading && !response.data.isNullOrEmpty()
                        rvUserGradients.isVisible = response !is Resource.Loading && !response.data.isNullOrEmpty()
                    }

                    response.data?.let { elements ->
                        userGradientAdapter.userGradientList = elements
                        userGradientAdapter.notifyDataSetChanged()
                    }

                    if(response is Resource.Error) {
                        Log.d(TAG, "setupUserGradientsFlowCollector: ${response.error}")
                    }
                }
            }
        }
    }

    private fun setupUiEventFlowCollector() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    when (event) {
                        is GradientViewModel.Event.UserGradientSaved -> {
                            dismissAfterDelay()
                            clickEvent.onCreateBtnClick(event.userGradient)
                            viewModel.onEventOccurred()
                        }
                        is GradientViewModel.Event.Idle -> {}
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    interface GradientBottomSheetClickEvent {
        fun onCreateBtnClick(userGradient: UserGradient)
    }
}