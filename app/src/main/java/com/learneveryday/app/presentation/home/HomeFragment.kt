package com.learneveryday.app.presentation.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.learneveryday.app.R
import com.learneveryday.app.SettingsActivity
import com.learneveryday.app.SuggestedTopics
import com.learneveryday.app.databinding.FragmentHomeBinding
import com.learneveryday.app.presentation.adapters.SuggestedTopicAdapter

import com.learneveryday.app.utils.AnimationHelper
import com.google.android.material.chip.Chip

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var suggestedAdapter: SuggestedTopicAdapter
    
    // Callback for generating curriculum (handled by MainActivity)
    var onGenerateCurriculum: ((String, String) -> Unit)? = null
    var onCustomTopic: (() -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize adapter
        suggestedAdapter = SuggestedTopicAdapter(SuggestedTopics.getPopular()) { topic ->
            onGenerateCurriculum?.invoke(topic.title, topic.description)
        }
        
        binding.suggestedTopicsRecyclerView.apply {
            layoutManager = GridLayoutManager(context, 2) // Hardcoded 2 for now or use resource
            adapter = suggestedAdapter
        }

        setupCategoryChips()

        // Setup search
        binding.searchButton.setOnClickListener {
            val query = binding.searchInput.text.toString()
            if (query.isNotEmpty()) {
                searchTopics(query)
            } else {
                suggestedAdapter.updateTopics(SuggestedTopics.getPopular())
            }
        }

        // Setup custom topic creation (FAB)
        binding.createCustomButton.setOnClickListener {
            onCustomTopic?.invoke()
        }
    }

    private fun setupCategoryChips() {
        binding.categoryChipGroup.removeAllViews()
        
        // Add "All" chip
        val allChip = Chip(context).apply {
            text = "All"
            isCheckable = true
            isChecked = true
            setOnClickListener {
                suggestedAdapter.updateTopics(SuggestedTopics.getPopular())
            }
        }
        binding.categoryChipGroup.addView(allChip)

        // Add category chips
        SuggestedTopics.getCategories().forEach { category ->
            val chip = Chip(context).apply {
                text = category
                isCheckable = true
                setOnClickListener {
                    suggestedAdapter.updateTopics(SuggestedTopics.getByCategory(category))
                }
            }
            binding.categoryChipGroup.addView(chip)
        }
    }

    private fun searchTopics(query: String) {
        val results = SuggestedTopics.searchTopics(query)
        suggestedAdapter.updateTopics(results)
        
        if (results.isEmpty()) {
            Toast.makeText(context, "No topics found for '$query'", Toast.LENGTH_SHORT).show()
        }
    }

    private fun animateViews() {
        AnimationHelper.slideInFromBottom(binding.welcomeText, 0)
        AnimationHelper.fadeInWithScale(binding.searchSection, 100)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
