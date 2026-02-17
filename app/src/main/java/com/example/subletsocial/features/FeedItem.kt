package com.example.subletsocial.features

import com.example.subletsocial.model.Listing

sealed class FeedItem {
    data class SingleListing(val listing: Listing) : FeedItem()
    data class ListingGroup(val title: String, val listings: List<Listing>) : FeedItem()
}
