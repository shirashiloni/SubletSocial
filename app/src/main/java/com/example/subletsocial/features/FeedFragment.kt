package com.example.subletsocial.features

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.subletsocial.R
import com.example.subletsocial.databinding.FragmentFeedBinding
import com.example.subletsocial.model.Model

class FeedFragment : Fragment() {

    private var _binding: FragmentFeedBinding? = null
    private val binding get() = _binding!!

    private var adapter: ListingsAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFeedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvListingsFeed.layoutManager = LinearLayoutManager(context)
        adapter = ListingsAdapter(emptyList())
        binding.rvListingsFeed.adapter = adapter

        Model.shared.getAllListings().observe(viewLifecycleOwner) { listings ->
            adapter?.updateListings(listings)

            if (listings.isEmpty()) {
                binding.tvEmptyState.visibility = View.VISIBLE
            } else {
                binding.tvEmptyState.visibility = View.GONE
            }
        }

        binding.fabAddListing.setOnClickListener {
            findNavController().navigate(R.id.action_feedFragment_to_createListingFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}