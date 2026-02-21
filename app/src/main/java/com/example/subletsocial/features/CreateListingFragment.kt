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
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.subletsocial.BuildConfig
import com.example.subletsocial.databinding.FragmentCreateListingBinding
import com.example.subletsocial.model.Listing
import com.example.subletsocial.model.LocationData
import com.example.subletsocial.model.Model
import com.google.android.gms.common.api.Status
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import com.google.android.material.chip.Chip
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.GeoPoint
import java.util.Calendar
import java.util.UUID

class CreateListingFragment : Fragment() {

    private var _binding: FragmentCreateListingBinding? = null
    private val binding get() = _binding!!

    private val selectedBitmaps = mutableListOf<Bitmap>()
    private var selectedPlace: Place? = null
    private lateinit var viewModel: FeedViewModel
    private var currentRates: Map<String, Double>? = null

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream = requireContext().contentResolver.openInputStream(it)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                selectedBitmaps.add(bitmap)
                addThumbnailToView(bitmap)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Failed to load image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            selectedBitmaps.add(bitmap)
            addThumbnailToView(bitmap)
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

        viewModel = ViewModelProvider(this)[FeedViewModel::class.java]
        setupPlacesAutocomplete()
        setupDatePickers()
        setupCurrencySpinner()

        viewModel.exchangeRates.observe(viewLifecycleOwner) { rates ->
            currentRates = rates
        }
        viewModel.fetchExchangeRates("USD")

        binding.cvImageUpload.setOnClickListener {
            showImageSourceDialog()
        }

        binding.btnPost.setOnClickListener {
            val title = binding.etTitle.text.toString()
            val priceStr = binding.etPrice.text.toString()
            val description = binding.etDescription.text.toString()
            val bedroomsStr = binding.etBedrooms.text.toString()
            val bathroomsStr = binding.etBathrooms.text.toString()
            val startDate = binding.etStartDate.text.toString()
            val endDate = binding.etEndDate.text.toString()

            if (title.isEmpty() || priceStr.isEmpty() || description.isEmpty() ||
                selectedPlace == null || bedroomsStr.isEmpty() || bathroomsStr.isEmpty() ||
                startDate.isEmpty() || endDate.isEmpty()) {
                Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (currentRates == null) {
                Toast.makeText(context, "Fetching exchange rates, please wait...", Toast.LENGTH_SHORT).show()
                viewModel.fetchExchangeRates("USD")
                return@setOnClickListener
            }

            binding.progressBar.visibility = View.VISIBLE
            binding.btnPost.isEnabled = false

            val listingId = UUID.randomUUID().toString()
            
            // Convert price to USD
            val inputPrice = priceStr.toDoubleOrNull() ?: 0.0
            val selectedCurrency = binding.spinnerCreateCurrency.selectedItem.toString()
            val rate = currentRates?.get(selectedCurrency) ?: 1.0
            val priceInUSD = (inputPrice / rate).toInt()

            if (selectedBitmaps.isNotEmpty()) {
                Model.shared.uploadImages(selectedBitmaps, listingId) { imageUrls ->
                    saveListingToDb(listingId, imageUrls, title, priceInUSD)
                }
            } else {
                saveListingToDb(listingId, emptyList(), title, priceInUSD)
            }
        }
    }

    private fun setupCurrencySpinner() {
        val currencies = arrayOf("USD", "ILS", "EUR")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, currencies)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCreateCurrency.adapter = adapter
    }

    private fun setupPlacesAutocomplete() {
        if (!Places.isInitialized()) {
            Places.initialize(requireContext(), BuildConfig.MAPS_API_KEY)
        }

        val autocompleteFragment = childFragmentManager.findFragmentById(com.example.subletsocial.R.id.autocomplete_fragment)
                as AutocompleteSupportFragment

        autocompleteFragment.setPlaceFields(listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ADDRESS))

        autocompleteFragment.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                selectedPlace = place
                binding.etLocation.setText(place.address ?: place.name)
            }

            override fun onError(status: Status) {
                Toast.makeText(requireContext(), "Error selecting location", Toast.LENGTH_SHORT).show()
            }
        })
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

    private fun addThumbnailToView(bitmap: Bitmap) {
        val imageView = ImageView(requireContext())
        val params = LinearLayout.LayoutParams(300, 300)
        params.setMargins(16, 0, 16, 0)
        imageView.layoutParams = params
        imageView.setImageBitmap(bitmap)
        imageView.scaleType = ImageView.ScaleType.CENTER_CROP

        binding.llImagesContainer.addView(imageView)
    }

    private fun saveListingToDb(id: String, imageUrls: List<String>, title: String, priceInUSD: Int) {
        val selectedAmenities = mutableListOf<String>()
        for (i in 0 until binding.cgAmenities.childCount) {
            val chip = binding.cgAmenities.getChildAt(i) as Chip
            if (chip.isChecked) selectedAmenities.add(chip.text.toString())
        }

        val latLng = selectedPlace?.latLng!!
        val geoPoint = GeoPoint(latLng.latitude, latLng.longitude)

        val listing = Listing(
            id = id,
            title = title,
            imageUrls = imageUrls,
            price = priceInUSD,
            description = binding.etDescription.text.toString(),
            ownerId = FirebaseAuth.getInstance().currentUser?.uid ?: "",
            locationName = binding.etLocation.text.toString(),
            locationData = LocationData(geoPoint, ""),
            bedrooms = binding.etBedrooms.text.toString().toIntOrNull() ?: 0,
            bathrooms = binding.etBathrooms.text.toString().toIntOrNull() ?: 0,
            startDate = binding.etStartDate.text.toString(),
            endDate = binding.etEndDate.text.toString(),
            amenities = selectedAmenities,
            lastUpdated = System.currentTimeMillis()
        )

        Model.shared.addListing(listing) {
            if (_binding != null) {
                binding.progressBar.visibility = View.GONE
                findNavController().popBackStack()
            }
        }
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