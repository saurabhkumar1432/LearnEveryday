# LearnEveryday

An Android app designed to help you learn new subjects through bite-sized, hourly lessons. Whether you want to learn Python, JavaScript, Kotlin, or other programming languages, LearnEveryday provides a structured learning path with progress tracking.

## Features

- **📚 Multiple Learning Topics**: Pre-built curriculums for Python, JavaScript, Kotlin, Java, Web Development, and Data Science
- **✅ Task-Based Learning**: Mark lessons as complete and track your progress
- **🎯 Bite-Sized Lessons**: Each lesson is designed to be consumed in short sessions
- **📊 Progress Tracking**: Visual progress indicators showing completed vs. total lessons
- **🤖 AI Integration**: Optional AI provider configuration (Gemini, OpenRouter, or Custom API)
- **🔔 Learning Reminders**: Hourly notifications to keep you on track (configurable)
- **💾 Offline Support**: All content stored locally, works without internet

## Installation

### From Releases (Recommended)
1. Go to the [Releases](https://github.com/saurabhkumar1432/LearnEveryday/releases) section
2. Download the latest APK file
3. Enable "Install from Unknown Sources" on your Android device
4. Install the APK

### Build from Source
```bash
# Clone the repository
git clone https://github.com/saurabhkumar1432/LearnEveryday.git
cd LearnEveryday

# Build the APK
./gradlew assembleRelease

# APK will be in: app/build/outputs/apk/release/
```

## Usage

1. **Select a Topic**: Choose what you want to learn from the main screen
2. **Start Learning**: Tap "Start Learning" to begin your journey
3. **Complete Lessons**: Read each lesson and mark it complete when done
4. **Track Progress**: Monitor your completion percentage and completed lessons
5. **Configure AI (Optional)**: Go to Settings to add AI provider details for enhanced learning

## AI Provider Configuration

The app supports multiple AI providers for generating dynamic learning content:

### Google Gemini
1. Get an API key from [Google AI Studio](https://makersuite.google.com/app/apikey)
2. Go to Settings in the app
3. Select "Google Gemini" as the provider
4. Enter your API key
5. Save settings

### OpenRouter
1. Get an API key from [OpenRouter](https://openrouter.ai/keys)
2. Go to Settings in the app
3. Select "OpenRouter" as the provider
4. Enter your API key
5. Save settings

### Custom API
1. Prepare your custom API endpoint that accepts POST requests
2. Go to Settings in the app
3. Select "Custom API" as the provider
4. Enter your API endpoint URL
5. Enter your API key
6. Save settings

### Built-in Curriculum
By default, the app uses pre-generated learning curriculums that don't require an API key. This provides a complete learning experience without external dependencies.

## CI/CD Pipeline

This project includes an automated CI/CD pipeline using GitHub Actions:

- **Automatic Builds**: APKs are built automatically on every push to `main`
- **Releases**: New releases are created automatically with versioned APKs
- **Artifacts**: APK files are saved as GitHub artifacts for 30 days

### Setting Up APK Signing (Optional)

To enable signed APKs in CI/CD, add these secrets to your repository:

1. Go to Repository Settings → Secrets → Actions
2. Add the following secrets:
   - `SIGNING_KEY`: Base64 encoded keystore file
   - `ALIAS`: Key alias
   - `KEY_STORE_PASSWORD`: Keystore password
   - `KEY_PASSWORD`: Key password

To generate a keystore and encode it:
```bash
# Generate keystore
keytool -genkey -v -keystore my-release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias my-key-alias

# Encode to base64
base64 my-release-key.jks | tr -d '\n' > keystore.txt
```

## Technology Stack

- **Language**: Kotlin
- **Minimum SDK**: Android 7.0 (API 24)
- **Target SDK**: Android 14 (API 34)
- **Architecture**: MVVM (Implicit with SharedPreferences)
- **UI**: Material Design Components
- **Storage**: SharedPreferences with Gson
- **Notifications**: WorkManager (future implementation)

## Project Structure

```
LearnEveryday/
├── app/
│   ├── src/main/
│   │   ├── java/com/learneveryday/app/
│   │   │   ├── MainActivity.kt
│   │   │   ├── LearningActivity.kt
│   │   │   ├── SettingsActivity.kt
│   │   │   ├── Models.kt
│   │   │   ├── LearningCurriculum.kt
│   │   │   ├── PreferencesManager.kt
│   │   │   ├── TopicAdapter.kt
│   │   │   └── LearningReminderReceiver.kt
│   │   ├── res/
│   │   │   ├── layout/
│   │   │   ├── values/
│   │   │   └── menu/
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── .github/workflows/
│   └── android-build.yml
└── README.md
```

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## Future Enhancements

- [ ] Implement WorkManager for true hourly notifications
- [ ] Add actual AI API integration for dynamic content generation
- [ ] Support for more learning topics
- [ ] Quiz mode for testing knowledge
- [ ] Spaced repetition algorithm
- [ ] Export/import progress
- [ ] Dark mode support
- [ ] Multiple language support

## License

This project is open source and available under the MIT License.

## Support

If you encounter any issues or have questions:
- Open an issue on GitHub
- Check existing issues for solutions

---

**Made with ❤️ for continuous learning**