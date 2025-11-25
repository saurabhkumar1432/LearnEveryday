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
            Difficulty.BEGINNER -> "Use simple language. Explain every concept thoroughly. Include basic examples. Define all technical terms."
            Difficulty.INTERMEDIATE -> "Use technical terms with brief explanations. Include practical examples. Show common patterns and real-world use cases."
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
You are an expert educator creating lesson content for a mobile learning app.

**Curriculum:** ${request.curriculumTitle}
**Lesson Title:** ${request.lessonTitle}
**Lesson Overview:** ${request.lessonDescription}
**Difficulty Level:** ${request.difficulty.name}
**Writing Style:** $difficultyGuide

$contextInfo

**Key Topics to Cover:**
${request.keyPoints.joinToString("\n") { "- $it" }}

---

## MARKDOWN FORMATTING (MOBILE-FRIENDLY - KEEP IT SIMPLE)

Use ONLY these simple markdown elements that render reliably on mobile:

### Headers
Use ## for main sections, ### for subsections. Always add a space after #.

### Text Formatting
- Use **bold** for important terms
- Use *italics* for emphasis
- Use `inline code` for technical terms

### Lists
Bullet lists:
- Item one
- Item two

Numbered lists:
1. First step
2. Second step

### Code Blocks
Use triple backticks with language:

```python
def example():
    return "Hello"
```

### Blockquotes for Tips/Notes
> **Tip:** This is helpful advice.

> **Note:** Important information here.

### Simple Tables (use sparingly)
| Header 1 | Header 2 |
| --- | --- |
| Data 1 | Data 2 |

---

## CONTENT STRUCTURE

1. **Introduction** (2-3 short paragraphs)
2. **Core Concepts** (explain with examples)
3. **Practical Examples** (1-2 code examples if technical)
4. **Key Takeaways** (bullet list summary)
5. **Practice** (simple exercise)

---

## THINGS TO AVOID (THESE BREAK ON MOBILE)
- NO Mermaid diagrams or flowcharts
- NO complex ASCII art or box drawings
- NO nested tables or complex table formatting
- NO emoji overuse
- NO HTML tags
- Keep tables simple (max 3-4 columns)

---

**JSON Response Format:**
```json
{
  "content": "Your markdown content here",
  "keyPoints": ["Key point 1", "Key point 2", "Key point 3"],
  "practiceExercise": "Simple exercise description",
  "prerequisites": ["Prior knowledge needed"],
  "nextSteps": ["What to learn next"]
}
```

**RULES:**
1. Return ONLY valid JSON - no markdown code fences around the response
2. Content field contains standard markdown (NOT escaped)
3. Content should be 800-1500 words (concise for mobile)
4. Focus on clarity over complexity
5. Make it engaging and practical

Generate the lesson now:
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
