package com.example.letslink.API_related


import android.app.Application
import com.example.letslink.API_related.AppContainer

/**
 *  class to hold the global AppContainer instance.
 *///(Philipp Lackner ,2021)
class MyApp : Application() {

    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        // Initialize the container immediately when the app starts
        container = AppContainer(applicationContext = applicationContext)
    }
}
