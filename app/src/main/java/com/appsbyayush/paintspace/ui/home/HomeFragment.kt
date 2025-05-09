package com.appsbyayush.paintspace.ui.home

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.credentials.CredentialManager
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.work.*
import com.appsbyayush.paintspace.R
import com.appsbyayush.paintspace.adapters.DrawingAdapter
import com.appsbyayush.paintspace.baseactivity.DrawingActivity
import com.appsbyayush.paintspace.baseactivity.HomeActivity
import com.appsbyayush.paintspace.databinding.FragmentHomeBinding
import com.appsbyayush.paintspace.models.Drawing
import com.appsbyayush.paintspace.ui.bottomsheets.DrawingSettingsBottomSheetFragment
import com.appsbyayush.paintspace.ui.bottomsheets.SettingsBottomSheetFragment
import com.appsbyayush.paintspace.ui.bottomsheets.SignupBottomSheetFragment
import com.appsbyayush.paintspace.ui.dialogs.DraftDialog
import com.appsbyayush.paintspace.ui.dialogs.PermissionDialog
import com.appsbyayush.paintspace.ui.dialogs.RenameDrawingDialog
import com.appsbyayush.paintspace.ui.onboarding.OnboardingFragment
import com.appsbyayush.paintspace.utils.AppPermission
import com.appsbyayush.paintspace.utils.CommonMethods
import com.appsbyayush.paintspace.utils.Resource
import com.appsbyayush.paintspace.utils.getNetworkStatus
import com.appsbyayush.paintspace.utils.launchOneTapSignInUI
import com.appsbyayush.paintspace.worker.SyncWorker
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.TimeUnit


@AndroidEntryPoint
class HomeFragment: Fragment(R.layout.fragment_home) {
    companion object {
        private const val TAG = "HomeFragmentyyy"
    }

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()

    private var drawingsSyncing: Boolean = false
    private lateinit var drawingAdapter: DrawingAdapter
    private lateinit var credentialManager: CredentialManager

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHomeBinding.bind(view)

        checkOnboarding()

//        viewModel.signInClient = Identity.getSignInClient(requireContext())

        setupDrawingRecyclerView()
        setupButtons()

        setupDrawingsCollector()
        setupUnsavedDrawingPresentCollector()
        setupAppSettingsCollector()
        setupSyncObserver()
        setupUIEventCollector()

        viewModel.onFragmentStarted()
        addSyncDrawingsWorker()
    }

    override fun onStart() {
        super.onStart()
        viewModel.checkUnsavedDrawing()
    }

    private fun checkOnboarding() {
        if(!viewModel.onboardingDone) {
            viewModel.setOnboardingDone()

            val onboardingFragment = OnboardingFragment()

            parentFragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment, onboardingFragment)
                .commit()
        }
    }

    private fun setupDrawingRecyclerView() {
        drawingAdapter = DrawingAdapter(object: DrawingAdapter.DrawingItemClickEvent {
            override fun onItemClick(drawing: Drawing) {
                viewModel.onDrawingClicked(drawing)
            }

            override fun onShareBtnClick(drawing: Drawing) {
                onDrawingShareBtnClicked(drawing)
            }

            override fun onCopyToDeviceBtnClick(drawing: Drawing) {
                viewModel.onDrawingCopyToDeviceBtnClick(drawing)
            }

            override fun onMoreSettingsBtnClick(drawing: Drawing) {
                openDrawingSettingsBottomSheet(drawing)
            }
        })

        binding.rvDrawings.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            adapter = drawingAdapter
            setHasFixedSize(true)
            itemAnimator = null
        }
    }

    private fun setupButtons() {
        binding.fabAddDrawing.setOnClickListener {
            viewModel.onAddDrawingBtnClicked()
        }

        binding.btnToolbarSettings.setOnClickListener {
            openSettingsBottomSheet()
        }

        binding.txtOpenUnsavedDrawing.setOnClickListener {
            if(viewModel.unsavedDrawingPresent.value) {
                openDrawingActivity(loadDraft = true)
            }
        }

        activity?.onBackPressedDispatcher
            ?.addCallback(viewLifecycleOwner, object: OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    binding.apply {
                        if(!toolbarSearchView.isIconified) {
                            onBackButtonClicked()

                        } else {
                            isEnabled = false
                            activity?.onBackPressed()
                        }
                    }
                }
            })

        setupToolbarSearch()
    }

    private fun onDrawingShareBtnClicked(drawing: Drawing) {
        drawing.localDrawingImgUri?.let { fileUri ->
            val contentUri = CommonMethods.getContentUriFromFileUri(requireContext(), fileUri)

            contentUri?.let { shareableContentUri ->
                Intent(Intent.ACTION_SEND).apply {
                    type = "image/*"
                    putExtra(Intent.EXTRA_STREAM, shareableContentUri)
                    putExtra(Intent.EXTRA_TEXT, getString(R.string.share_message))
                }.also {
                    startActivity(Intent.createChooser(it, "Choose App"))
                }
            }
        }
    }

    private fun onBackButtonClicked() {
        binding.apply {
            if(!toolbarSearchView.isIconified) {
                toolbarSearchView.isIconified = true
                toolbarSearchView.setQuery("", false)
                toolbarSearchView.clearFocus()
                viewModel.updateSearchQuery("")
            }
        }
    }

    private fun setupToolbarSearch() {
        binding.toolbarSearchView.setOnSearchClickListener {
            toggleToolbarSearch()
        }

        binding.toolbarSearchView.setOnCloseListener {
            viewModel.updateSearchQuery("")
            toggleToolbarSearch(closing = true)
            false
        }

        binding.toolbarSearchView.setOnQueryTextListener(object: SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
//                query?.let {
//                    viewModel.updateSearchQuery(it)
//                }
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                newText?.let {
                    viewModel.updateSearchQuery(it)
                }
                return false
            }
        })
    }

    private fun toggleToolbarSearch(closing: Boolean = false) {
        binding.apply {
            flToolbarTitle.isVisible = !flToolbarTitle.isVisible
            btnToolbarSettings.isVisible = !btnToolbarSettings.isVisible

            binding.toolbarSearchView.layoutParams.width = if(closing)
                ViewGroup.LayoutParams.WRAP_CONTENT else ViewGroup.LayoutParams.MATCH_PARENT
        }
    }

    private fun openSettingsBottomSheet() {
        val settingsBottomSheet = SettingsBottomSheetFragment(
            drawingsSyncing,
            object: SettingsBottomSheetFragment.SettingsBottomSheetClickEvent {
                override fun onBtnSyncDrawingsClick() {
                    syncDrawingsImmediately()
                }

                override fun onBtnMoreSettingsClick() {
                    findNavController().navigate(HomeFragmentDirections
                        .actionFragmentHomeToFragmentSettings())
                }
            })

        settingsBottomSheet.show(childFragmentManager, "Settings")
    }

    private fun openDrawingSettingsBottomSheet(drawing: Drawing) {
        val drawingSettingsBottomSheet = DrawingSettingsBottomSheetFragment(
            object: DrawingSettingsBottomSheetFragment.DrawingSettingsBottomSheetClickEvent {
                override fun onBtnRenameClick() {
                    showRenameDrawingDialog(drawing)
                }

                override fun onBtnMoveToTrashClick() {
                    viewModel.trashDrawing(drawing)
                }
            })

        drawingSettingsBottomSheet.show(childFragmentManager, "Drawing Settings")
    }

    private fun openSignupBottomSheet() {
        val signupBottomSheet = SignupBottomSheetFragment(
            object: SignupBottomSheetFragment.SignupBottomSheetClickEvent {
                override fun onLoginButtonClicked() {
                    viewModel.apply {
                        loginUserWithGoogle()
                    }
                }
            }
        )

        signupBottomSheet.show(childFragmentManager, "Signup")
        viewModel.updateSignupPopupLastShownTime()
    }

    private fun showRenameDrawingDialog(drawing: Drawing) {
        val renameDialog = RenameDrawingDialog(
            drawingName = drawing.name,
            object: RenameDrawingDialog.RenameDrawingDialogClickEvent {
                override fun onBtnSubmitClick(updatedName: String) {
                    val newName = updatedName.trim()

                    if(newName.isNotEmpty()) {
                        drawing.name = newName
                        viewModel.updateDrawing(drawing)
                    }
                }
            }
        )

        renameDialog.show(childFragmentManager, "Rename Drawing")
    }

    private fun showUnsavedDrawingDialog(drawing: Drawing? = null, newDrawing: Boolean = false) {
        val draftDialog = DraftDialog(newDrawing, object: DraftDialog.DraftDialogClickEvent {
            override fun onBtnOpenDraftClick() {
                openDrawingActivity(loadDraft = true)
            }

            override fun onBtnNewDrawingClick() {
                viewModel.onBtnNewDrawingClick()
            }

            override fun onBtnDiscardAndContinueClick() {
                viewModel.onBtnDiscardAndContinueClick(drawing)
            }
        })

        draftDialog.show(childFragmentManager, "Draft")
    }

    private fun openDrawingActivity(currentDrawing: Drawing? = null, loadDraft: Boolean = true) {
        Intent(context, DrawingActivity::class.java).also {
            it.putExtra(DrawingActivity.KEY_CURRENT_DRAWING, currentDrawing)
            it.putExtra(DrawingActivity.KEY_LOAD_DRAFT, loadDraft)
            startActivity(it)
        }
    }

    private fun syncDrawingsImmediately() {
        context?.let {
            if(getNetworkStatus(it) == 0) {
                Toast.makeText(it, "No internet connection..", Toast.LENGTH_SHORT).show()
                return
            }
        }

        if(viewModel.loggedInUser == null) {
            context?.let {
                WorkManager.getInstance(it).cancelUniqueWork(SyncWorker.ONE_TIME_REQUEST_NAME)
            }

            openSignupBottomSheet()
            return
        }

        val workRequestConstraints = Constraints.Builder().apply {
            setRequiredNetworkType(NetworkType.CONNECTED)
        }.build()

        val syncOneTimeRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(workRequestConstraints)
            .build()

        context?.let {
            WorkManager.getInstance(it).enqueueUniqueWork(
                SyncWorker.ONE_TIME_REQUEST_NAME, ExistingWorkPolicy.KEEP, syncOneTimeRequest)
        }

        val syncProcessId = syncOneTimeRequest.id.toString()

        viewModel.saveCurrentSyncProcessId(syncProcessId)
        setupSyncObserver(syncProcessId)
    }

    private fun setupSyncObserver(processId: String? = null) {
        context?.let {
            val savedSyncProcessId = viewModel.getCurrentSyncProcessId()
            if(savedSyncProcessId.isEmpty() && processId.isNullOrEmpty()) {
                binding.llSyncingDrawings.visibility = View.GONE
                return
            }

            val currentSyncProcessId = processId ?: savedSyncProcessId

            val from = if(!processId.isNullOrEmpty()) {
                "Immediate Sync Called"
            } else {
                "Saved Sync In process"
            }

            WorkManager.getInstance(it)
                .getWorkInfoByIdLiveData(UUID.fromString(currentSyncProcessId))
                .observe(viewLifecycleOwner) { workInfo ->
                    if(workInfo == null) {
                        return@observe
                    }

                    when(workInfo.state) {
                        WorkInfo.State.ENQUEUED, WorkInfo.State.RUNNING, WorkInfo.State.BLOCKED -> {
                            binding.llSyncingDrawings.visibility = View.VISIBLE
                            drawingsSyncing = true
                        }

                        WorkInfo.State.SUCCEEDED, WorkInfo.State.CANCELLED, WorkInfo.State.FAILED -> {
                            binding.llSyncingDrawings.visibility = View.GONE
                            drawingsSyncing = false
                            viewModel.saveCurrentSyncProcessId("")
                        }
                    }

                    if(workInfo.state == WorkInfo.State.SUCCEEDED) {
                        Toast.makeText(context, "Drawings synced successfully", Toast.LENGTH_SHORT).show()
                    }

                    if(workInfo.state == WorkInfo.State.FAILED) {
                        Toast.makeText(context, "Some error occurred while syncing drawings." +
                                " Please try again later", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    private fun setupDrawingsCollector() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.drawingsFlow.collect { response ->
                    binding.apply {
                        progressLoading.isVisible = response is Resource.Loading
                        llEmptyDrawings.isVisible = response is Resource.Success && response.data.isNullOrEmpty()
                        txtEmptyDrawings.text = if(binding.toolbarSearchView.isIconified)
                            getString(R.string.start_drawing) else
                            "No drawings found"

                        imgEmptyDrawings.isVisible = response is Resource.Success && response.data.isNullOrEmpty()
                                && viewModel.searchQuery.value.isEmpty()
//                        rvDrawings.isVisible = response is Resource.Success && !response.data.isNullOrEmpty()
                    }


                    if(response is Resource.Success && response.data != null) {
                        val manualUpdateNeeded = response.data.size == drawingAdapter.itemCount
                        drawingAdapter.submitList(response.data)

                        if(manualUpdateNeeded) {
                            drawingAdapter.notifyDataSetChanged()
                        }
//                        drawingAdapter.notifyDataSetChanged()
                    }
                }
            }
        }
    }

    private fun setupUnsavedDrawingPresentCollector() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.unsavedDrawingPresent.collect {
                    binding.llUnsavedDrawing.isVisible = it
                }
            }
        }
    }

    private fun setupAppSettingsCollector() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.appSettings.collect {
                    if(viewModel.showSignupMessage()) {
                        openSignupBottomSheet()
                    }
                }
            }
        }
    }

    private fun setupUIEventCollector() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    binding.progressLoading.isVisible = event is HomeViewModel.Event.Loading

                    when(event) {
                        is HomeViewModel.Event.UnsavedDrawingStatus -> {
                            if(event.unsavedDrawingFound) {
                                showUnsavedDrawingDialog(drawing = event.drawing,
                                    newDrawing = event.drawing == null)
                            } else {
                                openDrawingActivity()
                            }

                            viewModel.onEventOccurred()
                        }

                        is HomeViewModel.Event.LoadDrawing -> {
                            openDrawingActivity(currentDrawing = event.currentDrawing)
                            viewModel.onEventOccurred()
                        }

                        is HomeViewModel.Event.TrashDrawingSuccess -> {
//                            binding.rvDrawings.scrollToPosition(0)

                            Snackbar.make(requireView(), "Drawing Trashed",
                                Snackbar.LENGTH_SHORT).show()

                            viewModel.onEventOccurred()
                        }

                        is HomeViewModel.Event.SignInSuccess -> {
                            Toast.makeText(context, "Sign in successful", Toast.LENGTH_SHORT).show()
                            Intent(context, HomeActivity::class.java).also {
                                it.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                                startActivity(it)
                            }
                        }

                        is HomeViewModel.Event.BeginOneTapSignInProcess -> {
                            context?.let {
                                credentialManager = CredentialManager.create(it)
                                launchOneTapSignInUI(
                                    context = it,
                                    credentialManager = credentialManager,
                                    signInRequest = event.signInRequest,
                                    viewLifecycleOwner = viewLifecycleOwner,
                                    onResultRetrieved = { credential ->
                                        viewModel.onOneTapSignInResultRetrieved(credential)
                                    },
                                    onErrorOccurred = {}
                                )
                            }
                            viewModel.onEventOccurred()
                        }

                        is HomeViewModel.Event.BeginOneTapSignInFailure -> {
                            Toast.makeText(context, event.exception.message, Toast.LENGTH_SHORT).show()
                            viewModel.onEventOccurred()
                        }

                        is HomeViewModel.Event.WriteExternalStoragePermissionNotGranted -> {
                            showStoragePermissionDialog(event.appPermission)
                            viewModel.onEventOccurred()
                        }

                        is HomeViewModel.Event.DrawingCopiedToDevice -> {
                            viewImage(event.deviceFileUri)
                            viewModel.onEventOccurred()
                        }

                        is HomeViewModel.Event.ErrorOccurred -> {
                            Toast.makeText(context, event.exception.message, Toast.LENGTH_SHORT).show()
                            viewModel.onEventOccurred()
                        }

                        else -> {}
                    }
                }
            }
        }
    }

    private fun viewImage(uri: Uri) {
        try {
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "image/*")
            }.also {
                startActivity(Intent.createChooser(it, "Choose App"))
            }
        } catch(e: Exception) {
            Toast.makeText(context, "Drawing saved successfully in ${uri.path}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showStoragePermissionDialog(appPermission: AppPermission) {
        val permissionPermanentlyDenied = appPermission.timesDenied > 2

        val permissionDialog = PermissionDialog(
            showSettingsOption = permissionPermanentlyDenied,
            object: PermissionDialog.PermissionDialogClickEvent {
                override fun onBtnNotNowClick() {
                    viewModel.drawingPendingForCopyingToDevice = null
                }

                override fun onBtnContinueClick() {
                    if(permissionPermanentlyDenied) {
                        appPermissionSettingsLauncher.launch(viewModel.getAppPermissionSettingsIntent())
                        return
                    }

                    requestStoragePermissionsLauncher.launch(appPermission.name)
                }
            }
        )

        permissionDialog.show(childFragmentManager, "Permission")
    }

    private fun addSyncDrawingsWorker() {
        if(viewModel.loggedInUser == null) {
            context?.let {
                WorkManager.getInstance(it).cancelUniqueWork(SyncWorker.PERIODIC_REQUEST_NAME)
            }
            return
        }

        val workRequestConstraints = Constraints.Builder().apply {
            setRequiredNetworkType(NetworkType.CONNECTED)
        }.build()

        val syncPeriodicRequest = PeriodicWorkRequestBuilder<SyncWorker>(2, TimeUnit.HOURS)
            .setConstraints(workRequestConstraints)
            .build()

        context?.let {
            WorkManager.getInstance(it).enqueueUniquePeriodicWork(
                SyncWorker.PERIODIC_REQUEST_NAME, ExistingPeriodicWorkPolicy.KEEP, syncPeriodicRequest)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    private val requestStoragePermissionsLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()){
        viewModel.onPermissionResult()
    }

    private val appPermissionSettingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()) {
        viewModel.onPermissionResult()
    }
}