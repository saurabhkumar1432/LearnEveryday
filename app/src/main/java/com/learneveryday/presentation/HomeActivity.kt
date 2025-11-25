package com.learneveryday.app.presentation

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.learneveryday.app.presentation.detail.CurriculumDetailActivity
import com.learneveryday.app.R
import com.learneveryday.app.data.local.AppDatabase
import com.learneveryday.app.data.repository.*
import com.learneveryday.app.databinding.ActivityHomeBinding
import com.learneveryday.app.domain.model.Curriculum
import com.learneveryday.app.domain.model.Difficulty
import com.learneveryday.app.presentation.adapters.CurriculumAdapter
import com.learneveryday.app.presentation.home.HomeViewModel
import com.learneveryday.app.presentation.home.HomeUiState
import com.learneveryday.app.work.GenerationScheduler
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    
    private val viewModel: HomeViewModel by viewModels {
        ViewModelFactory(applicationContext)
    }

    private lateinit var curriculumAdapter: CurriculumAdapter
    private lateinit var lessonRepository: LessonRepositoryImpl

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRepository()
        setupToolbar()
        setupRecyclerView()
        setupFilters()
    setupSearch()
        setupFab()
        observeViewModel()
    }

    private fun setupRepository() {
        val database = AppDatabase.getInstance(applicationContext)
        lessonRepository = LessonRepositoryImpl(database.lessonDao())
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "LearnEveryday"
    }

    private fun setupRecyclerView() {
        curriculumAdapter = CurriculumAdapter(
            onItemClick = { curriculum ->
                openLearning(curriculum)
            },
            onMenuClick = { curriculum, view ->
                showCurriculumMenu(curriculum, view)
            }
        )

        binding.curriculumRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@HomeActivity)
            adapter = curriculumAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupFilters() {
        // Status filter
        binding.filterChipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                when (checkedIds.first()) {
                    R.id.chipAll -> viewModel.filterByType(com.learneveryday.app.presentation.home.FilterType.ALL)
                    R.id.chipInProgress -> viewModel.filterByType(com.learneveryday.app.presentation.home.FilterType.IN_PROGRESS)
                    R.id.chipCompleted -> viewModel.filterByType(com.learneveryday.app.presentation.home.FilterType.COMPLETED)
                    R.id.chipGenerating -> viewModel.filterByType(com.learneveryday.app.presentation.home.FilterType.GENERATING)
                }
            }
        }

        // Difficulty filter
        binding.difficultyChipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                val difficulty = when (checkedIds.first()) {
                    R.id.chipBeginner -> Difficulty.BEGINNER
                    R.id.chipIntermediate -> Difficulty.INTERMEDIATE
                    R.id.chipAdvanced -> Difficulty.ADVANCED
                    R.id.chipExpert -> Difficulty.EXPERT
                    else -> null
                }
                viewModel.filterByDifficulty(difficulty)
            }
        }

        // Select "All" by default
        binding.chipAll.isChecked = true
        binding.chipAllDifficulty.isChecked = true
    }

    private fun setupSearch() {
        // TODO: Wire up SearchBar with a SearchView if needed
    }

    private fun setupFab() {
        binding.fabAddCurriculum.setOnClickListener {
            // TODO: Navigate to GenerateActivity
            Snackbar.make(binding.root, "Generate screen coming soon!", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                updateUI(state)
            }
        }
    }

    private fun updateUI(state: HomeUiState) {
        // Update loading state first
        binding.loadingLayout.visibility = if (state.isLoading) View.VISIBLE else View.GONE

        // Update RecyclerView
        curriculumAdapter.submitList(state.filteredCurriculums)

        // Update empty state - only show when not loading and truly empty
        if (!state.isLoading && state.isEmpty) {
            binding.emptyStateLayout.visibility = View.VISIBLE
            binding.curriculumRecyclerView.visibility = View.GONE
        } else {
            binding.emptyStateLayout.visibility = View.GONE
            binding.curriculumRecyclerView.visibility = if (state.isLoading) View.GONE else View.VISIBLE
        }

        // Update error state
        state.error?.let { error ->
            Snackbar.make(binding.root, error as CharSequence, Snackbar.LENGTH_LONG).show()
        }
    }

    private fun openLearning(curriculum: Curriculum) {
        lifecycleScope.launch {
            viewModel.updateLastAccessed(curriculum.id)
            val intent = Intent(this@HomeActivity, CurriculumDetailActivity::class.java)
            intent.putExtra(CurriculumDetailActivity.EXTRA_CURRICULUM_ID, curriculum.id)
            startActivity(intent)
        }
    }

    private fun showCurriculumMenu(curriculum: Curriculum, @Suppress("UNUSED_PARAMETER") view: View) {
        val options = arrayOf(
            "Continue Learning",
            "Regenerate Outlines",
            "Generate All Content",
            "Share",
            "Delete"
        )
        MaterialAlertDialogBuilder(this)
            .setTitle(curriculum.title)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openLearning(curriculum)
                    1 -> regenerateOutlines(curriculum)
                    2 -> generateAllContent(curriculum)
                    3 -> shareCurriculum(curriculum)
                    4 -> confirmDeleteCurriculum(curriculum)
                }
            }
            .show()
    }

    private fun regenerateOutlines(curriculum: Curriculum) {
        GenerationScheduler.enqueueForCurriculum(this, curriculum.id)
        Snackbar.make(binding.root, "Regenerating lesson outlines in background", Snackbar.LENGTH_SHORT).show()
    }

    private fun generateAllContent(curriculum: Curriculum) {
        lifecycleScope.launch {
            try {
                val lessons = lessonRepository.getLessonsByCurriculum(curriculum.id).firstOrNull() ?: emptyList()
                val pending = lessons.filter { it.content.isBlank() }
                if (pending.isNotEmpty()) {
                    pending.forEach { lesson ->
                        GenerationScheduler.enqueueLessonContent(this@HomeActivity, lesson.id, curriculum.id)
                    }
                    Snackbar.make(binding.root, "Generating content for ${pending.size} lessons", Snackbar.LENGTH_SHORT).show()
                } else {
                    Snackbar.make(binding.root, "All lessons already have content", Snackbar.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Snackbar.make(binding.root, "Failed to start generation: ${e.message}", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun confirmDeleteCurriculum(curriculum: Curriculum) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Curriculum?")
            .setMessage("Are you sure you want to delete \"${curriculum.title}\"? This will also delete all lessons and progress.")
            .setPositiveButton("Delete") { _, _ ->
                deleteCurriculum(curriculum)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteCurriculum(curriculum: Curriculum) {
        lifecycleScope.launch {
            viewModel.deleteCurriculum(curriculum.id)
            Snackbar.make(binding.root, "Curriculum deleted", Snackbar.LENGTH_LONG)
                .setAction("Undo") {
                    // TODO: Implement undo
                }
                .show()
        }
    }

    private fun shareCurriculum(curriculum: Curriculum) {
        lifecycleScope.launch {
            try {
                val lessons = lessonRepository.getLessonsByCurriculum(curriculum.id).firstOrNull() ?: emptyList()
                val shareText = "${curriculum.title}\n\n${curriculum.description}\n\nLessons: ${lessons.size}"
                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, shareText)
                    type = "text/plain"
                }
                startActivity(Intent.createChooser(shareIntent, "Share curriculum"))
            } catch (e: Exception) {
                Snackbar.make(binding.root, "Failed to share: ${e.message}", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_home, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                // TODO: Navigate to SettingsActivity
                Snackbar.make(binding.root, "Settings coming soon!", Snackbar.LENGTH_SHORT).show()
                true
            }
            R.id.action_sync -> {
                // TODO: Implement sync
                Snackbar.make(binding.root, "Sync coming soon!", Snackbar.LENGTH_SHORT).show()
                true
            }
            R.id.action_about -> {
                showAboutDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showAboutDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("About LearnEveryday")
            .setMessage("Version 1.0.0\n\nAn AI-powered learning app that creates personalized curriculums and lessons.\n\nBuilt with Clean Architecture + MVVM")
            .setPositiveButton("OK", null)
            .show()
    }
}
