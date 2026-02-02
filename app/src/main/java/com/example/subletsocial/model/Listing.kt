package com.example.subletsocial.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "listings")
data class Listing(
    @PrimaryKey
    val id: String,
    val title: String,
    val price: Int,
    val description: String,
    val imageUrl: String,
    val ownerId: String,
    val lastUpdated: Long
) {
    constructor() : this("", "", 0, "", "", "", 0)
}