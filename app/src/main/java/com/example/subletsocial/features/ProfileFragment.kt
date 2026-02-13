package com.example.subletsocial.features

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.subletsocial.databinding.FragmentProfileBinding
import com.example.subletsocial.model.Listing
import com.example.subletsocial.model.Model
import com.google.firebase.auth.FirebaseAuth

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var listingsAdapter: ProfileListingsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val currentUser = FirebaseAuth.getInstance().currentUser

        if (currentUser == null) {
            // Handle not logged in state
            binding.tvProfileName.text = "Guest"
            binding.tvProfileEmail.text = "Please log in to view your profile."
            binding.rvMyListings.visibility = View.GONE
            binding.tvMyListingsHeader.visibility = View.GONE
        } else {
            // Handle logged in state
            binding.rvMyListings.visibility = View.VISIBLE
            binding.tvMyListingsHeader.visibility = View.VISIBLE
            binding.tvProfileName.text = currentUser.displayName ?: "No Name"
            binding.tvProfileEmail.text = currentUser.email ?: "No Email"

            listingsAdapter = ProfileListingsAdapter(emptyList())
            binding.rvMyListings.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = listingsAdapter
            }

            Log.d("ProfileFragment", "Fetching listings for user: ${currentUser.uid}")
            Model.shared.getListingsByOwner(currentUser.uid).observe(viewLifecycleOwner) { listings: List<Listing>? ->
                Log.d("ProfileFragment", "Listings received: ${listings?.size ?: 0}")
                listings?.let { listingsAdapter.updateListings(it) }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
