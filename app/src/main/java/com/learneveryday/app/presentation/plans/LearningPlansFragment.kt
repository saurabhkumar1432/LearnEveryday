package com.learneveryday.app.presentation.plans

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.learneveryday.app.LearningActivity
import com.learneveryday.app.data.local.AppDatabase
import com.learneveryday.app.data.repository.CurriculumRepositoryImpl
import com.learneveryday.app.databinding.FragmentLearningPlansBinding
import com.learneveryday.app.domain.model.Curriculum
import com.learneveryday.app.presentation.adapters.CurriculumAdapter
import com.learneveryday.app.presentation.detail.CurriculumDetailActivity
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

class LearningPlansFragment : Fragment() {

    private var _binding: FragmentLearningPlansBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var adapter: CurriculumAdapter
    private lateinit var repository: CurriculumRepositoryImpl

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLearningPlansBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRepository()
        setupRecyclerView()
        loadLearningPlans()
    }

    private fun setupRepository() {
        val database = AppDatabase.getInstance(requireContext())
        repository = CurriculumRepositoryImpl(database.curriculumDao(), database.lessonDao())
    }

    private fun setupRecyclerView() {
        adapter = CurriculumAdapter(
            onItemClick = { curriculum ->
                val intent = Intent(requireContext(), LearningActivity::class.java)
                intent.putExtra("TOPIC_ID", curriculum.id)
                startActivity(intent)
            },
            onMenuClick = { curriculum, view ->
                showCurriculumMenu(curriculum)
            }
        )
        
        binding.learningPlansRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.learningPlansRecyclerView.adapter = adapter
    }

    private fun showCurriculumMenu(curriculum: Curriculum) {
        val options = arrayOf("Open Details", "Continue Learning", "Delete")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(curriculum.title)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openCurriculumDetail(curriculum)
                    1 -> openLearning(curriculum)
                    2 -> confirmDelete(curriculum)
                }
            }
            .show()
    }

    private fun openCurriculumDetail(curriculum: Curriculum) {
        val intent = Intent(requireContext(), CurriculumDetailActivity::class.java)
        intent.putExtra(CurriculumDetailActivity.EXTRA_CURRICULUM_ID, curriculum.id)
        startActivity(intent)
    }

    private fun openLearning(curriculum: Curriculum) {
        val intent = Intent(requireContext(), LearningActivity::class.java)
        intent.putExtra("TOPIC_ID", curriculum.id)
        startActivity(intent)
    }

    private fun confirmDelete(curriculum: Curriculum) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Curriculum?")
            .setMessage("Are you sure you want to delete \"${curriculum.title}\"? This will also delete all lessons and progress.")
            .setPositiveButton("Delete") { _, _ ->
                deleteCurriculum(curriculum)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteCurriculum(curriculum: Curriculum) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                repository.deleteCurriculumById(curriculum.id)
                Snackbar.make(binding.root, "Curriculum deleted", Snackbar.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Snackbar.make(binding.root, "Failed to delete: ${e.message}", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadLearningPlans() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                repository.getAllCurriculums()
                    .onStart {
                        showLoading(true)
                        showEmptyState(false)
                    }
                    .catch { error ->
                        showLoading(false)
                        showEmptyState(true)
                        android.util.Log.e(TAG, "Error loading plans", error)
                    }
                    .collectLatest { curricula ->
                        showLoading(false)
                        if (curricula.isEmpty()) {
                            showEmptyState(true)
                            adapter.submitList(emptyList())
                        } else {
                            showEmptyState(false)
                            adapter.submitList(curricula)
                        }
                    }
            }
        }
    }

    private fun showLoading(show: Boolean) {
        _binding?.let { binding ->
            binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
            binding.learningPlansRecyclerView.visibility = if (show) View.GONE else View.VISIBLE
        }
    }

    private fun showEmptyState(show: Boolean) {
        _binding?.let { binding ->
            binding.emptyStateLayout.visibility = if (show) View.VISIBLE else View.GONE
            binding.learningPlansRecyclerView.visibility = if (show) View.GONE else View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "LearningPlansFragment"
    }
}
