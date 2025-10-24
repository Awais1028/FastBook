package com.example.myapplication.users
data class Users(
    private var uid: String = "",
    private var fullName: String = "",
    private var profileImageUrl: String = "",
    private var coverImageUrl: String = "",
    private var status: String = "",
    private var search: String = "",
    private var facebook: String = "",
    private var instagram: String = "",
    private var website: String = "",

    // --- ADD THESE MISSING FIELDS ---
    private var email: String = "",
    private var followers: Map<String, Boolean> = emptyMap(),
    private var following: Map<String, Boolean> = emptyMap()
) {
    // Getter methods are needed for Firebase to correctly map the data
    fun getUID(): String {
        return uid
    }

    fun getFullName(): String {
        return fullName
    }

    fun getProfileImageUrl(): String {
        return profileImageUrl
    }

    // Add other getters if you need them in your adapters
    fun getEmail(): String {
        return email
    }
}