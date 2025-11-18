package com.learneveryday.app

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.learneveryday.app.data.local.AppDatabase
import com.learneveryday.app.data.repository.CurriculumRepositoryImpl
import com.learneveryday.app.domain.model.Curriculum
import com.learneveryday.app.domain.model.Lesson
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class LearningActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var lessonNumberText: TextView
    private lateinit var lessonTitleText: TextView
    private lateinit var lessonContentText: TextView
    private lateinit var previousButton: MaterialButton
    private lateinit var nextButton: MaterialButton
    private lateinit var completeCheckbox: MaterialCheckBox
    private lateinit var progressBar: LinearProgressIndicator
    private lateinit var progressText: TextView

    private lateinit var prefsManager: PreferencesManager
    private lateinit var repository: CurriculumRepositoryImpl
    private lateinit var topic: LearningTopic
    private lateinit var progress: UserProgress
    private var currentLessonIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_learning)

        prefsManager = PreferencesManager(this)
        val database = AppDatabase.getInstance(applicationContext)
        repository = CurriculumRepositoryImpl(database.curriculumDao(), database.lessonDao())

        // Get views
        toolbar = findViewById(R.id.toolbar)
        lessonNumberText = findViewById(R.id.lessonNumberText)
        lessonTitleText = findViewById(R.id.lessonTitleText)
        lessonContentText = findViewById(R.id.lessonContentText)
        previousButton = findViewById(R.id.previousButton)
        nextButton = findViewById(R.id.nextButton)
        completeCheckbox = findViewById(R.id.completeCheckbox)
        progressBar = findViewById(R.id.progressBar)
        progressText = findViewById(R.id.progressText)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        // Get topic from intent
        val topicId = intent.getStringExtra("TOPIC_ID") ?: return
        
        loadTopic(topicId)
    }

    private fun loadTopic(topicId: String) {
        lifecycleScope.launch {
            try {
                val curriculum = repository.getCurriculumById(topicId).firstOrNull()
                if (curriculum == null) {
                    Toast.makeText(this@LearningActivity, "Topic not found", Toast.LENGTH_SHORT).show()
                    finish()
                    return@launch
                }

                val lessons = repository.getLessonsByCurriculumId(topicId).firstOrNull() ?: emptyList()
                
                // Map to LearningTopic
                topic = LearningTopic(
                    id = curriculum.id,
                    title = curriculum.title,
                    description = curriculum.description,
                    difficulty = curriculum.difficulty.name,
                    estimatedHours = curriculum.estimatedHours,
                    lessons = lessons,
                    isAIGenerated = true,
                    generatedAt = curriculum.createdAt,
                    tags = curriculum.tags
                )

                // Get or create progress
                progress = prefsManager.getUserProgress(topicId) ?: UserProgress(topicId)
                currentLessonIndex = progress.currentLessonIndex

                supportActionBar?.title = topic.title

                setupButtons()
                if (topic.lessons.isNotEmpty()) {
                    displayLesson()
                } else {
                    Toast.makeText(this@LearningActivity, "No lessons found for this topic", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@LearningActivity, "Error loading topic: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun setupButtons() {
        previousButton.setOnClickListener {
            if (currentLessonIndex > 0) {
                currentLessonIndex--
                displayLesson()
            }
        }

        nextButton.setOnClickListener {
            if (currentLessonIndex < topic.lessons.size - 1) {
                currentLessonIndex++
                displayLesson()
            } else {
                Toast.makeText(this, getString(R.string.completed), Toast.LENGTH_SHORT).show()
            }
        }

        completeCheckbox.setOnCheckedChangeListener { _, isChecked ->
            if (topic.lessons.isNotEmpty()) {
                val lessonId = topic.lessons[currentLessonIndex].id
                if (isChecked) {
                    progress.completedLessons.add(lessonId)
                    Toast.makeText(this, "Lesson marked as complete!", Toast.LENGTH_SHORT).show()
                } else {
                    progress.completedLessons.remove(lessonId)
                }
                progress = progress.copy(
                    currentLessonIndex = currentLessonIndex,
                    lastAccessedAt = System.currentTimeMillis()
                )
                prefsManager.saveUserProgress(progress)
                updateProgress()
                
                // Update repository progress as well
                lifecycleScope.launch {
                    repository.updateProgress(topic.id, progress.completedLessons.size, progress.completedLessons.size == topic.lessons.size)
                }
            }
        }
    }

    private fun displayLesson() {
        if (currentLessonIndex >= topic.lessons.size) {
            currentLessonIndex = 0
        }
        
        val lesson = topic.lessons[currentLessonIndex]
        
        lessonNumberText.text = getString(R.string.lesson_number, currentLessonIndex + 1, topic.lessons.size)
        lessonTitleText.text = lesson.title
        lessonContentText.text = lesson.content

        // Update completion checkbox
        completeCheckbox.isChecked = progress.completedLessons.contains(lesson.id)

        // Update button states
        previousButton.isEnabled = currentLessonIndex > 0
        nextButton.isEnabled = currentLessonIndex < topic.lessons.size - 1

        // Update next button text
        if (currentLessonIndex == topic.lessons.size - 1) {
            nextButton.text = "Finish"
        } else {
            nextButton.text = getString(R.string.next_lesson)
        }

        // Save current position
        progress = progress.copy(
            currentLessonIndex = currentLessonIndex,
            lastAccessedAt = System.currentTimeMillis()
        )
        prefsManager.saveUserProgress(progress)
        
        // Update repository last accessed
        lifecycleScope.launch {
            repository.updateLastAccessed(topic.id)
        }

        updateProgress()
    }

    private fun updateProgress() {
        val completionPercentage = progress.getCompletionPercentage(topic.lessons.size)
        progressBar.setProgressCompat(completionPercentage, true)
        progressText.text = "$completionPercentage% Complete (${progress.completedLessons.size}/${topic.lessons.size})"
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
