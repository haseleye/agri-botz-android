package com.example.agribotz.app.ui.home

import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.agribotz.R
import com.example.agribotz.app.repository.Repository
import com.example.agribotz.app.util.PreferencesManager
import com.example.agribotz.app.viewholders.home.SiteDetailsAdapter
import com.example.agribotz.app.viewmodels.home.SiteDetailsViewModel
import com.example.agribotz.app.viewmodels.home.SiteDetailsViewModelFactory
import com.example.agribotz.databinding.FragmentSiteDetailsBinding
import androidx.navigation.fragment.findNavController

class SiteDetailsFragment : Fragment() {

    private var _binding: FragmentSiteDetailsBinding? = null
    private val binding get() = _binding!!

    // siteId is passed as nav arg "siteId"
    private val siteId: String by lazy { arguments?.getString("siteId") ?: "" }

    private val viewModel: SiteDetailsViewModel by viewModels {
        SiteDetailsViewModelFactory(Repository(), PreferencesManager(requireContext()), siteId)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSiteDetailsBinding.inflate(inflater, container, false)

        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        val adapter = SiteDetailsAdapter(viewModel, onRenameClicked = { gadgetCardUi -> showRenameGadgetDialog(gadgetCardUi) }) { gadget ->
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
            override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
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

        val toolbar = view.findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.site_details_toolbar)
        toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
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

    fun showRenameGadgetDialog(gadget: GadgetCardUi) {
        val editText = EditText(requireContext()).apply {
            hint = getString(R.string.Enter_Gadget_Name)
            setText(gadget.name) // prefill current name
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
        _binding = null
    }
}
