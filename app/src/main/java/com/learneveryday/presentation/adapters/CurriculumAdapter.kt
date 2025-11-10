package com.learneveryday.app.presentation.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.learneveryday.app.R
import com.learneveryday.app.databinding.ItemCurriculumCardBinding
import com.learneveryday.app.domain.model.Curriculum
import com.learneveryday.app.domain.model.Difficulty
import com.learneveryday.app.domain.model.GenerationStatus

class CurriculumAdapter(
    private val onItemClick: (Curriculum) -> Unit,
    private val onMenuClick: (Curriculum, View) -> Unit
) : ListAdapter<Curriculum, CurriculumAdapter.CurriculumViewHolder>(CurriculumDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CurriculumViewHolder {
        val binding = ItemCurriculumCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CurriculumViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CurriculumViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CurriculumViewHolder(
        private val binding: ItemCurriculumCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(curriculum: Curriculum) {
            binding.apply {
                // Set click listeners
                cardCurriculum.setOnClickListener { onItemClick(curriculum) }
                btnMenu.setOnClickListener { onMenuClick(curriculum, it) }

                // Set title and description
                tvTitle.text = curriculum.title
                tvDescription.text = curriculum.description

                // Set difficulty
                chipDifficulty.text = when (curriculum.difficulty) {
                    Difficulty.BEGINNER -> "Beginner"
                    Difficulty.INTERMEDIATE -> "Intermediate"
                    Difficulty.ADVANCED -> "Advanced"
                    Difficulty.EXPERT -> "Expert"
                }
                
                // Set difficulty color
                chipDifficulty.setChipBackgroundColorResource(
                    when (curriculum.difficulty) {
                        Difficulty.BEGINNER -> R.color.difficulty_beginner
                        Difficulty.INTERMEDIATE -> R.color.difficulty_intermediate
                        Difficulty.ADVANCED -> R.color.difficulty_advanced
                        Difficulty.EXPERT -> R.color.difficulty_expert
                    }
                )

                // Set lesson count
                tvLessonCount.text = "${curriculum.totalLessons} lessons"

                // Set estimated hours
                val hours = curriculum.estimatedHours
                tvEstimatedHours.text = "${hours}h"

                // Set progress
                val progress = curriculum.progressPercentage.toInt()
                progressBar.progress = progress
                
                val completedLessons = curriculum.completedLessons
                tvProgress.text = "$completedLessons of ${curriculum.totalLessons} lessons completed · $progress%"

                // Show/hide progress based on status
                if (curriculum.completedLessons == 0 && curriculum.generationStatus != GenerationStatus.COMPLETE) {
                    progressContainer.visibility = View.GONE
                } else {
                    progressContainer.visibility = View.VISIBLE
                }

                // Set status indicator color
                val statusColor = when {
                    curriculum.generationStatus == GenerationStatus.GENERATING -> 
                        itemView.context.getColor(R.color.status_generating)
                    curriculum.isCompleted -> 
                        itemView.context.getColor(R.color.status_completed)
                    curriculum.isInProgress -> 
                        itemView.context.getColor(R.color.status_in_progress)
                    else -> 
                        itemView.context.getColor(R.color.status_not_started)
                }
                statusIndicator.setBackgroundColor(statusColor)

                // Setup tags
                tagsChipGroup.removeAllViews()
                curriculum.tags.take(3).forEach { tag ->
                    val chip = Chip(itemView.context).apply {
                        text = tag
                        isClickable = false
                        isCheckable = false
                    }
                    tagsChipGroup.addView(chip)
                }

                // Show generating status if applicable
                if (curriculum.generationStatus == GenerationStatus.GENERATING) {
                    generatingLayout.visibility = View.VISIBLE
                    tvGeneratingStatus.text = "Generating lessons..."
                } else {
                    generatingLayout.visibility = View.GONE
                }
            }
        }
    }

    private class CurriculumDiffCallback : DiffUtil.ItemCallback<Curriculum>() {
        override fun areItemsTheSame(oldItem: Curriculum, newItem: Curriculum): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Curriculum, newItem: Curriculum): Boolean {
            return oldItem == newItem
        }
    }
}
