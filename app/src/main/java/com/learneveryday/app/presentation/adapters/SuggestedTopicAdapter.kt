package com.learneveryday.app.presentation.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.learneveryday.app.SuggestedTopics
import com.learneveryday.app.databinding.ItemSuggestedTopicBinding

class SuggestedTopicAdapter(
    private var topics: List<SuggestedTopics.TopicSuggestion>,
    private val onTopicClick: (SuggestedTopics.TopicSuggestion) -> Unit
) : RecyclerView.Adapter<SuggestedTopicAdapter.TopicViewHolder>() {

    inner class TopicViewHolder(private val binding: ItemSuggestedTopicBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(topic: SuggestedTopics.TopicSuggestion) {
            binding.topicTitle.text = topic.title
            binding.topicDescription.text = topic.description
            binding.topicCategory.text = topic.category
            
            // Set icon if available, or use a default based on category
            binding.topicIcon.text = topic.icon

            binding.root.setOnClickListener {
                onTopicClick(topic)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TopicViewHolder {
        val binding = ItemSuggestedTopicBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TopicViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TopicViewHolder, position: Int) {
        holder.bind(topics[position])
    }

    override fun getItemCount(): Int = topics.size

    fun updateTopics(newTopics: List<SuggestedTopics.TopicSuggestion>) {
        topics = newTopics
        notifyDataSetChanged()
    }
}
