package com.example.subletsocial.features

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.example.subletsocial.model.Listing
import com.example.subletsocial.model.Model

class FeedViewModel : ViewModel() {
    val listings: LiveData<List<Listing>> = Model.shared.getAllListings()
}