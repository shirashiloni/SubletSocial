package com.example.subletsocial.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@Entity(tableName = "listings")
data class Listing(
    @PrimaryKey
    val id: String,
    val title: String,
    val price: Int,
    val description: String,
    val imageUrl: String,
    val ownerId: String,
    val location: String,
    val bedrooms: Int,
    val bathrooms: Int,
    val startDate: String,
    val endDate: String,
    val amenities: List<String>,
    val lastUpdated: Long
) {
    constructor() : this("", "", 0, "", "", "", "", 0, 0, "", "", listOf(), 0)
}

class ListingTypeConverters {
    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return Gson().toJson(value)
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        val listType = object : TypeToken<List<String>>() {}.type
        return Gson().fromJson(value, listType)
    }
}