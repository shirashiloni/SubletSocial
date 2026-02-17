package com.example.subletsocial.model

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.subletsocial.base.MyApplication
import com.example.subletsocial.dao.ListingDao

@TypeConverters(ListingTypeConverters::class)
@Database(entities = [Listing::class], version = 1)
abstract class AppLocalDbRepository : RoomDatabase() {
    abstract fun listingDao(): ListingDao
}

object AppLocalDb {

    val db: AppLocalDbRepository by lazy {

        val context = MyApplication.Globals.appContext
            ?: throw IllegalStateException("Application context not available")

        Room.databaseBuilder(
            context,
            AppLocalDbRepository::class.java,
            "sublet_database.db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }
}