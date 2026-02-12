package com.example.subletsocial.features

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.subletsocial.databinding.FragmentCreateListingBinding
import com.example.subletsocial.model.Listing
import com.example.subletsocial.model.Model
import com.google.android.material.chip.Chip
import com.google.firebase.auth.FirebaseAuth
import java.util.Calendar
import java.util.UUID

class CreateListingFragment : Fragment() {

    private var _binding: FragmentCreateListingBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateListingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupDatePickers()

        binding.btnPost.setOnClickListener {
            val title = binding.etTitle.text.toString()
            val priceStr = binding.etPrice.text.toString()
            val description = binding.etDescription.text.toString()
            val location = binding.etLocation.text.toString()
            val bedroomsStr = binding.etBedrooms.text.toString()
            val bathroomsStr = binding.etBathrooms.text.toString()
            val startDate = binding.etStartDate.text.toString()
            val endDate = binding.etEndDate.text.toString()

            val selectedAmenities = mutableListOf<String>()
            for (i in 0 until binding.cgAmenities.childCount) {
                val chip = binding.cgAmenities.getChildAt(i) as Chip
                if (chip.isChecked) {
                    selectedAmenities.add(chip.text.toString())
                }
            }

            if (title.isEmpty() || priceStr.isEmpty() || description.isEmpty() ||
                location.isEmpty() || bedroomsStr.isEmpty() || bathroomsStr.isEmpty() ||
                startDate.isEmpty() || endDate.isEmpty()) {
                Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val price = priceStr.toIntOrNull() ?: 0
            val bedrooms = bedroomsStr.toIntOrNull() ?: 0
            val bathrooms = bathroomsStr.toIntOrNull() ?: 0

            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser == null) {
                Toast.makeText(context, "User not logged in", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            binding.progressBar.visibility = View.VISIBLE
            binding.btnPost.isEnabled = false

            val listingId = UUID.randomUUID().toString()
            val ownerId = currentUser.uid

            val listing = Listing(
                id = listingId,
                title = title,
                price = price,
                description = description,
                imageUrl = "",
                ownerId = ownerId,
                location = location,
                bedrooms = bedrooms,
                bathrooms = bathrooms,
                startDate = startDate,
                endDate = endDate,
                amenities = selectedAmenities,
                lastUpdated = System.currentTimeMillis()
            )

            Model.shared.addListing(listing) {
                binding.progressBar.visibility = View.GONE
                findNavController().popBackStack()
            }
        }
    }

    private fun setupDatePickers() {
        binding.etStartDate.setOnClickListener {
            showDatePickerDialog { date ->
                binding.etStartDate.setText(date)
            }
        }

        binding.etEndDate.setOnClickListener {
            showDatePickerDialog { date ->
                binding.etEndDate.setText(date)
            }
        }
    }

    private fun showDatePickerDialog(onDateSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, selectedDay ->
                val formattedDate = "$selectedDay/${selectedMonth + 1}/$selectedYear"
                onDateSelected(formattedDate)
            },
            year, month, day
        )
        datePickerDialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}