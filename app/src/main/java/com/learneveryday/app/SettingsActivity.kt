package com.learneveryday.app

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.slider.Slider
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class SettingsActivity : AppCompatActivity() {

    private lateinit var prefsManager: PreferencesManager
    private lateinit var providerSpinner: Spinner
    private lateinit var modelLayout: TextInputLayout
    private lateinit var modelInput: TextInputEditText
    private lateinit var apiKeyLayout: TextInputLayout
    private lateinit var apiKeyInput: TextInputEditText
    private lateinit var customEndpointLayout: TextInputLayout
    private lateinit var customEndpointInput: TextInputEditText
    private lateinit var temperatureSlider: Slider
    private lateinit var temperatureText: TextView
    private lateinit var maxTokensInput: EditText
    private lateinit var saveButton: MaterialButton
    private lateinit var testButton: MaterialButton
    private lateinit var notificationsSwitch: Switch
    private lateinit var infoText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "AI Configuration"

        prefsManager = PreferencesManager(this)

        // Initialize views
        providerSpinner = findViewById(R.id.providerSpinner)
        modelLayout = findViewById(R.id.modelLayout)
        modelInput = findViewById(R.id.modelInput)
        apiKeyLayout = findViewById(R.id.apiKeyLayout)
        apiKeyInput = findViewById(R.id.apiKeyInput)
        customEndpointLayout = findViewById(R.id.customEndpointLayout)
        customEndpointInput = findViewById(R.id.customEndpointInput)
        temperatureSlider = findViewById(R.id.temperatureSlider)
        temperatureText = findViewById(R.id.temperatureText)
        maxTokensInput = findViewById(R.id.maxTokensInput)
        saveButton = findViewById(R.id.saveButton)
        testButton = findViewById(R.id.testButton)
        notificationsSwitch = findViewById(R.id.notificationsSwitch)
        infoText = findViewById(R.id.infoText)

        setupProviderSpinner()
        setupTemperatureSlider()
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
                loadProviderSettings(provider)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun loadProviderSettings(provider: AIProvider) {
        // Load saved settings for this specific provider
        val savedApiKey = prefsManager.getAPIKey(provider)
        val savedModel = prefsManager.getModelName(provider)
        
        // Update UI with saved values or defaults
        apiKeyInput.setText(savedApiKey ?: "")
        
        if (savedModel.isNotEmpty()) {
            modelInput.setText(savedModel)
        } else {
            // Set default model suggestion if no saved value
            setDefaultModelForProvider(provider)
        }
        
        if (provider == AIProvider.CUSTOM) {
            customEndpointInput.setText(prefsManager.getCustomAPIEndpoint() ?: "")
        }
    }

    private fun setDefaultModelForProvider(provider: AIProvider) {
        // Set a default model suggestion based on provider
        val currentModel = modelInput.text.toString()
        
        // Only set default if field is empty
        if (currentModel.isEmpty()) {
            val defaultModel = when (provider) {
                AIProvider.GEMINI -> "gemini-2.0-flash-exp"
                AIProvider.OPENROUTER -> "anthropic/claude-3.5-sonnet"
                AIProvider.OPENAI -> "gpt-4o"
                AIProvider.ANTHROPIC -> "claude-3-5-sonnet-20241022"
                AIProvider.CUSTOM -> ""
            }
            modelInput.setText(defaultModel)
        }
    }
    
    private fun setupTemperatureSlider() {
        temperatureSlider.addOnChangeListener { _, value, _ ->
            temperatureText.text = "Temperature: %.1f".format(value)
        }
    }

    private fun updateUIForProvider(provider: AIProvider) {
        when (provider) {
            AIProvider.GEMINI -> {
                apiKeyLayout.visibility = View.VISIBLE
                customEndpointLayout.visibility = View.GONE
                testButton.visibility = View.VISIBLE
                apiKeyLayout.hint = "Gemini API Key"
                modelLayout.hint = "Model Name"
                modelLayout.helperText = "e.g., gemini-2.0-flash-exp, gemini-1.5-pro, gemini-1.5-flash"
                infoText.text = "Get your API key from Google AI Studio:\nhttps://makersuite.google.com/app/apikey"
            }
            AIProvider.OPENROUTER -> {
                apiKeyLayout.visibility = View.VISIBLE
                customEndpointLayout.visibility = View.GONE
                testButton.visibility = View.VISIBLE
                apiKeyLayout.hint = "OpenRouter API Key"
                modelLayout.hint = "Model Name"
                modelLayout.helperText = "e.g., anthropic/claude-3.5-sonnet, openai/gpt-4o, google/gemini-2.0-flash-exp"
                infoText.text = "Get your API key from:\nhttps://openrouter.ai/keys\n\nAccess to multiple AI models with one API key!"
            }
            AIProvider.OPENAI -> {
                apiKeyLayout.visibility = View.VISIBLE
                customEndpointLayout.visibility = View.GONE
                testButton.visibility = View.VISIBLE
                apiKeyLayout.hint = "OpenAI API Key"
                modelLayout.hint = "Model Name"
                modelLayout.helperText = "e.g., gpt-4o, gpt-4-turbo, gpt-4, gpt-3.5-turbo"
                infoText.text = "Get your API key from:\nhttps://platform.openai.com/api-keys"
            }
            AIProvider.ANTHROPIC -> {
                apiKeyLayout.visibility = View.VISIBLE
                customEndpointLayout.visibility = View.GONE
                testButton.visibility = View.VISIBLE
                apiKeyLayout.hint = "Anthropic API Key"
                modelLayout.hint = "Model Name"
                modelLayout.helperText = "e.g., claude-3-5-sonnet-20241022, claude-3-opus-20240229, claude-3-sonnet-20240229"
                infoText.text = "Get your API key from:\nhttps://console.anthropic.com/"
            }
            AIProvider.CUSTOM -> {
                apiKeyLayout.visibility = View.VISIBLE
                customEndpointLayout.visibility = View.VISIBLE
                testButton.visibility = View.VISIBLE
                apiKeyLayout.hint = "API Key"
                customEndpointLayout.hint = "API Endpoint URL"
                modelLayout.hint = "Model Name"
                modelLayout.helperText = "Enter the model name supported by your API"
                infoText.text = "Enter your custom API endpoint and key.\nThe endpoint should accept POST requests with JSON."
            }
        }
    }

    private fun setupSaveButton() {
        saveButton.setOnClickListener {
            val selectedProvider = AIProvider.values()[providerSpinner.selectedItemPosition]
            val apiKey = apiKeyInput.text.toString().trim()
            val customEndpoint = customEndpointInput.text.toString().trim()
            val modelName = modelInput.text.toString().trim()
            val temperature = temperatureSlider.value
            val maxTokens = maxTokensInput.text.toString().toIntOrNull() ?: 8000

            // Validate inputs
            if (apiKey.isEmpty()) {
                apiKeyInput.error = "API key is required"
                return@setOnClickListener
            }
            if (modelName.isEmpty()) {
                modelInput.error = "Model name is required"
                return@setOnClickListener
            }
            if (selectedProvider == AIProvider.CUSTOM && customEndpoint.isEmpty()) {
                customEndpointInput.error = "API endpoint is required"
                return@setOnClickListener
            }

            // Save settings
            prefsManager.setAIProvider(selectedProvider)
            prefsManager.setAPIKey(apiKey, selectedProvider)
            prefsManager.setModelName(modelName, selectedProvider)
            prefsManager.setTemperature(temperature)
            prefsManager.setMaxTokens(maxTokens)
            
            if (selectedProvider == AIProvider.CUSTOM) {
                prefsManager.setCustomAPIEndpoint(customEndpoint)
            }

            Toast.makeText(this, "Settings saved for ${selectedProvider.displayName}!", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setupTestButton() {
        testButton.setOnClickListener {
            val apiKey = apiKeyInput.text.toString().trim()

            if (apiKey.isEmpty()) {
                Toast.makeText(this, "Please enter an API key first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Toast.makeText(
                this,
                "Settings look good! Save and try generating a curriculum from the main screen.",
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
        
        // Load settings for the current provider
        loadProviderSettings(savedProvider)
        
        // Load global settings (temperature, max tokens)
        temperatureSlider.value = prefsManager.getTemperature()
        temperatureText.text = "Temperature: %.1f".format(prefsManager.getTemperature())
        maxTokensInput.setText(prefsManager.getMaxTokens().toString())
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
