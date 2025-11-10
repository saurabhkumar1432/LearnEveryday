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
import com.learneveryday.app.R
import com.learneveryday.app.data.repository.*
import com.learneveryday.app.databinding.ActivityHomeBinding
import com.learneveryday.app.domain.model.Curriculum
import com.learneveryday.app.domain.model.Difficulty
import com.learneveryday.app.presentation.adapters.CurriculumAdapter
import com.learneveryday.app.presentation.home.HomeViewModel
import com.learneveryday.app.presentation.home.HomeUiState
import kotlinx.coroutines.launch

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    
    private val viewModel: HomeViewModel by viewModels {
        ViewModelFactory(applicationContext)
    }

    private lateinit var curriculumAdapter: CurriculumAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupFilters()
    setupSearch()
        setupFab()
        observeViewModel()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "LearnEveryday"
    }

    private fun setupRecyclerView() {
        curriculumAdapter = CurriculumAdapter(
            onItemClick = { curriculum ->
                openCurriculumDetail(curriculum)
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

    private fun openCurriculumDetail(curriculum: Curriculum) {
        lifecycleScope.launch {
            viewModel.updateLastAccessed(curriculum.id)
            // TODO: Navigate to CurriculumDetailActivity
            Snackbar.make(binding.root, "Opening ${curriculum.title}", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun showCurriculumMenu(curriculum: Curriculum, view: View) {
        val options = arrayOf("Open", "Delete", "Share", "Export")
        MaterialAlertDialogBuilder(this)
            .setTitle(curriculum.title)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openCurriculumDetail(curriculum)
                    1 -> confirmDeleteCurriculum(curriculum)
                    2 -> shareCurriculum(curriculum)
                    3 -> exportCurriculum(curriculum)
                }
            }
            .show()
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
    val shareText = "Check out this curriculum: ${curriculum.title}\n${curriculum.description}"
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }
        startActivity(Intent.createChooser(shareIntent, "Share curriculum"))
    }

    private fun exportCurriculum(curriculum: Curriculum) {
        // TODO: Implement curriculum export
        Snackbar.make(binding.root, "Export feature coming soon!", Snackbar.LENGTH_SHORT).show()
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
