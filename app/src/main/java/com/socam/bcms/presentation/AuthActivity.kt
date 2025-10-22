package com.socam.bcms.presentation

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.socam.bcms.R
import com.socam.bcms.databinding.ActivityAuthBinding
import com.socam.bcms.domain.AuthManager
import com.socam.bcms.utils.LocaleHelper

/**
 * Authentication activity that handles login flow
 * Contains navigation between login and main app screens
 */
class AuthActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityAuthBinding
    private lateinit var navController: NavController
    private lateinit var authManager: AuthManager
    
    /**
     * Apply saved language locale to Activity context
     * This ensures all strings are loaded in the correct language
     */
    override fun attachBaseContext(newBase: Context?) {
        val context = newBase ?: return
        val localizedContext = LocaleHelper.applyLanguageToActivityContext(context)
        super.attachBaseContext(localizedContext)
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        authManager = AuthManager.getInstance(this)
        
        setupNavigation()
        checkExistingAuthentication()
    }
    
    private fun setupNavigation(): Unit {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
    }
    
    private fun checkExistingAuthentication(): Unit {
        if (authManager.isAuthenticated()) {
            // User is already logged in, navigate to main app
            startActivity(Intent(this, com.socam.bcms.MainActivity::class.java))
            finish()
        }
    }
    
    /**
     * Called when user successfully logs in
     */
    fun onLoginSuccess(): Unit {
        println("AuthActivity: onLoginSuccess() called")
        val intent = Intent(this, com.socam.bcms.MainActivity::class.java)
        println("AuthActivity: Starting MainActivity with intent: $intent")
        startActivity(intent)
        println("AuthActivity: MainActivity started, calling finish()")
        finish()
    }
}
