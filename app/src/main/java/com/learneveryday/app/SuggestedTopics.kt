package com.learneveryday.app

object SuggestedTopics {
    
    data class TopicSuggestion(
        val id: String,
        val title: String,
        val description: String,
        val icon: String,
        val category: String,
        val popularityScore: Int,
        val tags: List<String>
    )
    
    fun getAllSuggestions(): List<TopicSuggestion> {
        return listOf(
            // Programming Languages
            TopicSuggestion(
                "python",
                "Python Programming",
                "Learn Python from basics to advanced concepts. Perfect for beginners and data science enthusiasts.",
                "🐍",
                "Programming",
                10,
                listOf("Programming", "Beginner-Friendly", "Data Science", "Popular")
            ),
            TopicSuggestion(
                "javascript",
                "JavaScript & Web Development",
                "Master JavaScript for modern web development, from DOM manipulation to frameworks.",
                "🌐",
                "Programming",
                10,
                listOf("Programming", "Web Dev", "Frontend", "Essential")
            ),
            TopicSuggestion(
                "kotlin",
                "Kotlin for Android",
                "Build Android apps with Kotlin, Google's preferred language for Android development.",
                "📱",
                "Programming",
                8,
                listOf("Programming", "Mobile", "Android", "Modern")
            ),
            TopicSuggestion(
                "java",
                "Java Programming",
                "Learn Java fundamentals and object-oriented programming. Great for enterprise development.",
                "☕",
                "Programming",
                9,
                listOf("Programming", "Enterprise", "OOP", "Classic")
            ),
            TopicSuggestion(
                "typescript",
                "TypeScript",
                "Add type safety to JavaScript and build robust applications.",
                "📘",
                "Programming",
                8,
                listOf("Programming", "Web Dev", "Type Safety", "Modern")
            ),
            TopicSuggestion(
                "rust",
                "Rust Programming",
                "Learn systems programming with memory safety and high performance.",
                "🦀",
                "Programming",
                7,
                listOf("Programming", "Systems", "Performance", "Advanced")
            ),
            TopicSuggestion(
                "go",
                "Go (Golang)",
                "Master Go for building scalable backend services and cloud applications.",
                "🔷",
                "Programming",
                8,
                listOf("Programming", "Backend", "Cloud", "Concurrent")
            ),
            
            // Web Development
            TopicSuggestion(
                "react",
                "React.js",
                "Build modern user interfaces with React, the most popular frontend library.",
                "⚛️",
                "Web Development",
                10,
                listOf("Web Dev", "Frontend", "UI", "Popular")
            ),
            TopicSuggestion(
                "nodejs",
                "Node.js & Express",
                "Create scalable backend applications with JavaScript.",
                "🟢",
                "Web Development",
                9,
                listOf("Web Dev", "Backend", "API", "JavaScript")
            ),
            TopicSuggestion(
                "nextjs",
                "Next.js",
                "Build full-stack React applications with server-side rendering.",
                "▲",
                "Web Development",
                9,
                listOf("Web Dev", "Full Stack", "React", "Modern")
            ),
            TopicSuggestion(
                "vue",
                "Vue.js",
                "Learn the progressive JavaScript framework for building UIs.",
                "💚",
                "Web Development",
                8,
                listOf("Web Dev", "Frontend", "Progressive", "Easy")
            ),
            
            // Data Science & AI
            TopicSuggestion(
                "data_science",
                "Data Science with Python",
                "Analyze data, create visualizations, and build predictive models.",
                "📊",
                "Data Science",
                9,
                listOf("Data Science", "Python", "Analytics", "ML")
            ),
            TopicSuggestion(
                "machine_learning",
                "Machine Learning",
                "Build intelligent systems that learn from data.",
                "🤖",
                "Data Science",
                10,
                listOf("AI", "ML", "Python", "Advanced")
            ),
            TopicSuggestion(
                "deep_learning",
                "Deep Learning & Neural Networks",
                "Master neural networks and create AI models.",
                "🧠",
                "Data Science",
                8,
                listOf("AI", "Deep Learning", "Neural Networks", "Advanced")
            ),
            TopicSuggestion(
                "nlp",
                "Natural Language Processing",
                "Build applications that understand and generate human language.",
                "💬",
                "Data Science",
                8,
                listOf("AI", "NLP", "Text Processing", "Advanced")
            ),
            
            // Mobile Development
            TopicSuggestion(
                "flutter",
                "Flutter",
                "Create beautiful cross-platform mobile apps with a single codebase.",
                "🎨",
                "Mobile Development",
                9,
                listOf("Mobile", "Cross-platform", "UI", "Dart")
            ),
            TopicSuggestion(
                "react_native",
                "React Native",
                "Build native mobile apps using React and JavaScript.",
                "📱",
                "Mobile Development",
                8,
                listOf("Mobile", "Cross-platform", "React", "JavaScript")
            ),
            TopicSuggestion(
                "ios_swift",
                "iOS Development with Swift",
                "Create native iOS apps with Apple's Swift language.",
                "🍎",
                "Mobile Development",
                8,
                listOf("Mobile", "iOS", "Swift", "Apple")
            ),
            
            // DevOps & Cloud
            TopicSuggestion(
                "docker",
                "Docker & Containerization",
                "Package and deploy applications in containers.",
                "🐳",
                "DevOps",
                9,
                listOf("DevOps", "Containers", "Deployment", "Cloud")
            ),
            TopicSuggestion(
                "kubernetes",
                "Kubernetes",
                "Orchestrate containerized applications at scale.",
                "☸️",
                "DevOps",
                8,
                listOf("DevOps", "Orchestration", "Cloud", "Advanced")
            ),
            TopicSuggestion(
                "aws",
                "Amazon Web Services (AWS)",
                "Master cloud computing with AWS services.",
                "☁️",
                "Cloud",
                9,
                listOf("Cloud", "AWS", "Infrastructure", "Popular")
            ),
            TopicSuggestion(
                "cicd",
                "CI/CD & GitHub Actions",
                "Automate testing, building, and deployment pipelines.",
                "🔄",
                "DevOps",
                8,
                listOf("DevOps", "Automation", "CI/CD", "GitHub")
            ),
            
            // Database
            TopicSuggestion(
                "sql",
                "SQL & Database Design",
                "Master relational databases and write efficient queries.",
                "🗄️",
                "Database",
                9,
                listOf("Database", "SQL", "Data", "Essential")
            ),
            TopicSuggestion(
                "mongodb",
                "MongoDB",
                "Work with NoSQL databases for flexible data storage.",
                "🍃",
                "Database",
                8,
                listOf("Database", "NoSQL", "Document DB", "Modern")
            ),
            TopicSuggestion(
                "postgresql",
                "PostgreSQL",
                "Learn the powerful open-source relational database.",
                "🐘",
                "Database",
                8,
                listOf("Database", "SQL", "Relational", "Advanced")
            ),
            
            // Other Topics
            TopicSuggestion(
                "git",
                "Git & Version Control",
                "Master version control for collaborative development.",
                "📚",
                "Tools",
                10,
                listOf("Tools", "Version Control", "Essential", "Collaboration")
            ),
            TopicSuggestion(
                "algorithms",
                "Data Structures & Algorithms",
                "Master fundamental CS concepts for coding interviews.",
                "🔢",
                "Computer Science",
                9,
                listOf("CS", "Algorithms", "Interview", "Fundamentals")
            ),
            TopicSuggestion(
                "cybersecurity",
                "Cybersecurity Fundamentals",
                "Learn to protect systems and data from threats.",
                "🔒",
                "Security",
                8,
                listOf("Security", "Ethical Hacking", "Protection", "Important")
            ),
            TopicSuggestion(
                "blockchain",
                "Blockchain & Web3",
                "Understand blockchain technology and decentralized applications.",
                "⛓️",
                "Emerging Tech",
                7,
                listOf("Blockchain", "Web3", "Crypto", "Emerging")
            ),
            TopicSuggestion(
                "ui_ux",
                "UI/UX Design",
                "Create beautiful and user-friendly interfaces.",
                "🎨",
                "Design",
                8,
                listOf("Design", "UI", "UX", "User Experience")
            ),
            TopicSuggestion(
                "game_dev",
                "Game Development",
                "Build games from simple 2D to complex 3D experiences.",
                "🎮",
                "Game Development",
                7,
                listOf("Games", "Unity", "Entertainment", "Creative")
            )
        )
    }
    
    fun getByCategory(category: String): List<TopicSuggestion> {
        return getAllSuggestions().filter { it.category == category }
    }
    
    fun getPopular(limit: Int = 10): List<TopicSuggestion> {
        return getAllSuggestions().sortedByDescending { it.popularityScore }.take(limit)
    }
    
    fun searchTopics(query: String): List<TopicSuggestion> {
        val lowerQuery = query.lowercase()
        return getAllSuggestions().filter {
            it.title.lowercase().contains(lowerQuery) ||
            it.description.lowercase().contains(lowerQuery) ||
            it.tags.any { tag -> tag.lowercase().contains(lowerQuery) }
        }
    }
    
    fun getCategories(): List<String> {
        return getAllSuggestions().map { it.category }.distinct().sorted()
    }
}
