package com.example.subletsocial.model

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.maps.model.LatLngBounds
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors
import android.graphics.Bitmap
import com.firebase.geofire.GeoFireUtils
import com.firebase.geofire.GeoLocation
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.QuerySnapshot

class Model private constructor() {

    private val database = AppLocalDb.db
    private val firestore = FirebaseFirestore.getInstance()
    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    val listingsLoadingState = MutableLiveData<LoadingState>()

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
        firestore.collection("listings")
            .get()
            .addOnSuccessListener { result ->
                val listings = result.toObjects(Listing::class.java)
                executor.execute {
                    database.listingDao().deleteAll()
                    database.listingDao().insertAll(listings)
                    listingsLoadingState.postValue(LoadingState.LOADED)
                }
            }
            .addOnFailureListener {
                listingsLoadingState.postValue(LoadingState.LOADED)
            }
    }

    fun refreshListingsInBounds(bounds: LatLngBounds) {
        listingsLoadingState.postValue(LoadingState.LOADING)

        val center = bounds.center
        val results = FloatArray(1)
        android.location.Location.distanceBetween(
            bounds.southwest.latitude, bounds.southwest.longitude,
            bounds.northeast.latitude, bounds.northeast.longitude, results
        )
        val radiusInMeters = (results[0] / 2).toDouble()

        val centerLocation = GeoLocation(center.latitude, center.longitude)
        val boundsList = GeoFireUtils.getGeoHashQueryBounds(centerLocation, radiusInMeters)
        
        val tasks = boundsList.map { b ->
            firestore.collection("listings")
                .orderBy("locationData.geohash")
                .startAt(b.startHash)
                .endAt(b.endHash)
                .get()
        }

        Tasks.whenAllSuccess<QuerySnapshot>(tasks)
            .addOnSuccessListener { snapshots ->
                val allListings = mutableListOf<Listing>()
                for (snapshot in snapshots) {
                    allListings.addAll(snapshot.toObjects(Listing::class.java))
                }

                val filtered = allListings.filter { listing ->
                    listing.locationData?.geoPoint?.let { geo ->
                        bounds.contains(com.google.android.gms.maps.model.LatLng(geo.latitude, geo.longitude))
                    } ?: false
                }.distinctBy { it.id }

                executor.execute {
                    database.listingDao().insertAll(filtered)
                    listingsLoadingState.postValue(LoadingState.LOADED)
                }
            }
            .addOnFailureListener {
                listingsLoadingState.postValue(LoadingState.LOADED)
            }
    }

    fun addListing(listing: Listing, callback: () -> Unit) {
        // Generate geohash before saving
        val updatedListing = listing.locationData?.geoPoint?.let { geo ->
            val geohash = GeoFireUtils.getGeoHashForLocation(GeoLocation(geo.latitude, geo.longitude))
            listing.copy(locationData = listing.locationData.copy(geohash = geohash))
        } ?: listing

        firestore.collection("listings")
            .document(updatedListing.id)
            .set(updatedListing)
            .addOnSuccessListener {
                executor.execute {
                    database.listingDao().insert(updatedListing)
                    mainHandler.post { callback() }
                }
            }
    }

    fun deleteListing(listingId: String, callback: () -> Unit) {
        firestore.collection("listings").document(listingId)
            .delete()
            .addOnSuccessListener {
                executor.execute {
                    database.listingDao().deleteById(listingId)
                    mainHandler.post { callback() }
                }
            }
    }

    fun updateListing(listing: Listing, callback: () -> Unit) {
        val updatedListing = listing.locationData?.geoPoint?.let { geo ->
            val geohash = GeoFireUtils.getGeoHashForLocation(GeoLocation(geo.latitude, geo.longitude))
            listing.copy(locationData = listing.locationData.copy(geohash = geohash))
        } ?: listing

        firestore.collection("listings").document(updatedListing.id)
            .set(updatedListing)
            .addOnSuccessListener {
                executor.execute {
                    database.listingDao().insert(updatedListing)
                    mainHandler.post { callback() }
                }
            }
    }

    fun getListingsByOwner(userId: String): LiveData<List<Listing>> {
        val liveData = MutableLiveData<List<Listing>>()
        firestore.collection("listings")
            .whereEqualTo("ownerId", userId)
            .get()
            .addOnSuccessListener { result ->
                val listings = result.toObjects(Listing::class.java)
                liveData.postValue(listings)
            }
        return liveData
    }

    fun getListingById(listingId: String): LiveData<Listing> {
        val liveData = MutableLiveData<Listing>()
        firestore.collection("listings").document(listingId).get()
            .addOnSuccessListener { document ->
                val listing = document.toObject(Listing::class.java)
                liveData.postValue(listing)
            }
        return liveData
    }

    fun getUserData(userId: String): LiveData<User> {
        val liveData = MutableLiveData<User>()
        firestore.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                val user = document.toObject(User::class.java)
                liveData.postValue(user)
            }
        return liveData
    }

    fun updateUserBio(userId: String, bio: String, callback: () -> Unit) {
        firestore.collection("users").document(userId)
            .update("bio", bio)
            .addOnSuccessListener { callback() }
    }

    private val storage = FirebaseStorage.getInstance()

    fun uploadImage(image: Bitmap, name: String, callback: (String?) -> Unit) {
        val storageRef = storage.reference
        val imageRef = storageRef.child("images/$name.jpg")

        val baos = ByteArrayOutputStream()
        image.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val data = baos.toByteArray()

        val uploadTask = imageRef.putBytes(data)
        uploadTask.addOnFailureListener { callback(null) }
            .addOnSuccessListener {
                imageRef.downloadUrl.addOnSuccessListener { uri ->
                    callback(uri.toString())
                }
            }
    }

    fun uploadImages(bitmaps: List<Bitmap>, name: String, callback: (List<String>) -> Unit) {
        if (bitmaps.isEmpty()) {
            callback(emptyList())
            return
        }

        val uploadedUrls = mutableListOf<String>()
        var count = 0

        for (i in bitmaps.indices) {
            val bitmap = bitmaps[i]
            val uniqueName = "${name}_$i"

            uploadImage(bitmap, uniqueName) { url ->
                if (url != null) uploadedUrls.add(url)
                count++
                if (count == bitmaps.size) callback(uploadedUrls)
            }
        }
    }
}
