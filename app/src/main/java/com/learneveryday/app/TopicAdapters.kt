package com.learneveryday.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView

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
        val progress: TextView = view.findViewById(R.id.topicProgress)
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
        holder.stats.text = "${topic.lessons.size} lessons • ${topic.estimatedHours}h • ${topic.difficulty}"
        
        if (progress != null) {
            val percentage = progress.getCompletionPercentage(topic.lessons.size)
            holder.progress.text = "$percentage% complete"
            holder.progress.visibility = View.VISIBLE
        } else {
            holder.progress.visibility = View.GONE
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
