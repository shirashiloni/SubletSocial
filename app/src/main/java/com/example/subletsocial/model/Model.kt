package com.example.subletsocial.model

import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.firestore.FirebaseFirestore
import java.util.concurrent.Executors
import android.graphics.Bitmap
import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream

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

    fun deleteListing(listingId: String, callback: () -> Unit) {
        firestore.collection("listings").document(listingId)
            .delete()
            .addOnSuccessListener {
                executor.execute {
                    database.listingDao().deleteById(listingId)
                    mainHandler.post {
                        callback()
                    }
                }
            }
    }

    fun updateListing(listing: Listing, callback: () -> Unit) {
        firestore.collection("listings").document(listing.id)
            .set(listing)
            .addOnSuccessListener {
                executor.execute {
                    database.listingDao().insert(listing)
                    mainHandler.post {
                        callback()
                    }
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
            .addOnFailureListener {
                // Handle error
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
            .addOnSuccessListener {
                callback()
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
        uploadTask.addOnFailureListener { exception ->
            android.util.Log.e("UPLOAD_ERROR", "Upload failed for $name", exception)
            callback(null)
        }.addOnSuccessListener { taskSnapshot ->
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

    fun toggleFollow(currentUserId: String, targetUserId: String, isFollowing: Boolean, callback: () -> Unit) {
        val db = FirebaseFirestore.getInstance()
        val followDocId = "${currentUserId}_${targetUserId}"
        val followRef = db.collection("follows").document(followDocId)
        val currentUserRef = db.collection("users").document(currentUserId)
        val targetUserRef = db.collection("users").document(targetUserId)

        db.runTransaction { transaction ->
            if (isFollowing) {
                transaction.delete(followRef)

                transaction.update(currentUserRef, "followingCount", FieldValue.increment(-1))
                transaction.update(targetUserRef, "followersCount", FieldValue.increment(-1))
            } else {
                val data = hashMapOf(
                    "followerId" to currentUserId,
                    "followedId" to targetUserId,
                    "timestamp" to System.currentTimeMillis()
                )
                transaction.set(followRef, data)

                transaction.update(currentUserRef, "followingCount", FieldValue.increment(1))
                transaction.update(targetUserRef, "followersCount", FieldValue.increment(1))
            }
        }.addOnSuccessListener {
            callback()
        }.addOnFailureListener { e ->
            Log.e("FollowSystem", "Transaction failed: ", e)
        }
    }

    fun checkIfFollowing(currentUserId: String?, targetUserId: String): LiveData<Boolean> {
        if(currentUserId == null) return MutableLiveData(false)

        val result = MutableLiveData<Boolean>()
        val docId = "${currentUserId}_${targetUserId}"

        FirebaseFirestore.getInstance().collection("follows").document(docId)
            .addSnapshotListener { document, _ ->
                result.value = document != null && document.exists()
            }

        return result
    }
}