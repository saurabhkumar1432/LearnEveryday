package com.learneveryday.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.learneveryday.app.data.local.AppDatabase
import com.learneveryday.app.data.repository.CurriculumRepositoryImpl
import com.learneveryday.app.databinding.ActivityLearningPlansBinding
import com.learneveryday.app.domain.model.Curriculum
import com.learneveryday.app.presentation.adapters.CurriculumAdapter
import kotlinx.coroutines.launch

class LearningPlansActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLearningPlansBinding
    private lateinit var adapter: CurriculumAdapter
    private lateinit var repository: CurriculumRepositoryImpl

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLearningPlansBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRepository()
        setupRecyclerView()
        setupClickListeners()
        loadLearningPlans()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            val intent = Intent(this, LearningActivity::class.java)
            startActivity(intent)
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }
    }

    private fun setupRepository() {
        val database = AppDatabase.getInstance(applicationContext)
        repository = CurriculumRepositoryImpl(database.curriculumDao())
    }

    private fun setupRecyclerView() {
        adapter = CurriculumAdapter(
            onItemClick = { curriculum ->
                // Navigate to home activity with curriculum selected
                val intent = Intent(this, com.learneveryday.app.presentation.HomeActivity::class.java)
                intent.putExtra("curriculum_id", curriculum.id)
                startActivity(intent)
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            },
            onMenuClick = { curriculum, view ->
                // Show menu options
            }
        )
        
        binding.learningPlansRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.learningPlansRecyclerView.adapter = adapter
    }

    private fun setupClickListeners() {
        binding.fabCreatePlan.setOnClickListener {
            // Navigate to create new plan
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
        
        binding.btnCreatePlan.setOnClickListener {
            // Navigate to create new plan
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        binding.chipFilter.setOnClickListener {
            // TODO: Show filter dialog
            showFilterDialog()
        }

        binding.viewToggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.btnGridView -> switchToGridView()
                    R.id.btnListView -> switchToListView()
                }
            }
        }
    }

    private fun loadLearningPlans() {
        lifecycleScope.launch {
            try {
                // Show loading state
                showLoading(true)
                
                // Load curricula from database
                repository.getAllCurriculums().collect { curricula ->
                    showLoading(false)
                    
                    if (curricula.isEmpty()) {
                        showEmptyState(true)
                    } else {
                        showEmptyState(false)
                        adapter.submitList(curricula)
                        
                        // Update statistics
                        updateStatistics(curricula)
                    }
                }
            } catch (e: Exception) {
                showLoading(false)
                showEmptyState(true)
            }
        }
    }

    private fun updateStatistics(curricula: List<Curriculum>) {
        val total = curricula.size
        val inProgress = curricula.count { it.isInProgress }
        val completed = curricula.count { it.isCompleted }
        
        binding.tvTotalPlans.text = total.toString()
        binding.tvInProgress.text = inProgress.toString()
        binding.tvCompleted.text = completed.toString()
        
        // Animate the numbers
        animateNumber(binding.tvTotalPlans, 0, total)
        animateNumber(binding.tvInProgress, 0, inProgress)
        animateNumber(binding.tvCompleted, 0, completed)
    }

    private fun animateNumber(textView: android.widget.TextView, from: Int, to: Int) {
        android.animation.ValueAnimator.ofInt(from, to).apply {
            duration = 800
            interpolator = android.view.animation.DecelerateInterpolator()
            addUpdateListener { animator ->
                textView.text = animator.animatedValue.toString()
            }
            start()
        }
    }

    private fun showLoading(show: Boolean) {
        binding.learningPlansRecyclerView.visibility = if (show) View.GONE else View.VISIBLE
        // TODO: Add shimmer loading effect
    }

    private fun showEmptyState(show: Boolean) {
        binding.emptyStateLayout.visibility = if (show) View.VISIBLE else View.GONE
        binding.learningPlansRecyclerView.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun switchToGridView() {
        binding.learningPlansRecyclerView.layoutManager = 
            androidx.recyclerview.widget.GridLayoutManager(this, 2)
    }

    private fun switchToListView() {
        binding.learningPlansRecyclerView.layoutManager = 
            LinearLayoutManager(this)
    }

    private fun showFilterDialog() {
        val filters = arrayOf("All", "In Progress", "Completed", "Not Started")
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Filter Learning Plans")
            .setItems(filters) { dialog, which ->
                binding.chipFilter.text = filters[which]
                // TODO: Implement filtering logic
                dialog.dismiss()
            }
            .show()
    }
}
