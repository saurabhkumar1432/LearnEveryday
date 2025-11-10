package com.learneveryday.app

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var suggestedTopicsRecyclerView: RecyclerView
    private lateinit var generatedTopicsRecyclerView: RecyclerView
    private lateinit var categoryChipGroup: ChipGroup
    private lateinit var searchInput: EditText
    private lateinit var searchButton: MaterialButton
    private lateinit var createCustomButton: FloatingActionButton
    private lateinit var emptyStateLayout: LinearLayout
    private lateinit var setupAIButton: MaterialButton
    private lateinit var prefsManager: PreferencesManager
    private lateinit var toolbar: MaterialToolbar
    
    private lateinit var suggestedAdapter: SuggestedTopicAdapter
    private lateinit var generatedAdapter: GeneratedTopicAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefsManager = PreferencesManager(this)

        // Initialize views
        toolbar = findViewById(R.id.toolbar)
        suggestedTopicsRecyclerView = findViewById(R.id.suggestedTopicsRecyclerView)
        generatedTopicsRecyclerView = findViewById(R.id.generatedTopicsRecyclerView)
        categoryChipGroup = findViewById(R.id.categoryChipGroup)
        searchInput = findViewById(R.id.searchInput)
        searchButton = findViewById(R.id.searchButton)
        createCustomButton = findViewById(R.id.createCustomButton)
        emptyStateLayout = findViewById(R.id.emptyStateLayout)
        setupAIButton = findViewById(R.id.setupAIButton)

        setSupportActionBar(toolbar)

        setupUI()
        checkAIConfiguration()
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
    
    override fun onResume() {
        super.onResume()
        checkAIConfiguration()
        loadGeneratedTopics()
        animateViews()
    }

    private fun animateViews() {
        // Animate welcome text
        findViewById<android.widget.TextView>(R.id.welcomeText)?.let {
            com.learneveryday.app.utils.AnimationHelper.slideInFromBottom(it, 0)
        }
        
        // Animate search section
        findViewById<com.google.android.material.card.MaterialCardView>(R.id.searchSection)?.let {
            com.learneveryday.app.utils.AnimationHelper.fadeInWithScale(it, 100)
        }
        
        // Animate learning plans card
        findViewById<com.google.android.material.card.MaterialCardView>(R.id.learningPlansCard)?.let {
            com.learneveryday.app.utils.AnimationHelper.slideInFromRight(it, 200)
        }
    }

    private fun setupUI() {
        // Setup suggested topics
        suggestedAdapter = SuggestedTopicAdapter(SuggestedTopics.getPopular()) { suggestion ->
            if (prefsManager.isAIEnabled()) {
                showGenerateCurriculumDialog(suggestion.title, suggestion.description)
            } else {
                showAISetupPrompt()
            }
        }
        
        suggestedTopicsRecyclerView.apply {
            layoutManager = GridLayoutManager(this@MainActivity, 2)
            adapter = suggestedAdapter
        }

        // Setup generated topics
        generatedAdapter = GeneratedTopicAdapter(
            topics = emptyList(),
            onTopicClick = { topic -> startLearning(topic) },
            onDeleteClick = { topic -> deleteTopic(topic) }
        )
        
        generatedTopicsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = generatedAdapter
        }

        // Setup category chips
        setupCategoryChips()

        // Setup search
        searchButton.setOnClickListener {
            val query = searchInput.text.toString()
            if (query.isNotEmpty()) {
                searchTopics(query)
            } else {
                suggestedAdapter.updateTopics(SuggestedTopics.getPopular())
            }
        }

        // Setup custom topic creation
        createCustomButton.setOnClickListener {
            if (prefsManager.isAIEnabled()) {
                showCustomTopicDialog()
            } else {
                showAISetupPrompt()
            }
        }

        // Setup AI button
        setupAIButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // Setup Learning Plans card (with null safety)
        findViewById<com.google.android.material.card.MaterialCardView>(R.id.learningPlansCard)?.setOnClickListener {
            val intent = Intent(this, LearningPlansActivity::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
    }

    private fun setupCategoryChips() {
        categoryChipGroup.removeAllViews()
        
        // Add "All" chip
        val allChip = Chip(this).apply {
            text = "All"
            isCheckable = true
            isChecked = true
            setOnClickListener {
                suggestedAdapter.updateTopics(SuggestedTopics.getPopular())
            }
        }
        categoryChipGroup.addView(allChip)

        // Add category chips
        SuggestedTopics.getCategories().forEach { category ->
            val chip = Chip(this).apply {
                text = category
                isCheckable = true
                setOnClickListener {
                    suggestedAdapter.updateTopics(SuggestedTopics.getByCategory(category))
                }
            }
            categoryChipGroup.addView(chip)
        }
    }

    private fun searchTopics(query: String) {
        val results = SuggestedTopics.searchTopics(query)
        suggestedAdapter.updateTopics(results)
        
        if (results.isEmpty()) {
            Toast.makeText(this, "No topics found for '$query'", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkAIConfiguration() {
        if (prefsManager.isAIEnabled()) {
            emptyStateLayout.visibility = View.GONE
            suggestedTopicsRecyclerView.visibility = View.VISIBLE
            generatedTopicsRecyclerView.visibility = View.VISIBLE
            createCustomButton.visibility = View.VISIBLE
            loadGeneratedTopics()
        } else {
            emptyStateLayout.visibility = View.VISIBLE
            suggestedTopicsRecyclerView.visibility = View.GONE
            generatedTopicsRecyclerView.visibility = View.GONE
            createCustomButton.visibility = View.GONE
        }
    }

    private fun loadGeneratedTopics() {
        val topics = prefsManager.getAllGeneratedTopics()
        generatedAdapter.updateTopics(topics)
        
        findViewById<TextView>(R.id.generatedTopicsLabel).visibility = 
            if (topics.isEmpty()) View.GONE else View.VISIBLE
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

    private fun showGenerateCurriculumDialog(title: String, description: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_custom_learning_artistic, null)
        val topicInput = dialogView.findViewById<TextInputEditText>(R.id.topicInput)
        val lessonsInput = dialogView.findViewById<EditText>(R.id.lessonsInput)
        val difficultyChipGroup = dialogView.findViewById<com.google.android.material.chip.ChipGroup>(R.id.difficultyChipGroup)
        val btnGenerate = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnGenerate)
        val btnCancel = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCancel)
        
        // Pre-fill with suggestion
        topicInput.setText(title)
        lessonsInput.setText("20")
        
        // Create dialog with transparent background for rounded corners
        val dialog = AlertDialog.Builder(this, R.style.TransparentDialog)
            .setView(dialogView)
            .create()
        
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        btnGenerate.setOnClickListener {
            val topic = topicInput.text.toString().trim()
            val lessons = lessonsInput.text.toString().toIntOrNull() ?: 20
            
            val difficulty = when (difficultyChipGroup.checkedChipId) {
                R.id.chipBeginner -> "Beginner"
                R.id.chipIntermediate -> "Intermediate"
                R.id.chipAdvanced -> "Advanced"
                R.id.chipProgressive -> "Beginner to Advanced"
                else -> "Beginner to Advanced"
            }
            
            if (topic.isNotEmpty()) {
                generateCurriculum(topic, difficulty, lessons)
                dialog.dismiss()
            } else {
                topicInput.error = "Please enter a topic"
                topicInput.requestFocus()
            }
        }
        
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }
        
        dialog.show()
    }

    private fun showCustomTopicDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_custom_learning_artistic, null)
        val topicInput = dialogView.findViewById<TextInputEditText>(R.id.topicInput)
        val lessonsInput = dialogView.findViewById<EditText>(R.id.lessonsInput)
        val difficultyChipGroup = dialogView.findViewById<com.google.android.material.chip.ChipGroup>(R.id.difficultyChipGroup)
        val btnGenerate = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnGenerate)
        val btnCancel = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCancel)
        
        // Create dialog with transparent background for rounded corners
        val dialog = AlertDialog.Builder(this, R.style.TransparentDialog)
            .setView(dialogView)
            .create()
        
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        btnGenerate.setOnClickListener {
            val topic = topicInput.text.toString().trim()
            val lessons = lessonsInput.text.toString().toIntOrNull() ?: 20
            
            val difficulty = when (difficultyChipGroup.checkedChipId) {
                R.id.chipBeginner -> "Beginner"
                R.id.chipIntermediate -> "Intermediate"
                R.id.chipAdvanced -> "Advanced"
                R.id.chipProgressive -> "Beginner to Advanced"
                else -> "Beginner to Advanced"
            }
            
            if (topic.isNotEmpty()) {
                generateCurriculum(topic, difficulty, lessons)
                dialog.dismiss()
            } else {
                topicInput.error = "Please enter a topic"
                topicInput.requestFocus()
            }
        }
        
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }
        
        dialog.show()
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

        val aiService = AIService(config)
        val request = CurriculumRequest(
            topic = topic,
            difficulty = difficulty,
            numberOfLessons = numberOfLessons
        )

        lifecycleScope.launch {
            try {
                val response = aiService.generateCurriculum(request)
                progressDialog.dismiss()

                if (response.success && response.topic != null) {
                    prefsManager.saveGeneratedTopic(response.topic)
                    loadGeneratedTopics()
                    
                    AlertDialog.Builder(this@MainActivity)
                        .setTitle("Success!")
                        .setMessage("Your learning path for '${response.topic.title}' has been created with ${response.topic.lessons.size} lessons!")
                        .setPositiveButton("Start Learning") { _, _ ->
                            startLearning(response.topic)
                        }
                        .setNegativeButton("Later", null)
                        .show()
                } else {
                    AlertDialog.Builder(this@MainActivity)
                        .setTitle("Generation Failed")
                        .setMessage(response.error ?: "Unknown error occurred")
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

    private fun startLearning(topic: LearningTopic) {
        prefsManager.setCurrentTopicId(topic.id)

        var progress = prefsManager.getUserProgress(topic.id)
        if (progress == null) {
            progress = UserProgress(topic.id)
            prefsManager.saveUserProgress(progress)
        }

        val intent = Intent(this, LearningActivity::class.java)
        intent.putExtra("TOPIC_ID", topic.id)
        startActivity(intent)
    }

    private fun deleteTopic(topic: LearningTopic) {
        AlertDialog.Builder(this)
            .setTitle("Delete Topic")
            .setMessage("Are you sure you want to delete '${topic.title}'? This will also delete your progress.")
            .setPositiveButton("Delete") { _, _ ->
                prefsManager.deleteTopic(topic.id)
                loadGeneratedTopics()
                Toast.makeText(this, "Topic deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
