package com.learneveryday.app.presentation.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.learneveryday.app.PreferencesManager
import com.learneveryday.app.R
import com.learneveryday.app.SettingsActivity
import com.learneveryday.app.SuggestedTopics
import com.learneveryday.app.databinding.FragmentHomeBinding
import com.learneveryday.app.domain.service.TopicSuggestion
import com.learneveryday.app.presentation.adapters.SuggestedTopicAdapter

import com.learneveryday.app.utils.AnimationHelper
import com.google.android.material.chip.Chip
import java.util.Calendar

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var suggestedAdapter: SuggestedTopicAdapter
    private lateinit var prefsManager: PreferencesManager
    private var rotateAnimation: RotateAnimation? = null
    
    // Callback for generating curriculum (handled by MainActivity)
    var onGenerateCurriculum: ((String, String) -> Unit)? = null
    var onCustomTopic: (() -> Unit)? = null
    var onRefreshTopics: (() -> Unit)? = null

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
        
        // Check if fragment is properly attached
        if (!isAdded || context == null) {
            android.util.Log.w("HomeFragment", "Fragment not properly attached, skipping initialization")
            return
        }
        
        prefsManager = PreferencesManager(requireContext())
        
        try {
            initializeViews()
        } catch (e: Exception) {
            android.util.Log.e("HomeFragment", "Error initializing HomeFragment", e)
            // Show error to user instead of crashing
            try {
                if (isAdded && context != null) {
                    Toast.makeText(context, "Error loading Home: ${e.message}", Toast.LENGTH_LONG).show()
                }
            } catch (e2: Exception) {
                // Context might be null, silent failure
            }
        }
    }
    
    private fun initializeViews() {
        // Set dynamic greeting
        updateGreeting()
        
        // Load saved AI topics or default topics
        val initialTopics = loadInitialTopics()
        
        // Initialize adapter
        suggestedAdapter = SuggestedTopicAdapter(initialTopics) { topic ->
            onGenerateCurriculum?.invoke(topic.title, topic.description)
        }
        
        binding.suggestedTopicsRecyclerView.apply {
                layoutManager = GridLayoutManager(context, 2)
                adapter = suggestedAdapter
        }
        
        setupCategoryChips()
        setupSearch()
        setupRefreshButton()
    }
    
    private fun loadInitialTopics(): List<SuggestedTopics.TopicSuggestion> {
        // Check if we have saved AI-generated topics
        val savedTopics = prefsManager.getSuggestedTopics()
        
        return if (savedTopics != null && savedTopics.isNotEmpty()) {
            // Convert AI TopicSuggestion to SuggestedTopics.TopicSuggestion
            savedTopics.map { aiTopic ->
                SuggestedTopics.TopicSuggestion(
                    id = aiTopic.id,
                    title = aiTopic.title,
                    description = aiTopic.description,
                    icon = aiTopic.icon,
                    category = aiTopic.category,
                    popularityScore = 10,
                    tags = aiTopic.tags
                )
            }
        } else {
            // Return default topics
            SuggestedTopics.getPopular()
        }
    }
    
    private fun setupRefreshButton() {
        binding.refreshTopicsButton.setOnClickListener {
            onRefreshTopics?.invoke()
        }
    }
    
    fun setRefreshLoading(isLoading: Boolean) {
        if (!isAdded || _binding == null) return
        
        if (isLoading) {
            // Start rotation animation on the refresh button
            rotateAnimation = RotateAnimation(
                0f, 360f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
            ).apply {
                duration = 1000
                repeatCount = Animation.INFINITE
                interpolator = LinearInterpolator()
            }
            binding.refreshTopicsButton.startAnimation(rotateAnimation)
            binding.refreshTopicsButton.isEnabled = false
        } else {
            rotateAnimation?.cancel()
            binding.refreshTopicsButton.clearAnimation()
            binding.refreshTopicsButton.isEnabled = true
        }
    }
    
    fun updateWithAITopics(topics: List<TopicSuggestion>) {
        if (!isAdded || _binding == null) return
        
        // Convert AI TopicSuggestion to SuggestedTopics.TopicSuggestion
        val convertedTopics = topics.map { aiTopic ->
            SuggestedTopics.TopicSuggestion(
                id = aiTopic.id,
                title = aiTopic.title,
                description = aiTopic.description,
                icon = aiTopic.icon,
                category = aiTopic.category,
                popularityScore = 10, // AI-generated topics are considered popular
                tags = aiTopic.tags
            )
        }
        suggestedAdapter.updateTopics(convertedTopics)
        
        // Show success message
        Toast.makeText(context, "Topics refreshed! ✨", Toast.LENGTH_SHORT).show()
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

    private fun setupSearch() {
        // Handle search button click
        binding.searchButton.setOnClickListener {
            performSearch()
        }
        
        // Handle keyboard search action
        binding.searchInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch()
                true
            } else {
                false
            }
        }
        
        // Handle focus - scroll the search bar into view when focused
        binding.searchInput.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                // Scroll to make search bar visible
                binding.scrollView.post {
                    binding.scrollView.smoothScrollTo(0, 0)
                }
            }
        }
    }
    
    private fun performSearch() {
        val query = binding.searchInput.text.toString().trim()
        
        // Hide keyboard
        hideKeyboard()
        
        if (query.isNotEmpty()) {
            searchTopics(query)
        } else {
            suggestedAdapter.updateTopics(SuggestedTopics.getPopular())
        }
    }
    
    private fun hideKeyboard() {
        val imm = context?.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(binding.searchInput.windowToken, 0)
        binding.searchInput.clearFocus()
        // Return focus to scroll view
        binding.scrollView.requestFocus()
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

    private fun updateGreeting() {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val greeting = when {
            hour < 12 -> "Good morning ☀️"
            hour < 17 -> "Good afternoon 👋"
            hour < 21 -> "Good evening 🌙"
            else -> "Good night 🌟"
        }
        binding.greetingText.text = greeting
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
