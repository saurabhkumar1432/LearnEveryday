package com.learneveryday.app

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

class LearnEverydayApp : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Apply saved theme preference
        val prefsManager = PreferencesManager(this)
        val savedMode = prefsManager.getDarkModePreference()
        
        if (savedMode != -1) {
            AppCompatDelegate.setDefaultNightMode(savedMode)
        }
    }
}
