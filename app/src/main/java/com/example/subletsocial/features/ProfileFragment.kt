package com.example.subletsocial.features

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.example.subletsocial.R
import com.example.subletsocial.databinding.FragmentProfileBinding
import com.example.subletsocial.model.Listing
import com.example.subletsocial.model.Model
import com.example.subletsocial.model.User
import com.google.firebase.auth.FirebaseAuth
import com.squareup.picasso.Picasso

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
            binding.tvBio.text = "Please log in to view your profile."
            binding.rvMyListings.visibility = View.GONE
            binding.ivEditBio.visibility = View.GONE
        } else {
            // Handle logged in state
            binding.rvMyListings.visibility = View.VISIBLE
            binding.ivEditBio.visibility = View.VISIBLE

            listingsAdapter = ProfileListingsAdapter(emptyList())
            binding.rvMyListings.apply {
                layoutManager = GridLayoutManager(context, 3)
                adapter = listingsAdapter
            }

            Model.shared.getUserData(currentUser.uid).observe(viewLifecycleOwner) { user: User? ->
                user?.let {
                    binding.tvProfileName.text = it.name
                    binding.tvBio.text = it.bio
                    if (it.avatarUrl.isNotEmpty()) {
                        Picasso.get().load(it.avatarUrl).into(binding.ivProfileImage)
                    } else {
                        binding.ivProfileImage.setImageResource(R.drawable.ic_default_avatar)
                    }
                }
            }

            Log.d("ProfileFragment", "Fetching listings for user: ${currentUser.uid}")
            Model.shared.getListingsByOwner(currentUser.uid).observe(viewLifecycleOwner) { listings: List<Listing>? ->
                Log.d("ProfileFragment", "Listings received: ${listings?.size ?: 0}")
                binding.tvNumListings.text = "${listings?.size ?: 0} Listings"
                listings?.let { listingsAdapter.updateListings(it) }
            }

            binding.ivEditBio.setOnClickListener {
                showEditBioDialog()
            }
        }
    }

    private fun showEditBioDialog() {
        val editText = EditText(requireContext()).apply {
            setText(binding.tvBio.text)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Bio")
            .setView(editText)
            .setPositiveButton("Save") { _, _ ->
                val newBio = editText.text.toString()
                updateBio(newBio)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateBio(newBio: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUser?.let {
            Model.shared.updateUserBio(it.uid, newBio) {
                binding.tvBio.text = newBio
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
