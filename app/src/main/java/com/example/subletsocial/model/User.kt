package com.example.subletsocial.model

data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val avatarUrl: String = "",
    val bio: String = "",
    val followersCount: Int = 0,
    val followingCount: Int = 0
)