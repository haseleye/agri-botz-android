package com.example.agribotz.app.ui.home

import android.app.Dialog
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
import com.example.agribotz.app.viewmodels.home.EditWakeupRateViewModel
import com.example.agribotz.app.viewmodels.home.EditWakeupRateViewModelFactory
import com.example.agribotz.databinding.DialogEditWakeupRateBinding
import com.google.android.material.snackbar.Snackbar
import java.util.Locale

class EditWakeupRateDialogFragment : DialogFragment() {

    private var variableId: String? = null
    private var currentWakeupRate: Float = 0f

    private var _binding: DialogEditWakeupRateBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: EditWakeupRateViewModel

    private var wakeupRates: List<String> = emptyList()

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
        currentWakeupRate = requireArguments().getFloat("currentWakeupRate", 0f)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogEditWakeupRateBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner

        val app = requireActivity().application
        val prefManager = PreferencesManager(app)

        val factory = EditWakeupRateViewModelFactory(
            prefManager = prefManager,
            currentWakeupRate = currentWakeupRate
        )

        viewModel = ViewModelProvider(this, factory)[EditWakeupRateViewModel::class.java]
        binding.viewModel = viewModel

        variableId?.let { viewModel.setVariableId(it) }

        setupObservers()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupWakeupRates()
        setupDropdown()
        setupActions()

        viewModel.dismissDialog.observe(viewLifecycleOwner) { shouldDismiss ->
            if (shouldDismiss == true) {
                parentFragmentManager.setFragmentResult(
                    "edit_wakeup_rate_result",
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

    private fun setupWakeupRates() {
        wakeupRates = resources.getStringArray(R.array.wakeup_refresh_list).toList()
    }

    private fun setupDropdown() {
        binding.actWakeupRate.threshold = 0
        binding.actWakeupRate.setAdapter(createFilterAdapter(wakeupRates))

        val initialIndex = currentWakeupRate.toInt() - 1
        if (initialIndex in wakeupRates.indices) {
            val initialDisplayValue = wakeupRates[initialIndex]

            binding.actWakeupRate.setText(initialDisplayValue, false)
            viewModel.setWakeupRate(
                displayValue = initialDisplayValue,
                backendValue = currentWakeupRate
            )
        }

        binding.actWakeupRate.setOnClickListener {
            binding.actWakeupRate.showDropDown()
        }

        binding.actWakeupRate.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.actWakeupRate.showDropDown()
            }
        }

        binding.actWakeupRate.setOnItemClickListener { parent, _, position, _ ->
            val selectedDisplay = parent.getItemAtPosition(position) as? String ?: return@setOnItemClickListener
            val selectedBackend = resolveBackendValue(selectedDisplay) ?: return@setOnItemClickListener

            viewModel.setWakeupRate(
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
                                matchesWakeupRateSearch(it, query)
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

    private fun matchesWakeupRateSearch(value: String, query: String): Boolean {
        val cleanQuery = query.trim().lowercase(Locale.ROOT)
        if (cleanQuery.isEmpty()) return true

        return value.lowercase(Locale.ROOT).contains(cleanQuery) ||
                extractNumber(value)?.toString() == cleanQuery
    }

    private fun extractNumber(value: String): Int? {
        return Regex("\\d+")
            .find(value)
            ?.value
            ?.toIntOrNull()
    }

    private fun resolveBackendValue(input: String): Float? {
        val trimmed = input.trim()
        if (trimmed.isBlank()) return null

        val index = wakeupRates.indexOf(trimmed)
        if (index in wakeupRates.indices) {
            return (index + 1).toFloat()
        }

        val number = extractNumber(trimmed)
        if (number != null && number in 1..24) {
            return number.toFloat()
        }

        return null
    }

    private fun setupActions() {
        binding.btnCancel.setOnClickListener { dismiss() }

        binding.btnSave.setOnClickListener {
            val selectedText = binding.actWakeupRate.text?.toString().orEmpty()
            val backendValue = resolveBackendValue(selectedText)

            if (backendValue == null) {
                Snackbar.make(
                    binding.root,
                    getString(R.string.Invalid_Wakeup_Rate),
                    Snackbar.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            viewModel.saveWakeupRate(backendValue)
        }
    }

    private fun observeApiStatus() {
        viewModel.apiStatus.observe(viewLifecycleOwner) { status ->
            val isLoading = status == ApiStatus.LOADING

            binding.btnSave.isEnabled = !isLoading
            binding.btnCancel.isEnabled = !isLoading
            binding.actWakeupRate.isEnabled = !isLoading

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