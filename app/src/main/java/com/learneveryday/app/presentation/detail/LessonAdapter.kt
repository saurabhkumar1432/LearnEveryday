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
    
    override fun onBindViewHolder(holder: LessonViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
        } else {
            // Partial update - only update changed fields
            val lesson = getItem(position)
            holder.bindPartial(lesson, payloads)
        }
    }

    inner class LessonViewHolder(
        private val binding: ItemLessonRowBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        private var currentLesson: Lesson? = null
        
        fun bind(lesson: Lesson) {
            currentLesson = lesson
            binding.apply {
                tvOrder.text = "#${lesson.orderIndex + 1}"
                tvLessonTitle.text = lesson.title
                tvDuration.text = "${lesson.estimatedMinutes} min"
                tvStatus.text = if (lesson.isGenerated) "Generated" else "Pending"
                
                // Remove listener before setting to prevent unwanted triggers
                checkCompleted.setOnCheckedChangeListener(null)
                checkCompleted.isChecked = lesson.isCompleted
                checkCompleted.setOnCheckedChangeListener { _, isChecked ->
                    currentLesson?.let { onCompletionToggle(it, isChecked) }
                }

                root.setOnClickListener { currentLesson?.let { onItemClick(it) } }
            }
        }
        
        fun bindPartial(lesson: Lesson, payloads: List<Any>) {
            currentLesson = lesson
            payloads.forEach { payload ->
                when (payload) {
                    PAYLOAD_COMPLETION -> {
                        binding.checkCompleted.setOnCheckedChangeListener(null)
                        binding.checkCompleted.isChecked = lesson.isCompleted
                        binding.checkCompleted.setOnCheckedChangeListener { _, isChecked ->
                            currentLesson?.let { onCompletionToggle(it, isChecked) }
                        }
                    }
                    PAYLOAD_STATUS -> {
                        binding.tvStatus.text = if (lesson.isGenerated) "Generated" else "Pending"
                    }
                }
            }
        }
    }

    private class LessonDiffCallback : DiffUtil.ItemCallback<Lesson>() {
        override fun areItemsTheSame(oldItem: Lesson, newItem: Lesson): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Lesson, newItem: Lesson): Boolean =
            oldItem == newItem
        
        override fun getChangePayload(oldItem: Lesson, newItem: Lesson): Any? {
            return when {
                oldItem.isCompleted != newItem.isCompleted -> PAYLOAD_COMPLETION
                oldItem.isGenerated != newItem.isGenerated -> PAYLOAD_STATUS
                else -> null
            }
        }
    }
    
    companion object {
        private const val PAYLOAD_COMPLETION = "completion"
        private const val PAYLOAD_STATUS = "status"
    }
}
