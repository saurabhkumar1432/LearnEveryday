# LearnEveryday 📚

An AI-powered Android learning app that creates personalized curriculums and lessons on any topic. Built with Clean Architecture and MVVM pattern for a robust, maintainable codebase.

![Android](https://img.shields.io/badge/Android-3DDC84?style=flat&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=flat&logo=kotlin&logoColor=white)
![API 24+](https://img.shields.io/badge/API-24%2B-brightgreen)
![License](https://img.shields.io/badge/License-MIT-blue.svg)

## ✨ Features

### 🎯 Core Features
- **AI-Powered Curriculum Generation** - Generate comprehensive learning curriculums on any topic using AI
- **Multiple AI Providers** - Support for Google Gemini, OpenAI, Anthropic Claude, and OpenRouter
- **Smart Lesson Generation** - Three generation modes: Quick Start, Full Generation, and Smart Mode
- **Markdown Lesson Content** - Rich, formatted lesson content with code highlighting, tables, and more
- **Progress Tracking** - Track your learning progress across all curriculums and lessons
- **Offline Support** - All content is stored locally with Room database for offline access

### 📱 User Experience
- **Material Design 3** - Modern, beautiful UI following Material Design guidelines
- **Dark Mode Support** - Automatic or manual dark/light theme switching
- **Topic Suggestions** - Curated and AI-generated topic suggestions to inspire learning
- **Full-Screen Reader** - Immersive reading experience for lessons
- **Learning Reminders** - Configurable notifications to maintain your learning habit
- **Export/Import** - Backup and restore your learning data

### 🔧 Technical Features
- **Background Generation** - WorkManager-based background content generation
- **Custom API Endpoints** - Support for custom AI API endpoints
- **Configurable AI Parameters** - Adjust temperature, max tokens, and model selection

## 🏗️ Architecture

LearnEveryday follows **Clean Architecture** principles with **MVVM** pattern:

```
app/
├── data/                    # Data layer
│   ├── local/              # Room database, DAOs, entities
│   ├── mapper/             # Entity ↔ Domain model mappers
│   ├── repository/         # Repository implementations
│   └── service/            # AI provider implementations
├── domain/                  # Domain layer
│   ├── model/              # Domain models (Curriculum, Lesson, etc.)
│   ├── repository/         # Repository interfaces
│   └── service/            # AI service interfaces and prompt builders
├── presentation/            # Presentation layer
│   ├── adapters/           # RecyclerView adapters
│   ├── detail/             # Curriculum detail screen
│   ├── generate/           # Curriculum generation screen
│   ├── home/               # Home screen with topic suggestions
│   ├── plans/              # Learning plans list screen
│   └── reader/             # Lesson reader screen
├── ui/                      # UI components
│   └── content/            # Custom content blocks for rendering
├── util/                    # Utility classes
├── utils/                   # Helper utilities
└── work/                    # WorkManager workers for background tasks
```

## 🛠️ Tech Stack

| Category | Technology |
|----------|------------|
| **Language** | Kotlin |
| **Min SDK** | 24 (Android 7.0) |
| **Target SDK** | 34 (Android 14) |
| **Architecture** | Clean Architecture + MVVM |
| **UI** | View Binding, Material Design 3 |
| **Database** | Room |
| **Async** | Kotlin Coroutines & Flow |
| **Background Tasks** | WorkManager |
| **Networking** | Retrofit + Gson |
| **Markdown** | Markwon |
| **DI** | Manual dependency injection |

## 📦 Dependencies

```kotlin
// Core Android
androidx.core:core-ktx
androidx.appcompat:appcompat
com.google.android.material:material
androidx.constraintlayout:constraintlayout

// Room Database
androidx.room:room-runtime
androidx.room:room-ktx

// Lifecycle & ViewModel
androidx.lifecycle:lifecycle-runtime-ktx
androidx.lifecycle:lifecycle-viewmodel-ktx
androidx.lifecycle:lifecycle-livedata-ktx

// Navigation
androidx.navigation:navigation-fragment-ktx
androidx.navigation:navigation-ui-ktx

// WorkManager
androidx.work:work-runtime-ktx

// Networking
com.squareup.retrofit2:retrofit
com.squareup.retrofit2:converter-gson

// Markdown Rendering
io.noties.markwon:core
io.noties.markwon:ext-tables
io.noties.markwon:ext-strikethrough
io.noties.markwon:ext-tasklist
```

## 🚀 Getting Started

### Prerequisites
- Android Studio Arctic Fox (2020.3.1) or later
- JDK 8 or higher
- Android SDK with API level 24+

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/yourusername/LearnEveryday.git
   cd LearnEveryday
   ```

2. **Open in Android Studio**
   - Open Android Studio
   - Select "Open an existing project"
   - Navigate to the cloned directory and select it

3. **Build the project**
   ```bash
   ./gradlew build
   ```

4. **Run on device/emulator**
   - Connect an Android device or start an emulator
   - Click the Run button in Android Studio or use:
   ```bash
   ./gradlew installDebug
   ```

### Configuration

1. **Get an API Key** from one of the supported providers:
   - [Google AI Studio](https://makersuite.google.com/app/apikey) (Gemini)
   - [OpenAI Platform](https://platform.openai.com/api-keys)
   - [Anthropic Console](https://console.anthropic.com/)
   - [OpenRouter](https://openrouter.ai/keys)

2. **Configure the app**
   - Open the app and go to Settings (gear icon)
   - Select your AI Provider
   - Enter your API Key
   - Choose a model (recommended defaults are provided)

## 📖 Usage

### Creating a Learning Plan

1. **From the Home Screen**
   - Tap the **+** floating action button
   - Enter a topic (e.g., "Python Programming", "Machine Learning Basics")
   - Optionally add a description and adjust settings
   - Choose a generation mode:
     - **Quick Start**: Generates lesson outlines only (fastest)
     - **Full Generation**: Generates complete content for all lessons
     - **Smart Mode**: Full content for first 3 lessons, outlines for rest (recommended)
   - Tap "Generate" and wait for AI to create your curriculum

2. **Using Suggested Topics**
   - Browse curated topic suggestions on the Home screen
   - Tap any topic to start generating a curriculum
   - Use the refresh button to get AI-generated topic suggestions

### Studying Lessons

1. Navigate to **Learning Plans** tab
2. Select a curriculum to view its lessons
3. Tap on a lesson to open the reader
4. Use the checkbox to mark lessons as complete
5. Navigate between lessons using the bottom navigation

### Managing Progress

- View your progress on the curriculum detail screen
- Completed lessons show a checkmark
- Resume reading from where you left off
- Share your curriculum with others

## 🔌 AI Providers

LearnEveryday supports multiple AI providers:

| Provider | Models | Notes |
|----------|--------|-------|
| **Google Gemini** | gemini-2.0-flash, gemini-1.5-pro, gemini-2.5-pro | Recommended for best results |
| **OpenRouter** | Various (including free models) | Access to multiple providers with one API key |
| **OpenAI** | gpt-4o, gpt-4o-mini, gpt-4-turbo | High-quality responses |
| **Anthropic** | claude-sonnet-4.5, claude-opus-4.5 | Excellent for educational content |
| **Custom** | Any OpenAI-compatible API | For self-hosted or other providers |

## 🗂️ Data Model

### Curriculum
```kotlin
data class Curriculum(
    val id: String,
    val title: String,
    val description: String,
    val difficulty: Difficulty,       // BEGINNER, INTERMEDIATE, ADVANCED, EXPERT
    val estimatedHours: Int,
    val provider: String,
    val modelUsed: String,
    val tags: List<String>,
    val totalLessons: Int,
    val completedLessons: Int,
    val generationMode: GenerationMode,
    val generationStatus: GenerationStatus,
    // ... more fields
)
```

### Lesson
```kotlin
data class Lesson(
    val id: String,
    val curriculumId: String,
    val orderIndex: Int,
    val title: String,
    val description: String,
    val content: String,              // Markdown content
    val difficulty: Difficulty,
    val estimatedMinutes: Int,
    val keyPoints: List<String>,
    val practiceExercise: String?,
    val isCompleted: Boolean,
    // ... more fields
)
```

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

### Development Guidelines

- Follow Kotlin coding conventions
- Use meaningful commit messages
- Add comments for complex logic
- Write unit tests for new features
- Ensure the app builds successfully before submitting

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- [Markwon](https://github.com/noties/Markwon) for excellent markdown rendering
- [Material Components](https://material.io/develop/android) for beautiful UI components
- All AI providers for enabling intelligent content generation

## 📬 Contact

For questions, suggestions, or issues:
- Open an issue on GitHub
- Submit a pull request with improvements

---

**Made with ❤️ for lifelong learners**
