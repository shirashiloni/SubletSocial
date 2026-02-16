package com.example.subletsocial.features

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.subletsocial.databinding.ItemListingsGroupBinding
import com.example.subletsocial.databinding.ListingListItemBinding
import com.example.subletsocial.model.Listing
import com.squareup.picasso.Picasso

class ListingsAdapter(
    private var items: List<FeedItem>,
    private val onListingClick: (Listing) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_SINGLE = 0
        private const val TYPE_GROUP = 1
    }

    class SingleViewHolder(val binding: ListingListItemBinding) : RecyclerView.ViewHolder(binding.root)
    class GroupViewHolder(val binding: ItemListingsGroupBinding) : RecyclerView.ViewHolder(binding.root)

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is FeedItem.SingleListing -> TYPE_SINGLE
            is FeedItem.ListingGroup -> TYPE_GROUP
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_SINGLE) {
            SingleViewHolder(ListingListItemBinding.inflate(inflater, parent, false))
        } else {
            GroupViewHolder(ItemListingsGroupBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        if (holder is SingleViewHolder && item is FeedItem.SingleListing) {
            bindSingle(holder, item.listing)
        } else if (holder is GroupViewHolder && item is FeedItem.ListingGroup) {
            bindGroup(holder, item)
        }
    }

    private fun bindSingle(holder: SingleViewHolder, listing: Listing) {
        holder.binding.tvListingTitle.text = listing.title
        holder.binding.tvListingLocation.text = listing.locationName
        holder.binding.tvListingPrice.text = "$${listing.price}"
        holder.binding.tvListingDates.text = "${listing.startDate} - ${listing.endDate}"
        holder.binding.tvListingRooms.text = "${listing.bedrooms} bed • ${listing.bathrooms} bath"

        if (listing.imageUrls.isNotEmpty()) {
            Picasso.get().load(listing.imageUrls[0]).fit().centerCrop().into(holder.binding.ivListingImage)
        }
        holder.itemView.setOnClickListener { onListingClick(listing) }
    }

    private fun bindGroup(holder: GroupViewHolder, group: FeedItem.ListingGroup) {
        holder.binding.tvGroupTitle.text = group.title
        holder.binding.rvGroupListings.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = NearbyListingsAdapter(group.listings, onListingClick)
        }
    }

    override fun getItemCount(): Int = items.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateItems(newItems: List<FeedItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}
