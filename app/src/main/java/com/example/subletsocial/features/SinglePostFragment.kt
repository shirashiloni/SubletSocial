package com.example.subletsocial.features

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.viewpager2.widget.ViewPager2
import com.example.subletsocial.R
import com.example.subletsocial.databinding.FragmentSinglePostBinding
import com.example.subletsocial.model.Model
import com.google.android.material.chip.Chip
import com.google.firebase.auth.FirebaseAuth

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

        val listingId = args.listingId

        binding.ivBack.setOnClickListener {
            findNavController().popBackStack()
        }

        Model.shared.getListingById(listingId).observe(viewLifecycleOwner) { listing ->
            listing?.let {
                val imageSliderAdapter = ImageSliderAdapter(it.imageUrls)
                binding.vpImages.adapter = imageSliderAdapter

                if (it.imageUrls.size > 1) {
                    binding.tvImageIndicator.text = "1 / ${it.imageUrls.size}"
                    binding.vpImages.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                        override fun onPageSelected(position: Int) {
                            super.onPageSelected(position)
                            binding.tvImageIndicator.text = "${position + 1} / ${it.imageUrls.size}"
                        }
                    })
                } else {
                    binding.tvImageIndicator.visibility = View.GONE
                }

                binding.tvPostTitle.text = it.title
                binding.tvPostPrice.text = "$${it.price}"
                binding.tvPostDescription.text = it.description
                binding.tvPostLocation.text = it.locationName
                binding.tvPostDates.text = "${it.startDate} - ${it.endDate}"
                binding.tvPostRooms.text = "${it.bedrooms} Bedrooms | ${it.bathrooms} Bathrooms"

                binding.cgPostAmenities.removeAllViews()
                for (amenity in it.amenities) {
                    val chip = Chip(context)
                    chip.text = amenity
                    binding.cgPostAmenities.addView(chip)
                }

                if (it.ownerId == FirebaseAuth.getInstance().currentUser?.uid) {
                    binding.ivPostOptions.visibility = View.VISIBLE
                    binding.ivPostOptions.setOnClickListener { view ->
                        showPostOptionsMenu(view, listingId)
                    }
                } else {
                    binding.ivPostOptions.visibility = View.GONE
                }
            }
        }
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