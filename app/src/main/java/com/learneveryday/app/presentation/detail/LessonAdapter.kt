package com.learneveryday.app.presentation.detail

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.learneveryday.app.databinding.ItemLessonRowBinding
import com.learneveryday.app.domain.model.Lesson

class LessonAdapter(
    private val onItemClick: (Lesson) -> Unit,
    private val onCompletionToggle: (Lesson, Boolean) -> Unit
) : ListAdapter<Lesson, LessonAdapter.LessonViewHolder>(LessonDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LessonViewHolder {
        val binding = ItemLessonRowBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return LessonViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LessonViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class LessonViewHolder(
        private val binding: ItemLessonRowBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(lesson: Lesson) {
            binding.apply {
                tvOrder.text = "#${lesson.orderIndex + 1}"
                tvLessonTitle.text = lesson.title
                tvDuration.text = "${lesson.estimatedMinutes} min"
                tvStatus.text = if (lesson.isGenerated) "Generated" else "Pending"
                checkCompleted.isChecked = lesson.isCompleted

                root.setOnClickListener { onItemClick(lesson) }
                checkCompleted.setOnCheckedChangeListener { _, isChecked ->
                    onCompletionToggle(lesson, isChecked)
                }
            }
        }
    }

    private class LessonDiffCallback : DiffUtil.ItemCallback<Lesson>() {
        override fun areItemsTheSame(oldItem: Lesson, newItem: Lesson): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Lesson, newItem: Lesson): Boolean =
            oldItem == newItem
    }
}
