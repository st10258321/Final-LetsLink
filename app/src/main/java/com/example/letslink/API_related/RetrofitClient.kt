package com.example.letslink.API_related

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.example.letslink.API_related.LetsLinkAPI

/**
 * object to configure and provide the Retrofit client and API services.
 * //(Philipp Lackner ,2021)
 *
 */
object RetrofitClient {

    //link for the api later on
    private const val BASE_URL = "https://letslink-api.onrender.com/"

    // Initialize Retrofit
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            // Use Gson to convert Kotlin data classes to JSON
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val letsLinkAPI: LetsLinkAPI by lazy {
        retrofit.create(LetsLinkAPI::class.java)
    }
}