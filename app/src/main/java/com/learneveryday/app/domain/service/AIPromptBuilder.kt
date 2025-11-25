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
You are an expert educator creating detailed, well-formatted lesson content for a mobile learning app.

**Curriculum:** ${request.curriculumTitle}
**Lesson Title:** ${request.lessonTitle}
**Lesson Overview:** ${request.lessonDescription}
**Difficulty Level:** ${request.difficulty.name}
**Writing Style:** $difficultyGuide

$contextInfo

**Key Topics to Cover:**
${request.keyPoints.joinToString("\n") { "- $it" }}

---

## CRITICAL FORMATTING REQUIREMENTS

Your content MUST be properly formatted markdown that renders beautifully on mobile devices. Follow these rules strictly:

### 1. STRUCTURE
- Start with a brief introduction (2-3 paragraphs)
- Use `## ` for main sections (always with space after ##)
- Use `### ` for subsections
- Use `#### ` for minor headings
- Add blank lines between sections

### 2. TEXT FORMATTING
- Use **bold** for important terms and concepts
- Use *italics* for emphasis or introducing new terms
- Use `inline code` for technical terms, commands, file names
- Use > blockquotes for tips, notes, or important callouts

### 3. LISTS
- Use bullet points for unordered information:
  - Item one
  - Item two
- Use numbered lists for sequential steps:
  1. First step
  2. Second step

### 4. CODE BLOCKS
For any code, use triple backticks with language identifier:

```python
def example():
    return "Hello World"
```

### 5. TABLES (IMPORTANT - Follow exact format)
Tables MUST follow this exact GFM format for proper mobile rendering:

| Column 1 | Column 2 | Column 3 |
| --- | --- | --- |
| Data 1 | Data 2 | Data 3 |
| Data 4 | Data 5 | Data 6 |

CRITICAL table rules:
- Always include the separator row (| --- | --- |) immediately after header
- Use spaces around content within cells
- Keep table content concise (tables will scroll horizontally on mobile)
- Don't use complex formatting inside table cells

### 6. FLOWCHARTS AND DIAGRAMS
For process flows, use Mermaid diagram syntax:

```mermaid
flowchart LR
    A[Start] --> B[Process]
    B --> C[Decision]
    C -->|Yes| D[Action 1]
    C -->|No| E[Action 2]
    D --> F[End]
    E --> F
```

For simple linear flows, you can also use arrow notation:
**Process Flow:** Input → Processing → Validation → Output

### 7. CALLOUT BOXES (using blockquotes)
> **💡 Pro Tip:** Important advice here

> **⚠️ Warning:** Caution about common mistakes

> **📝 Note:** Additional context or information

### 8. COMPARISONS
Use tables for comparing options:

| Feature | Option A | Option B |
| --- | --- | --- |
| Speed | Fast | Slow |
| Cost | Low | High |

---

## CONTENT STRUCTURE TO FOLLOW

1. **Introduction** (2-3 paragraphs explaining what and why)
2. **Core Concepts** (main teaching content with examples)
3. **Practical Examples** (2-3 real-world code examples or scenarios)
4. **Common Mistakes** (what to avoid)
5. **Summary** (bullet point recap)
6. **Practice Exercise** (hands-on task)

---

**JSON Response Format:**
```json
{
  "content": "Your full markdown lesson content here",
  "keyPoints": [
    "Key takeaway 1",
    "Key takeaway 2",
    "Key takeaway 3",
    "Key takeaway 4"
  ],
  "practiceExercise": "Exercise description with objective and instructions",
  "prerequisites": [
    "Required prior knowledge"
  ],
  "nextSteps": [
    "Suggested follow-up topic"
  ]
}
```

**CRITICAL RULES:**
1. Return ONLY valid JSON - no markdown code fences around the JSON response
2. The "content" field should contain properly formatted markdown
3. Use REAL newlines in JSON strings (not \\n escape sequences) - JSON parsers handle this
4. Content should be 1500-2500 words
5. Include AT LEAST 2 code examples (if topic is technical)
6. Include AT LEAST 1 properly formatted table with header separator row (| --- |)
7. Make content engaging with real-world applications
8. Every section must have proper markdown formatting
9. Do NOT use complex ASCII art or box drawings - they render poorly on mobile

Generate the complete, well-formatted lesson now:
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
