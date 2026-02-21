package com.example.subletsocial.features

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.subletsocial.R
import com.example.subletsocial.databinding.FragmentMapBinding
import com.example.subletsocial.model.Listing
import com.example.subletsocial.model.Model
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions

class MapFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!
    private lateinit var googleMap: GoogleMap
    private var mapInitialized = false
    private val idMarkerMap = mutableMapOf<String, Marker>()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        ) {
            setupMapWithLocation()
        } else {
            setupMapWithDefaultLocation()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap.uiSettings.isZoomControlsEnabled = true
        
        // Navigate to SinglePostFragment when the info window is clicked
        googleMap.setOnInfoWindowClickListener { marker ->
            (marker.tag as? Listing)?.let { listing ->
                val action = MapFragmentDirections.actionMapFragmentToSinglePostFragment(listing.id)
                findNavController().navigate(action)
            }
        }

        checkLocationPermissionAndSetupMap()
    }

    private fun checkLocationPermissionAndSetupMap() {
        if (ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            setupMapWithLocation()
        } else {
            requestPermissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    private fun setupMapWithLocation() {
        if (mapInitialized) return
        
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            googleMap.isMyLocationEnabled = true
            googleMap.uiSettings.isMyLocationButtonEnabled = true
        }

        binding.progressBarMap.visibility = View.VISIBLE
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val currentLatLng = LatLng(location.latitude, location.longitude)
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 12f))
                initializeListenersAndObservers()
            } else {
                setupMapWithDefaultLocation()
            }
        }.addOnFailureListener {
            setupMapWithDefaultLocation()
            binding.progressBarMap.visibility = View.GONE
        }
    }

    private fun setupMapWithDefaultLocation() {
        if (mapInitialized) return
        val defaultLatLng = LatLng(32.0853, 34.7818) // Israel
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLatLng, 10f))
        initializeListenersAndObservers()
    }

    private fun initializeListenersAndObservers() {
        if (mapInitialized) return
        mapInitialized = true

        val initialBounds = googleMap.projection.visibleRegion.latLngBounds
        Model.shared.refreshListingsInBounds(initialBounds)

        googleMap.setOnCameraIdleListener {
            binding.progressBarMap.visibility = View.VISIBLE
            val bounds = googleMap.projection.visibleRegion.latLngBounds
            Model.shared.refreshListingsInBounds(bounds)
        }

        observeListings()
    }

    private fun observeListings() {
        Model.shared.mapListings.observe(viewLifecycleOwner) { listings ->
            binding.progressBarMap.visibility = View.GONE
            val listingsInScope = listings.map { it.id }.toSet()

            // 1. Remove markers that are no longer in view
            val iterator = idMarkerMap.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                if (!listingsInScope.contains(entry.key)) {
                    entry.value.remove()
                    iterator.remove()
                }
            }

            // 2. Add only new markers
            listings.forEach { listing ->
                if (!idMarkerMap.containsKey(listing.id)) {
                    val geo = listing.locationData.geoPoint
                    val latLng = LatLng(geo.latitude, geo.longitude)
                    val marker = googleMap.addMarker(
                        MarkerOptions()
                            .position(latLng)
                            .title(listing.title)
                            .snippet("${listing.price}$")
                    )
                    marker?.let {
                        it.tag = listing // Store the whole object in the tag
                        idMarkerMap[listing.id] = it
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
