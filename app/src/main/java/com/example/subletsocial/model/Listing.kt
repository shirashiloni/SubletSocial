package com.example.subletsocial.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.PropertyName
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class LocationData(
    val geoPoint: GeoPoint? = null,
    val geohash: String? = null
) {
    constructor() : this(null, null)
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
    
    @get:PropertyName("location")
    @set:PropertyName("location")
    var locationName: String = "",
    
    val locationData: LocationData? = null,
    val bedrooms: Int = 0,
    val bathrooms: Int = 0,
    val startDate: String = "",
    val endDate: String = "",
    val amenities: List<String> = listOf(),
    val lastUpdated: Long = 0
) {
    // Empty constructor for Firebase
    constructor() : this("", "", 0, "", listOf(), "", "", null, 0, 0, "", "", listOf(), 0)
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
    fun fromLocationData(value: LocationData?): String? = value?.let {
        val map = mapOf(
            "lat" to it.geoPoint?.latitude,
            "lng" to it.geoPoint?.longitude,
            "geohash" to it.geohash
        )
        gson.toJson(map)
    }

    @TypeConverter
    fun toLocationData(value: String?): LocationData? = value?.let {
        val map: Map<String, Any?> = gson.fromJson(it, object : TypeToken<Map<String, Any?>>() {}.type)
        val lat = map["lat"] as? Double
        val lng = map["lng"] as? Double
        val geohash = map["geohash"] as? String
        LocationData(if (lat != null && lng != null) GeoPoint(lat, lng) else null, geohash)
    }
}