package com.example.subletsocial.features

import android.app.AlertDialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import com.example.subletsocial.R
import com.example.subletsocial.databinding.FragmentProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.squareup.picasso.Picasso

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val args: ProfileFragmentArgs by navArgs()

    private lateinit var listingsAdapter: ProfileListingsAdapter
    private lateinit var viewModel: ProfileViewModel

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream = requireContext().contentResolver.openInputStream(it)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                viewModel.uploadAndSetAvatar(bitmap, FirebaseAuth.getInstance().currentUser?.uid ?: "") { success ->
                    if (success) {
                        binding.ivProfileImage.setImageBitmap(bitmap)
                        Toast.makeText(context, "Profile picture updated!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Failed to upload image", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Failed to load image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            viewModel.uploadAndSetAvatar(bitmap, FirebaseAuth.getInstance().currentUser?.uid ?: "") { success ->
                if (success) {
                    binding.ivProfileImage.setImageBitmap(bitmap)
                    Toast.makeText(context, "Profile picture updated!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Failed to upload image", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(requireContext(), "Failed to capture photo", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this).get(ProfileViewModel::class.java)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        val profileUserId = args.userId ?: currentUserId

        if (profileUserId == null) {
            setupGuestView()
        } else {
            viewModel.loadProfile(profileUserId)
            setupObservers(profileUserId, currentUserId)
            setupUI(profileUserId, currentUserId)
        }

        binding.ivLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            findNavController().navigate(R.id.loginFragment)
        }
    }

    private fun setupGuestView() {
        binding.tvProfileName.text = "Guest"
        binding.tvBio.text = "Please log in to view your profile."
        binding.rvMyListings.visibility = View.GONE
        binding.ivEditBio.visibility = View.GONE
        binding.ivProfileImage.setImageResource(R.drawable.ic_default_avatar)
    }

    private fun setupUI(profileUserId: String, currentUserId: String?) {
        val isOwnProfile = (profileUserId == currentUserId)

        if (isOwnProfile) {
            binding.ivEditBio.visibility = View.VISIBLE
            binding.ivLogout.visibility = View.VISIBLE
            binding.ivEditBio.setOnClickListener { showEditBioDialog() }
            binding.ivProfileImage.setOnClickListener { showImageSourceDialog() }
            binding.btnFollow.visibility = View.GONE
        } else {
            binding.ivEditBio.visibility = View.GONE
            binding.ivLogout.visibility = View.GONE
            binding.ivProfileImage.setOnClickListener(null)
            binding.btnFollow.visibility = View.VISIBLE
            binding.btnFollow.setOnClickListener {
                binding.btnFollow.isEnabled = false
                viewModel.toggleFollow(currentUserId!!, profileUserId, viewModel.isFollowing.value ?: false) {
                    binding.btnFollow.isEnabled = true
                }
            }
        }

        listingsAdapter = ProfileListingsAdapter(emptyList())
        binding.rvMyListings.apply {
            layoutManager = GridLayoutManager(context, 3)
            adapter = listingsAdapter
        }
    }

    private fun setupObservers(profileUserId: String, currentUserId: String?) {
        viewModel.user.observe(viewLifecycleOwner) { user ->
            if (user != null) {
                binding.tvProfileName.text = user.name
                binding.tvBio.text = if (user.bio.isNotEmpty()) user.bio else "No bio available."
                binding.tvNumFollowers.text = "${user.followersCount} Followers"
                binding.tvNumFollowing.text = "${user.followingCount} Following"

                if (user.avatarUrl.isNotEmpty()) {
                    Picasso.get()
                        .load(user.avatarUrl)
                        .placeholder(R.drawable.ic_default_avatar)
                        .into(binding.ivProfileImage)
                } else {
                    binding.ivProfileImage.setImageResource(R.drawable.ic_default_avatar)
                }
            }
        }

        viewModel.listings.observe(viewLifecycleOwner) { listings ->
            Log.d("ProfileFragment", "Listings received: ${listings?.size ?: 0}")
            binding.tvNumListings.text = "${listings?.size ?: 0} Listings"
            listings?.let { listingsAdapter.updateListings(it) }
        }

        if (currentUserId != null && currentUserId != profileUserId) {
            viewModel.checkFollowStatus(currentUserId, profileUserId)
            viewModel.isFollowing.observe(viewLifecycleOwner) {
                updateFollowButtonState(it)
            }
        }
    }

    private fun showImageSourceDialog() {
        val options = arrayOf("Take Photo", "Choose from Gallery")
        AlertDialog.Builder(requireContext())
            .setTitle("Change Profile Picture")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> cameraLauncher.launch(null)
                    1 -> galleryLauncher.launch("image/*")
                }
            }
            .show()
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
                viewModel.updateUserBio(FirebaseAuth.getInstance().currentUser?.uid ?: "", newBio) {
                    binding.tvBio.text = newBio
                    Toast.makeText(context, "Bio updated!", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateFollowButtonState(following: Boolean) {
        if (following) {
            binding.btnFollow.text = "Following"
            binding.btnFollow.setBackgroundColor(Color.GRAY)
        } else {
            binding.btnFollow.text = "Follow"
            binding.btnFollow.setBackgroundColor(Color.parseColor("#00897B"))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}