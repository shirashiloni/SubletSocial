package com.example.subletsocial.features

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.example.subletsocial.databinding.ListingListItemBinding
import com.example.subletsocial.model.Listing
import com.example.subletsocial.model.Model
import com.google.firebase.auth.FirebaseAuth
import com.squareup.picasso.Picasso

class ListingAdapter(private var listings: List<Listing>, private val onItemClicked: (String) -> Unit) :
    RecyclerView.Adapter<ListingAdapter.ListingViewHolder>() {

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

        holder.binding.root.setOnClickListener {
            onItemClicked(listing.id)
        }

        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

        if(currentUserId == null || currentUserId == listing.ownerId) return

        // The lifecycle owner would need to be passed in to use this feature
        // Model.shared.getMutualConnectionsUsersIds(currentUserId, listing.ownerId).observe(lifecycleOwner) { ids ->
        //     if (ids.isNotEmpty()) {
        //         holder.binding.cvMutualTag.visibility = View.VISIBLE
        //         holder.binding.tvMutualCount.text = "${ids.size} mutual"
        //     } else {
        //         holder.binding.cvMutualTag.visibility = View.GONE
        //     }
        // }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateListings(newListings: List<Listing>) {
        listings = newListings
        notifyDataSetChanged()
    }
}
