package com.example.subletsocial.features

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.subletsocial.databinding.ItemNearbyListingBinding
import com.example.subletsocial.model.Listing
import com.squareup.picasso.Picasso

class NearbyListingsAdapter(private val listings: List<Listing>, private val onItemClick: (Listing) -> Unit) :
    RecyclerView.Adapter<NearbyListingsAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemNearbyListingBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemNearbyListingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val listing = listings[position]
        holder.binding.tvListingTitle.text = listing.title
        holder.binding.tvListingPrice.text = "$${listing.price}/mo"
        
        if (listing.imageUrls.isNotEmpty()) {
            Picasso.get().load(listing.imageUrls[0]).fit().centerCrop().into(holder.binding.ivListingImage)
        }
        
        holder.itemView.setOnClickListener { onItemClick(listing) }
    }

    override fun getItemCount(): Int = listings.size
}
