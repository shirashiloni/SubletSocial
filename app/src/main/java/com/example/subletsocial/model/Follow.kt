package com.example.subletsocial.model

data class Follow(
    val id: String = "",
    val followerId: String = "",
    val followedId: String = "",
    val timestamp: Long = 0
)