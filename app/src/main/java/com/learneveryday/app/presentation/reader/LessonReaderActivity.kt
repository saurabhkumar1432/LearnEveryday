package com.learneveryday.app.presentation.reader

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.lifecycleScope
import com.learneveryday.app.R
import com.learneveryday.app.databinding.ActivityLessonReaderBinding
import com.learneveryday.app.presentation.ViewModelFactory
import com.learneveryday.app.util.SimpleMarkdownRenderer
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Activity for reading lesson content using Markwon markdown renderer.
 * Uses a single TextView with proper markdown rendering for reliable mobile display.
 */
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
    
    // Cache rendered content hash to prevent unnecessary re-renders
    private var lastRenderedContentHash: Int = 0
    private var hasRestoredScrollPosition = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLessonReaderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupContentView()
        bindUi()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun setupContentView() {
        // Hide RecyclerView, use TextView with Markwon for reliable rendering
        binding.rvContent.visibility = View.GONE
        binding.tvContent.visibility = View.VISIBLE
        
        android.util.Log.d("LessonReader", "Using SimpleMarkdownRenderer for content")
    }

    private fun bindUi() {
        // Observe loading state separately for quick UI response
        lifecycleScope.launch {
            viewModel.uiState
                .map { it.isLoading }
                .distinctUntilChanged()
                .collect { isLoading ->
                    binding.progressLoading.visibility = if (isLoading) View.VISIBLE else View.GONE
                }
        }
        
        // Observe lesson metadata changes (title, time, etc.)
        lifecycleScope.launch {
            viewModel.uiState
                .map { Triple(it.lesson?.title, it.estimatedReadTime, it.lesson?.orderIndex) }
                .distinctUntilChanged()
                .collect { (title, estimatedTime, _) ->
                    binding.tvLessonTitle.text = title ?: getString(R.string.app_name)
                    binding.tvEstimatedTime.text = estimatedTime
                }
        }
        
        // Observe content changes - only re-render when content actually changes
        lifecycleScope.launch {
            viewModel.uiState
                .map { Pair(it.lesson?.content ?: "", it.isLoading) }
                .distinctUntilChanged { old, new -> old.first.hashCode() == new.first.hashCode() && old.second == new.second }
                .collect { (content, isLoading) ->
                    val contentHash = content.hashCode()
                    
                    if (content.isBlank() && !isLoading) {
                        showPlaceholder()
                    } else if (content.isNotBlank() && contentHash != lastRenderedContentHash) {
                        lastRenderedContentHash = contentHash
                        renderContent(content)
                        viewModel.onContentRendered()
                    }
                }
        }
        
        // Observe scroll position restoration - only once after initial load
        lifecycleScope.launch {
            viewModel.uiState
                .map { Triple(it.readPosition, it.isLoading, it.lesson?.content?.isNotBlank() == true) }
                .distinctUntilChanged()
                .collect { (readPosition, isLoading, hasContent) ->
                    if (!isLoading && hasContent && !hasRestoredScrollPosition && readPosition > 0) {
                        hasRestoredScrollPosition = true
                        binding.scrollView.post {
                            binding.scrollView.scrollTo(0, readPosition)
                        }
                    }
                }
        }
        
        // Observe completion state
        lifecycleScope.launch {
            viewModel.uiState
                .map { it.isCompleted || it.lesson?.isCompleted == true }
                .distinctUntilChanged()
                .collect { isCompleted ->
                    updateCompletionButton(isCompleted)
                }
        }
        
        // Observe errors
        lifecycleScope.launch {
            viewModel.uiState
                .map { it.error }
                .distinctUntilChanged()
                .collect { error ->
                    error?.let {
                        binding.toolbar.subtitle = it
                        viewModel.clearError()
                    }
                }
        }

        binding.scrollView.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { _, _, scrollY, _, _ ->
            viewModel.updateReadPosition(scrollY)
        })

        binding.fabToggleComplete.setOnClickListener {
            val completed = viewModel.uiState.value.isCompleted || viewModel.uiState.value.lesson?.isCompleted == true
            if (completed) viewModel.markIncomplete() else viewModel.markComplete()
        }
    }
    
    private fun updateCompletionButton(isCompleted: Boolean) {
        if (isCompleted) {
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
    }

    private fun renderContent(content: String) {
        android.util.Log.d("LessonReader", "renderContent called with ${content.length} chars")
        
        binding.tvContent.visibility = View.VISIBLE
        binding.rvContent.visibility = View.GONE
        
        // Use SimpleMarkdownRenderer for reliable markdown rendering (async with caching)
        SimpleMarkdownRenderer.renderAsync(binding.tvContent, content) {
            android.util.Log.d("LessonReader", "Content rendered with Markwon")
        }
    }

    private fun showPlaceholder() {
        binding.tvContent.visibility = View.VISIBLE
        binding.rvContent.visibility = View.GONE
        binding.tvContent.text = getString(R.string.lesson_content_placeholder)
    }

    private fun showPlainText(content: String) {
        binding.rvContent.visibility = View.GONE
        binding.tvContent.visibility = View.VISIBLE
        binding.tvContent.text = content
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