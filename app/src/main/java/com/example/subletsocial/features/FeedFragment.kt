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

        adapter = ListingsAdapter(listOf())
        binding.rvListingsFeed.layoutManager = LinearLayoutManager(context)
        binding.rvListingsFeed.adapter = adapter

        Model.shared.getAllListings().observe(viewLifecycleOwner) { listings ->
            allListings = listings

            val currentQuery = binding.searchView.query.toString()
            filterList(currentQuery)
        }

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

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
        if (query.isNullOrEmpty()) {
            adapter.updateListings(allListings)
            binding.tvEmptyState.visibility = View.GONE
        } else {
            val lowerCaseQuery = query.lowercase()

            val filteredList = allListings.filter { listing ->
                listing.title.lowercase().contains(lowerCaseQuery) ||
                        listing.locationName.lowercase().contains(lowerCaseQuery)
            }

            adapter.updateListings(filteredList)

            if (filteredList.isEmpty()) {
                binding.tvEmptyState.visibility = View.VISIBLE
            } else {
                binding.tvEmptyState.visibility = View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}