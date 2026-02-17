package com.example.subletsocial.features

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.viewpager2.widget.ViewPager2
import com.example.subletsocial.R
import com.example.subletsocial.databinding.FragmentSinglePostBinding
import com.example.subletsocial.model.Listing
import com.example.subletsocial.model.Model
import com.example.subletsocial.model.User
import com.google.android.material.chip.Chip
import com.google.firebase.auth.FirebaseAuth
import com.squareup.picasso.Picasso

class SinglePostFragment : Fragment() {

    private var _binding: FragmentSinglePostBinding? = null
    private val binding get() = _binding!!

    private val args: SinglePostFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSinglePostBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.ivBack.setOnClickListener { findNavController().popBackStack() }

        val listingId = args.listingId
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

        Model.shared.getListingById(listingId).observe(viewLifecycleOwner) { listing ->
            listing?.let {
                setupImageSlider(it)
                setupListingDetails(it)
                setupAmenities(it)
                setupPostOptions(it, listingId)
                setupHostInfo(it)
                setupMutualConnections(it, currentUserId)
            }
        }
    }

    private fun setupImageSlider(listing: Listing) {
        val imageSliderAdapter = ImageSliderAdapter(listing.imageUrls)
        binding.vpImages.adapter = imageSliderAdapter

        if (listing.imageUrls.size > 1) {
            binding.tvImageIndicator.visibility = View.VISIBLE
            binding.tvImageIndicator.text = "1 / ${listing.imageUrls.size}"
            binding.vpImages.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    binding.tvImageIndicator.text = "${position + 1} / ${listing.imageUrls.size}"
                }
            })
        } else {
            binding.tvImageIndicator.visibility = View.GONE
        }
    }

    private fun setupListingDetails(listing: Listing) {
        binding.tvPostTitle.text = listing.title
        binding.tvPostPrice.text = "$${listing.price}"
        binding.tvPostDescription.text = listing.description
        binding.tvPostLocation.text = listing.location
        binding.tvPostDates.text = "${listing.startDate} - ${listing.endDate}"
        binding.tvPostRooms.text = "${listing.bedrooms} Bedrooms | ${listing.bathrooms} Bathrooms"
    }

    private fun setupAmenities(listing: Listing) {
        binding.cgPostAmenities.removeAllViews()
        for (amenity in listing.amenities) {
            val chip = Chip(context)
            chip.text = amenity
            binding.cgPostAmenities.addView(chip)
        }
    }

    private fun setupPostOptions(listing: Listing, listingId: String) {
        if (listing.ownerId == FirebaseAuth.getInstance().currentUser?.uid) {
            binding.ivPostOptions.visibility = View.VISIBLE
            binding.ivPostOptions.setOnClickListener { view ->
                showPostOptionsMenu(view, listingId)
            }
        } else {
            binding.ivPostOptions.visibility = View.GONE
        }
    }

    private fun setupHostInfo(listing: Listing) {
        Model.shared.getUserData(listing.ownerId).observe(viewLifecycleOwner) { user ->
            if (user != null) {
                binding.tvHostName.text = user.name
                binding.tvHostBio.text = user.bio
                binding.tvHostStats.text = "${user.followersCount} followers • ${user.followingCount} following"

                if (user.avatarUrl.isNotEmpty()) {
                    Picasso.get()
                        .load(user.avatarUrl)
                        .placeholder(R.drawable.ic_default_avatar)
                        .into(binding.ivHostAvatar)
                } else {
                    binding.ivHostAvatar.setImageResource(R.drawable.ic_default_avatar)
                }

                binding.cvHostProfile.setOnClickListener {
                    val action = SinglePostFragmentDirections
                        .actionSinglePostFragmentToProfileFragment(user.id)
                    findNavController().navigate(action)
                }
            }
        }
    }

    private fun setupMutualConnections(listing: Listing, currentUserId: String?) {
        if (currentUserId != null && listing.ownerId != currentUserId) {
            Model.shared.getMutualConnectionsUsers(currentUserId, listing.ownerId)
                .observe(viewLifecycleOwner) { mutualUsers ->
                    if (mutualUsers.isNotEmpty()) {
                        binding.tvMutualHeader.visibility = View.VISIBLE
                        binding.llMutualConnectionsContainer.removeAllViews()
                        mutualUsers.forEach { user -> inflateMutualUserRow(user) }
                    } else {
                        binding.tvMutualHeader.visibility = View.GONE
                    }
                }
        } else {
            binding.tvMutualHeader.visibility = View.GONE
        }
    }

    private fun inflateMutualUserRow(user: User) {
        val userView = layoutInflater.inflate(
            R.layout.item_mutual_connection,
            binding.llMutualConnectionsContainer,
            false
        )

        userView.findViewById<TextView>(R.id.tvUserName).text = user.name
        userView.findViewById<TextView>(R.id.tvUserFollowers).text = "${user.followersCount} followers"

        val imageView = userView.findViewById<ImageView>(R.id.ivUserAvatar)
        if (user.avatarUrl.isNotEmpty()) {
            Picasso.get()
                .load(user.avatarUrl)
                .placeholder(R.drawable.ic_default_avatar)
                .into(imageView)
        } else {
            imageView.setImageResource(R.drawable.ic_default_avatar)
        }

        userView.setOnClickListener {
            val action = SinglePostFragmentDirections
                .actionSinglePostFragmentToProfileFragment(user.id)
            findNavController().navigate(action)
        }

        binding.llMutualConnectionsContainer.addView(userView)
    }

    private fun showPostOptionsMenu(view: View, listingId: String) {
        val popup = PopupMenu(requireContext(), view)
        popup.menuInflater.inflate(R.menu.post_options_menu, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.edit_listing_option -> {
                    val action = SinglePostFragmentDirections.actionSinglePostFragmentToEditListingFragment(listingId)
                    findNavController().navigate(action)
                    true
                }
                R.id.delete_listing_option -> {
                    Model.shared.deleteListing(listingId) {
                        findNavController().popBackStack()
                    }
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}