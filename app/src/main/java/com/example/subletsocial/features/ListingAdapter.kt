package com.example.subletsocial.features

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.subletsocial.databinding.ListingListItemBinding
import com.example.subletsocial.model.Listing
import com.squareup.picasso.Picasso

class ListingsAdapter(private var listings: List<Listing>) :
    RecyclerView.Adapter<ListingsAdapter.ListingViewHolder>() {

    class ListingViewHolder(val binding: ListingListItemBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListingViewHolder {
        val binding = ListingListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ListingViewHolder(binding)
    }

    override fun getItemCount(): Int = listings.size

    override fun onBindViewHolder(holder: ListingViewHolder, position: Int) {
        val listing = listings[position]
        holder.binding.tvListingTitle.text = listing.title
        holder.binding.tvListingLocation.text = listing.locationName
        holder.binding.tvListingPrice.text = "$${listing.price}"

        holder.binding.tvListingDates.text = "${listing.startDate} - ${listing.endDate}"
        holder.binding.tvListingRooms.text = "${listing.bedrooms} bed • ${listing.bathrooms} bath"

        if (listing.imageUrls.isNotEmpty() && listing.imageUrls[0].isNotEmpty()) {
            Picasso.get().load(listing.imageUrls[0]).fit().centerCrop().into(holder.binding.ivListingImage)
        } else {
            holder.binding.ivListingImage.setImageResource(android.R.drawable.ic_menu_gallery)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateListings(newListings: List<Listing>) {
        listings = newListings
        notifyDataSetChanged()
    }
}