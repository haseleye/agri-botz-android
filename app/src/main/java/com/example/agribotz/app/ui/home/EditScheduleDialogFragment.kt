package com.example.agribotz.app.ui.home

import android.app.DatePickerDialog
import android.app.Dialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.example.agribotz.R
import com.example.agribotz.app.domain.ScheduleRepeatMode
import com.example.agribotz.app.viewmodels.home.EditScheduleViewModel
import com.example.agribotz.app.viewmodels.home.EditScheduleViewModelFactory
import com.example.agribotz.databinding.DialogEditScheduleBinding
import java.util.Calendar
import java.util.Locale

class EditScheduleDialogFragment : DialogFragment() {

    private var scheduleIndex: Int = -1
    private var schedule: ScheduleUi? = null

    private var _binding: DialogEditScheduleBinding? = null
    private val binding get() = _binding!!

    private val viewModel: EditScheduleViewModel by viewModels {
        EditScheduleViewModelFactory(schedule)
    }

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

        scheduleIndex = requireArguments().getInt("scheduleIndex")
        schedule = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            requireArguments().getParcelable("schedule", ScheduleUi::class.java)
        } else {
            @Suppress("DEPRECATION")
            requireArguments().getParcelable("schedule")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogEditScheduleBinding.inflate(inflater, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupDropdowns()
        setupDateTimeInputs()
        setupWeekDayChips()
        setupEndRecurrence()
        setupDropdownClicks()
        setupActions()
    }

    private fun setupDropdowns() {
        // Duration -> Hours
        val hoursItems = (0..23).map { it.toString().padStart(2, '0') }
        binding.actDurationHours.setAdapter(createNoFilterAdapter(hoursItems))
        binding.actDurationHours.setText(viewModel.durationHours.value ?: "00", false)
        binding.actDurationHours.setOnItemClickListener { _, _, position, _ ->
            viewModel.setDurationHours(hoursItems[position])
        }

        // Duration -> Minutes
        val minuteItems = (0..59).map { it.toString().padStart(2, '0') }
        binding.actDurationMinutes.setAdapter(createNoFilterAdapter(minuteItems))
        binding.actDurationMinutes.setText(viewModel.durationMinutes.value ?: "", false)
        binding.actDurationMinutes.setOnItemClickListener { _, _, position, _ ->
            viewModel.setDurationMinutes(minuteItems[position])
        }

        // Duration -> Seconds
        val secondItems = (0..59).map { it.toString().padStart(2, '0') }
        binding.actDurationSeconds.setAdapter(createNoFilterAdapter(secondItems))
        binding.actDurationSeconds.setText(viewModel.durationSeconds.value ?: "", false)
        binding.actDurationSeconds.setOnItemClickListener { _, _, position, _ ->
            viewModel.setDurationSeconds(secondItems[position])
        }

        // Repeat Every (resource-based)
        val repeatModes = resources.getStringArray(R.array.schedule_repeat_modes).toList()
        binding.actRepeatMode.setAdapter(createNoFilterAdapter(repeatModes))

        // localize the initial value using resources (no Context in ViewModel)
        val initialMode = ScheduleRepeatMode.fromScheduleUi(schedule)
        val initialPosition = when (initialMode) {
            ScheduleRepeatMode.NONE -> 0
            ScheduleRepeatMode.HOUR -> 1
            ScheduleRepeatMode.DAY -> 2
            ScheduleRepeatMode.WEEK -> 3
            ScheduleRepeatMode.MONTH -> 4
            ScheduleRepeatMode.YEAR -> 5
        }

        // Update BOTH UI + ViewModel label (localized)
        viewModel.setRepeatMode(initialMode, repeatModes[initialPosition])
        binding.actRepeatMode.setText(repeatModes[initialPosition], false)

        binding.actRepeatMode.setOnItemClickListener { _, _, position, _ ->
            val mode = when (position) {
                1 -> ScheduleRepeatMode.HOUR
                2 -> ScheduleRepeatMode.DAY
                3 -> ScheduleRepeatMode.WEEK
                4 -> ScheduleRepeatMode.MONTH
                5 -> ScheduleRepeatMode.YEAR
                else -> ScheduleRepeatMode.NONE
            }
            viewModel.setRepeatMode(mode, repeatModes[position])
        }

        // Month day and year day (resource-based ordinals)
        val dayItems = resources.getStringArray(R.array.month_day_ordinals).toList()
        val rawDayItems = (1..31).map { it.toString() }

        binding.actMonthDay.setAdapter(createNoFilterAdapter(dayItems))
        binding.actYearDay.setAdapter(createNoFilterAdapter(dayItems))

        viewModel.monthDay.value?.toIntOrNull()?.let { day ->
            val idx = day - 1
            if (idx in dayItems.indices) binding.actMonthDay.setText(dayItems[idx], false)
        }

        viewModel.yearDay.value?.toIntOrNull()?.let { day ->
            val idx = day - 1
            if (idx in dayItems.indices) binding.actYearDay.setText(dayItems[idx], false)
        }

        binding.actMonthDay.setOnItemClickListener { _, _, position, _ ->
            viewModel.setMonthDay(rawDayItems[position])
            binding.actMonthDay.setText(rawDayItems[position], false)
            binding.actMonthDay.dismissDropDown()
            binding.actMonthDay.clearFocus()
        }

        binding.actYearDay.setOnItemClickListener { _, _, position, _ ->
            viewModel.setYearDay(rawDayItems[position])
            binding.actYearDay.setText(rawDayItems[position], false)
            binding.actYearDay.dismissDropDown()
            binding.actYearDay.clearFocus()
        }

        // Month list (resource-based display, backend keys stay fixed)
        val monthsBackend = listOf(
            "Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
        )
        val months = resources.getStringArray(R.array.months_list).toList()

        binding.actYearMonth.setAdapter(createNoFilterAdapter(months))

        // Show localized month name initially
        val initialMonthBackend = viewModel.yearMonthBackend.value ?: "Jan"
        val initialMonthIndex = monthsBackend.indexOf(initialMonthBackend).takeIf { it >= 0 } ?: 0
        viewModel.setYearMonth(initialMonthBackend, months[initialMonthIndex])
        binding.actYearMonth.setText(months[initialMonthIndex], false)

        binding.actYearMonth.setOnItemClickListener { _, _, position, _ ->
            viewModel.setYearMonth(monthsBackend[position], months[position])
        }
    }

    private fun createNoFilterAdapter(items: List<String>): ArrayAdapter<String> {
        return object : ArrayAdapter<String>(
            requireContext(),
            android.R.layout.simple_list_item_1,
            items.toMutableList()
        ) {
            private val allItems = items.toList()

            override fun getFilter(): Filter {
                return object : Filter() {
                    override fun performFiltering(constraint: CharSequence?): FilterResults {
                        return FilterResults().apply {
                            values = allItems
                            count = allItems.size
                        }
                    }

                    override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                        clear()
                        addAll(allItems)
                        notifyDataSetChanged()
                    }

                    override fun convertResultToString(resultValue: Any?): CharSequence {
                        return resultValue as? String ?: ""
                    }
                }
            }
        }
    }

    private fun setupDateTimeInputs() {
        binding.etStartDate.setOnClickListener {
            showDatePicker { formatted -> viewModel.setStartDate(formatted) }
        }

        binding.tilStartDate.setEndIconOnClickListener {
            showDatePicker { formatted -> viewModel.setStartDate(formatted) }
        }

        binding.tvStartTime.setOnClickListener {
            showTimePicker { formatted -> viewModel.setStartTime(formatted) }
        }

        binding.etEndDate.setOnClickListener {
            showDatePicker { formatted -> viewModel.setEndDate(formatted) }
        }

        binding.tilEndDate.setEndIconOnClickListener {
            showDatePicker { formatted -> viewModel.setEndDate(formatted) }
        }

        binding.tvEndTime.setOnClickListener {
            showTimePicker { formatted -> viewModel.setEndTime(formatted) }
        }
    }

    private fun setupWeekDayChips() {
        val chips: Map<String, TextView> = mapOf(
            "sun" to binding.chipSun,
            "mon" to binding.chipMon,
            "tue" to binding.chipTue,
            "wed" to binding.chipWed,
            "thu" to binding.chipThu,
            "fri" to binding.chipFri,
            "sat" to binding.chipSat
        )

        chips.forEach { (key, tv) ->
            tv.isClickable = true
            tv.isFocusable = true
            tv.setOnClickListener { viewModel.toggleWeekDay(key) }
        }

        viewModel.selectedWeekDays.observe(viewLifecycleOwner) { selected ->
            chips.forEach { (key, tv) ->
                applyDayChipStyle(tv, selected.contains(key))
            }
        }
    }

    private fun applyDayChipStyle(tv: TextView, selected: Boolean) {
        tv.setBackgroundResource(
            if (selected) R.drawable.bg_chip_selected else R.drawable.bg_chip_unselected
        )
        tv.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                if (selected) R.color.colorOnPrimary else R.color.colorPrimary
            )
        )
    }

    private fun setupEndRecurrence() {
        viewModel.endRecurrenceOn.observe(viewLifecycleOwner) { enabled ->
            if (enabled == true) {
                binding.rbOn.isChecked = true
            } else {
                binding.rbNever.isChecked = true
            }
        }

        binding.rbNever.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) viewModel.setEndRecurrenceOn(false)
        }

        binding.rbOn.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) viewModel.setEndRecurrenceOn(true)
        }
    }

    private fun setupDropdownClicks() {
        listOf(
            binding.actDurationHours,
            binding.actDurationMinutes,
            binding.actDurationSeconds,
            binding.actRepeatMode,
            binding.actMonthDay,
            binding.actYearDay,
            binding.actYearMonth
        ).forEach { view ->
            view.threshold = 0

            view.setOnClickListener {
                val currentText = view.text?.toString().orEmpty()
                view.setText("", false)

                view.post {
                    view.showDropDown()
                    view.setText(currentText, false)
                    view.setSelection(view.text?.length ?: 0)
                }
            }

            view.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    val currentText = view.text?.toString().orEmpty()
                    view.setText("", false)

                    view.post {
                        view.showDropDown()
                        view.setText(currentText, false)
                        view.setSelection(view.text?.length ?: 0)
                    }
                }
            }
        }
    }

    private fun setupActions() {
        binding.btnCancel.setOnClickListener { dismiss() }

        binding.btnSave.setOnClickListener {
            // TODO:
            // Build the final ScheduleValue / payload here,
            // then send it back to GadgetManager screen using FragmentResult or callback.
            dismiss()
        }
    }

    private fun showDatePicker(onSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()

        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val value = String.format(
                    Locale.US,
                    "%02d / %02d / %04d",
                    dayOfMonth,
                    month + 1,
                    year
                )
                onSelected(value)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun showTimePicker(onSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()

        TimePickerDialog(
            requireContext(),
            { _, hourOfDay, minute ->
                val value = String.format(Locale.US, "%02d : %02d", hourOfDay, minute)
                onSelected(value)
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        ).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
