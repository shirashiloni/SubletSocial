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
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import com.example.subletsocial.R
import com.example.subletsocial.databinding.FragmentProfileBinding
import com.example.subletsocial.model.Model
import com.google.firebase.auth.FirebaseAuth
import com.squareup.picasso.Picasso

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val args: ProfileFragmentArgs by navArgs()

    private lateinit var listingsAdapter: ProfileListingsAdapter
    private var profileUserId: String? = null

    private var isFollowing = false

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream = requireContext().contentResolver.openInputStream(it)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                uploadProfileImage(bitmap)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Failed to load image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            uploadProfileImage(bitmap)
        } else {
            Toast.makeText(requireContext(), "Failed to capture photo", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

        profileUserId = args.userId ?: currentUserId

        if (profileUserId == null) {
            setupGuestView()
        } else {
            setupProfileView(profileUserId!!, currentUserId)
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

    private fun setupProfileView(userIdToDisplay: String, currentLoggedInId: String?) {
        val isOwnProfile = (userIdToDisplay == currentLoggedInId)

        if (isOwnProfile) {
            binding.ivEditBio.visibility = View.VISIBLE
            binding.ivLogout.visibility = View.VISIBLE
            binding.ivEditBio.setOnClickListener { showEditBioDialog() }
            binding.ivProfileImage.setOnClickListener { showImageSourceDialog() }
        } else {
            binding.ivEditBio.visibility = View.GONE
            binding.ivLogout.visibility = View.GONE
            binding.ivProfileImage.setOnClickListener(null)
        }

        binding.rvMyListings.visibility = View.VISIBLE

        listingsAdapter = ProfileListingsAdapter(emptyList())
        binding.rvMyListings.apply {
            layoutManager = GridLayoutManager(context, 3)
            adapter = listingsAdapter
        }

        if (currentLoggedInId == userIdToDisplay) {
            binding.btnFollow.visibility = View.GONE
        } else if (currentLoggedInId != null){
            binding.btnFollow.visibility = View.VISIBLE

            Model.shared.checkIfFollowing(currentLoggedInId, userIdToDisplay)
                .observe(viewLifecycleOwner) { isFollowingNow ->
                    isFollowing = isFollowingNow
                    updateFollowButtonState(isFollowing)
                }

            binding.btnFollow.setOnClickListener {
                binding.btnFollow.isEnabled = false

                Model.shared.toggleFollow(currentLoggedInId, userIdToDisplay, isFollowing) {
                    binding.btnFollow.isEnabled = true
                }

                val currentText = binding.tvNumFollowers.text.toString()
                val currentCount = currentText.filter { it.isDigit() }.toIntOrNull() ?: 0
                val newCount = if (isFollowing) currentCount - 1 else currentCount + 1
                binding.tvNumFollowers.text = "$newCount Followers"
            }
        }

        binding.progressBarProfile.visibility = View.VISIBLE
        Model.shared.getUserData(userIdToDisplay).observe(viewLifecycleOwner) { user ->
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
            checkLoadingFinished()
        }

        Log.d("ProfileFragment", "Fetching listings for user: $userIdToDisplay")
        Model.shared.getListingsByOwner(userIdToDisplay).observe(viewLifecycleOwner) { listings ->
            Log.d("ProfileFragment", "Listings received: ${listings?.size ?: 0}")
            binding.tvNumListings.text = "${listings?.size ?: 0} Listings"
            listings?.let { listingsAdapter.updateListings(it) }
            checkLoadingFinished()
        }
    }

    private fun checkLoadingFinished() {
        // Simple check: if we have user name (loaded from getUserData) and we've at least received the listings observer
        if (binding.tvProfileName.text != "User Name") {
             binding.progressBarProfile.visibility = View.GONE
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

    private fun uploadProfileImage(bitmap: Bitmap) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        
        binding.progressBarProfile.visibility = View.VISIBLE
        Model.shared.uploadImage(bitmap, "avatar_$userId") { url ->
            if (url != null) {
                Model.shared.updateUserAvatar(userId, url) {
                    binding.ivProfileImage.setImageBitmap(bitmap)
                    binding.progressBarProfile.visibility = View.GONE
                    Toast.makeText(context, "Profile picture updated!", Toast.LENGTH_SHORT).show()
                }
            } else {
                binding.progressBarProfile.visibility = View.GONE
                Toast.makeText(context, "Failed to upload image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showEditBioDialog() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        if (profileUserId != currentUserId) return

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
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

        if (currentUserId != null && currentUserId == profileUserId) {
            binding.progressBarProfile.visibility = View.VISIBLE
            Model.shared.updateUserBio(currentUserId, newBio) {
                binding.tvBio.text = newBio
                binding.progressBarProfile.visibility = View.GONE
                Toast.makeText(context, "Bio updated!", Toast.LENGTH_SHORT).show()
            }
        }
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
