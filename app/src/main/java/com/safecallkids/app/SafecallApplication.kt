package com.safecallkids.app

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import java.util.Locale

class SafecallApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        // Apply saved language on app start
        applySavedLanguage()
    }
    
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(updateBaseContextLocale(base))
    }
    
    private fun applySavedLanguage() {
        val prefs = getSharedPreferences("safecall_prefs", Context.MODE_PRIVATE)
        val language = prefs.getString("language", "pt") ?: "pt"
        setAppLocale(language)
    }
    
    private fun updateBaseContextLocale(context: Context): Context {
        val prefs = context.getSharedPreferences("safecall_prefs", Context.MODE_PRIVATE)
        val language = prefs.getString("language", "pt") ?: "pt"
        
        val locale = Locale(language)
        Locale.setDefault(locale)
        
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.createConfigurationContext(config)
        } else {
            @Suppress("DEPRECATION")
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
            context
        }
    }
    
    private fun setAppLocale(languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        
        val config = Configuration()
        config.setLocale(locale)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            createConfigurationContext(config)
        } else {
            @Suppress("DEPRECATION")
            resources.updateConfiguration(config, resources.displayMetrics)
        }
    }
}
