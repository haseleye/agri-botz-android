package com.example.agribotz.app.ui.home

import android.graphics.Rect
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.agribotz.R
import com.example.agribotz.app.repository.Repository
import com.example.agribotz.app.util.PreferencesManager
import com.example.agribotz.app.viewholders.home.SitesAdapter
import com.example.agribotz.app.viewmodels.home.SitesViewModel
import com.example.agribotz.app.viewmodels.home.SitesViewModelFactory
import com.example.agribotz.databinding.FragmentSitesBinding
import kotlin.getValue

class SitesFragment : Fragment() {
    private var _binding: FragmentSitesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SitesViewModel by viewModels {
        SitesViewModelFactory(Repository(), PreferencesManager(requireContext()))
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSitesBinding.inflate(inflater, container, false)

        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        val adapter = SitesAdapter(viewModel,
            onDeleteClicked = { site -> showDeleteSiteDialog(site) },
            onRenameClicked = { site -> showRenameSiteDialog(site) }
        )
        binding.sitesRecycler.adapter = adapter

        // Dynamically calculate span count based on width (e.g., 600dp per card)
        val displayMetrics = resources.displayMetrics
        val dpWidth = displayMetrics.widthPixels / displayMetrics.density
        val spanCount = (dpWidth / 320).toInt().coerceAtLeast(1) // ~320dp per card
        binding.sitesRecycler.layoutManager = GridLayoutManager(requireContext(), spanCount)

        // Push the first card down to avoid overlaying with the title bar
        val firstItemTopDp = 16
        val density = resources.displayMetrics.density
        val firstItemTopPx = (firstItemTopDp * density).toInt()

        binding.sitesRecycler.addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(
                outRect: Rect,
                view: View,
                parent: RecyclerView,
                state: RecyclerView.State
            ) {
                val pos = parent.getChildAdapterPosition(view)
                if (pos == 0) {
                    outRect.top = firstItemTopPx
                }
            }
        })

        viewModel.sites.observe(viewLifecycleOwner) {
            adapter.submitList(it)
        }

        setupObservers()

        // Pull-to-refresh action
        binding.swipeRefresh.setOnRefreshListener {
            binding.swipeRefresh.isRefreshing = false
            viewModel.onLoad()
        }

        binding.addSiteFab.setOnClickListener {
            showAddSiteDialog()
        }

        return binding.root
    }

    private fun setupObservers() {
        // Observe navigation to site details
        viewModel.navigateToSite.observe(viewLifecycleOwner) { siteId ->
            siteId?.let {
                val action = SitesFragmentDirections.actionSitesFragmentToSiteDetailsFragment(siteId)
                findNavController().navigate(action)
                viewModel.onNavigated()
            }
        }

        // Show event error messages
        viewModel.eventTransError.observe(viewLifecycleOwner) { errorResId ->
            errorResId?.let {
                Toast.makeText(requireContext(), getString(it), Toast.LENGTH_LONG).show()
                viewModel.onTransErrorCompleted()
            }
        }

        // Show dynamic server error messages
        viewModel.errorServerMessage.observe(viewLifecycleOwner) { errorMsg ->
            errorMsg?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
            }
        }

        // Show Res server error messages
        viewModel.errorServerMessageRes.observe(viewLifecycleOwner) { resId ->
            resId?.let {
                Toast.makeText(requireContext(), getString(it), Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showAddSiteDialog() {
        val context = requireContext()
        val input = android.widget.EditText(context).apply {
            hint = getString(R.string.Enter_Site_Name)
            setPadding(32, 32, 32, 32)
        }

        androidx.appcompat.app.AlertDialog.Builder(context)
            .setTitle(R.string.Add_Site)
            .setView(input)
            .setPositiveButton(R.string.Save) { dialog, _ ->
                val siteName = input.text.toString().trim()
                if (siteName.isNotEmpty()) {
                    viewModel.onAddSite(siteName)
                }
                dialog.dismiss()
            }
            .setNegativeButton(R.string.Cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun showDeleteSiteDialog(site: SiteUi) {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.Delete_Site))
            .setMessage(getString(R.string.Confirmation))
            .setPositiveButton(R.string.Yes) { _, _ ->
                viewModel.onDeleteSiteClicked(site.toDomain())
            }
            .setNegativeButton(R.string.No, null)
            .show()
    }

    fun showRenameSiteDialog(site: SiteUi) {
        val editText = EditText(requireContext()).apply {
            hint = getString(R.string.Enter_Site_Name)
            setText(site.name) // prefill current name
        }

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.Rename_Site))
            .setView(editText)
            .setPositiveButton(R.string.Rename) { _, _ ->
                val newName = editText.text.toString().trim()
                if (newName.isNotEmpty() && newName != site.name) {
                    viewModel.onRenameSite(site.id, newName)
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