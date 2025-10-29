package com.example.letslink

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import java.util.UUID
import androidx.core.content.edit
//this class handles tracking users locally so that their id can be tracked  ( Ketul Patel ,2015.)
open class SessionManager(context: Context) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("UserSession", Context.MODE_PRIVATE)
    companion object {
        const val KEY_USER_ID = "user_id"
         const val KEY_IS_LOGGED_IN = "is_logged_in"
         const val KEY_USER_EMAIL = "user_email"
         const val KEY_USER_NAME = "user_name"
    }

    fun saveUserSession(userId: String, email: String, name: String) {
        sharedPreferences.edit {
            putString(KEY_USER_ID, userId.toString())
            putString(KEY_USER_EMAIL, email)
            putString(KEY_USER_NAME, name)
            putBoolean(KEY_IS_LOGGED_IN, true)
        }

    }

    open fun getUserId(): UUID? {
        val userIdString = sharedPreferences.getString(KEY_USER_ID, null) ?: return null
        return try {
            UUID.fromString(userIdString)
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    fun isLoggedIn(): Boolean {
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false) && getUserId() != null
    }

    fun clearSession() {
        val editor = sharedPreferences.edit()
        editor.clear()
        editor.apply()
    }

}