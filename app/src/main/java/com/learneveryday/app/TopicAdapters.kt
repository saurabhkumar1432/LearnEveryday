package com.learneveryday.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.progressindicator.LinearProgressIndicator
import kotlin.math.max

class SuggestedTopicAdapter(
    private var topics: List<SuggestedTopics.TopicSuggestion>,
    private val onTopicClick: (SuggestedTopics.TopicSuggestion) -> Unit
) : RecyclerView.Adapter<SuggestedTopicAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val card: MaterialCardView = view.findViewById(R.id.topicCard)
        val icon: TextView = view.findViewById(R.id.topicIcon)
        val title: TextView = view.findViewById(R.id.topicTitle)
        val description: TextView = view.findViewById(R.id.topicDescription)
        val category: TextView = view.findViewById(R.id.topicCategory)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_suggested_topic, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val topic = topics[position]
        
        holder.icon.text = topic.icon
        holder.title.text = topic.title
        holder.description.text = topic.description
        holder.category.text = topic.category
        
        holder.card.setOnClickListener {
            onTopicClick(topic)
        }
    }

    override fun getItemCount() = topics.size

    fun updateTopics(newTopics: List<SuggestedTopics.TopicSuggestion>) {
        topics = newTopics
        notifyDataSetChanged()
    }
}

class GeneratedTopicAdapter(
    private var topics: List<LearningTopic>,
    private val onTopicClick: (LearningTopic) -> Unit,
    private val onDeleteClick: (LearningTopic) -> Unit
) : RecyclerView.Adapter<GeneratedTopicAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val card: MaterialCardView = view.findViewById(R.id.topicCard)
        val title: TextView = view.findViewById(R.id.topicTitle)
        val description: TextView = view.findViewById(R.id.topicDescription)
        val stats: TextView = view.findViewById(R.id.topicStats)
        val difficulty: TextView = view.findViewById(R.id.topicDifficulty)
        val progressLabel: TextView = view.findViewById(R.id.topicProgress)
        val progressCount: TextView = view.findViewById(R.id.topicProgressCount)
        val progressContainer: LinearLayout = view.findViewById(R.id.progressContainer)
        val progressBar: LinearProgressIndicator = view.findViewById(R.id.progressBar)
        val deleteButton: View = view.findViewById(R.id.deleteButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_generated_topic, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val topic = topics[position]
        val context = holder.itemView.context
        val prefsManager = PreferencesManager(context)
        val progress = prefsManager.getUserProgress(topic.id)
        
        holder.title.text = topic.title
        holder.description.text = topic.description
        val lessons = topic.lessons.size
        val hours = if (topic.estimatedHours > 0) {
            "${topic.estimatedHours}h"
        } else {
            val approxHours = max(1, ((lessons * 30) + 59) / 60)
            "~${approxHours}h"
        }
        holder.stats.text = "$lessons lessons · $hours"
        holder.difficulty.text = topic.difficulty
        
        if (progress != null && topic.lessons.isNotEmpty()) {
            val percentage = progress.getCompletionPercentage(topic.lessons.size)
            holder.progressContainer.visibility = View.VISIBLE
            holder.progressLabel.text = "$percentage% complete"
            holder.progressCount.text = "${progress.completedLessons.size}/${topic.lessons.size}"
            holder.progressBar.setProgressCompat(percentage, true)
        } else {
            holder.progressContainer.visibility = View.GONE
        }
        
        holder.card.setOnClickListener {
            onTopicClick(topic)
        }
        
        holder.deleteButton.setOnClickListener {
            onDeleteClick(topic)
        }
    }

    override fun getItemCount() = topics.size

    fun updateTopics(newTopics: List<LearningTopic>) {
        topics = newTopics
        notifyDataSetChanged()
    }
}
