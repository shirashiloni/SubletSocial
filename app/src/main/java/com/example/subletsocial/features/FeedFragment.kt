package com.example.subletsocial.features

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.subletsocial.R
import com.example.subletsocial.databinding.FragmentFeedBinding
import com.example.subletsocial.model.Listing
import com.example.subletsocial.model.Model

class FeedFragment : Fragment() {

    private var _binding: FragmentFeedBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: ListingsAdapter
    private var allListings: List<Listing> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFeedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = ListingsAdapter(emptyList()) { listing ->
            val action = FeedFragmentDirections.actionFeedFragmentToSinglePostFragment(listing.id)
            findNavController().navigate(action)
        }
        
        binding.rvListingsFeed.layoutManager = LinearLayoutManager(context)
        binding.rvListingsFeed.adapter = adapter

        Model.shared.getAllListings().observe(viewLifecycleOwner) { listings ->
            allListings = listings
            filterList(binding.searchView.query.toString())
        }

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean {
                filterList(newText)
                return true
            }
        })

        binding.fabAddListing.setOnClickListener {
            findNavController().navigate(R.id.action_feedFragment_to_createListingFragment)
        }
    }

    private fun filterList(query: String?) {
        val filtered = if (query.isNullOrEmpty()) {
            allListings
        } else {
            val lowerCaseQuery = query.lowercase()
            allListings.filter { 
                it.title.lowercase().contains(lowerCaseQuery) || 
                it.locationName.lowercase().contains(lowerCaseQuery) 
            }
        }
        
        val feedItems = groupListingsByLocation(filtered)
        adapter.updateItems(feedItems)
        binding.tvEmptyState.visibility = if (feedItems.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun groupListingsByLocation(listings: List<Listing>): List<FeedItem> {
        if (listings.isEmpty()) return emptyList()

        // 1. נקבץ לפי Geohash (6 תווים ראשונים מייצגים אזור של כ-1 ק"מ)
        val groups = listings.filter { it.locationData?.geohash != null }
            .groupBy { it.locationData?.geohash?.take(6) }
            .filter { it.value.size >= 2 } // רק אזורים עם לפחות 2 סאבלטים

        val result = mutableListOf<FeedItem>()
        val processedIds = mutableSetOf<String>()

        // 2. נוסיף את הקבוצות שמצאנו
        groups.forEach { (_, listingsInGroup) ->
            val firstListing = listingsInGroup.first()
            val areaName = firstListing.locationName.split(",").firstOrNull() ?: "Nearby Area"
            result.add(FeedItem.ListingGroup("Trending in $areaName", listingsInGroup))
            processedIds.addAll(listingsInGroup.map { it.id })
        }

        // 3. נוסיף את כל השאר כבודדים
        listings.forEach { listing ->
            if (!processedIds.contains(listing.id)) {
                result.add(FeedItem.SingleListing(listing))
            }
        }

        return result
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
