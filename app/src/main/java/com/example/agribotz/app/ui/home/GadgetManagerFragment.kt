package com.example.agribotz.app.view.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.agribotz.R
import com.example.agribotz.app.domain.ApiStatus
import com.example.agribotz.app.util.PreferencesManager
import com.example.agribotz.app.viewmodels.home.GadgetManagerViewModel
import com.example.agribotz.app.viewmodels.home.GadgetManagerViewModelFactory
import com.example.agribotz.databinding.FragmentGadgetManagerBinding
import com.google.android.material.snackbar.Snackbar

class GadgetManagerFragment : Fragment() {

    private var _binding: FragmentGadgetManagerBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: GadgetManagerViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGadgetManagerBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner

        val app = requireActivity().application
        val prefManager = PreferencesManager(app)

        val gadgetId = GadgetManagerFragmentArgs.fromBundle(requireArguments()).gadgetId

        val factory = GadgetManagerViewModelFactory(
            app = app,
            prefManager = prefManager,
            gadgetId = gadgetId
        )

        viewModel = ViewModelProvider(this, factory)[GadgetManagerViewModel::class.java]
        binding.viewModel = viewModel

        setupToolbar()
        setupObservers()

        viewModel.onLoad()

        return binding.root
    }

    private fun setupToolbar() {
        binding.gadgetManagerToolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupObservers() {
        observeApiStatus()
        observeServerErrors()
        observeStatusDetails()
        observeTransactionError()
    }

    private fun observeApiStatus() {
        viewModel.apiStatus.observe(viewLifecycleOwner) { status ->
            when (status) {
                ApiStatus.LOADING -> {
                    // Optional: show shimmer/progress if you have one in layout
                    // binding.progressBar.visibility = View.VISIBLE
                }

                ApiStatus.DONE -> {
                    // Optional: hide shimmer/progress
                    // binding.progressBar.visibility = View.GONE
                }

                ApiStatus.ERROR -> {
                    // Optional: hide shimmer/progress
                    // binding.progressBar.visibility = View.GONE

                    // Connection/general fallback UI
                    Snackbar.make(
                        binding.root,
                        getString(R.string.Error_Internet_Connection),
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun observeServerErrors() {
        viewModel.errorServerMessage.observe(viewLifecycleOwner) { msg ->
            if (!msg.isNullOrBlank()) {
                Snackbar.make(binding.root, msg, Snackbar.LENGTH_LONG).show()
            }
        }

        viewModel.errorServerMessageRes.observe(viewLifecycleOwner) { resId ->
            if (resId != null) {
                Snackbar.make(binding.root, getString(resId), Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun observeStatusDetails() {
        viewModel.showStatusDetails.observe(viewLifecycleOwner) { payload ->
            if (payload != null) {
                val (statusResId, statusDate) = payload

                // Keep same behavior style you used in SiteDetails:
                // show message derived from res + date
                val text = try {
                    getString(statusResId, statusDate)
                } catch (_: Exception) {
                    // Fallback if this resource isn't formatted
                    "$statusDate"
                }

                Toast.makeText(requireContext(), text, Toast.LENGTH_SHORT).show()
                viewModel.onStatusDetailsShown()
            }
        }
    }

    private fun observeTransactionError() {
        viewModel.eventTransError.observe(viewLifecycleOwner) { resId ->
            if (resId != null) {
                Snackbar.make(binding.root, getString(resId), Snackbar.LENGTH_LONG).show()
                viewModel.onTransErrorCompleted()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
