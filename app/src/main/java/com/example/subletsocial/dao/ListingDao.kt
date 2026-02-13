package com.example.subletsocial.dao
import com.example.subletsocial.model.Listing

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ListingDao {

    @Query("SELECT * FROM listings ORDER BY lastUpdated DESC")
    fun getAll(): LiveData<List<Listing>>

    @Query("SELECT * FROM listings WHERE id = :listingId")
    fun getListingById(listingId: String): LiveData<Listing>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(listing: Listing)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(listings: List<Listing>)

    @Delete
    fun delete(listing: Listing)

    @Query("DELETE FROM listings")
    fun deleteAll()
}