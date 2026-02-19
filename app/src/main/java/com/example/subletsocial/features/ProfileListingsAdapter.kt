package com.example.subletsocial.features

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.example.subletsocial.R
import com.example.subletsocial.model.Listing
import com.squareup.picasso.Picasso

class ProfileListingsAdapter(private var listings: List<Listing>) :
    RecyclerView.Adapter<ProfileListingsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val listingImage: ImageView = view.findViewById(R.id.ivListingImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_profile_listing, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val listing = listings[position]
        if (listing.imageUrls.isNotEmpty() && listing.imageUrls[0].isNotEmpty()) {
            Picasso.get()
                .load(listing.imageUrls[0])
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_gallery)
                .into(holder.listingImage)
        } else {
            holder.listingImage.setImageResource(android.R.drawable.ic_menu_gallery)
        }

        holder.itemView.setOnClickListener {
            val action = ProfileFragmentDirections.actionProfileFragmentToSinglePostFragment(listing.id)
            holder.itemView.findNavController().navigate(action)
        }
    }

    override fun getItemCount() = listings.size

    fun updateListings(newListings: List<Listing>) {
        listings = newListings
        notifyDataSetChanged()
    }
}