package com.example.agribotz.app.view.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.agribotz.R
import com.example.agribotz.app.domain.ApiStatus
import com.example.agribotz.app.util.PreferencesManager
import com.example.agribotz.app.viewmodels.home.GadgetManagerViewModel
import com.example.agribotz.app.viewmodels.home.GadgetManagerViewModelFactory
import com.example.agribotz.databinding.FragmentGadgetManagerBinding
import com.google.android.material.snackbar.Snackbar
import androidx.navigation.fragment.findNavController

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
        setupSwipeRefresh()
        setupObservers()
        setupDefaultTab()

        viewModel.onLoad()

        return binding.root
    }

    private fun setupToolbar() {
        binding.gadgetManagerToolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.onLoad()
        }

        viewModel.apiStatus.observe(viewLifecycleOwner) { status ->
            binding.swipeRefresh.isRefreshing = status == ApiStatus.LOADING
        }
    }

    private fun setupObservers() {
        observeApiStatus()
        observeServerErrors()
        observeStatusDetails()
        observeTransactionError()
        observeRenameDialog()
        observeNavigateToMap()
        observeNavigateToSetLocation()
    }

    private fun observeApiStatus() {
        viewModel.apiStatus.observe(viewLifecycleOwner) { status ->
            // Keep SwipeRefresh spinner fully synchronized with loading state
            binding.swipeRefresh.isRefreshing = status == ApiStatus.LOADING

            if (status == ApiStatus.ERROR) {
                Snackbar.make(
                    binding.root,
                    getString(R.string.Error_Internet_Connection),
                    Snackbar.LENGTH_LONG
                ).show()
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
            if (resId != null && resId != 0) {
                Snackbar.make(binding.root, getString(resId), Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun observeStatusDetails() {
        viewModel.showStatusDetails.observe(viewLifecycleOwner) { payload ->
            payload?.let { (statusResId, statusDate) ->
                val message = try {
                    getString(statusResId, statusDate)
                } catch (_: Exception) {
                    statusDate
                }

                androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setMessage(message)
                    .setPositiveButton(android.R.string.ok, null)
                    .show()

                viewModel.onStatusDetailsShown()
            }
        }
    }

    private fun observeTransactionError() {
        viewModel.eventTransError.observe(viewLifecycleOwner) { resId ->
            if (resId != null && resId != 0) {
                Snackbar.make(binding.root, getString(resId), Snackbar.LENGTH_LONG).show()
                viewModel.onTransErrorCompleted()
            }
        }
    }

    private fun observeRenameDialog() {
        viewModel.openRenameDialog.observe(viewLifecycleOwner) { gadget ->
            gadget?.let {
                showRenameGadgetDialog(it)
                viewModel.onRenameDialogConsumed()
            }
        }
    }

    private fun setupDefaultTab() {
        binding.gadgetTabs.getTabAt(2)?.select()   // Settings
    }

    private fun showRenameGadgetDialog(gadget: com.example.agribotz.app.ui.home.GadgetCardUi) {
        val editText = android.widget.EditText(requireContext()).apply {
            hint = getString(R.string.Enter_Gadget_Name)
            setText(gadget.name)
        }

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.Rename_Gadget))
            .setView(editText)
            .setPositiveButton(R.string.Rename) { _, _ ->
                val newName = editText.text.toString().trim()
                if (newName.isNotEmpty() && newName != gadget.name) {
                    viewModel.renameGadget(newName)
                }
            }
            .setNegativeButton(R.string.Cancel, null)
            .show()
    }

    private fun observeNavigateToMap() {
        viewModel.navigateToMap.observe(viewLifecycleOwner) { locationNav ->
            locationNav?.let {
                val lat = it.gps?.lat
                val lng = it.gps?.long

                if (lat != null && lng != null) {
                    val bundle = Bundle().apply {
                        putFloat("lat", lat.toFloat())
                        putFloat("lng", lng.toFloat())
                        putString("gadgetName", it.gadgetName)
                    }

                    findNavController().navigate(
                        R.id.action_gadgetManagerFragment_to_gadgetLocationFragment,
                        bundle
                    )

                    viewModel.onMapNavigated()
                } else {
                    // Safety fallback (should rarely happen because click checks canOpenMap)
                    val fallback = Bundle().apply {
                        putString("gadgetId", it.gadgetId)
                        putString("gadgetName", it.gadgetName)
                    }
                    findNavController().navigate(
                        R.id.action_gadgetManagerFragment_to_setGadgetLocationFragment,
                        fallback
                    )
                    viewModel.onMapNavigated()
                }
            }
        }
    }

    private fun observeNavigateToSetLocation() {
        viewModel.navigateToSetLocation.observe(viewLifecycleOwner) { nav ->
            nav?.let {
                val bundle = Bundle().apply {
                    putString("gadgetId", it.gadgetId)
                    putString("gadgetName", it.gadgetName)

                    it.gps?.let { gps ->
                        gps.lat?.let { lat -> putFloat("gadgetLat", lat.toFloat()) }
                        gps.long?.let { lng -> putFloat("gadgetLng", lng.toFloat()) }
                    }
                }

                findNavController().navigate(
                    R.id.action_gadgetManagerFragment_to_setGadgetLocationFragment,
                    bundle
                )

                viewModel.onSetGpsNavigated()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
