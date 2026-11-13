package com.example.agribotz.app.ui.home

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import com.example.agribotz.R
import com.example.agribotz.databinding.DialogEditGadgetBinding
import com.google.android.material.snackbar.Snackbar

class EditGadgetDialogFragment : DialogFragment() {

    companion object {
        const val RESULT_KEY = "edit_gadget_result"
        const val RESULT_ACTION = "action"
        const val ACTION_RENAME = "rename"
        const val ACTION_UPDATE_CROP = "update_crop"
        const val RESULT_GADGET_ID = "gadgetId"
        const val RESULT_NAME = "name"
        const val RESULT_CROP_NAME = "cropName"
        const val RESULT_GROWTH_STAGE = "growthStage"

        private val GROWTH_STAGE_RES_IDS = listOf(
            R.string.Growth_Stage_0,
            R.string.Growth_Stage_1,
            R.string.Growth_Stage_2,
            R.string.Growth_Stage_3,
            R.string.Growth_Stage_4,
            R.string.Growth_Stage_5,
            R.string.Growth_Stage_6
        )

        fun newInstance(gadget: GadgetCardUi): EditGadgetDialogFragment {
            return EditGadgetDialogFragment().apply {
                arguments = Bundle().apply {
                    putString("gadgetId", gadget.id)
                    putString("gadgetName", gadget.name)
                    putString("cropName", gadget.cropName)
                    putInt("growthStage", gadget.growthStage ?: -1)
                }
            }
        }
    }

    private var gadgetId: String = ""
    private var gadgetName: String = ""
    private var currentCropName: String? = null
    private var currentGrowthStage: Int = -1

    private var selectedGrowthStage: Int = -1

    private var _binding: DialogEditGadgetBinding? = null
    private val binding get() = _binding!!

    private lateinit var growthStageNames: List<String>

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

        gadgetId = requireArguments().getString("gadgetId").orEmpty()
        gadgetName = requireArguments().getString("gadgetName").orEmpty()
        currentCropName = requireArguments().getString("cropName")
        currentGrowthStage = requireArguments().getInt("growthStage", -1)

        selectedGrowthStage = savedInstanceState?.getInt("selectedGrowthStage", currentGrowthStage)
            ?: currentGrowthStage
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogEditGadgetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        growthStageNames = GROWTH_STAGE_RES_IDS.map { getString(it) }

        if (savedInstanceState == null) {
            binding.etGadgetName.setText(gadgetName)
            binding.etCropName.setText(currentCropName.orEmpty())
        }

        setupDropdown()
        setupActions()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("selectedGrowthStage", selectedGrowthStage)
    }

    private fun setupDropdown() {
        binding.actGrowthStage.setAdapter(createDropdownAdapter(growthStageNames))

        if (selectedGrowthStage in growthStageNames.indices) {
            binding.actGrowthStage.setText(growthStageNames[selectedGrowthStage], false)
        }

        binding.actGrowthStage.setOnClickListener {
            binding.actGrowthStage.showDropDown()
        }

        binding.actGrowthStage.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.actGrowthStage.showDropDown()
            }
        }

        binding.actGrowthStage.setOnItemClickListener { _, _, position, _ ->
            selectedGrowthStage = position
        }
    }

    // The list is a fixed choice, so it is never filtered by the currently selected text
    private fun createDropdownAdapter(items: List<String>): ArrayAdapter<String> {
        return object : ArrayAdapter<String>(
            requireContext(),
            android.R.layout.simple_list_item_1,
            items
        ) {
            override fun getFilter(): Filter {
                return object : Filter() {
                    override fun performFiltering(constraint: CharSequence?): FilterResults {
                        return FilterResults().apply {
                            values = items
                            count = items.size
                        }
                    }

                    override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                        notifyDataSetChanged()
                    }
                }
            }
        }
    }

    private fun setupActions() {
        binding.btnCancel.setOnClickListener { dismiss() }

        binding.btnRename.setOnClickListener {
            val newName = binding.etGadgetName.text.toString().trim()

            if (newName.isNotEmpty() && newName != gadgetName) {
                parentFragmentManager.setFragmentResult(
                    RESULT_KEY,
                    bundleOf(
                        RESULT_ACTION to ACTION_RENAME,
                        RESULT_GADGET_ID to gadgetId,
                        RESULT_NAME to newName
                    )
                )
            }

            dismiss()
        }

        binding.btnUpdateCrop.setOnClickListener {
            val cropName = binding.etCropName.text.toString().trim()

            if (cropName.isEmpty() || selectedGrowthStage !in growthStageNames.indices) {
                Snackbar.make(
                    binding.root,
                    getString(R.string.Error_Something_Wrong),
                    Snackbar.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            parentFragmentManager.setFragmentResult(
                RESULT_KEY,
                bundleOf(
                    RESULT_ACTION to ACTION_UPDATE_CROP,
                    RESULT_GADGET_ID to gadgetId,
                    RESULT_CROP_NAME to cropName,
                    RESULT_GROWTH_STAGE to selectedGrowthStage
                )
            )

            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
