package com.learneveryday.app.domain.service

import com.learneveryday.app.domain.model.Difficulty
import com.learneveryday.app.domain.model.GenerationMode

/**
 * AI prompt builder for generating curriculum and lessons
 */
object AIPromptBuilder {
    
    fun buildCurriculumOutlinePrompt(request: GenerationRequest): String {
        val difficultyGuide = when (request.difficulty) {
            Difficulty.BEGINNER -> "Assume no prior knowledge. Start with fundamentals and basic concepts. Use simple language and provide plenty of examples."
            Difficulty.INTERMEDIATE -> "Assume basic knowledge. Focus on practical applications and common patterns. Include some advanced concepts."
            Difficulty.ADVANCED -> "Assume solid foundation. Cover complex topics, edge cases, and best practices. Include architectural patterns."
            Difficulty.EXPERT -> "Assume expert knowledge. Focus on optimization, research, cutting-edge techniques, and deep technical details."
        }
        
        val modeInstructions = when (request.mode) {
            GenerationMode.QUICK_START -> "Generate an outline with lesson titles and brief descriptions only. No detailed content needed."
            GenerationMode.FULL_GENERATION -> "Generate complete lessons with full content, examples, and exercises for each lesson."
            GenerationMode.SMART_MODE -> "Generate detailed content for the first 3 lessons. For remaining lessons, provide outlines only."
        }
        
        return """
You are an expert curriculum designer creating a comprehensive learning path.

**Topic:** ${request.topic}
**Description:** ${request.description}
**Difficulty Level:** ${request.difficulty.name}
**Estimated Duration:** ${request.estimatedHours} hours
**Target Audience:** $difficultyGuide

$modeInstructions

**Requirements:**
1. Create ${request.maxLessons} or fewer lessons that progressively build knowledge
2. Each lesson should be 15-45 minutes long
3. Structure lessons in a logical learning sequence
4. Include 3-5 key learning points per lesson
5. Add relevant tags for categorization (maximum 5 tags)

**JSON Response Format (STRICTLY FOLLOW THIS STRUCTURE):**
```json
{
  "title": "Complete curriculum title",
  "description": "Detailed overview of what learners will achieve",
  "difficulty": "${request.difficulty.name}",
  "estimatedHours": ${request.estimatedHours},
  "lessons": [
    {
      "title": "Lesson title",
      "description": "What this lesson covers",
      "estimatedMinutes": 30,
      "keyPoints": [
        "Key concept 1",
        "Key concept 2",
        "Key concept 3"
      ]
    }
  ],
  "tags": ["tag1", "tag2", "tag3"]
}
```

**IMPORTANT:** 
- Return ONLY valid JSON, no markdown formatting or code blocks
- Ensure all JSON strings are properly escaped
- Include realistic time estimates
- Make lesson titles specific and actionable
- Ensure logical progression from basic to advanced concepts

Generate the curriculum now:
        """.trimIndent()
    }
    
    fun buildLessonContentPrompt(request: LessonGenerationRequest): String {
        val difficultyGuide = when (request.difficulty) {
            Difficulty.BEGINNER -> "Use simple language. Explain every concept. Include basic examples. Avoid jargon or explain it thoroughly."
            Difficulty.INTERMEDIATE -> "Use technical terms with brief explanations. Include practical examples. Show common patterns and use cases."
            Difficulty.ADVANCED -> "Use precise technical language. Show advanced patterns, edge cases, and optimizations. Include architectural considerations."
            Difficulty.EXPERT -> "Focus on deep technical details, performance implications, research findings, and cutting-edge techniques."
        }
        
        val contextInfo = if (request.previousLessonTitles.isNotEmpty()) {
            "**Previous Lessons Completed:**\n${request.previousLessonTitles.joinToString("\n") { "- $it" }}\n\n" +
            "Build upon concepts from previous lessons where appropriate."
        } else {
            "This is one of the first lessons. Introduce foundational concepts clearly."
        }
        
        return """
You are an expert educator creating detailed lesson content.

**Curriculum:** ${request.curriculumTitle}
**Lesson Title:** ${request.lessonTitle}
**Lesson Overview:** ${request.lessonDescription}
**Difficulty Level:** ${request.difficulty.name}
**Writing Style:** $difficultyGuide

$contextInfo

**Key Topics to Cover:**
${request.keyPoints.joinToString("\n") { "- $it" }}

**Content Requirements:**
1. Write in clear, engaging markdown format
2. Use proper markdown syntax (headers, lists, code blocks, emphasis)
3. Include practical examples with code when relevant
4. Add diagrams or visual descriptions where helpful (use markdown tables/ascii)
5. Provide a hands-on practice exercise at the end
6. Suggest prerequisites and next learning steps
7. Target 15-30 minutes of reading time (~1500-3000 words)

**JSON Response Format (STRICTLY FOLLOW THIS STRUCTURE):**
```json
{
  "content": "# ${request.lessonTitle}\n\n## Introduction\n\n[Detailed markdown content here with sections, examples, and explanations]\n\n## Key Concepts\n\n[Core concepts]\n\n## Examples\n\n[Practical examples]\n\n## Summary\n\n[Recap of main points]",
  "keyPoints": [
    "Refined key point 1",
    "Refined key point 2",
    "Refined key point 3"
  ],
  "practiceExercise": "**Exercise:** [Clear instructions for hands-on practice]\n\n**Goal:** [What they should achieve]\n\n**Hints:** [Helpful guidance]",
  "prerequisites": [
    "Concept or lesson needed before this"
  ],
  "nextSteps": [
    "Suggested next topic or skill to learn"
  ]
}
```

**IMPORTANT:**
- Return ONLY valid JSON, no markdown code blocks or formatting around JSON
- Escape all special characters in JSON strings properly
- Use \\n for line breaks in content
- Make content engaging, practical, and educational
- Include real-world applications and use cases

Generate the lesson content now:
        """.trimIndent()
    }
    
    fun buildChunkedLessonPrompt(
        curriculumTitle: String,
        lessonTitles: List<String>,
        difficulty: Difficulty,
        chunkSize: Int
    ): String {
        return """
You are generating multiple lesson outlines efficiently.

**Curriculum:** $curriculumTitle
**Difficulty:** ${difficulty.name}
**Generate outlines for these $chunkSize lessons:**

${lessonTitles.joinToString("\n") { "- $it" }}

**JSON Response Format:**
```json
{
  "lessons": [
    {
      "title": "Exact lesson title from list above",
      "description": "What this lesson covers (2-3 sentences)",
      "estimatedMinutes": 25,
      "keyPoints": [
        "Key concept 1",
        "Key concept 2", 
        "Key concept 3"
      ]
    }
  ]
}
```

Return ONLY valid JSON. Generate all $chunkSize lessons:
        """.trimIndent()
    }
}
