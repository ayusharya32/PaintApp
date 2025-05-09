package com.appsbyayush.paintspace.ui.onboarding

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.appsbyayush.paintspace.R
import com.appsbyayush.paintspace.adapters.OnboardingItemAdapter
import com.appsbyayush.paintspace.databinding.FragmentOnboardingBinding
import com.appsbyayush.paintspace.ui.home.HomeFragment

class OnboardingFragment: Fragment(R.layout.fragment_onboarding) {
    companion object {
        private const val TAG = "OnboardingFragmeyy"
    }

    private var _binding: FragmentOnboardingBinding? = null
    private val binding get() = _binding!!

    private lateinit var onboardingItemAdapter: OnboardingItemAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentOnboardingBinding.bind(view)

        setupOnboardingViewPager()
        setupButtons()
    }

    private fun setupOnboardingViewPager() {
        onboardingItemAdapter = OnboardingItemAdapter()

        binding.viewPagerOnboarding.apply {
            adapter = onboardingItemAdapter
            registerOnPageChangeCallback(object: ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)

                    binding.apply {
                        val lastPage = position == onboardingItemAdapter.onboardingItems.size - 1
                        txtPrev.isVisible = position != 0

                        txtNext.text = if(lastPage) "Done" else "Next"
                        txtSkip.visibility = if(lastPage) View.INVISIBLE else View.VISIBLE
                    }
                }
            })
        }
    }

    private fun setupButtons() {
        binding.apply {
            txtPrev.setOnClickListener {
                viewPagerOnboarding.currentItem -= 1
            }

            txtNext.setOnClickListener {
                if(viewPagerOnboarding.currentItem != onboardingItemAdapter.onboardingItems.size - 1) {
                    viewPagerOnboarding.currentItem += 1
                    return@setOnClickListener
                }

                navigateToHomeFragment()
            }

            txtSkip.setOnClickListener {
                navigateToHomeFragment()
            }
        }
    }

    private fun navigateToHomeFragment() {
        val homeFragment = HomeFragment()

        parentFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, homeFragment)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}