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
import com.google.maps.android.clustering.ClusterItem
import com.google.maps.android.clustering.ClusterManager

class MapFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!
    private lateinit var googleMap: GoogleMap
    private lateinit var clusterManager: ClusterManager<ListingClusterItem>

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

        setUpClusterManager()

        // ברירת מחדל: ישראל
        val defaultLatLng = LatLng(32.0853, 34.7818)
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLatLng, 10f))

        // האזנה לשינויים במפה - כאן קורה הקסם שחוסך כסף
        googleMap.setOnCameraIdleListener {
            clusterManager.onCameraIdle() // עדכון ה-Clusters
            
            // טעינת נתונים רק עבור מה שרואים כרגע על המסך
            val bounds = googleMap.projection.visibleRegion.latLngBounds
            Model.shared.refreshListingsInBounds(bounds)
        }

        observeListings()
    }

    private fun setUpClusterManager() {
        clusterManager = ClusterManager(requireContext(), googleMap)
        googleMap.setOnMarkerClickListener(clusterManager)
    }

    private fun observeListings() {
        // אנחנו מאזינים ל-DB המקומי. ה-Model יעדכן אותו רק בנתונים שרלוונטיים למפה.
        Model.shared.getAllListings().observe(viewLifecycleOwner) { listings ->
            clusterManager.clearItems()
            listings.forEach { listing ->
                listing.locationData?.geoPoint?.let { geo ->
                    val item = ListingClusterItem(
                        geo.latitude,
                        geo.longitude,
                        listing.title,
                        "${listing.price}$"
                    )
                    clusterManager.addItem(item)
                }
            }
            clusterManager.cluster()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    inner class ListingClusterItem(
        lat: Double,
        lng: Double,
        private val title: String,
        private val snippet: String
    ) : ClusterItem {
        private val position: LatLng = LatLng(lat, lng)
        override fun getPosition(): LatLng = position
        override fun getTitle(): String = title
        override fun getSnippet(): String = snippet
        override fun getZIndex(): Float? = null
    }
}
