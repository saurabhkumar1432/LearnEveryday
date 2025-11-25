package com.learneveryday.app.presentation.detail

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.learneveryday.app.R
import com.learneveryday.app.databinding.ActivityCurriculumDetailBinding
import com.learneveryday.app.presentation.ViewModelFactory
import com.learneveryday.app.work.GenerationScheduler
import kotlinx.coroutines.launch

class CurriculumDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_CURRICULUM_ID = "curriculum_id"
    }

    private lateinit var binding: ActivityCurriculumDetailBinding

    private val curriculumId: String by lazy {
        intent.getStringExtra(EXTRA_CURRICULUM_ID) ?: ""
    }

    private val viewModel: com.learneveryday.app.presentation.detail.CurriculumDetailViewModel by viewModels {
        ViewModelFactory(applicationContext, curriculumId = curriculumId)
    }

    private lateinit var lessonAdapter: LessonAdapter
    
    private var isGenerating = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCurriculumDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        observeViewModel()
        setupFab()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun setupRecyclerView() {
        lessonAdapter = LessonAdapter(
            onItemClick = { lesson ->
                val intent = Intent(this, com.learneveryday.app.presentation.reader.LessonReaderActivity::class.java).apply {
                    putExtra(com.learneveryday.app.presentation.reader.LessonReaderActivity.EXTRA_LESSON_ID, lesson.id)
                }
                // If outline-only (no content yet) trigger background content generation
                if (lesson.content.isBlank()) {
                    GenerationScheduler.enqueueLessonContent(this, lesson.id)
                    Snackbar.make(binding.root, "Generating content...", Snackbar.LENGTH_SHORT).show()
                }
                startActivity(intent)
            },
            onCompletionToggle = { lesson, isChecked ->
                if (isChecked) viewModel.markLessonComplete(lesson.id)
                else viewModel.markLessonIncomplete(lesson.id)
            }
        )
        binding.lessonsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@CurriculumDetailActivity)
            adapter = lessonAdapter
            setHasFixedSize(true)
        }
        
        // Empty state button
        binding.btnGenerateLessons.setOnClickListener {
            GenerationScheduler.enqueueForCurriculum(this, curriculumId)
            Snackbar.make(binding.root, "Generating lessons in background", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun observeViewModel() {
        // StateFlow already provides distinctUntilChanged semantics
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                updateUI(state)
            }
        }
        
        // Observe WorkManager generation progress
        observeGenerationProgress()
    }
    
    private fun observeGenerationProgress() {
        WorkManager.getInstance(applicationContext)
            .getWorkInfosByTagLiveData("generation_$curriculumId")
            .observe(this) { workInfos ->
                val hasRunning = workInfos?.any { it.state == WorkInfo.State.RUNNING } == true
                val hasFailed = workInfos?.any { it.state == WorkInfo.State.FAILED } == true
                
                isGenerating = hasRunning
                
                if (hasRunning) {
                    binding.chipGenerating.visibility = View.VISIBLE
                    val pending = viewModel.uiState.value.lessons.count { it.content.isBlank() }
                    binding.chipGenerating.text = if (pending > 0) "Generating… $pending left" else "Generating…"
                    binding.chipGenerating.isEnabled = true
                    binding.chipGenerating.setOnLongClickListener {
                        showCancelGenerationDialog()
                        true
                    }
                } else if (hasFailed) {
                    binding.chipGenerating.visibility = View.VISIBLE
                    binding.chipGenerating.text = "Generation failed · Tap to retry"
                    binding.chipGenerating.isEnabled = true
                    binding.chipGenerating.setOnClickListener {
                        if (!isGenerating) {
                            GenerationScheduler.enqueueForCurriculum(this, curriculumId)
                            Snackbar.make(binding.root, "Retrying generation…", Snackbar.LENGTH_SHORT).show()
                        }
                    }
                    binding.chipGenerating.setOnLongClickListener(null)
                } else {
                    // Completed or no active work - hide if all lessons have content
                    val pending = viewModel.uiState.value.lessons.count { it.content.isBlank() }
                    binding.chipGenerating.visibility = if (pending > 0) View.VISIBLE else View.GONE
                    if (pending > 0) {
                        binding.chipGenerating.text = "$pending need content"
                        binding.chipGenerating.isEnabled = true
                        binding.chipGenerating.setOnClickListener {
                            if (!isGenerating) {
                                val state = viewModel.uiState.value
                                state.lessons.filter { it.content.isBlank() }.forEach { lesson ->
                                    GenerationScheduler.enqueueLessonContent(this, lesson.id)
                                }
                                Snackbar.make(binding.root, "Generating content for $pending lessons", Snackbar.LENGTH_SHORT).show()
                            }
                        }
                    }
                    binding.chipGenerating.setOnLongClickListener(null)
                }
            }
    }
    
    private fun showCancelGenerationDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Cancel Generation?")
            .setMessage("This will stop the background generation process.")
            .setPositiveButton("Cancel Generation") { _, _ ->
                WorkManager.getInstance(applicationContext)
                    .cancelAllWorkByTag("generation_$curriculumId")
                Snackbar.make(binding.root, "Generation cancelled", Snackbar.LENGTH_SHORT).show()
            }
            .setNegativeButton("Keep Running", null)
            .show()
    }

    private fun setupFab() {
        binding.fabStartNext.setOnClickListener {
            val next = viewModel.uiState.value.nextLesson
            if (next != null) {
                viewModel.setCurrentLesson(next.id)
                val intent = Intent(this, com.learneveryday.app.presentation.reader.LessonReaderActivity::class.java).apply {
                    putExtra(com.learneveryday.app.presentation.reader.LessonReaderActivity.EXTRA_LESSON_ID, next.id)
                }
                if (next.content.isBlank()) {
                    GenerationScheduler.enqueueLessonContent(this, next.id, expedite = true)
                    Snackbar.make(binding.root, "Generating next lesson content...", Snackbar.LENGTH_SHORT).show()
                }
                startActivity(intent)
            } else {
                Snackbar.make(binding.root, "All lessons completed!", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateUI(state: com.learneveryday.app.presentation.detail.CurriculumDetailUiState) {
        // Toolbar title
        binding.toolbar.title = state.curriculum?.title ?: "Curriculum"

        // Header
        binding.tvTitle.text = state.curriculum?.title ?: ""
        binding.tvDescription.text = state.curriculum?.description ?: ""
        binding.tvLessonCount.text = "${state.totalLessons} lessons"
        binding.tvEstimatedTime.text = "~ ${state.totalEstimatedTimeFormatted}"

        binding.progressBar.progress = state.progressPercentage.toInt()
        binding.tvProgress.text = "${state.completedLessons} of ${state.totalLessons} complete · ${state.progressPercentage.toInt()}%"

        // Difficulty chip text
        binding.chipDifficulty.text = state.curriculum?.difficulty?.name?.lowercase()?.replaceFirstChar { it.titlecase() }

        // Lessons
        lessonAdapter.submitList(state.lessons)

        // Empty state
        binding.emptyStateLayout.visibility = if (state.lessons.isEmpty()) View.VISIBLE else View.GONE

        // Generation status chip visibility managed by WorkManager observer
        // Only update text here based on pending count
        val pending = state.lessons.count { it.content.isBlank() }
        if (binding.chipGenerating.visibility == View.VISIBLE && pending > 0) {
            binding.chipGenerating.text = "$pending need content"
        }

        // Error
        state.error?.let {
            Snackbar.make(binding.root, it as CharSequence, Snackbar.LENGTH_LONG).show()
            viewModel.clearError()
        }

        // Deleted
        if (state.isDeleted) {
            Snackbar.make(binding.root, "Curriculum deleted", Snackbar.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_curriculum_detail, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_delete -> {
                confirmDelete()
                true
            }
            R.id.action_share -> {
                share()
                true
            }
            R.id.action_regenerate -> {
                if (!isGenerating) {
                    GenerationScheduler.enqueueForCurriculum(this, curriculumId)
                    Snackbar.make(binding.root, "Generating missing lessons in background", Snackbar.LENGTH_SHORT).show()
                } else {
                    Snackbar.make(binding.root, "Generation already in progress", Snackbar.LENGTH_SHORT).show()
                }
                true
            }
            R.id.action_generate_all_content -> {
                if (!isGenerating) {
                    // Enqueue content generation for all lessons missing content
                    val state = viewModel.uiState.value
                    val pending = state.lessons.filter { it.content.isBlank() }
                    if (pending.isNotEmpty()) {
                        pending.forEach { lesson ->
                            GenerationScheduler.enqueueLessonContent(this, lesson.id)
                        }
                        Snackbar.make(binding.root, "Generating content for ${pending.size} lessons", Snackbar.LENGTH_SHORT).show()
                    } else {
                        Snackbar.make(binding.root, "All lessons already have content", Snackbar.LENGTH_SHORT).show()
                    }
                } else {
                    Snackbar.make(binding.root, "Generation already in progress", Snackbar.LENGTH_SHORT).show()
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun confirmDelete() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Curriculum?")
            .setMessage("This will delete the curriculum and all its lessons and progress.")
            .setPositiveButton("Delete") { _, _ -> viewModel.deleteCurriculum() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun share() {
        val c = viewModel.uiState.value.curriculum ?: return
        val shareText = "${c.title}\n\n${c.description}\n\nLessons: ${viewModel.uiState.value.totalLessons}"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        startActivity(Intent.createChooser(intent, "Share curriculum"))
    }
}
