package com.example.agribotz.app.viewholders.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.agribotz.app.ui.home.GadgetCardUi
import com.example.agribotz.app.viewmodels.home.SiteDetailsViewModel
import com.example.agribotz.databinding.ItemSiteDetailsBinding

class SiteDetailsAdapter(
    private val viewModel: SiteDetailsViewModel,
    private val onClick: (GadgetCardUi) -> Unit
) : ListAdapter<GadgetCardUi, SiteDetailsAdapter.GadgetViewHolder>(DiffCallback) {

    companion object DiffCallback : DiffUtil.ItemCallback<GadgetCardUi>() {
        override fun areItemsTheSame(oldItem: GadgetCardUi, newItem: GadgetCardUi): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: GadgetCardUi, newItem: GadgetCardUi): Boolean =
            oldItem == newItem
    }

    inner class GadgetViewHolder(private val binding: ItemSiteDetailsBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(gadget: GadgetCardUi) {
            binding.gadget = gadget
            binding.viewModel = viewModel

            binding.root.setOnClickListener {
                onClick(gadget)
            }

            binding.executePendingBindings()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GadgetViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemSiteDetailsBinding.inflate(inflater, parent, false)
        return GadgetViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GadgetViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}
