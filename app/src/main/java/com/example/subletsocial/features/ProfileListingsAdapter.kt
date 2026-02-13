package com.example.subletsocial.features

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.subletsocial.R
import com.example.subletsocial.model.Listing

class ProfileListingsAdapter(private var listings: List<Listing>) :
    RecyclerView.Adapter<ProfileListingsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.tvListingTitle)
        val price: TextView = view.findViewById(R.id.tvListingPrice)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_profile_listing, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val listing = listings[position]
        holder.title.text = listing.title
        holder.price.text = "$${listing.price}"
    }

    override fun getItemCount() = listings.size

    fun updateListings(newListings: List<Listing>) {
        listings = newListings
        notifyDataSetChanged()
    }
}