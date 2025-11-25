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
        lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                android.util.Log.d("LessonReader", "UI State: loading=${state.isLoading}, hasLesson=${state.lesson != null}")
                
                binding.progressLoading.visibility = if (state.isLoading) View.VISIBLE else View.GONE

                binding.tvLessonTitle.text = state.lesson?.title ?: getString(R.string.app_name)
                binding.tvEstimatedTime.text = state.estimatedReadTime

                val content = state.lesson?.content ?: ""
                
                if (content.isBlank() && !state.isLoading) {
                    showPlaceholder()
                } else if (content.isNotBlank()) {
                    renderContent(content)
                }

                if (!state.isLoading && content.isNotBlank()) {
                    binding.scrollView.post {
                        binding.scrollView.scrollTo(0, state.readPosition)
                    }
                }

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

    private fun renderContent(content: String) {
        android.util.Log.d("LessonReader", "renderContent called with ${content.length} chars")
        
        binding.tvContent.visibility = View.VISIBLE
        binding.rvContent.visibility = View.GONE
        
        // Use SimpleMarkdownRenderer for reliable markdown rendering
        SimpleMarkdownRenderer.render(binding.tvContent, content)
        
        android.util.Log.d("LessonReader", "Content rendered with Markwon")
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