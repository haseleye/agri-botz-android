package com.example.agribotz.app.ui.home

import android.app.Dialog
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.example.agribotz.R
import com.example.agribotz.app.domain.ApiStatus
import com.example.agribotz.app.util.PreferencesManager
import com.example.agribotz.app.viewmodels.home.EditGmtViewModel
import com.example.agribotz.app.viewmodels.home.EditGmtViewModelFactory
import com.example.agribotz.databinding.DialogEditGmtBinding
import com.google.android.material.snackbar.Snackbar
import java.util.Locale

class EditGmtDialogFragment : DialogFragment() {

    private var variableId: String? = null
    private var currentGmt: String? = null

    private var _binding: DialogEditGmtBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: EditGmtViewModel

    private var displayTimeZones: List<String> = emptyList()
    private var backendTimeZones: List<String> = emptyList()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).apply {
            window?.setBackgroundDrawableResource(android.R.color.transparent)
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.92f).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        variableId = requireArguments().getString("variableId")
        currentGmt = requireArguments().getString("currentGmt")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogEditGmtBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner

        val app = requireActivity().application
        val prefManager = PreferencesManager(app)

        val factory = EditGmtViewModelFactory(
            prefManager = prefManager,
            currentGmt = currentGmt
        )

        viewModel = ViewModelProvider(this, factory)[EditGmtViewModel::class.java]
        binding.viewModel = viewModel

        variableId?.let { viewModel.setVariableId(it) }

        setupObservers()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupTimeZones()
        setupDropdown()
        setupActions()

        viewModel.dismissDialog.observe(viewLifecycleOwner) { shouldDismiss ->
            if (shouldDismiss == true) {
                parentFragmentManager.setFragmentResult(
                    "edit_gmt_result",
                    Bundle()
                )
                dismiss()
                viewModel.onDismissConsumed()
            }
        }
    }

    private fun setupObservers() {
        observeApiStatus()
        observeServerErrors()
        observeTransactionError()
    }

    private fun setupTimeZones() {
        displayTimeZones = resources.getStringArray(R.array.gmt_cities_list).toList()

        val englishConfiguration = Configuration(resources.configuration).apply {
            setLocale(Locale.ENGLISH)
        }

        val englishResources = requireContext()
            .createConfigurationContext(englishConfiguration)
            .resources

        backendTimeZones = englishResources.getStringArray(R.array.gmt_cities_list).toList()
    }

    private fun setupDropdown() {
        binding.actGmtZone.threshold = 0
        binding.actGmtZone.setAdapter(createFilterAdapter(displayTimeZones))

        val initialBackendValue = currentGmt.orEmpty()
        val initialDisplayValue = displayValueForBackend(initialBackendValue)

        if (initialDisplayValue.isNotBlank()) {
            binding.actGmtZone.setText(initialDisplayValue, false)
            viewModel.setTimeZone(
                displayValue = initialDisplayValue,
                backendValue = initialBackendValue
            )
        }

        binding.actGmtZone.setOnClickListener {
            binding.actGmtZone.showDropDown()
        }

        binding.actGmtZone.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.actGmtZone.showDropDown()
            }
        }

        binding.actGmtZone.setOnItemClickListener { parent, _, position, _ ->
            val selectedDisplay = parent.getItemAtPosition(position) as? String ?: return@setOnItemClickListener
            val selectedBackend = backendValueForDisplay(selectedDisplay) ?: selectedDisplay

            viewModel.setTimeZone(
                displayValue = selectedDisplay,
                backendValue = selectedBackend
            )
        }
    }

    private fun createFilterAdapter(items: List<String>): ArrayAdapter<String> {
        return object : ArrayAdapter<String>(
            requireContext(),
            android.R.layout.simple_list_item_1,
            items.toMutableList()
        ) {
            private val originalItems = items.toList()

            override fun getFilter(): Filter {
                return object : Filter() {
                    override fun performFiltering(constraint: CharSequence?): FilterResults {
                        val query = constraint?.toString().orEmpty()

                        val filteredItems = if (query.isBlank()) {
                            originalItems
                        } else {
                            originalItems.filter {
                                matchesTimeZoneSearch(it, query)
                            }
                        }

                        return FilterResults().apply {
                            values = filteredItems
                            count = filteredItems.size
                        }
                    }

                    @Suppress("UNCHECKED_CAST")
                    override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                        clear()

                        val filteredItems = results?.values as? List<String> ?: emptyList()
                        addAll(filteredItems)

                        if (filteredItems.isNotEmpty()) {
                            notifyDataSetChanged()
                        } else {
                            notifyDataSetInvalidated()
                        }
                    }
                }
            }
        }
    }

    private fun matchesTimeZoneSearch(timeZoneId: String, query: String): Boolean {
        val cleanQuery = query.trim().lowercase(Locale.ROOT)
        if (cleanQuery.isEmpty()) return true

        val fullZoneName = timeZoneId
            .replace("_", " ")
            .lowercase(Locale.ROOT)

        val cityName = timeZoneId
            .substringAfterLast("/")
            .replace("_", " ")
            .lowercase(Locale.ROOT)

        return fullZoneName.contains(cleanQuery) ||
                cityName.contains(cleanQuery)
    }

    private fun displayValueForBackend(backendValue: String): String {
        val index = backendTimeZones.indexOf(backendValue)
        return if (index in displayTimeZones.indices) {
            displayTimeZones[index]
        } else {
            backendValue
        }
    }

    private fun backendValueForDisplay(displayValue: String): String? {
        val index = displayTimeZones.indexOf(displayValue)
        return if (index in backendTimeZones.indices) {
            backendTimeZones[index]
        } else {
            null
        }
    }

    private fun resolveBackendValue(input: String): String? {
        val trimmed = input.trim()
        if (trimmed.isBlank()) return null

        backendValueForDisplay(trimmed)?.let { return it }

        backendTimeZones.firstOrNull {
            it.equals(trimmed, ignoreCase = true)
        }?.let { return it }

        val displayIndexByCityName = displayTimeZones.indexOfFirst {
            it.substringAfterLast("/")
                .replace("_", " ")
                .equals(trimmed, ignoreCase = true)
        }

        if (displayIndexByCityName in backendTimeZones.indices) {
            return backendTimeZones[displayIndexByCityName]
        }

        return backendTimeZones.firstOrNull {
            it.substringAfterLast("/")
                .replace("_", " ")
                .equals(trimmed, ignoreCase = true)
        }
    }

    private fun setupActions() {
        binding.btnCancel.setOnClickListener { dismiss() }

        binding.btnSave.setOnClickListener {
            val selectedText = binding.actGmtZone.text?.toString().orEmpty()
            val backendValue = resolveBackendValue(selectedText)

            if (backendValue.isNullOrBlank()) {
                Snackbar.make(
                    binding.root,
                    getString(R.string.Invalid_GMT_Zone),
                    Snackbar.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            viewModel.saveGmtZone(backendValue)
        }
    }

    private fun observeApiStatus() {
        viewModel.apiStatus.observe(viewLifecycleOwner) { status ->
            val isLoading = status == ApiStatus.LOADING

            binding.btnSave.isEnabled = !isLoading
            binding.btnCancel.isEnabled = !isLoading
            binding.actGmtZone.isEnabled = !isLoading

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

    private fun observeTransactionError() {
        viewModel.eventTransError.observe(viewLifecycleOwner) { resId ->
            if (resId != null && resId != 0) {
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