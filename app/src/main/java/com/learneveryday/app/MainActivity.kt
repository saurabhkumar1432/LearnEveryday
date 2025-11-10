package com.learneveryday.app

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity() {

    private lateinit var topicsRecyclerView: RecyclerView
    private lateinit var startButton: MaterialButton
    private lateinit var topicAdapter: TopicAdapter
    private lateinit var prefsManager: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefsManager = PreferencesManager(this)

        topicsRecyclerView = findViewById(R.id.topicsRecyclerView)
        startButton = findViewById(R.id.startButton)

        setupRecyclerView()
        setupStartButton()
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

    private fun setupRecyclerView() {
        val topics = LearningCurriculum.getAllTopics()
        topicAdapter = TopicAdapter(topics) { topic ->
            // Topic selected, enable start button
            startButton.isEnabled = true
        }

        topicsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = topicAdapter
        }
    }

    private fun setupStartButton() {
        startButton.isEnabled = false
        startButton.setOnClickListener {
            val selectedTopic = topicAdapter.getSelectedTopic()
            if (selectedTopic != null) {
                // Save selected topic
                prefsManager.setCurrentTopicId(selectedTopic.id)

                // Create or get existing progress
                var progress = prefsManager.getUserProgress(selectedTopic.id)
                if (progress == null) {
                    progress = UserProgress(selectedTopic.id)
                    prefsManager.saveUserProgress(progress)
                }

                // Start learning activity
                val intent = Intent(this, LearningActivity::class.java)
                intent.putExtra("TOPIC_ID", selectedTopic.id)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Please select a topic", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
