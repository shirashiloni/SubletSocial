package com.example.subletsocial.features

import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.switchMap
import com.example.subletsocial.model.Listing
import com.example.subletsocial.model.Model
import com.example.subletsocial.model.User

class ProfileViewModel : ViewModel() {

    private val _profileUserId = MutableLiveData<String>()

    val user: LiveData<User> = _profileUserId.switchMap { userId ->
        Model.shared.getUserData(userId)
    }

    val listings: LiveData<List<Listing>> = _profileUserId.switchMap { userId ->
        Model.shared.getListingsByOwner(userId)
    }

    private val _followStatusTrigger = MutableLiveData<Pair<String, String>>()

    val isFollowing: LiveData<Boolean> = _followStatusTrigger.switchMap { (currentUserId, profileUserId) ->
        Model.shared.checkIfFollowing(currentUserId, profileUserId)
    }

    fun loadProfile(userId: String) {
        if (_profileUserId.value == userId) return
        _profileUserId.value = userId
    }

    fun checkFollowStatus(currentUserId: String, profileUserId: String) {
        val newStatus = Pair(currentUserId, profileUserId)
        if (_followStatusTrigger.value == newStatus) return
        _followStatusTrigger.value = newStatus
    }

    fun toggleFollow(currentUserId: String, profileUserId: String, isCurrentlyFollowing: Boolean, onComplete: () -> Unit) {
        Model.shared.toggleFollow(currentUserId, profileUserId, isCurrentlyFollowing, onComplete)
    }

    fun updateUserBio(userId: String, newBio: String, onComplete: () -> Unit) {
        Model.shared.updateUserBio(userId, newBio, onComplete)
    }

    fun uploadAndSetAvatar(bitmap: Bitmap, userId: String, onComplete: (Boolean) -> Unit) {
        Model.shared.uploadImage(bitmap, "avatar_$userId") { url ->
            if (url != null) {
                Model.shared.updateUserAvatar(userId, url) {
                    onComplete(true)
                }
            } else {
                onComplete(false)
            }
        }
    }
}