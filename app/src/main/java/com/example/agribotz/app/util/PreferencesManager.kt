package com.example.agribotz.app.util

import android.content.Context
import com.example.agribotz.app.domain.Site
import com.example.agribotz.app.domain.User
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class PreferencesManager(context: Context) {
    private val prefs = context.getSharedPreferences("com.agribotz.app", Context.MODE_PRIVATE)
    private val gson = Gson()

    // --- LOGIN DATA ---
    fun saveLoginData(accessToken: String, renewToken: String, user: User, sites: List<Site>) {
        prefs.edit()
            .putString("accessToken", accessToken)
            .putString("renewToken", renewToken)
            .putString("user", gson.toJson(user))
            .putString("sites", gson.toJson(sites))
            .apply()
    }

    fun getAccessToken(): String? = prefs.getString("accessToken", null)
    fun getRenewToken(): String? = prefs.getString("renewToken", null)

    // --- USER ---
    fun getUser(): User? {
        val json = prefs.getString("user", null) ?: return null
        return try {
            gson.fromJson(json, User::class.java)
        } catch (e: Exception) {
            null
        }
    }

    fun saveUser(user: User) {
        prefs.edit().putString("user", gson.toJson(user)).apply()
    }

    // --- SITES ---
    fun getSites(): List<Site>? {
        val json = prefs.getString("sites", null) ?: return null
        return try {
            val type = object : TypeToken<List<Site>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            null
        }
    }

    fun saveSites(sites: List<Site>) {
        prefs.edit().putString("sites", gson.toJson(sites)).apply()
    }

    // --- CLEAR ALL ---
    fun clearAll() {
        prefs.edit().clear().apply()
    }
}
