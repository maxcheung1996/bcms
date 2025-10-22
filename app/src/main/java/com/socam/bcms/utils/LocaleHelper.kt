package com.socam.bcms.utils

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.util.Log
import java.util.Locale

/**
 * LocaleHelper - Utility for runtime locale changes
 * Handles language switching for the entire application
 * 
 * Usage:
 * - Call LocaleHelper.setLocale(context, "zh-rTW") to change to Traditional Chinese
 * - Call LocaleHelper.applyLanguageToActivity(activity, "en") to apply to current activity
 */
object LocaleHelper {

    /**
     * Available languages in the app
     */
    object Languages {
        const val ENGLISH = "en"
        const val TRADITIONAL_CHINESE = "tc"  // Maps to zh-rTW
        const val SIMPLIFIED_CHINESE = "cn"   // Maps to zh-rCN
    }

    /**
     * Convert UI language codes to Android locale strings
     */
    private fun mapLanguageToLocale(language: String): String {
        return when (language) {
            Languages.ENGLISH -> "en"
            Languages.TRADITIONAL_CHINESE -> "zh-rTW"
            Languages.SIMPLIFIED_CHINESE -> "zh-rCN"
            else -> "en" // Default to English
        }
    }

    /**
     * Set locale for the application context
     * This affects the entire app's string resources
     */
    fun setLocale(context: Context, language: String): Context {
        val localeString = mapLanguageToLocale(language)
        val locale = createLocale(localeString)
        
        Locale.setDefault(locale)
        
        return updateResources(context, locale)
    }

    /**
     * Apply language change to current Activity with smooth transition
     * Uses manual resource update for better UX, with recreation as fallback
     */
    fun applyLanguageToActivity(activity: Activity, language: String) {
        val localeString = mapLanguageToLocale(language)
        val locale = createLocale(localeString)
        
        Log.d("LocaleHelper", "Applying language to activity: $language -> $localeString")
        
        // Set as default locale globally  
        Locale.setDefault(locale)
        
        // Update application context resources first
        setLocale(activity.applicationContext, language)
        
        // Save language preference immediately
        try {
            activity.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
                .edit()
                .putString("current_language", language)
                .apply()
        } catch (e: Exception) {
            Log.w("LocaleHelper", "Could not save language preference: ${e.message}")
        }
        
        // Use smooth manual update method (no flickering)
        try {
            applyLanguageWithoutRecreation(activity, language)
            Log.d("LocaleHelper", "✅ Language applied smoothly without recreation: $language")
        } catch (e: Exception) {
            Log.w("LocaleHelper", "Manual update failed, using recreation as fallback: ${e.message}")
            // Fallback: Activity recreation (causes flicker but is most reliable)
            try {
                activity.recreate()
                Log.d("LocaleHelper", "✅ Fallback: Activity recreated with new language: $language")
            } catch (recreateException: Exception) {
                Log.e("LocaleHelper", "❌ Both methods failed: ${recreateException.message}")
            }
        }
    }
    
    /**
     * Apply language change without Activity recreation (smooth method)
     * Enhanced manual resource update for better UX
     */
    private fun applyLanguageWithoutRecreation(activity: Activity, language: String) {
        val localeString = mapLanguageToLocale(language)
        val locale = createLocale(localeString)
        
        // Update activity resources (deprecated but still works)
        val activityConfig = Configuration(activity.resources.configuration)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            activityConfig.setLocale(locale)
        } else {
            @Suppress("DEPRECATION")
            activityConfig.locale = locale
        }
        
        // Update activity resources
        @Suppress("DEPRECATION")
        activity.resources.updateConfiguration(activityConfig, activity.resources.displayMetrics)
        
        // Also update application context resources to ensure consistency
        val appConfig = Configuration(activity.applicationContext.resources.configuration)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            appConfig.setLocale(locale)
        } else {
            @Suppress("DEPRECATION")
            appConfig.locale = locale
        }
        @Suppress("DEPRECATION")
        activity.applicationContext.resources.updateConfiguration(appConfig, activity.applicationContext.resources.displayMetrics)
        
        // Enhanced UI refresh - force recreation of views with new locale
        try {
            val rootView = activity.findViewById<android.view.View>(android.R.id.content)
            
            // Force view tree refresh
            rootView?.let { root ->
                root.invalidate()
                root.requestLayout()
                
                // Trigger a configuration change event to refresh all views
                if (activity is androidx.appcompat.app.AppCompatActivity) {
                    // Post to UI thread to ensure smooth execution
                    root.post {
                        try {
                            // Force action bar refresh
                            activity.supportActionBar?.let { actionBar ->
                                val title = actionBar.title
                                actionBar.title = ""
                                actionBar.title = title
                            }
                            
                            // Trigger fragment refresh if we're in SettingsFragment
                            val fragmentManager = activity.supportFragmentManager
                            val currentFragment = fragmentManager.findFragmentById(android.R.id.content)
                            if (currentFragment != null) {
                                // Notify fragment of configuration change
                                currentFragment.onConfigurationChanged(activityConfig)
                            }
                        } catch (e: Exception) {
                            Log.w("LocaleHelper", "Could not refresh action bar: ${e.message}")
                        }
                    }
                }
            }
            
            Log.d("LocaleHelper", "✅ Enhanced manual language update completed: $language")
        } catch (e: Exception) {
            Log.w("LocaleHelper", "Could not perform enhanced UI refresh: ${e.message}")
        }
    }

    /**
     * Get current system locale for the context
     */
    fun getCurrentLocale(context: Context): Locale {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.resources.configuration.locales[0]
        } else {
            @Suppress("DEPRECATION")
            context.resources.configuration.locale
        }
    }

    /**
     * Convert Android locale back to UI language code
     */
    fun getLanguageFromLocale(context: Context): String {
        val locale = getCurrentLocale(context)
        return when {
            locale.language == "zh" && locale.country == "TW" -> Languages.TRADITIONAL_CHINESE
            locale.language == "zh" && locale.country == "CN" -> Languages.SIMPLIFIED_CHINESE
            else -> Languages.ENGLISH
        }
    }

    /**
     * Create locale object from locale string
     */
    private fun createLocale(localeString: String): Locale {
        return when {
            localeString.contains("-r") -> {
                // Handle region codes like "zh-rTW", "zh-rCN"
                val parts = localeString.split("-r")
                Locale(parts[0], parts[1])
            }
            else -> {
                // Handle simple language codes like "en"
                Locale(localeString)
            }
        }
    }

    /**
     * Update context resources with new locale
     */
    private fun updateResources(context: Context, locale: Locale): Context {
        val config = Configuration(context.resources.configuration)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(locale)
            return context.createConfigurationContext(config)
        } else {
            @Suppress("DEPRECATION")
            config.locale = locale
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
            return context
        }
    }

    /**
     * Check if a language is supported by the app
     */
    fun isSupportedLanguage(language: String): Boolean {
        return language in listOf(
            Languages.ENGLISH,
            Languages.TRADITIONAL_CHINESE,
            Languages.SIMPLIFIED_CHINESE
        )
    }

    /**
     * Get display name for language (for debugging)
     */
    fun getLanguageDisplayName(language: String): String {
        return when (language) {
            Languages.ENGLISH -> "English"
            Languages.TRADITIONAL_CHINESE -> "繁體中文"
            Languages.SIMPLIFIED_CHINESE -> "简体中文"
            else -> "Unknown"
        }
    }
    
    /**
     * Get current language from SharedPreferences (fallback method)
     */
    fun getCurrentLanguageFromPrefs(context: Context): String {
        return try {
            context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                .getString("current_language", "en") ?: "en"
        } catch (e: Exception) {
            "en"
        }
    }
    
    /**
     * Apply saved language to Activity - to be called in attachBaseContext
     */
    fun applyLanguageToActivityContext(context: Context): Context {
        val savedLanguage = getCurrentLanguageFromPrefs(context)
        return setLocale(context, savedLanguage)
    }
}
