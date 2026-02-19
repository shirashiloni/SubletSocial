package com.example.subletsocial.features

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.subletsocial.R
import com.example.subletsocial.databinding.FragmentMapBinding
import com.example.subletsocial.model.Listing
import com.example.subletsocial.model.Model
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class MapFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!
    private lateinit var googleMap: GoogleMap

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

        // ברירת מחדל: ישראל
        val defaultLatLng = LatLng(32.0853, 34.7818)
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLatLng, 10f))

        // האזנה לשינויים במפה
        googleMap.setOnCameraIdleListener {
            val bounds = googleMap.projection.visibleRegion.latLngBounds
            Model.shared.refreshListingsInBounds(bounds)
        }

        observeListings()
    }

    private fun observeListings() {
        // עכשיו מאזינים ל-LiveData הנפרד של המפה
        Model.shared.mapListings.observe(viewLifecycleOwner) { listings ->
            googleMap.clear()
            listings.forEach { listing ->
                val geo = listing.locationData.geoPoint
                val latLng = LatLng(geo.latitude, geo.longitude)
                googleMap.addMarker(
                    MarkerOptions()
                        .position(latLng)
                        .title(listing.title)
                        .snippet("${listing.price}$")
                )
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
