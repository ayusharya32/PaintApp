package com.appsbyayush.paintspace.ui.settings

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.isVisible
import androidx.credentials.CredentialManager
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.work.*
import com.appsbyayush.paintspace.R
import com.appsbyayush.paintspace.baseactivity.TermsActivity
import com.appsbyayush.paintspace.databinding.FragmentSettingsBinding
import com.appsbyayush.paintspace.ui.dialogs.LoadingDialog
import com.appsbyayush.paintspace.utils.CommonMethods
import com.appsbyayush.paintspace.utils.Constants
import com.appsbyayush.paintspace.utils.getNetworkStatus
import com.appsbyayush.paintspace.utils.launchOneTapSignInUI
import com.appsbyayush.paintspace.worker.SyncWorker
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class SettingsFragment: Fragment(R.layout.fragment_settings) {
    companion object {
        private const val TAG = "SettingsFragmentyy"
    }

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SettingsViewModel by viewModels()

    private lateinit var logoutLoadingDialog: LoadingDialog
    private lateinit var credentialManager: CredentialManager


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSettingsBinding.bind(view)

        setupButtons()
        setupLogoutLoadingDialog()

        setupUserDetails()
        setupLastSyncDetails()
        setupSyncObserver()
        setupUIEventCollector()
    }

    private fun setupLogoutLoadingDialog() {
        logoutLoadingDialog = LoadingDialog("Logging Out..")
    }

    private fun setupSyncObserver(processId: String? = null) {
        context?.let {
            var savedSyncProcessId = viewModel.getCurrentSyncProcessId()

            if(savedSyncProcessId.isEmpty() && processId.isNullOrEmpty()) {
                return
            }

            val currentSyncProcessId = processId ?: savedSyncProcessId

            WorkManager.getInstance(it)
                .getWorkInfoByIdLiveData(UUID.fromString(currentSyncProcessId))
                .observe(viewLifecycleOwner) { workInfo ->
                    if(workInfo == null) {
                        return@observe
                    }

                    when(workInfo.state) {
                        WorkInfo.State.ENQUEUED, WorkInfo.State.RUNNING, WorkInfo.State.BLOCKED -> {
                            binding.btnSyncDrawings.apply {
                                text = "Syncing Drawings..."
                                icon = null
                                isEnabled = false
                            }
                        }

                        WorkInfo.State.SUCCEEDED, WorkInfo.State.CANCELLED, WorkInfo.State.FAILED -> {
                            binding.btnSyncDrawings.apply {
                                text = "Sync Drawings"
                                icon = AppCompatResources.getDrawable(requireContext(),
                                    R.drawable.ic_sync)
                                isEnabled = true

                                viewModel.saveCurrentSyncProcessId("")
                            }
                        }
                    }

                    if(workInfo.state == WorkInfo.State.SUCCEEDED) {
                        Toast.makeText(context, "Drawings synced successfully", Toast.LENGTH_SHORT).show()
                        setupLastSyncDetails()
                    }
                }
        }
    }

    private fun setupUserDetails() {
        viewModel.loggedInUser?.let { user ->
            binding.apply {
                txtUserName.text = user.displayName
                txtUserEmail.text = user.email

                Glide.with(root)
                    .load(user.photoUrl)
                    .into(imgUser)
            }
        }
    }

    private fun setupLastSyncDetails() {
        binding.apply {
            val lastSync = viewModel.getUpdatedAppSettings().lastSyncTime
            txtLastSyncTime.isVisible = lastSync != null

            if(lastSync != null) {
                val lastSyncString = "Last Sync: ${CommonMethods.getTimeAgoString(lastSync)} " +
                        "(${CommonMethods.getFormattedDateTime(lastSync, Constants.DATE_FORMAT_2)})"
                txtLastSyncTime.text = lastSyncString
            }
        }
    }

    private fun setupButtons() {
        binding.btnSignIn.setOnClickListener {
            viewModel.loginUserWithGoogle()
        }

        binding.btnSignOut.setOnClickListener {
            showSignOutDialog()
        }

        binding.btnSyncDrawings.setOnClickListener {
            syncDrawingsImmediately()
        }

        binding.btnToolbarBack.setOnClickListener {
            activity?.onBackPressed()
        }

        binding.btnTrashCan.setOnClickListener {
            findNavController().navigate(SettingsFragmentDirections
                .actionFragmentSettingsToFragmentTrash())
        }

        binding.txtPrivacyPolicy.setOnClickListener {
            Intent(context, TermsActivity::class.java).also {
                startActivity(it)
            }
        }
    }

    private fun showSignOutDialog() {
        val dialog = MaterialAlertDialogBuilder(requireContext()).apply {
            setTitle("Sign Out")
            setMessage(getString(R.string.sign_out_message))
            setPositiveButton("Confirm") { dialog, _ ->
                viewModel.signOut()
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

    private fun setupFragmentViews(loading: Boolean = false) {
        binding.apply {
            progressLoading.isVisible = loading

            llNotLoggedIn.isVisible = !loading && viewModel.loggedInUser == null
            llUserInfo.isVisible = !loading && viewModel.loggedInUser != null

            btnSyncDrawings.isVisible = !loading && viewModel.loggedInUser != null
            btnSignOut.isVisible = !loading && viewModel.loggedInUser != null
            btnTrashCan.isVisible = !loading
        }
    }

    private fun setupUIEventCollector() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.events.collect { event ->
                setupFragmentViews(false)

                when(event) {
                    is SettingsViewModel.Event.SignInSuccess -> {
                        Toast.makeText(context, "Sign in successful", Toast.LENGTH_SHORT).show()
                        setupFragmentViews()
                        setupUserDetails()
                        addSyncDrawingsWorker()
                    }

                    is SettingsViewModel.Event.BeginOneTapSignInProcess -> {
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

                    is SettingsViewModel.Event.BeginOneTapSignInFailure -> {
                        Toast.makeText(context, event.exception.message, Toast.LENGTH_SHORT).show()
                        viewModel.onEventOccurred()
                    }

                    is SettingsViewModel.Event.LogoutLoading -> {
                        logoutLoadingDialog.show(childFragmentManager, "Logout")
                    }

                    is SettingsViewModel.Event.LogoutSuccess -> {
                        Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show()
                        logoutLoadingDialog.dismiss()

                        findNavController().popBackStack()
                    }

                    is SettingsViewModel.Event.LogoutError -> {
                        Toast.makeText(context, event.exception.message, Toast.LENGTH_SHORT).show()
                        logoutLoadingDialog.dismiss()
                    }

                    is SettingsViewModel.Event.ErrorOccurred -> {
                        Toast.makeText(context, event.exception.message, Toast.LENGTH_SHORT).show()
                    }

                    is SettingsViewModel.Event.Loading -> {
                        setupFragmentViews(true)
                    }

                    is SettingsViewModel.Event.Idle -> {}
                }
            }
        }
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

    override fun onStart() {
        super.onStart()
        setupFragmentViews()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}