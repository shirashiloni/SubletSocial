package com.example.subletsocial.model

import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.firebase.geofire.GeoFireUtils
import com.firebase.geofire.GeoLocation
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors

class Model private constructor() {

    private val database = AppLocalDb.db
    private val firestore = FirebaseFirestore.getInstance()
    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    val listingsLoadingState = MutableLiveData<LoadingState>()
    
    private val _mapListings = MutableLiveData<List<Listing>>()
    val mapListings: LiveData<List<Listing>> = _mapListings

    enum class LoadingState {
        LOADING,
        LOADED
    }

    companion object {
        val shared = Model()
    }

    fun getAllListings(): LiveData<List<Listing>> {
        val localData = database.listingDao().getAll()

        refreshAllListings()

        return localData
    }

    fun refreshAllListings() {
        listingsLoadingState.postValue(LoadingState.LOADING)
        firestore.collection("listings").get().addOnSuccessListener { result ->
            val listings = result.toObjects(Listing::class.java)
            executor.execute {
                database.listingDao().deleteAll()
                database.listingDao().insertAll(listings)
                listingsLoadingState.postValue(LoadingState.LOADED)
            }
        }.addOnFailureListener { listingsLoadingState.postValue(LoadingState.LOADED) }
    }

    //for displaying only relevant listings on map
    fun refreshListingsInBounds(bounds: LatLngBounds) {
        val center = bounds.center
        val results = FloatArray(1)
        android.location.Location.distanceBetween(
            bounds.southwest.latitude, bounds.southwest.longitude,
            bounds.northeast.latitude, bounds.northeast.longitude, results
        )
        val radiusInMeters = (results[0] / 2).toDouble()

        val boundsList = GeoFireUtils.getGeoHashQueryBounds(GeoLocation(center.latitude, center.longitude), radiusInMeters)
        val tasks = boundsList.map { b ->
            firestore.collection("listings")
                .orderBy("locationData.geohash")
                .startAt(b.startHash).endAt(b.endHash).get()
        }

        Tasks.whenAllSuccess<QuerySnapshot>(tasks).addOnSuccessListener { snapshots ->
            val allListings = snapshots.flatMap { it.toObjects(Listing::class.java) }
            val filtered = allListings.filter { listing ->
                bounds.contains(com.google.android.gms.maps.model.LatLng(
                    listing.locationData.geoPoint.latitude,
                    listing.locationData.geoPoint.longitude
                ))
            }.distinctBy { it.id }
            _mapListings.postValue(filtered)
        }
    }

    fun addListing(listing: Listing, callback: () -> Unit) {
        val geohash = GeoFireUtils.getGeoHashForLocation(GeoLocation(listing.locationData.geoPoint.latitude, listing.locationData.geoPoint.longitude))
        val updatedListing = listing.copy(locationData = listing.locationData.copy(geohash = geohash))
        firestore.collection("listings").document(updatedListing.id).set(updatedListing).addOnSuccessListener {
            executor.execute { database.listingDao().insert(updatedListing)
                mainHandler.post { callback() }
            }
        }
    }

    fun deleteListing(listingId: String, callback: () -> Unit) {
        firestore.collection("listings").document(listingId).delete().addOnSuccessListener {
            executor.execute { database.listingDao().deleteById(listingId)
                mainHandler.post { callback() }
            }
        }
    }

    fun updateListing(listing: Listing, callback: () -> Unit) {
        val geohash = GeoFireUtils.getGeoHashForLocation(GeoLocation(listing.locationData.geoPoint.latitude, listing.locationData.geoPoint.longitude))
        val updatedListing = listing.copy(locationData = listing.locationData.copy(geohash = geohash))
        firestore.collection("listings").document(updatedListing.id).set(updatedListing).addOnSuccessListener {
            executor.execute { database.listingDao().insert(updatedListing)
                mainHandler.post { callback() }
            }
        }
    }

    fun getListingsByOwner(userId: String) = MutableLiveData<List<Listing>>().apply {
        firestore.collection("listings").whereEqualTo("ownerId", userId).get().addOnSuccessListener { value = it.toObjects(Listing::class.java) }
    }

    fun getListingById(listingId: String) = MutableLiveData<Listing>().apply {
        firestore.collection("listings").document(listingId).get().addOnSuccessListener { value = it.toObject(Listing::class.java) }
    }

    fun getUserData(userId: String) = MutableLiveData<User>().apply {
        firestore.collection("users").document(userId).get().addOnSuccessListener { value = it.toObject(User::class.java) }
    }

    fun updateUserBio(userId: String, bio: String, callback: () -> Unit) {
        firestore.collection("users").document(userId).update("bio", bio).addOnSuccessListener { callback() }
    }

    private val storage = FirebaseStorage.getInstance()
    fun uploadImage(image: Bitmap, name: String, callback: (String?) -> Unit) {
        val imageRef = storage.reference.child("images/$name.jpg")
        val baos = ByteArrayOutputStream()
        image.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        imageRef.putBytes(baos.toByteArray()).addOnSuccessListener { imageRef.downloadUrl.addOnSuccessListener { callback(it.toString()) } }.addOnFailureListener { callback(null) }
    }

    fun uploadImages(bitmaps: List<Bitmap>, name: String, callback: (List<String>) -> Unit) {
        val urls = mutableListOf<String>()
        if (bitmaps.isEmpty()) return callback(urls)
        var count = 0
        bitmaps.forEachIndexed { i, bmp -> uploadImage(bmp, "${name}_$i") { url -> url?.let { urls.add(it) }
            if (++count == bitmaps.size) callback(urls)
        }}
    }
}
