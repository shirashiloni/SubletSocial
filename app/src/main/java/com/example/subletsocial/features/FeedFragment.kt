package com.example.subletsocial.features

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.subletsocial.databinding.FragmentFeedBinding
import com.example.subletsocial.model.Listing
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class FeedFragment : Fragment() {

    private var _binding: FragmentFeedBinding? = null
    private val binding get() = _binding!!

    private lateinit var listingAdapter: ListingAdapter
    private lateinit var viewModel: FeedViewModel

    private val originalListings = mutableListOf<Listing>()
    private var currentRates: Map<String, Double>? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFeedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[FeedViewModel::class.java]

        setupRecyclerView()
        setupSearchView()
        setupDatePickers()
        setupPriceFilters()
        setupCurrencySpinner()

        binding.fabAddListing.setOnClickListener {
            findNavController().navigate(FeedFragmentDirections.actionFeedFragmentToCreateListingFragment())
        }

        viewModel.listings.observe(viewLifecycleOwner) { listings ->
            originalListings.clear()
            originalListings.addAll(listings)
            filterAndDisplayListings()
        }

        viewModel.exchangeRates.observe(viewLifecycleOwner) { rates ->
            currentRates = rates
            updateAdapterCurrency()
            filterAndDisplayListings() // Re-filter when rates arrive or change
        }

        viewModel.fetchExchangeRates("USD")
    }

    private fun setupRecyclerView() {
        listingAdapter = ListingAdapter(emptyList()) { listingId ->
            val action = FeedFragmentDirections.actionFeedFragmentToSinglePostFragment(listingId)
            findNavController().navigate(action)
        }
        binding.rvListingsFeed.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = listingAdapter
        }
    }

    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                filterAndDisplayListings()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterAndDisplayListings()
                return true
            }
        })
    }

    private fun setupDatePickers() {
        binding.etStartDate.setOnClickListener {
            showDatePickerDialog { date -> binding.etStartDate.setText(date) }
        }
        binding.etEndDate.setOnClickListener {
            showDatePickerDialog { date -> binding.etEndDate.setText(date) }
        }
        binding.ivApplyDateFilter.setOnClickListener {
            filterAndDisplayListings()
        }
        binding.ivClearDateFilter.setOnClickListener {
            binding.etStartDate.text.clear()
            binding.etEndDate.text.clear()
            filterAndDisplayListings()
        }
    }

    private fun setupPriceFilters() {
        binding.ivApplyPriceFilter.setOnClickListener {
            filterAndDisplayListings()
        }
        binding.ivClearPriceFilter.setOnClickListener {
            binding.etMinPrice.text.clear()
            binding.etMaxPrice.text.clear()
            filterAndDisplayListings()
        }
    }

    private fun setupCurrencySpinner() {
        val currencies = arrayOf("USD", "ILS", "EUR", "GBP")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, currencies)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCurrency.adapter = adapter

        binding.spinnerCurrency.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                updateAdapterCurrency()
                filterAndDisplayListings() // Re-filter when currency changes
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun updateAdapterCurrency() {
        val selectedCurrency = binding.spinnerCurrency.selectedItem.toString()
        val rate = currentRates?.get(selectedCurrency) ?: 1.0
        listingAdapter.updateCurrency(selectedCurrency, rate)
    }

    private fun showDatePickerDialog(onDateSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val selectedDate = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth)
                }
                val formattedDate = SimpleDateFormat("dd/MM/yyyy", Locale.US).format(selectedDate.time)
                onDateSelected(formattedDate)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun filterAndDisplayListings() {
        val searchQuery = binding.searchView.query.toString().lowercase(Locale.getDefault())
        val startDate = binding.etStartDate.text.toString()
        val endDate = binding.etEndDate.text.toString()
        val minPrice = binding.etMinPrice.text.toString().toDoubleOrNull() ?: 0.0
        val maxPrice = binding.etMaxPrice.text.toString().toDoubleOrNull() ?: Double.MAX_VALUE

        val selectedCurrency = binding.spinnerCurrency.selectedItem?.toString() ?: "USD"
        val rate = currentRates?.get(selectedCurrency) ?: 1.0

        val filteredListings = originalListings.filter { listing ->
            val titleMatches = listing.title.lowercase(Locale.getDefault()).contains(searchQuery)
            val locationMatches = listing.locationName.lowercase(Locale.getDefault()).contains(searchQuery)
            
            val dateMatches = if (startDate.isNotEmpty() && endDate.isNotEmpty()) {
                isDateRangeOverlap(listing, startDate, endDate)
            } else {
                true
            }

            val convertedPrice = listing.price * rate
            val priceMatches = convertedPrice in minPrice..maxPrice

            (titleMatches || locationMatches) && dateMatches && priceMatches
        }

        listingAdapter.updateListings(filteredListings)
        binding.tvEmptyState.visibility = if (filteredListings.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun isDateRangeOverlap(listing: Listing, filterStart: String, filterEnd: String): Boolean {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.US)
        try {
            val listingStartDate = dateFormat.parse(listing.startDate)
            val listingEndDate = dateFormat.parse(listing.endDate)
            val filterStartDate = dateFormat.parse(filterStart)
            val filterEndDate = dateFormat.parse(filterEnd)


            return (listingStartDate.before(filterEndDate) || listingStartDate == filterEndDate) &&
                    (listingEndDate.after(filterStartDate) || listingEndDate == filterStartDate)


        } catch (e: Exception) {
            return false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}