# LearnEveryday - Developer Setup Guide

## Project Overview

LearnEveryday is an Android learning app with the following characteristics:
- **Language**: Kotlin
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 34 (Android 14)
- **Build System**: Gradle with Kotlin DSL
- **Architecture**: Simple MVVM-like pattern with SharedPreferences

## Prerequisites

- JDK 17 or higher
- Android Studio Hedgehog or newer (recommended)
- Git

## Local Development Setup

### 1. Clone the Repository

```bash
git clone https://github.com/saurabhkumar1432/LearnEveryday.git
cd LearnEveryday
```

### 2. Open in Android Studio

1. Open Android Studio
2. Select "Open an Existing Project"
3. Navigate to the cloned directory and select it
4. Wait for Gradle sync to complete

### 3. Build the Project

Using Android Studio:
- Click "Build" → "Make Project" (Ctrl+F9 / Cmd+F9)

Using Command Line:
```bash
./gradlew build
```

### 4. Run the App

Using Android Studio:
- Click the "Run" button or press Shift+F10
- Select an emulator or connected device

Using Command Line:
```bash
./gradlew installDebug
```

## Project Structure

```
LearnEveryday/
├── .github/workflows/          # CI/CD pipeline
│   └── android-build.yml
├── app/
│   ├── src/main/
│   │   ├── java/com/learneveryday/app/
│   │   │   ├── MainActivity.kt           # Topic selection screen
│   │   │   ├── LearningActivity.kt       # Lesson display with progress
│   │   │   ├── SettingsActivity.kt       # AI provider configuration
│   │   │   ├── Models.kt                 # Data models
│   │   │   ├── LearningCurriculum.kt     # Pre-built lesson content
│   │   │   ├── PreferencesManager.kt     # Settings & progress storage
│   │   │   ├── TopicAdapter.kt           # RecyclerView adapter
│   │   │   └── LearningReminderReceiver.kt # Notification handler
│   │   ├── res/
│   │   │   ├── layout/                   # XML layouts
│   │   │   ├── values/                   # Strings, colors, themes
│   │   │   ├── menu/                     # Menu resources
│   │   │   └── mipmap-*/                 # App icons
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── gradle/
│   └── wrapper/
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

## Key Components

### Data Models (Models.kt)

- **LearningTopic**: Represents a learning subject with lessons
- **Lesson**: Individual lesson with title, content, and completion status
- **UserProgress**: Tracks user's progress through a topic

### Storage (PreferencesManager.kt)

Uses SharedPreferences with Gson for:
- User progress tracking
- Current topic selection
- AI provider configuration
- API keys (stored securely in private preferences)

### Curriculum (LearningCurriculum.kt)

Pre-generated content includes:
- Python Programming (24 detailed lessons)
- JavaScript (24 lessons)
- Kotlin for Android (24 lessons)
- Java Programming (24 lessons)
- Web Development (24 lessons)
- Data Science Basics (24 lessons)

## Building Release APK

### Debug Build

```bash
./gradlew assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk
```

### Release Build (Unsigned)

```bash
./gradlew assembleRelease
# Output: app/build/outputs/apk/release/app-release-unsigned.apk
```

### Release Build (Signed)

1. Create a keystore:
```bash
keytool -genkey -v -keystore my-release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias my-key-alias
```

2. Create `keystore.properties` in project root:
```properties
storePassword=your_store_password
keyPassword=your_key_password
keyAlias=my-key-alias
storeFile=path/to/my-release-key.jks
```

3. Update `app/build.gradle.kts` to include signing config (optional enhancement)

4. Build:
```bash
./gradlew assembleRelease
```

## CI/CD Pipeline

### GitHub Actions Workflow

The project includes an automated CI/CD pipeline (`.github/workflows/android-build.yml`) that:

1. **Triggers on**:
   - Push to `main` branch
   - Pull requests to `main`
   - Manual workflow dispatch

2. **Build Process**:
   - Sets up JDK 17
   - Caches Gradle dependencies
   - Builds the project
   - Generates release APK
   - (Optional) Signs APK with repository secrets
   - Uploads APK as artifact
   - Creates GitHub release with APK

3. **Required Secrets** (for signed APKs):
   - `SIGNING_KEY`: Base64-encoded keystore
   - `ALIAS`: Key alias
   - `KEY_STORE_PASSWORD`: Keystore password
   - `KEY_PASSWORD`: Key password

### Setting Up Repository Secrets

1. Go to repository Settings → Secrets and variables → Actions
2. Click "New repository secret"
3. Add each secret with its value

### Generating Base64 Keystore

```bash
# Encode keystore to base64
base64 -i my-release-key.jks | tr -d '\n' > keystore_base64.txt

# On macOS:
base64 -i my-release-key.jks | tr -d '\n' | pbcopy

# Copy the content and add as SIGNING_KEY secret
```

## Testing the App

### Manual Testing Checklist

1. **Topic Selection**:
   - [ ] All 6 topics are displayed
   - [ ] Selecting a topic highlights it
   - [ ] Start Learning button activates when topic selected

2. **Learning Activity**:
   - [ ] Lessons display correctly
   - [ ] Progress bar updates when marking complete
   - [ ] Previous/Next navigation works
   - [ ] Progress persists across sessions

3. **Settings**:
   - [ ] Can access Settings from menu
   - [ ] Provider selection updates UI
   - [ ] API key can be saved
   - [ ] Settings persist across sessions

### Running Tests

```bash
# Run unit tests
./gradlew test

# Run instrumented tests (requires emulator/device)
./gradlew connectedAndroidTest
```

## Troubleshooting

### Gradle Build Fails

```bash
# Clean and rebuild
./gradlew clean build
```

### Gradle Sync Issues

1. File → Invalidate Caches → Invalidate and Restart
2. Delete `.gradle` and `.idea` folders
3. Reimport project

### APK Installation Fails

- Enable "Install from Unknown Sources" on your device
- Check minimum SDK version (API 24+)

## Adding New Learning Topics

1. Open `LearningCurriculum.kt`
2. Create a new function similar to `getPythonCurriculum()`
3. Add 24 lessons with unique IDs
4. Add the new topic to `getAllTopics()` list
5. Update strings.xml with topic name if needed

## Extending AI Integration

The app currently has placeholders for AI integration. To implement:

1. Create an `AIService.kt` class
2. Implement API calls for each provider (Gemini, OpenRouter, Custom)
3. Parse AI responses into `Lesson` objects
4. Update `LearningCurriculum.kt` to use AI service
5. Handle API errors gracefully

## Performance Considerations

- Lessons are loaded from memory (no disk I/O during browsing)
- SharedPreferences used for lightweight storage
- RecyclerView for efficient list rendering
- No network calls in current implementation (all offline)

## Contributing

1. Create a feature branch from `main`
2. Make your changes
3. Test thoroughly
4. Update documentation if needed
5. Create a pull request

## License

MIT License - See LICENSE file for details

## Support

For issues or questions:
- GitHub Issues: https://github.com/saurabhkumar1432/LearnEveryday/issues
- Email: (Add your support email)

---

**Last Updated**: November 2024
