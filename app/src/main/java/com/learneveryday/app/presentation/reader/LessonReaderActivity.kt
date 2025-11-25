package com.learneveryday.app.presentation.reader

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
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
    private var menuItemFullscreen: MenuItem? = null
    private var isFullscreen = false
    private var isCheckboxProgrammatic = false // Flag to avoid triggering listener during programmatic changes

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
        setupNavigation()
        setupCheckbox()
        setupBackHandler()
        bindUi()
    }
    
    private fun setupBackHandler() {
        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isFullscreen) {
                    toggleFullscreen()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { 
            if (isFullscreen) {
                toggleFullscreen()
            } else {
                onBackPressedDispatcher.onBackPressed() 
            }
        }
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_lesson_reader, menu)
        menuItemFullscreen = menu.findItem(R.id.action_fullscreen)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_fullscreen -> {
                toggleFullscreen()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun toggleFullscreen() {
        isFullscreen = !isFullscreen
        
        if (isFullscreen) {
            // Enter fullscreen mode
            enterFullscreen()
        } else {
            // Exit fullscreen mode
            exitFullscreen()
        }
        
        // Update menu icon
        menuItemFullscreen?.setIcon(
            if (isFullscreen) R.drawable.ic_fullscreen_exit else R.drawable.ic_fullscreen
        )
        menuItemFullscreen?.title = if (isFullscreen) "Exit Fullscreen" else "Fullscreen"
    }
    
    private fun enterFullscreen() {
        // Hide system bars
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, binding.root).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        
        // Hide UI elements for more reading space
        binding.appBarLayout.visibility = View.GONE
        binding.headerContainer.visibility = View.GONE
        binding.divider.visibility = View.GONE
        binding.bottomNavContainer.visibility = View.GONE
        
        // Adjust content padding for fullscreen
        binding.scrollView.setPadding(
            resources.getDimensionPixelSize(R.dimen.spacing_md),
            resources.getDimensionPixelSize(R.dimen.spacing_lg),
            resources.getDimensionPixelSize(R.dimen.spacing_md),
            resources.getDimensionPixelSize(R.dimen.spacing_lg)
        )
    }
    
    private fun exitFullscreen() {
        // Show system bars
        WindowCompat.setDecorFitsSystemWindows(window, true)
        WindowInsetsControllerCompat(window, binding.root).show(WindowInsetsCompat.Type.systemBars())
        
        // Show UI elements
        binding.appBarLayout.visibility = View.VISIBLE
        binding.headerContainer.visibility = View.VISIBLE
        binding.divider.visibility = View.VISIBLE
        binding.bottomNavContainer.visibility = View.VISIBLE
        
        // Reset content padding
        binding.scrollView.setPadding(
            resources.getDimensionPixelSize(R.dimen.spacing_md),
            resources.getDimensionPixelSize(R.dimen.spacing_md),
            resources.getDimensionPixelSize(R.dimen.spacing_md),
            0
        )
    }

    private fun setupContentView() {
        // Hide RecyclerView, use TextView with Markwon for reliable rendering
        binding.rvContent.visibility = View.GONE
        binding.tvContent.visibility = View.VISIBLE
        
        android.util.Log.d("LessonReader", "Using SimpleMarkdownRenderer for content")
    }
    
    private fun setupNavigation() {
        binding.btnPrevious.setOnClickListener {
            viewModel.navigateToPrevious()?.let { prevLessonId ->
                navigateToLesson(prevLessonId)
            }
        }
        
        binding.btnNext.setOnClickListener {
            viewModel.navigateToNext()?.let { nextLessonId ->
                navigateToLesson(nextLessonId)
            }
        }
    }
    
    private fun setupCheckbox() {
        binding.checkboxComplete.setOnCheckedChangeListener { _, isChecked ->
            if (isCheckboxProgrammatic) return@setOnCheckedChangeListener
            
            if (isChecked) {
                viewModel.markComplete()
                Toast.makeText(this, "Lesson marked complete", Toast.LENGTH_SHORT).show()
            } else {
                viewModel.markIncomplete()
                Toast.makeText(this, "Lesson marked incomplete", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun navigateToLesson(lessonId: String) {
        val intent = Intent(this, LessonReaderActivity::class.java).apply {
            putExtra(EXTRA_LESSON_ID, lessonId)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
        finish()
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
                .map { Triple(it.lesson?.title, it.estimatedReadTime, it.lessonProgress) }
                .distinctUntilChanged()
                .collect { (title, estimatedTime, lessonProgress) ->
                    binding.tvLessonTitle.text = title ?: getString(R.string.app_name)
                    binding.tvEstimatedTime.text = estimatedTime
                    binding.chipLessonNumber.text = lessonProgress
                }
        }
        
        // Observe navigation state
        lifecycleScope.launch {
            viewModel.uiState
                .map { Triple(it.hasPrevious, it.hasNext, it.totalLessons) }
                .distinctUntilChanged()
                .collect { (hasPrevious, hasNext, _) ->
                    binding.btnPrevious.isEnabled = hasPrevious
                    binding.btnPrevious.alpha = if (hasPrevious) 1f else 0.5f
                    binding.btnNext.isEnabled = hasNext
                    binding.btnNext.alpha = if (hasNext) 1f else 0.5f
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
        
        // Observe completion state - update checkbox
        lifecycleScope.launch {
            viewModel.uiState
                .map { it.isCompleted || it.lesson?.isCompleted == true }
                .distinctUntilChanged()
                .collect { isCompleted ->
                    isCheckboxProgrammatic = true
                    binding.checkboxComplete.isChecked = isCompleted
                    isCheckboxProgrammatic = false
                }
        }
        
        // Observe curriculum progress - update progress indicator
        lifecycleScope.launch {
            viewModel.uiState
                .map { it.curriculumProgressPercent }
                .distinctUntilChanged()
                .collect { progress ->
                    binding.progressIndicator.setProgressCompat(progress, true)
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