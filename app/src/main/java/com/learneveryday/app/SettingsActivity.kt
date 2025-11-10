package com.learneveryday.app

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class SettingsActivity : AppCompatActivity() {

    private lateinit var prefsManager: PreferencesManager
    private lateinit var providerSpinner: Spinner
    private lateinit var apiKeyLayout: TextInputLayout
    private lateinit var apiKeyInput: TextInputEditText
    private lateinit var customEndpointLayout: TextInputLayout
    private lateinit var customEndpointInput: TextInputEditText
    private lateinit var saveButton: MaterialButton
    private lateinit var testButton: MaterialButton
    private lateinit var notificationsSwitch: Switch
    private lateinit var infoText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Settings"

        prefsManager = PreferencesManager(this)

        // Initialize views
        providerSpinner = findViewById(R.id.providerSpinner)
        apiKeyLayout = findViewById(R.id.apiKeyLayout)
        apiKeyInput = findViewById(R.id.apiKeyInput)
        customEndpointLayout = findViewById(R.id.customEndpointLayout)
        customEndpointInput = findViewById(R.id.customEndpointInput)
        saveButton = findViewById(R.id.saveButton)
        testButton = findViewById(R.id.testButton)
        notificationsSwitch = findViewById(R.id.notificationsSwitch)
        infoText = findViewById(R.id.infoText)

        setupProviderSpinner()
        setupSaveButton()
        setupTestButton()
        setupNotificationsSwitch()
        loadSavedSettings()
    }

    private fun setupProviderSpinner() {
        val providers = AIProvider.values().map { it.displayName }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, providers)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        providerSpinner.adapter = adapter

        providerSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val provider = AIProvider.values()[position]
                updateUIForProvider(provider)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun updateUIForProvider(provider: AIProvider) {
        when (provider) {
            AIProvider.NONE -> {
                apiKeyLayout.visibility = View.GONE
                customEndpointLayout.visibility = View.GONE
                testButton.visibility = View.GONE
                infoText.text = "Using built-in pre-generated curriculum. No API key required."
            }
            AIProvider.GEMINI -> {
                apiKeyLayout.visibility = View.VISIBLE
                customEndpointLayout.visibility = View.GONE
                testButton.visibility = View.VISIBLE
                apiKeyLayout.hint = "Gemini API Key"
                infoText.text = "Get your API key from Google AI Studio (makersuite.google.com/app/apikey)"
            }
            AIProvider.OPENROUTER -> {
                apiKeyLayout.visibility = View.VISIBLE
                customEndpointLayout.visibility = View.GONE
                testButton.visibility = View.VISIBLE
                apiKeyLayout.hint = "OpenRouter API Key"
                infoText.text = "Get your API key from OpenRouter (openrouter.ai/keys)"
            }
            AIProvider.CUSTOM -> {
                apiKeyLayout.visibility = View.VISIBLE
                customEndpointLayout.visibility = View.VISIBLE
                testButton.visibility = View.VISIBLE
                apiKeyLayout.hint = "API Key"
                customEndpointLayout.hint = "API Endpoint URL"
                infoText.text = "Enter your custom API endpoint and key. The endpoint should accept POST requests."
            }
        }
    }

    private fun setupSaveButton() {
        saveButton.setOnClickListener {
            val selectedProvider = AIProvider.values()[providerSpinner.selectedItemPosition]
            val apiKey = apiKeyInput.text.toString().trim()
            val customEndpoint = customEndpointInput.text.toString().trim()

            // Validate inputs
            if (selectedProvider != AIProvider.NONE) {
                if (apiKey.isEmpty()) {
                    apiKeyInput.error = "API key is required"
                    return@setOnClickListener
                }
                if (selectedProvider == AIProvider.CUSTOM && customEndpoint.isEmpty()) {
                    customEndpointInput.error = "API endpoint is required"
                    return@setOnClickListener
                }
            }

            // Save settings
            prefsManager.setAIProvider(selectedProvider)
            prefsManager.setAPIKey(apiKey)
            if (selectedProvider == AIProvider.CUSTOM) {
                prefsManager.setCustomAPIEndpoint(customEndpoint)
            }

            Toast.makeText(this, "Settings saved successfully!", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setupTestButton() {
        testButton.setOnClickListener {
            val selectedProvider = AIProvider.values()[providerSpinner.selectedItemPosition]
            val apiKey = apiKeyInput.text.toString().trim()

            if (apiKey.isEmpty()) {
                Toast.makeText(this, "Please enter an API key first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Show test message
            Toast.makeText(
                this,
                "API connection test would be performed here. For now, please save and try using the app.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun setupNotificationsSwitch() {
        notificationsSwitch.isChecked = prefsManager.isNotificationsEnabled()
        notificationsSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefsManager.setNotificationsEnabled(isChecked)
            if (isChecked) {
                Toast.makeText(this, "Notifications enabled", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Notifications disabled", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadSavedSettings() {
        val savedProvider = prefsManager.getAIProvider()
        providerSpinner.setSelection(savedProvider.ordinal)
        
        prefsManager.getAPIKey()?.let {
            apiKeyInput.setText(it)
        }
        
        prefsManager.getCustomAPIEndpoint()?.let {
            customEndpointInput.setText(it)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
