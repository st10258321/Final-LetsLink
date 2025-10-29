package com.example.letslink.API_related

import com.example.letslink.SessionManager
import kotlin.getValue
import android.content.Context
import com.example.letslink.local_database.GroupDao
import com.example.letslink.local_database.LetsLinkDB
import com.example.letslink.local_database.UserDao
import com.google.firebase.database.DatabaseReference

/**
 * This class defines what is needed
 * (Android Developers ,(n.d.).)
 */
class AppContainer(applicationContext: Context) {

    //sessionManger
    val sessionManager: SessionManager by lazy {
        SessionManager(applicationContext)
    }

    // Database
    val database: LetsLinkDB by lazy {
        // Initialize Room here
        LetsLinkDB.getDatabase(applicationContext)
    }

    private val groupDao: GroupDao by lazy {
        database.groupDao()
    }

    // Network Dependencies (Retrofit Only)
    // that provides the API interface.
    //(Philipp Lackner ,2021)
    private val groupApiService: LetsLinkAPI by lazy {
        RetrofitClient.letsLinkAPI
    }
    private val userDao : UserDao by lazy{
        database.userDao()
    }

    private val db : DatabaseReference = com.google.firebase.database.FirebaseDatabase.getInstance().reference

    // group repo acts as a bridge between the app and api
    val groupRepository: GroupRepo by lazy {
        GroupRepo(
            groupDao = groupDao,
            groupApiService = groupApiService,
            sessionManager = sessionManager,
            db  = db,
            userDao = userDao
        )
    }
}