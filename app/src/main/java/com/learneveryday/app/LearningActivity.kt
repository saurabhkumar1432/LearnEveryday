package com.learneveryday.app

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.checkbox.MaterialCheckBox

class LearningActivity : AppCompatActivity() {

    private lateinit var lessonNumberText: TextView
    private lateinit var lessonTitleText: TextView
    private lateinit var lessonContentText: TextView
    private lateinit var previousButton: Button
    private lateinit var nextButton: Button
    private lateinit var completeCheckbox: MaterialCheckBox
    private lateinit var progressBar: ProgressBar
    private lateinit var progressText: TextView

    private lateinit var prefsManager: PreferencesManager
    private lateinit var topic: LearningTopic
    private lateinit var progress: UserProgress
    private var currentLessonIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_learning)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        prefsManager = PreferencesManager(this)

        // Get views
        lessonNumberText = findViewById(R.id.lessonNumberText)
        lessonTitleText = findViewById(R.id.lessonTitleText)
        lessonContentText = findViewById(R.id.lessonContentText)
        previousButton = findViewById(R.id.previousButton)
        nextButton = findViewById(R.id.nextButton)
        completeCheckbox = findViewById(R.id.completeCheckbox)
        progressBar = findViewById(R.id.progressBar)
        progressText = findViewById(R.id.progressText)

        // Get topic from intent
        val topicId = intent.getStringExtra("TOPIC_ID") ?: return
        topic = prefsManager.getGeneratedTopic(topicId) ?: run {
            Toast.makeText(this, "Topic not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        // Get or create progress
        progress = prefsManager.getUserProgress(topicId) ?: UserProgress(topicId)
        currentLessonIndex = progress.currentLessonIndex

        supportActionBar?.title = topic.title

        setupButtons()
        displayLesson()
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
        }
    }

    private fun displayLesson() {
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

        updateProgress()
    }

    private fun updateProgress() {
        val completionPercentage = progress.getCompletionPercentage(topic.lessons.size)
        progressBar.progress = completionPercentage
        progressText.text = "$completionPercentage% Complete (${progress.completedLessons.size}/${topic.lessons.size})"
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
