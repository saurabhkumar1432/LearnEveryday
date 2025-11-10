package com.learneveryday.app.presentation.reader

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.lifecycleScope
import com.learneveryday.app.R
import com.learneveryday.app.databinding.ActivityLessonReaderBinding
import com.learneveryday.app.presentation.ViewModelFactory
import io.noties.markwon.Markwon
// Syntax highlighting plugin optional; core rendering is sufficient for now
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class LessonReaderActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_LESSON_ID = "lesson_id"
    }

    private lateinit var binding: ActivityLessonReaderBinding

    private val lessonId: String by lazy {
        intent.getStringExtra(EXTRA_LESSON_ID) ?: ""
    }

    private val viewModel: LessonViewModel by viewModels {
        ViewModelFactory(applicationContext, lessonId = lessonId)
    }

    private lateinit var markwon: Markwon

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLessonReaderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupMarkwon()
        bindUi()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun setupMarkwon() {
        markwon = Markwon.create(this)
        binding.tvContent.movementMethod = LinkMovementMethod.getInstance()
    }

    private fun bindUi() {
        lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                // Loading indicator
                binding.progressLoading.visibility = if (state.isLoading) android.view.View.VISIBLE else android.view.View.GONE

                // Title & meta
                binding.tvLessonTitle.text = state.lesson?.title ?: getString(R.string.app_name)
                binding.tvEstimatedTime.text = state.estimatedReadTime

                // Content
                val content = state.lesson?.content ?: ""
                if (content.isBlank() && !state.isLoading) {
                    renderMarkdown(binding.tvContent, "_Content is being generated. Please check back in a moment..._")
                } else {
                    renderMarkdown(binding.tvContent, content)
                }

                // Restore scroll position when content is ready
                if (!state.isLoading && content.isNotBlank()) {
                    binding.scrollView.post {
                        binding.scrollView.scrollTo(0, state.readPosition)
                    }
                }

                // Update FAB icon
                if (state.isCompleted || state.lesson?.isCompleted == true) {
                    binding.fabToggleComplete.setImageResource(R.drawable.ic_check_circle)
                    binding.fabToggleComplete.backgroundTintList = android.content.res.ColorStateList.valueOf(
                        getColor(R.color.text_tertiary)
                    )
                } else {
                    binding.fabToggleComplete.setImageResource(R.drawable.ic_check_circle)
                    binding.fabToggleComplete.backgroundTintList = android.content.res.ColorStateList.valueOf(
                        getColor(R.color.success)
                    )
                }

                state.error?.let {
                    // For now, show as toolbar subtitle
                    binding.toolbar.subtitle = it
                    viewModel.clearError()
                }
            }
        }

        // Track scroll to persist read position periodically
        binding.scrollView.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { v, _, scrollY, _, _ ->
            viewModel.updateReadPosition(scrollY)
        })

        // Toggle completion
        binding.fabToggleComplete.setOnClickListener {
            val completed = viewModel.uiState.value.isCompleted || viewModel.uiState.value.lesson?.isCompleted == true
            if (completed) viewModel.markIncomplete() else viewModel.markComplete()
        }
    }

    private fun renderMarkdown(textView: TextView, markdown: String) {
        markwon.setMarkdown(textView, markdown)
    }

    override fun onPause() {
        super.onPause()
        viewModel.onPause()
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }
}
