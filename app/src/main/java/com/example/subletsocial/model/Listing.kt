package com.example.subletsocial.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.google.firebase.firestore.GeoPoint
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class LocationData(
    val geoPoint: GeoPoint = GeoPoint(0.0, 0.0),
    val geohash: String = ""
) {
    constructor() : this(GeoPoint(0.0, 0.0), "")
}

@Entity(tableName = "listings")
data class Listing(
    @PrimaryKey
    val id: String = "",
    val title: String = "",
    val price: Int = 0,
    val description: String = "",
    val imageUrls: List<String> = listOf(),
    val ownerId: String = "",
    val locationName: String = "",
    val locationData: LocationData = LocationData(),
    val bedrooms: Int = 0,
    val bathrooms: Int = 0,
    val startDate: String = "",
    val endDate: String = "",
    val amenities: List<String> = listOf(),
    val lastUpdated: Long = 0
) {
    // Empty constructor for Firebase
    constructor() : this("", "", 0, "", listOf(), "", "", LocationData(), 0, 0, "", "", listOf(), 0)
}

class ListingTypeConverters {
    private val gson = Gson()

    @TypeConverter
    fun fromStringList(value: List<String>): String = gson.toJson(value)

    @TypeConverter
    fun toStringList(value: String): List<String> {
        val listType = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, listType)
    }

    @TypeConverter
    fun fromLocationData(value: LocationData): String {
        val map = mapOf(
            "lat" to value.geoPoint.latitude,
            "lng" to value.geoPoint.longitude,
            "geohash" to value.geohash
        )
        return gson.toJson(map)
    }

    @TypeConverter
    fun toLocationData(value: String): LocationData {
        val map: Map<String, Any?> = gson.fromJson(value, object : TypeToken<Map<String, Any?>>() {}.type)
        val lat = map["lat"] as? Double ?: 0.0
        val lng = map["lng"] as? Double ?: 0.0
        val geohash = map["geohash"] as? String ?: ""
        return LocationData(GeoPoint(lat, lng), geohash)
    }
}
