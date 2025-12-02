package com.example.agribotz.app.viewholders.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.agribotz.app.ui.home.SiteUi
import com.example.agribotz.app.viewmodels.home.SitesViewModel
import com.example.agribotz.databinding.ItemSiteBinding

class SitesAdapter(
    private val viewModel: SitesViewModel,
    private val onDeleteClicked: (SiteUi) -> Unit,
    private val onRenameClicked: (SiteUi) -> Unit
) : ListAdapter<SiteUi, SitesAdapter.SiteViewHolder>(DiffCallback) {

    companion object DiffCallback : DiffUtil.ItemCallback<SiteUi>() {
        override fun areItemsTheSame(oldItem: SiteUi, newItem: SiteUi): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: SiteUi, newItem: SiteUi): Boolean =
            oldItem == newItem
    }

    inner class SiteViewHolder(private val binding: ItemSiteBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(site: SiteUi) {
            binding.site = site
            binding.viewModel = viewModel

            binding.root.setOnClickListener {
                viewModel.onSiteClicked(site)
            }

            binding.deleteSiteBtn.setOnClickListener {
                onDeleteClicked(site)
            }

            binding.renameSiteBtn.setOnClickListener {
                onRenameClicked(site)
            }

            binding.executePendingBindings()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SiteViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = ItemSiteBinding.inflate(layoutInflater, parent, false)
        return SiteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SiteViewHolder, position: Int) {
        val site = getItem(position)
        holder.bind(site)
    }
}
