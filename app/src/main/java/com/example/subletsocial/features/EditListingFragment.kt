package com.example.subletsocial.features

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.subletsocial.databinding.FragmentCreateListingBinding
import com.example.subletsocial.model.Listing
import com.example.subletsocial.model.Model
import com.google.android.material.chip.Chip
import com.squareup.picasso.Picasso
import java.util.Calendar

class EditListingFragment : Fragment() {

    private var _binding: FragmentCreateListingBinding? = null
    private val binding get() = _binding!!

    private val args: EditListingFragmentArgs by navArgs()
    private val selectedBitmaps = mutableListOf<Bitmap>()
    private val existingImageUrls = mutableListOf<String>()

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream = requireContext().contentResolver.openInputStream(it)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                selectedBitmaps.add(bitmap)
                addThumbnailToView(bitmap, null)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Failed to load image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            selectedBitmaps.add(bitmap)
            addThumbnailToView(bitmap, null)
        } else {
            Toast.makeText(requireContext(), "Failed to capture photo", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateListingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val listingId = args.listingId

        setupDatePickers()

        binding.cvImageUpload.setOnClickListener {
            showImageSourceDialog()
        }

        Model.shared.getListingById(listingId).observe(viewLifecycleOwner) { listing ->
            listing?.let { currentListing ->
                binding.etTitle.setText(currentListing.title)
                binding.etPrice.setText(currentListing.price.toString())
                binding.etDescription.setText(currentListing.description)
                binding.etLocation.setText(currentListing.locationName)
                binding.etBedrooms.setText(currentListing.bedrooms.toString())
                binding.etBathrooms.setText(currentListing.bathrooms.toString())
                binding.etStartDate.setText(currentListing.startDate)
                binding.etEndDate.setText(currentListing.endDate)

                existingImageUrls.clear()
                existingImageUrls.addAll(currentListing.imageUrls)
                binding.llImagesContainer.removeAllViews()
                for (imageUrl in existingImageUrls) {
                    addThumbnailToView(null, imageUrl)
                }

                for (i in 0 until binding.cgAmenities.childCount) {
                    val chip = binding.cgAmenities.getChildAt(i) as Chip
                    if (currentListing.amenities.contains(chip.text.toString())) {
                        chip.isChecked = true
                    }
                }

                binding.btnPost.text = "Save Changes"
                binding.btnPost.setOnClickListener {
                    updateListing(currentListing)
                }
            }
        }
    }

    private fun showImageSourceDialog() {
        val options = arrayOf("Take Photo", "Choose from Gallery")
        AlertDialog.Builder(requireContext())
            .setTitle("Add Photo")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> cameraLauncher.launch(null)
                    1 -> galleryLauncher.launch("image/*")
                }
            }
            .show()
    }

    private fun addThumbnailToView(bitmap: Bitmap?, imageUrl: String?) {
        val imageView = ImageView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(300, 300).apply {
                setMargins(16, 0, 16, 0)
            }
            scaleType = ImageView.ScaleType.CENTER_CROP
            setOnClickListener { 
                showRemoveImageDialog(this, bitmap, imageUrl)
            }
        }

        if (bitmap != null) {
            imageView.setImageBitmap(bitmap)
        } else if (imageUrl != null) {
            Picasso.get().load(imageUrl).into(imageView)
        }

        binding.llImagesContainer.addView(imageView)
    }
    
    private fun showRemoveImageDialog(view: View, bitmap: Bitmap?, imageUrl: String?) {
        AlertDialog.Builder(requireContext())
            .setTitle("Remove Image")
            .setMessage("Are you sure you want to remove this image?")
            .setPositiveButton("Remove") { _, _ ->
                binding.llImagesContainer.removeView(view)
                if (bitmap != null) {
                    selectedBitmaps.remove(bitmap)
                } else if (imageUrl != null) {
                    existingImageUrls.remove(imageUrl)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateListing(originalListing: Listing) {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnPost.isEnabled = false

        Model.shared.uploadImages(selectedBitmaps, originalListing.id) { newImageUrls ->
            val allImageUrls = existingImageUrls + newImageUrls

            val updatedListing = originalListing.copy(
                title = binding.etTitle.text.toString(),
                price = binding.etPrice.text.toString().toIntOrNull() ?: 0,
                description = binding.etDescription.text.toString(),
                locationName = binding.etLocation.text.toString(),
                bedrooms = binding.etBedrooms.text.toString().toIntOrNull() ?: 0,
                bathrooms = binding.etBathrooms.text.toString().toIntOrNull() ?: 0,
                startDate = binding.etStartDate.text.toString(),
                endDate = binding.etEndDate.text.toString(),
                amenities = getSelectedAmenities(),
                imageUrls = allImageUrls
            )

            Model.shared.updateListing(updatedListing) {
                if (_binding != null) {
                    binding.progressBar.visibility = View.GONE
                    findNavController().popBackStack()
                }
            }
        }
    }

    private fun getSelectedAmenities(): List<String> {
        val selectedAmenities = mutableListOf<String>()
        for (i in 0 until binding.cgAmenities.childCount) {
            val chip = binding.cgAmenities.getChildAt(i) as Chip
            if (chip.isChecked) {
                selectedAmenities.add(chip.text.toString())
            }
        }
        return selectedAmenities
    }

    private fun setupDatePickers() {
        binding.etStartDate.setOnClickListener {
            showDatePickerDialog { date -> binding.etStartDate.setText(date) }
        }
        binding.etEndDate.setOnClickListener {
            showDatePickerDialog { date -> binding.etEndDate.setText(date) }
        }
    }

    private fun showDatePickerDialog(onDateSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, selectedDay ->
                val formattedDate = "$selectedDay/${selectedMonth + 1}/$selectedYear"
                onDateSelected(formattedDate)
            },
            year, month, day
        ).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
