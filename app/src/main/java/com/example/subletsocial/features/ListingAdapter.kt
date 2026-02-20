package com.example.subletsocial.features

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.subletsocial.databinding.ListingListItemBinding
import com.example.subletsocial.model.Listing
import com.google.firebase.auth.FirebaseAuth
import com.squareup.picasso.Picasso
import java.util.Locale

class ListingAdapter(private var listings: List<Listing>, private val onItemClicked: (String) -> Unit) :
    RecyclerView.Adapter<ListingAdapter.ListingViewHolder>() {

    private var currentCurrency: String = "USD"
    private var exchangeRate: Double = 1.0

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
        
        val convertedPrice = listing.price * exchangeRate
        holder.binding.tvListingPrice.text = String.format(Locale.US, "%.2f %s", convertedPrice, currentCurrency)

        holder.binding.tvListingDates.text = "${listing.startDate} - ${listing.endDate}"
        holder.binding.tvListingRooms.text = "${listing.bedrooms} bed • ${listing.bathrooms} bath"

        if (listing.imageUrls.isNotEmpty() && listing.imageUrls[0].isNotEmpty()) {
            Picasso.get().load(listing.imageUrls[0]).fit().centerCrop().into(holder.binding.ivListingImage)
        } else {
            holder.binding.ivListingImage.setImageResource(android.R.drawable.ic_menu_gallery)
        }

        holder.binding.root.setOnClickListener {
            onItemClicked(listing.id)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateListings(newListings: List<Listing>) {
        listings = newListings
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateCurrency(currency: String, rate: Double) {
        currentCurrency = currency
        exchangeRate = rate
        notifyDataSetChanged()
    }
}