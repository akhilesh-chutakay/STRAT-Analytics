package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.data.model.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

class UserProfileRepository(private val context: Context) {

    private val prefs = context.getSharedPreferences("iastrat_user_profile", Context.MODE_PRIVATE)

    private val _userProfile = MutableStateFlow(loadProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    private fun loadProfile(): UserProfile {
        val rawJson = prefs.getString("profile_data", null)
            ?: context.getSharedPreferences("ia_analytics_user_profile", Context.MODE_PRIVATE).getString("profile_data", null)
        if (rawJson != null) {
            try {
                val json = JSONObject(rawJson)
                return UserProfile(
                    id = json.optString("id", "user_primary"),
                    name = json.optString("name", "Akhilesh Chutakay"),
                    email = json.optString("email", "akhilesh.chutakay8@gmail.com"),
                    title = json.optString("title", "Venture Architect & Angel Strategist"),
                    avatarUrl = json.optString("avatarUrl", "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=400&auto=format&fit=crop&q=80"),
                    bio = json.optString("bio", "Building high-conviction B2B SaaS & AI automation ventures with validated founder demand and data-driven moats."),
                    organization = json.optString("organization", "Strat Analytics Labs"),
                    experienceLevel = json.optString("experienceLevel", "Venture Partner / Founder"),
                    targetPortfolioARR = json.optDouble("targetPortfolioARR", 1_000_000.0),
                    currencySymbol = json.optString("currencySymbol", "$"),
                    isGoogleDriveLinked = json.optBoolean("isGoogleDriveLinked", true),
                    joinedDate = json.optString("joinedDate", "Aug 2026"),
                    tier = json.optString("tier", "Executive Pro")
                )
            } catch (e: Exception) {
                // fallback to default
            }
        }
        return UserProfile()
    }

    fun updateProfile(profile: UserProfile) {
        val json = JSONObject().apply {
            put("id", profile.id)
            put("name", profile.name)
            put("email", profile.email)
            put("title", profile.title)
            put("avatarUrl", profile.avatarUrl)
            put("bio", profile.bio)
            put("organization", profile.organization)
            put("experienceLevel", profile.experienceLevel)
            put("targetPortfolioARR", profile.targetPortfolioARR)
            put("currencySymbol", profile.currencySymbol)
            put("isGoogleDriveLinked", profile.isGoogleDriveLinked)
            put("joinedDate", profile.joinedDate)
            put("tier", profile.tier)
        }

        prefs.edit().putString("profile_data", json.toString()).apply()
        _userProfile.value = profile
    }

    fun updateAvatar(newAvatarUrl: String) {
        val current = _userProfile.value
        updateProfile(current.copy(avatarUrl = newAvatarUrl))
    }

    /**
     * Copies selected image Uri into app's persistent internal storage
     * and sets it as the user avatar.
     */
    suspend fun saveUploadedImageFromUri(uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val avatarDir = File(context.filesDir, "avatars").apply { mkdirs() }
            val destFile = File(avatarDir, "user_custom_dp_${System.currentTimeMillis()}.jpg")

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }

            val savedUriString = Uri.fromFile(destFile).toString()
            withContext(Dispatchers.Main) {
                updateAvatar(savedUriString)
            }
            savedUriString
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
