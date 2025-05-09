package com.appsbyayush.paintspace.ui.bottomsheets.pickimage

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
import androidx.recyclerview.widget.SimpleItemAnimator
import com.appsbyayush.paintspace.adapters.GraphicElementTypeAdapter
import com.appsbyayush.paintspace.adapters.UserGraphicElementAdapter
import com.appsbyayush.paintspace.databinding.BottomSheetPickImageBinding
import com.appsbyayush.paintspace.models.GraphicElement
import com.appsbyayush.paintspace.models.UserGraphicElement
import com.appsbyayush.paintspace.paging.graphicelements.GraphicElementPagingAdapter
import com.appsbyayush.paintspace.ui.bottomsheets.base.BaseBottomSheetDialogFragment
import com.appsbyayush.paintspace.utils.Constants
import com.appsbyayush.paintspace.utils.Resource
import com.appsbyayush.paintspace.utils.enums.GraphicElementType
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PickImageBottomSheetFragment(
    private val clickEvent: PickImageBottomSheetClickEvent
): BaseBottomSheetDialogFragment() {

    companion object {
        private const val TAG = "PickImageBottomSheetyy"
    }

    private var _binding: BottomSheetPickImageBinding? = null
    private val binding get() = _binding!!

    private lateinit var searchGraphicElementPagingAdapter: GraphicElementPagingAdapter
    private lateinit var userGraphicElementAdapter: UserGraphicElementAdapter

    private val viewModel: PickImageViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetPickImageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupButtons()

        if(viewModel.internetAvailable) {
            setupElementTypeSpinner()
            setupUserGraphicElementsPagingAdapter()
            setupSearchGraphicElementsRecyclerView()
            setupGraphicSearch()

            setupUiEventCollector()
            setupUserGraphicElementsFlowCollector()
            setupSearchGraphicElementsFlowCollector()
            setupGraphicElementLoadStateCollector()
        }

        setupFragmentViews()
        viewModel.onFragmentStarted()
    }

    private fun setupButtons() {
        binding.apply {
            btnPickImageFromGallery.setOnClickListener {
                clickEvent.onBtnPickImageFromGalleryClick()
                dismissAfterDelay()
            }

            btnCreateGradient.setOnClickListener {
                clickEvent.onBtnCreateGradientClick()
                dismissAfterDelay()
            }

            btnRetryElementSearch.setOnClickListener {
                searchGraphicElementPagingAdapter.retry()
            }

            imgSearch.setOnClickListener {
                viewModel.searchMode = true
                setupFragmentViews()
            }

            imgCloseSearch.setOnClickListener {
                viewModel.apply {
                    searchMode = false
                    onSearchQueryChanged("")
                }
                setupFragmentViews()
            }
        }
    }

    private fun setupFragmentViews() {
        binding.apply {
            llNoInternet.isVisible = !viewModel.internetAvailable

            if(!viewModel.internetAvailable) {
                txtUsedGraphics.isVisible = false
                rvUserGraphicElements.isVisible = false
                llLoadingImages.isVisible = false
                llRetryLoadingImages.isVisible = false
                llElementSpinner.isVisible = false
                txtNoSearchResults.isVisible = false
                txtSearchForElements.isVisible = false
                rvSearchGraphicElements.isVisible = false

                return
            }

            spinnerElementType.isVisible = !viewModel.searchMode
            editTextSearchGraphics.isVisible = viewModel.searchMode

            imgSearch.isVisible = !viewModel.searchMode
            imgCloseSearch.isVisible = viewModel.searchMode
        }
    }

    private fun setupUserGraphicElementsPagingAdapter() {
        userGraphicElementAdapter = UserGraphicElementAdapter(
            object: UserGraphicElementAdapter.UserGraphicElementItemClickEvent {
                override fun onItemClick(userGraphicElement: UserGraphicElement) {
                    viewModel.onUserGraphicElementClicked(userGraphicElement)
                }
            })

        binding.rvUserGraphicElements.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            setHasFixedSize(true)
            adapter = userGraphicElementAdapter
            itemAnimator = null
        }
    }

    private fun setupSearchGraphicElementsRecyclerView() {
        searchGraphicElementPagingAdapter = GraphicElementPagingAdapter(
            object: GraphicElementPagingAdapter.GraphicElementItemClickEvent {
                override fun onItemClick(element: GraphicElement) {
                    viewModel.onGraphicElementClicked(element)
                }
            })

        binding.rvSearchGraphicElements.apply {
            layoutManager = GridLayoutManager(context, 3)
            setHasFixedSize(true)
            adapter = searchGraphicElementPagingAdapter
        }
    }

    private fun setupElementTypeSpinner() {
        val graphicElementTypeAdapter = GraphicElementTypeAdapter(requireContext(),
            Constants.GRAPHIC_ELEMENT_TYPE_LIST)

        binding.apply {
            spinnerElementType.adapter = graphicElementTypeAdapter

            spinnerElementType.onItemSelectedListener = object: AdapterView.OnItemSelectedListener{
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val selectedElementType = graphicElementTypeAdapter.getItem(position)?.elementType
                        ?: GraphicElementType.ELEMENT_SIMPLE
                    viewModel.onElementTypeChanged(selectedElementType)
                }

                override fun onNothingSelected(parent: AdapterView<*>?) { /* DO NOTHING */}
            }
        }
    }

    private fun setupGraphicSearch() {
        binding.editTextSearchGraphics.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.onSearchQueryChanged(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupUserGraphicElementsFlowCollector() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.userGraphicElementsFlow.collect { response ->
                    Log.d(TAG, "setupUserGraphicElementsFlowCollector: ${response}")
                    Log.d(TAG, "setupUserGraphicElementsFlowCollector: ${response.data}")
                    binding.apply {
                        txtUsedGraphics.isVisible = response !is Resource.Loading
                                && !response.data.isNullOrEmpty()
                        rvUserGraphicElements.isVisible = response !is Resource.Loading
                                && !response.data.isNullOrEmpty()
                    }

                    response.data?.let { elements ->
                        Log.d(TAG, "setupUserGraphicElementsFlowCollector: Updating..")
                        Log.d(TAG, "setupUserGraphicElementsFlowCollector: RecyclerView..${binding.rvUserGraphicElements.isVisible}")
                        userGraphicElementAdapter.elementsList = elements
                        userGraphicElementAdapter.notifyDataSetChanged()
                    }
                    
                    if(response is Resource.Error) {
                        Log.d(TAG, "setupUserGraphicElementsFlowCollector: ${response.error}")
                    }
                }
            }
        }
    }

    private fun setupSearchGraphicElementsFlowCollector() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.searchGraphicElementsFlow.collect { pagingData ->
                    Log.d(TAG, "setupGraphicElementsFlowCollector: New Value $pagingData")
                    searchGraphicElementPagingAdapter.submitData(pagingData)
                }
            }
        }
    }

    private fun setupGraphicElementLoadStateCollector() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                searchGraphicElementPagingAdapter.loadStateFlow.collect { loadStates ->
                    binding.apply {

                        llLoadingImages.isVisible = loadStates.refresh is LoadState.Loading
                                && searchGraphicElementPagingAdapter.itemCount < 1
                        llRetryLoadingImages.isVisible = loadStates.refresh is LoadState.Error

                        txtNoSearchResults.isVisible = loadStates.refresh is LoadState.NotLoading
                                && searchGraphicElementPagingAdapter.itemCount < 1
                    }
                }
            }
        }
    }

    private fun setupUiEventCollector() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    when(event) {
                        is PickImageViewModel.Event.UserGraphicElementSaved -> {
                            dismissAfterDelay()
                            clickEvent.onGraphicElementItemClick(event.userGraphicElement)
                        }
                        is PickImageViewModel.Event.Idle -> {}
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    interface PickImageBottomSheetClickEvent {
        fun onBtnPickImageFromGalleryClick()
        fun onBtnCreateGradientClick()
        fun onGraphicElementItemClick(userGraphicElement: UserGraphicElement)
    }
}