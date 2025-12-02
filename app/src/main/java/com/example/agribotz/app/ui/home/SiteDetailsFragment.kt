package com.example.agribotz.app.ui.home

import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

        val adapter = SiteDetailsAdapter(viewModel) { gadget ->
            // you can change this to navigate to gadget details later
            viewModel.onGadgetClicked(gadget.id)
        }
        binding.gadgetsRecycler.adapter = adapter

        // layout manager - same responsive logic as sites
        val displayMetrics = resources.displayMetrics
        val dpWidth = displayMetrics.widthPixels / displayMetrics.density
        val spanCount = (dpWidth / 320).toInt().coerceAtLeast(1)
        binding.gadgetsRecycler.layoutManager = GridLayoutManager(requireContext(), spanCount)

        // small top offset for first item like SitesFragment
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

        // swipe refresh: hide native spinner immediately so only custom loader appears
        binding.swipeRefresh.setOnRefreshListener {
            binding.swipeRefresh.isRefreshing = false
            viewModel.onLoad()
        }

        // initial load
        viewModel.onLoad()

        return binding.root
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
            gadgetId?.let {
                // placeholder: show a simple dialog/toast for now
                AlertDialog.Builder(requireContext())
                    .setTitle(R.string.Water_Valves)
                    .setMessage(getString(R.string.Gadgets_Count, 0)) // sample; adjust as desired
                    .setPositiveButton(android.R.string.ok, null)
                    .show()

                viewModel.onNavigatedToGadget()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
