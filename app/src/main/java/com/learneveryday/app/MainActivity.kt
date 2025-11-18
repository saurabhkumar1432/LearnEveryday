package com.learneveryday.app

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.learneveryday.app.data.local.AppDatabase
import com.learneveryday.app.data.repository.CurriculumRepositoryImpl

import com.learneveryday.app.data.service.AIProviderFactory
import com.learneveryday.app.data.service.AIProviderFactory.ProviderType
import com.learneveryday.app.domain.model.Curriculum
import com.learneveryday.app.domain.model.Difficulty
import com.learneveryday.app.domain.model.GenerationMode
import com.learneveryday.app.domain.model.GenerationStatus
import com.learneveryday.app.domain.service.AIService
import com.learneveryday.app.domain.service.AIServiceImpl
import com.learneveryday.app.domain.service.AIResult
import com.learneveryday.app.domain.service.CurriculumRequest
import com.learneveryday.app.domain.service.GenerationRequest
import com.learneveryday.app.presentation.home.HomeFragment
import com.learneveryday.app.presentation.plans.LearningPlansFragment
import kotlinx.coroutines.launch
import java.util.UUID

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNav: BottomNavigationView
    private lateinit var prefsManager: PreferencesManager
    private lateinit var repository: CurriculumRepositoryImpl
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSupportActionBar(findViewById(R.id.toolbar))

        prefsManager = PreferencesManager(this)
        setupRepository()
        setupPermissions()
        setupBottomNav()

        // Load initial fragment
        if (savedInstanceState == null) {
            loadFragment(HomeFragment())
        }
        
        checkAndPromptForNotifications()
    }

    private fun setupRepository() {
        val database = AppDatabase.getInstance(applicationContext)
        repository = CurriculumRepositoryImpl(database.curriculumDao())
    }

    private fun setupPermissions() {
        requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                prefsManager.setNotificationsEnabled(true)
                NotificationScheduler.scheduleDailyReminder(this)
                Toast.makeText(this, "Notifications enabled", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupBottomNav() {
        bottomNav = findViewById(R.id.bottom_navigation)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    loadFragment(HomeFragment())
                    true
                }
                R.id.nav_plans -> {
                    loadFragment(LearningPlansFragment())
                    true
                }
                else -> false
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        // Inject callbacks if it's HomeFragment
        if (fragment is HomeFragment) {
            fragment.onGenerateCurriculum = { topic, _ ->
                if (prefsManager.isAIEnabled()) {
                    showGenerateCurriculumDialog(topic)
                } else {
                    showAISetupPrompt()
                }
            }
            fragment.onCustomTopic = {
                if (prefsManager.isAIEnabled()) {
                    showCustomTopicDialog()
                } else {
                    showAISetupPrompt()
                }
            }
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, fragment)
            .commit()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun checkAndPromptForNotifications() {
        val isEnabledInPrefs = prefsManager.isNotificationsEnabled()
        var isPermissionGranted = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            isPermissionGranted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        }

        if (!isEnabledInPrefs || !isPermissionGranted) {
            // Only prompt if not already prompted recently (logic simplified here)
            // showNotificationPromptDialog(isPermissionGranted)
        }
    }

    private fun showAISetupPrompt() {
        AlertDialog.Builder(this)
            .setTitle("AI Configuration Required")
            .setMessage("To generate personalized learning curriculums, please configure your AI provider in Settings.")
            .setPositiveButton("Open Settings") { _, _ ->
                startActivity(Intent(this, SettingsActivity::class.java))
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showGenerateCurriculumDialog(topic: String) {
        // Simplified dialog logic for brevity, ideally reuse a DialogFragment
        generateCurriculum(topic, "Beginner to Advanced", 20)
    }

    private fun showCustomTopicDialog() {
        // TODO: Implement custom topic dialog input
        // For now, redirect to a simple input or reuse existing logic
        val input = android.widget.EditText(this)
        AlertDialog.Builder(this)
            .setTitle("What do you want to learn?")
            .setView(input)
            .setPositiveButton("Generate") { _, _ ->
                val topic = input.text.toString()
                if (topic.isNotEmpty()) {
                    generateCurriculum(topic, "Beginner to Advanced", 20)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun generateCurriculum(topic: String, difficulty: String, numberOfLessons: Int) {
        val progressDialog = AlertDialog.Builder(this)
            .setTitle("Generating Curriculum")
            .setMessage("Creating your personalized learning path for $topic...\nThis may take 30-60 seconds.")
            .setCancelable(false)
            .create()
        
        progressDialog.show()

        val config = prefsManager.getAIConfig()
        if (config == null) {
            progressDialog.dismiss()
            showAISetupPrompt()
            return
        }

        val aiService = AIServiceImpl(
            AIProviderFactory.createProvider(
                AIProviderFactory.getProviderFromName(config.provider.name),
                config.customEndpoint
            )
        )
        
        // Map simple request to full GenerationRequest
        val generationRequest = GenerationRequest(
            topic = topic,
            description = "A comprehensive course on $topic",
            difficulty = Difficulty.BEGINNER, // Default
            estimatedHours = 10, // Default
            mode = GenerationMode.FULL_GENERATION, // Changed from FULL_COURSE to match enum
            maxLessons = numberOfLessons,
            provider = config.provider.name,
            apiKey = config.apiKey,
            modelName = config.modelName
        )

        lifecycleScope.launch {
            try {
                val response = aiService.generateCurriculumOutline(generationRequest)
                progressDialog.dismiss()

                if (response is AIResult.Success) {
                    val outline = response.data
                    // CRITICAL FIX: Save to Repository (Database) instead of Preferences
                    // Convert LearningTopic to Curriculum entity
                    val curriculum = Curriculum(
                        id = UUID.randomUUID().toString(),
                        title = outline.title,
                        description = outline.description,
                        difficulty = Difficulty.BEGINNER, // Default or map from string
                        totalLessons = outline.lessons.size,
                        completedLessons = 0,
                        isCompleted = false,
                        // isInProgress is a calculated property, not a constructor param
                        lastAccessedAt = System.currentTimeMillis(),
                        createdAt = System.currentTimeMillis(),
                        generationStatus = GenerationStatus.COMPLETE, // Changed from COMPLETED to match enum
                        estimatedHours = outline.estimatedHours,
                        provider = config.provider.name,
                        modelUsed = config.modelName,
                        tags = outline.tags,
                        generationMode = GenerationMode.FULL_GENERATION, // Changed from FULL_COURSE
                        isOutlineOnly = true,
                        lastGeneratedAt = System.currentTimeMillis()
                    )
                    
                    repository.insertCurriculum(curriculum)
                    
                    // Also insert lessons (This part requires LessonRepository, assuming it exists or we need to add it)
                    // For now, we just ensure the Curriculum is in the DB so it shows up in the list.
                    // A full implementation would insert lessons into the lesson table.
                    
                    AlertDialog.Builder(this@MainActivity)
                        .setTitle("Success!")
                        .setMessage("Your learning path for '${outline.title}' has been created!")
                        .setPositiveButton("View Plan") { _, _ ->
                            // Switch to Plans tab
                            bottomNav.selectedItemId = R.id.nav_plans
                        }
                        .setNegativeButton("Close", null)
                        .show()
                } else if (response is AIResult.Error) {
                    AlertDialog.Builder(this@MainActivity)
                        .setTitle("Generation Failed")
                        .setMessage(response.message)
                        .setPositiveButton("OK", null)
                        .show()
                }
            } catch (e: Exception) {
                progressDialog.dismiss()
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("Error")
                    .setMessage("Failed to generate curriculum: ${e.message}")
                    .setPositiveButton("OK", null)
                    .show()
            }
        }
    }
}
