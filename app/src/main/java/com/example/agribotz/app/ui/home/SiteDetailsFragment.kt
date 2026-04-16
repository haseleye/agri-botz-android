package com.example.agribotz.app.ui.home

import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.agribotz.R
import com.example.agribotz.app.repository.Repository
import com.example.agribotz.app.ui.home.GadgetCardUi
import com.example.agribotz.app.util.PreferencesManager
import com.example.agribotz.app.viewholders.home.SiteDetailsAdapter
import com.example.agribotz.app.viewmodels.home.SiteDetailsViewModel
import com.example.agribotz.app.viewmodels.home.SiteDetailsViewModelFactory
import com.example.agribotz.databinding.FragmentSiteDetailsBinding
import com.google.android.material.appbar.MaterialToolbar
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning

class SiteDetailsFragment : Fragment() {

    private var _binding: FragmentSiteDetailsBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val MENU_FILTER_ID = 1001
    }

    private var serialFilterEditText: EditText? = null
    private var toolbar: MaterialToolbar? = null

    private val siteId: String by lazy { arguments?.getString("siteId") ?: "" }

    private val viewModel: SiteDetailsViewModel by viewModels {
        SiteDetailsViewModelFactory(Repository(), PreferencesManager(requireContext()), siteId)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSiteDetailsBinding.inflate(inflater, container, false)

        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        val adapter = SiteDetailsAdapter(
            viewModel,
            onRenameClicked = { gadgetCardUi -> showRenameGadgetDialog(gadgetCardUi) }
        ) { gadget ->
            viewModel.onGadgetClicked(gadget.id)
        }

        binding.gadgetsRecycler.adapter = adapter

        val displayMetrics = resources.displayMetrics
        val dpWidth = displayMetrics.widthPixels / displayMetrics.density
        val spanCount = (dpWidth / 320).toInt().coerceAtLeast(1)
        binding.gadgetsRecycler.layoutManager = GridLayoutManager(requireContext(), spanCount)

        val firstItemTopDp = 12
        val density = resources.displayMetrics.density
        val firstItemTopPx = (firstItemTopDp * density).toInt()

        binding.gadgetsRecycler.addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(
                outRect: Rect,
                view: View,
                parent: RecyclerView,
                state: RecyclerView.State
            ) {
                val pos = parent.getChildAdapterPosition(view)
                if (pos == 0) outRect.top = firstItemTopPx
            }
        })

        viewModel.gadgets.observe(viewLifecycleOwner) {
            adapter.submitList(it)
        }

        setupObservers()

        binding.swipeRefresh.setOnRefreshListener {
            binding.swipeRefresh.isRefreshing = false
            viewModel.onLoad()
        }

        viewModel.onLoad()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar = view.findViewById(R.id.site_details_toolbar)
        toolbar?.let { setupToolbar(it) }

        viewModel.isFilterActive.observe(viewLifecycleOwner) { isActive ->
            updateFilterIcon(isActive)
        }
    }

    private fun setupToolbar(toolbar: MaterialToolbar) {
        toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        toolbar.menu.clear()
        toolbar.menu.add(Menu.NONE, MENU_FILTER_ID, Menu.NONE, R.string.Filter)
            .setIcon(R.drawable.ic_filter)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)

        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                MENU_FILTER_ID -> {
                    showFilterOptionsDialog()
                    true
                }
                else -> false
            }
        }
    }

    private fun updateFilterIcon(isActive: Boolean) {
        toolbar?.menu?.findItem(MENU_FILTER_ID)?.setIcon(
            if (isActive) R.drawable.ic_filter_active else R.drawable.ic_filter
        )
    }

    private fun showFilterOptionsDialog() {
        val isFilterActive = viewModel.isFilterActive.value == true

        val items = if (isFilterActive) {
            arrayOf(
                getString(R.string.Filter_By_Name),
                getString(R.string.Filter_By_Serial_Number),
                getString(R.string.Clear_Filter)
            )
        } else {
            arrayOf(
                getString(R.string.Filter_By_Name),
                getString(R.string.Filter_By_Serial_Number)
            )
        }

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.Filter_Gadgets)
            .setItems(items) { _, which ->
                when {
                    which == 0 -> showNameFilterDialog()
                    which == 1 -> showSerialNumberFilterDialog()
                    isFilterActive && which == 2 -> viewModel.onClearFilter()
                }
            }
            .show()
    }

    private fun showNameFilterDialog() {
        val editText = EditText(requireContext()).apply {
            hint = getString(R.string.Enter_Gadget_Name_Filter)
        }

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.Filter_By_Name)
            .setView(editText)
            .setPositiveButton(R.string.Apply) { _, _ ->
                viewModel.onFilterByName(editText.text.toString())
            }
            .setNegativeButton(R.string.Cancel, null)
            .show()
    }

    private fun showSerialNumberFilterDialog() {
        val editText = EditText(requireContext()).apply {
            hint = getString(R.string.Enter_Serial_Number)
        }

        serialFilterEditText = editText

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(R.string.Filter_By_Serial_Number)
            .setView(editText)
            .setPositiveButton(R.string.Apply, null)
            .setNeutralButton(R.string.Scan_QR, null)
            .setNegativeButton(R.string.Cancel, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                viewModel.onFilterBySerialNumber(editText.text.toString())
                dialog.dismiss()
            }

            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                startQrScanner()
            }
        }

        dialog.setOnDismissListener {
            serialFilterEditText = null
        }

        dialog.show()
    }

    private fun startQrScanner() {
        val options = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()

        val scanner = GmsBarcodeScanning.getClient(requireActivity(), options)

        scanner.startScan()
            .addOnSuccessListener { barcode ->
                if (!isAdded) return@addOnSuccessListener

                val serialNumber = barcode.rawValue?.trim().orEmpty()

                if (serialNumber.isNotEmpty()) {
                    serialFilterEditText?.setText(serialNumber)
                    serialFilterEditText?.setSelection(serialNumber.length)
                } else {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.Invalid_QR_Code),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            .addOnFailureListener {
                if (!isAdded) return@addOnFailureListener

                Toast.makeText(
                    requireContext(),
                    getString(R.string.QR_Scan_Failed),
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun setupObservers() {
        viewModel.eventTransError.observe(viewLifecycleOwner) { errRes ->
            errRes?.let {
                Toast.makeText(requireContext(), getString(it), Toast.LENGTH_LONG).show()
                viewModel.onTransErrorCompleted()
            }
        }

        viewModel.errorServerMessage.observe(viewLifecycleOwner) { msg ->
            msg?.let { Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show() }
        }

        viewModel.errorServerMessageRes.observe(viewLifecycleOwner) { res ->
            res?.let { Toast.makeText(requireContext(), getString(it), Toast.LENGTH_LONG).show() }
        }

        viewModel.navigateToGadget.observe(viewLifecycleOwner) { gadgetId ->
            gadgetId?.let { id ->
                val bundle = Bundle().apply {
                    putString("gadgetId", id)
                }

                findNavController().navigate(
                    R.id.action_siteDetailsFragment_to_gadgetManagerFragment,
                    bundle
                )

                viewModel.onNavigatedToGadget()
            }
        }

        viewModel.showStatusDetails.observe(viewLifecycleOwner) { data ->
            data?.let { (resId, date) ->
                AlertDialog.Builder(requireContext())
                    .setMessage(getString(resId, date))
                    .setPositiveButton(android.R.string.ok, null)
                    .show()

                viewModel.onStatusDetailsShown()
            }
        }

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
                        R.id.action_siteDetailsFragment_to_gadgetLocationFragment,
                        bundle
                    )

                    viewModel.onMapNavigated()
                }
            }
        }

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
                    R.id.action_siteDetailsFragment_to_setGadgetLocationFragment,
                    bundle
                )

                viewModel.onSetGpsNavigated()
            }
        }
    }

    private fun showRenameGadgetDialog(gadget: GadgetCardUi) {
        val editText = EditText(requireContext()).apply {
            hint = getString(R.string.Enter_Gadget_Name)
            setText(gadget.name)
        }

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.Rename_Gadget))
            .setView(editText)
            .setPositiveButton(R.string.Rename) { _, _ ->
                val newName = editText.text.toString().trim()
                if (newName.isNotEmpty() && newName != gadget.name) {
                    viewModel.onRenameGadget(gadget.id, newName)
                }
            }
            .setNegativeButton(R.string.Cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        serialFilterEditText = null
        toolbar = null
        _binding = null
    }
}