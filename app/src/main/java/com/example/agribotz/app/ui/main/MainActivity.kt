package com.example.agribotz.app.ui.main

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import com.example.agribotz.databinding.ActivityMainBinding
import com.example.agribotz.R

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)

        // Check if the activity was launched from a Logout action
        if (intent.getBooleanExtra("NAVIGATE_TO_LOGIN", false)) {
            // Find the Navigation Controller
            val navHostFragment = supportFragmentManager
                .findFragmentById(R.id.nav_host_fragment_main) as NavHostFragment
            val navController = navHostFragment.navController

            // Immediately navigate past the StarterFragment to the LoginFragment
            navController.navigate(R.id.action_starterFragment_to_loginFragment)
        }
    }
}