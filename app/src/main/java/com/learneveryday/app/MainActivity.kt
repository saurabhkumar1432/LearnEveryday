package com.learneveryday.app

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.learneveryday.app.data.local.AppDatabase
import com.learneveryday.app.data.repository.CurriculumRepositoryImpl

import com.learneveryday.app.data.service.AIProviderFactory
import com.learneveryday.app.domain.model.Curriculum
import com.learneveryday.app.domain.model.Difficulty
import com.learneveryday.app.domain.model.GenerationMode
import com.learneveryday.app.domain.model.GenerationStatus
import com.learneveryday.app.domain.service.AIService
import com.learneveryday.app.domain.service.AIServiceImpl
import com.learneveryday.app.domain.service.AIResult
import com.learneveryday.app.domain.service.CurriculumRequest
import com.learneveryday.app.domain.service.GenerationRequest
import com.learneveryday.app.domain.service.LessonGenerationRequest
import com.learneveryday.app.presentation.home.HomeFragment
import com.learneveryday.app.presentation.plans.LearningPlansFragment
import com.learneveryday.app.work.GenerationScheduler
import kotlinx.coroutines.launch
import java.util.UUID

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNav: BottomNavigationView
    private lateinit var fab: FloatingActionButton
    private lateinit var prefsManager: PreferencesManager
    private lateinit var repository: CurriculumRepositoryImpl
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>
    private var currentDestinationId: Int = R.id.nav_home

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSupportActionBar(findViewById(R.id.toolbar))

        prefsManager = PreferencesManager(this)
        setupRepository()
        setupPermissions()
        setupBottomNav()

        // Load initial fragment or restore callbacks
        if (savedInstanceState == null) {
            loadFragment(HomeFragment())
            currentDestinationId = R.id.nav_home
            bottomNav.selectedItemId = R.id.nav_home
        } else {
            val currentFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
            currentDestinationId = when (currentFragment) {
                is LearningPlansFragment -> R.id.nav_plans
                else -> R.id.nav_home
            }
            bottomNav.selectedItemId = currentDestinationId
            if (currentFragment is HomeFragment) {
                setupHomeCallbacks(currentFragment)
            }
        }
        
        checkAndPromptForNotifications()
    }

    private fun setupRepository() {
        val database = AppDatabase.getInstance(applicationContext)
        repository = CurriculumRepositoryImpl(database.curriculumDao(), database.lessonDao())
    }

    private fun setupPermissions() {
        requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                prefsManager.setNotificationsEnabled(true)
                NotificationScheduler.scheduleHourlyReminder(this)
                Toast.makeText(this, "Notifications enabled", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupBottomNav() {
        bottomNav = findViewById(R.id.bottom_navigation)
        fab = findViewById(R.id.fab_create_plan)
        
        fab.setOnClickListener {
            if (prefsManager.isAIEnabled()) {
                showCustomTopicDialog()
            } else {
                showAISetupPrompt()
            }
        }

        bottomNav.setOnItemSelectedListener { item ->
            navigateToDestination(item.itemId)
        }

        currentDestinationId = bottomNav.selectedItemId
    }

    private fun setupHomeCallbacks(fragment: HomeFragment) {
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

    private fun loadFragment(fragment: Fragment) {
        try {
            if (supportFragmentManager.isStateSaved) {
                return
            }

            // Inject callbacks if it's HomeFragment
            if (fragment is HomeFragment) {
                setupHomeCallbacks(fragment)
            }

            supportFragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment, fragment)
                .commit()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error navigating: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateToDestination(@IdRes destinationId: Int): Boolean {
        if (currentDestinationId == destinationId) {
            return true
        }

        currentDestinationId = destinationId

        return when (destinationId) {
            R.id.nav_home -> {
                fab.show()
                loadFragment(HomeFragment())
                true
            }
            R.id.nav_plans -> {
                fab.hide()
                loadFragment(LearningPlansFragment())
                true
            }
            else -> false
        }
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
        val dialogView = layoutInflater.inflate(R.layout.dialog_custom_topic, null)
        val topicInput = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.topicInput)
        val difficultySpinner = dialogView.findViewById<android.widget.Spinner>(R.id.difficultySpinner)
        val lessonCountSlider = dialogView.findViewById<com.google.android.material.slider.Slider>(R.id.lessonCountSlider)

        AlertDialog.Builder(this)
            .setTitle("Create Custom Plan")
            .setView(dialogView)
            .setPositiveButton("Generate") { _, _ ->
                val topic = topicInput.text.toString()
                val difficulty = difficultySpinner.selectedItem.toString()
                val lessonCount = lessonCountSlider.value.toInt()

                if (topic.isNotEmpty()) {
                    generateCurriculum(topic, difficulty, lessonCount)
                } else {
                    Toast.makeText(this, "Please enter a topic", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun generateCurriculum(topic: String, difficulty: String, numberOfLessons: Int) {
        // Create custom progress dialog
        val dialogView = layoutInflater.inflate(R.layout.dialog_generation_progress, null)
        val tvTitle = dialogView.findViewById<TextView>(R.id.tvTitle)
        val tvSubtitle = dialogView.findViewById<TextView>(R.id.tvSubtitle)
        val tvCurrentStep = dialogView.findViewById<TextView>(R.id.tvCurrentStep)
        val progressBar = dialogView.findViewById<com.google.android.material.progressindicator.LinearProgressIndicator>(R.id.progressBar)
        val tvProgressText = dialogView.findViewById<TextView>(R.id.tvProgressText)
        val lessonInfoContainer = dialogView.findViewById<android.view.View>(R.id.lessonInfoContainer)
        val tvLessonNumber = dialogView.findViewById<TextView>(R.id.tvLessonNumber)
        val tvLessonTitle = dialogView.findViewById<TextView>(R.id.tvLessonTitle)
        val ivGeneratingIcon = dialogView.findViewById<android.widget.ImageView>(R.id.ivGeneratingIcon)
        
        // Set initial state
        tvTitle.text = "Creating Your Learning Path"
        tvSubtitle.text = "AI is crafting personalized lessons for \"$topic\""
        tvCurrentStep.text = "Initializing..."
        progressBar.isIndeterminate = true
        tvProgressText.text = ""
        
        // Animate the icon
        val rotateAnimation = android.view.animation.AnimationUtils.loadAnimation(this, android.R.anim.fade_in).apply {
            repeatCount = android.view.animation.Animation.INFINITE
            repeatMode = android.view.animation.Animation.REVERSE
            duration = 1000
        }
        ivGeneratingIcon.startAnimation(rotateAnimation)
        
        val progressDialog = AlertDialog.Builder(this, R.style.TransparentDialog)
            .setView(dialogView)
            .setCancelable(false)
            .create()
        
        progressDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
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
            difficulty = Difficulty.valueOf(difficulty.uppercase().replace(" ", "_").takeIf { it in Difficulty.values().map { d -> d.name } } ?: "BEGINNER"),
            estimatedHours = 10, // Default
            mode = GenerationMode.FULL_GENERATION,
            maxLessons = numberOfLessons,
            provider = config.provider.name,
            apiKey = config.apiKey,
            modelName = config.modelName
        )

        lifecycleScope.launch {
            try {
                // Step 1: Generate curriculum outline
                tvCurrentStep.text = "Creating curriculum outline..."
                tvProgressText.text = "Step 1 of 2"
                
                val response = aiService.generateCurriculumOutline(generationRequest)

                if (response is AIResult.Success) {
                    val outline = response.data
                    val curriculumId = UUID.randomUUID().toString()
                    val totalLessons = outline.lessons.size
                    
                    // Switch to determinate progress for lesson generation
                    progressBar.isIndeterminate = false
                    progressBar.max = totalLessons
                    progressBar.progress = 0
                    tvProgressText.text = "Step 2 of 2"
                    lessonInfoContainer.visibility = android.view.View.VISIBLE
                    
                    // Step 2: Generate content for each lesson
                    val lessonsWithContent = mutableListOf<com.learneveryday.app.domain.model.Lesson>()
                    val previousTitles = mutableListOf<String>()
                    
                    for ((index, lessonItem) in outline.lessons.withIndex()) {
                        // Update UI
                        tvCurrentStep.text = "Generating lesson content..."
                        tvLessonNumber.text = "Lesson ${index + 1} of $totalLessons"
                        tvLessonTitle.text = lessonItem.title
                        progressBar.progress = index
                        
                        // Generate content for this lesson
                        val contentRequest = LessonGenerationRequest(
                            curriculumTitle = outline.title,
                            lessonTitle = lessonItem.title,
                            lessonDescription = lessonItem.description,
                            difficulty = generationRequest.difficulty,
                            keyPoints = lessonItem.keyPoints,
                            previousLessonTitles = previousTitles.toList(),
                            provider = config.provider.name,
                            apiKey = config.apiKey,
                            modelName = config.modelName,
                            temperature = 0.7f,
                            maxTokens = 8000
                        )
                        
                        val contentResult = aiService.generateLessonContent(contentRequest)
                        
                        val lessonContent = when (contentResult) {
                            is AIResult.Success -> contentResult.data.content
                            else -> "" // If content generation fails, leave empty
                        }
                        
                        val lesson = com.learneveryday.app.domain.model.Lesson(
                            id = UUID.randomUUID().toString(),
                            curriculumId = curriculumId,
                            orderIndex = index,
                            title = lessonItem.title,
                            description = lessonItem.description,
                            content = lessonContent,
                            difficulty = com.learneveryday.app.domain.model.Difficulty.valueOf(outline.difficulty.uppercase().replace(" ", "_").takeIf { it in com.learneveryday.app.domain.model.Difficulty.values().map { d -> d.name } } ?: "BEGINNER"),
                            estimatedMinutes = lessonItem.estimatedMinutes,
                            keyPoints = if (contentResult is AIResult.Success) contentResult.data.keyPoints else lessonItem.keyPoints,
                            practiceExercise = if (contentResult is AIResult.Success) contentResult.data.practiceExercise else null,
                            prerequisites = if (contentResult is AIResult.Success) contentResult.data.prerequisites else emptyList(),
                            nextSteps = if (contentResult is AIResult.Success) contentResult.data.nextSteps else emptyList(),
                            isGenerated = lessonContent.isNotBlank(),
                            isCompleted = false,
                            completedAt = null,
                            lastReadPosition = 0,
                            timeSpentMinutes = 0
                        )
                        
                        lessonsWithContent.add(lesson)
                        previousTitles.add(lessonItem.title)
                    }
                    
                    // Final step: Save everything
                    progressBar.progress = totalLessons
                    tvCurrentStep.text = "Saving your learning path..."
                    lessonInfoContainer.visibility = android.view.View.GONE
                    
                    // Create curriculum entity
                    val curriculum = Curriculum(
                        id = curriculumId,
                        title = outline.title,
                        description = outline.description,
                        difficulty = generationRequest.difficulty,
                        totalLessons = outline.lessons.size,
                        completedLessons = 0,
                        isCompleted = false,
                        lastAccessedAt = System.currentTimeMillis(),
                        createdAt = System.currentTimeMillis(),
                        generationStatus = GenerationStatus.COMPLETE,
                        estimatedHours = outline.estimatedHours,
                        provider = config.provider.name,
                        modelUsed = config.modelName,
                        tags = outline.tags,
                        generationMode = GenerationMode.FULL_GENERATION,
                        isOutlineOnly = false, // All content generated
                        lastGeneratedAt = System.currentTimeMillis()
                    )
                    
                    repository.insertCurriculum(curriculum)
                    repository.insertLessons(lessonsWithContent, curriculumId)
                    
                    // Also set as current topic for notifications
                    prefsManager.setCurrentTopicId(curriculumId)

                    progressDialog.dismiss()
                    ivGeneratingIcon.clearAnimation()
                    
                    // Show success dialog
                    showSuccessDialog(outline.title, lessonsWithContent.size)
                    
                } else if (response is AIResult.Error) {
                    progressDialog.dismiss()
                    ivGeneratingIcon.clearAnimation()
                    showErrorDialog("Generation Failed", response.message)
                }
            } catch (e: Exception) {
                progressDialog.dismiss()
                ivGeneratingIcon.clearAnimation()
                showErrorDialog("Error", "Failed to generate curriculum: ${e.message}")
            }
        }
    }
    
    private fun showSuccessDialog(title: String, lessonCount: Int) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_generation_success, null)
        val tvSuccessTitle = dialogView.findViewById<TextView>(R.id.tvSuccessTitle)
        val tvSuccessMessage = dialogView.findViewById<TextView>(R.id.tvSuccessMessage)
        val btnViewPlan = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnViewPlan)
        val btnClose = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnClose)
        
        tvSuccessTitle.text = "🎉 Learning Path Created!"
        tvSuccessMessage.text = "\"$title\" is ready with $lessonCount comprehensive lessons. Start learning now!"
        
        val dialog = AlertDialog.Builder(this, R.style.TransparentDialog)
            .setView(dialogView)
            .setCancelable(true)
            .create()
        
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        btnViewPlan.setOnClickListener {
            dialog.dismiss()
            navigateToDestination(R.id.nav_plans)
            bottomNav.selectedItemId = R.id.nav_plans
        }
        
        btnClose.setOnClickListener {
            dialog.dismiss()
        }
        
        dialog.show()
    }
    
    private fun showErrorDialog(title: String, message: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }
}
