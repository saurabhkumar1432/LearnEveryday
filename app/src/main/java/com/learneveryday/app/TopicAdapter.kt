package com.learneveryday.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView

class TopicAdapter(
    private val topics: List<LearningTopic>,
    private val onTopicSelected: (LearningTopic) -> Unit
) : RecyclerView.Adapter<TopicAdapter.TopicViewHolder>() {

    private var selectedPosition = -1

    inner class TopicViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val card: MaterialCardView = view as MaterialCardView
        val title: TextView = view.findViewById(R.id.topicTitle)
        val description: TextView = view.findViewById(R.id.topicDescription)
        val lessonsCount: TextView = view.findViewById(R.id.topicLessons)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TopicViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_topic, parent, false)
        return TopicViewHolder(view)
    }

    override fun onBindViewHolder(holder: TopicViewHolder, position: Int) {
        val topic = topics[position]
        holder.title.text = topic.title
        holder.description.text = topic.description
        holder.lessonsCount.text = "${topic.lessons.size} lessons"

        // Highlight selected item
        holder.card.isChecked = position == selectedPosition
        holder.card.strokeWidth = if (position == selectedPosition) 4 else 0

        holder.card.setOnClickListener {
            val previousPosition = selectedPosition
            selectedPosition = holder.adapterPosition
            notifyItemChanged(previousPosition)
            notifyItemChanged(selectedPosition)
            onTopicSelected(topic)
        }
    }

    override fun getItemCount() = topics.size

    fun getSelectedTopic(): LearningTopic? {
        return if (selectedPosition >= 0) topics[selectedPosition] else null
    }
}
