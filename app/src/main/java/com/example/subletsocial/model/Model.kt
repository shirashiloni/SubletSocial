package com.example.subletsocial.model // Make sure this matches your package!

import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.firestore.FirebaseFirestore
import java.util.concurrent.Executors
import android.graphics.Bitmap
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream

class Model private constructor() {

    private val database = AppLocalDb.db

    private val firestore = FirebaseFirestore.getInstance()

    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    // 4. Loading State (Optional: lets the UI show a spinner when syncing)
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
                    database.listingDao().insertAll(listings)

                    listingsLoadingState.postValue(LoadingState.LOADED)
                }
            }
            .addOnFailureListener {
                listingsLoadingState.postValue(LoadingState.LOADED)
            }
    }

    fun addListing(listing: Listing, callback: () -> Unit) {
        firestore.collection("listings")
            .document(listing.id)
            .set(listing)
            .addOnSuccessListener {
                executor.execute {
                    database.listingDao().insert(listing)

                    mainHandler.post {
                        callback()
                    }
                }
            }
            .addOnFailureListener {
            }
    }

    private val storage = FirebaseStorage.getInstance()

    fun uploadImage(image: Bitmap, name: String, callback: (String?) -> Unit) {
        val storageRef = storage.reference
        val imageRef = storageRef.child("images/$name.jpg")

        val baos = ByteArrayOutputStream()
        image.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val data = baos.toByteArray()

        val uploadTask = imageRef.putBytes(data)
        uploadTask.addOnFailureListener {
            callback(null)
        }.addOnSuccessListener { taskSnapshot ->
            imageRef.downloadUrl.addOnSuccessListener { uri ->
                callback(uri.toString())
            }
        }
    }

    fun uploadImages(bitmaps: List<Bitmap>, name: String, callback: (List<String>) -> Unit) {
        val uploadedUrls = mutableListOf<String>()
        var count = 0

        for (i in bitmaps.indices) {
            val bitmap = bitmaps[i]
            val uniqueName = "${name}_$i"

            uploadImage(bitmap, uniqueName) { url ->
                if (url != null) {
                    uploadedUrls.add(url)
                }
                count++

                if (count == bitmaps.size) {
                    callback(uploadedUrls)
                }
            }
        }
    }
}