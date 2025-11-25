package com.learneveryday.app.util

import android.util.Log

/**
 * Post-processes AI-generated markdown content to fix common formatting issues
 * and prepare content for rich rendering (WebView or Markwon)
 */
object MarkdownProcessor {
    
    private const val TAG = "MarkdownProcessor"
    
    /**
     * Cleans and fixes common markdown formatting issues from AI-generated content
     */
    fun process(content: String): String {
        if (content.isBlank()) return content
        
        Log.d(TAG, "Processing content of length: ${content.length}")
        Log.d(TAG, "First 200 chars raw: ${content.take(200)}")
        
        // Check for common escape sequences in raw form
        val hasLiteralEscapes = content.contains("\\n") || content.contains("\\t") || 
                                content.contains("\\\"") || content.contains("\\*")
        Log.d(TAG, "Content has literal escape sequences: $hasLiteralEscapes")
        
        var processed = content
        
        // === STEP 1: Fix escape sequences (most critical) ===
        // These handle cases where JSON escaping wasn't fully resolved
        // The string "\\n" (backslash + n as separate chars) needs to become actual newline
        
        // First pass: handle double-escaped (\\\\n -> \\n -> \n)
        processed = processed
            .replace("\\\\n", "\n")    // Double-escaped newline
            .replace("\\\\t", "\t")    // Double-escaped tab
            .replace("\\\\r", "\r")    // Double-escaped carriage return
            .replace("\\\\\\\"", "\"") // Triple-escaped quote
            .replace("\\\\\"", "\"")   // Double-escaped quote
        
        // Second pass: handle single-escaped (literal backslash + char)
        // Use regex to be more precise about what we're matching
        processed = processed
            .replace(Regex("""\\n"""), "\n")    // Literal \n -> newline
            .replace(Regex("""\\t"""), "\t")    // Literal \t -> tab
            .replace(Regex("""\\r"""), "\r")    // Literal \r -> CR
            .replace(Regex("""\\""""), "\"")    // Literal \" -> quote
            .replace(Regex("""\\\'"""), "'")    // Literal \' -> apostrophe
        
        // Handle remaining double backslashes
        processed = processed.replace("\\\\", "\\")
        
        // Handle Unicode escapes that might have been left as literals
        processed = processed.replace(Regex("""\\u([0-9a-fA-F]{4})""")) { match ->
            val codePoint = match.groupValues[1].toInt(16)
            codePoint.toChar().toString()
        }
        
        // === STEP 2: Fix markdown asterisks for bold/italic ===
        
        // Fix escaped asterisks: \* should be * (for bold/italic)
        // But only when part of markdown formatting, not math
        processed = processed.replace("\\*\\*", "**")  // Bold
        processed = processed.replace("\\*", "*")       // Italic or bullet
        
        // Fix escaped underscores for italic/bold
        processed = processed.replace("\\_\\_", "__")  // Bold
        processed = processed.replace("\\_", "_")       // Italic
        
        // Fix escaped backticks
        processed = processed.replace("\\`\\`\\`", "```")  // Code blocks
        processed = processed.replace("\\`", "`")           // Inline code
        
        // Fix escaped pipes for tables
        processed = processed.replace("\\|", "|")
        
        // Fix escaped brackets for links
        processed = processed.replace("\\[", "[")
        processed = processed.replace("\\]", "]")
        processed = processed.replace("\\(", "(")
        processed = processed.replace("\\)", ")")
        
        // Fix escaped hash for headers
        processed = processed.replace("\\#", "#")
        
        // === STEP 3: Fix structural markdown issues ===
        
        // Ensure headers have space after # symbols
        processed = processed.replace(Regex("^(#{1,6})([^#\\s\\n])"), "$1 $2")
        processed = processed.replace(Regex("\\n(#{1,6})([^#\\s\\n])"), "\n$1 $2")
        
        // Ensure blank lines before headers for proper rendering
        processed = processed.replace(Regex("([^\\n])\\n(#{1,6} )"), "$1\n\n$2")
        
        // Fix bullet points without proper spacing
        processed = processed.replace(Regex("^([*\\-+])([^\\s*\\-+])"), "$1 $2")
        processed = processed.replace(Regex("\\n([*\\-+])([^\\s*\\-+])"), "\n$1 $2")
        
        // Fix numbered lists without proper spacing
        processed = processed.replace(Regex("^(\\d+\\.)([^\\s])"), "$1 $2")
        processed = processed.replace(Regex("\\n(\\d+\\.)([^\\s])"), "\n$1 $2")
        
        // === STEP 4: Fix code blocks ===
        
        // Ensure code blocks have proper newlines
        processed = processed.replace(Regex("```(\\w*)([^\\n])"), "```$1\n$2")
        processed = processed.replace(Regex("([^\\n])```"), "$1\n```")
        
        // === STEP 5: Fix table formatting ===
        processed = fixTables(processed)
        
        // === STEP 6: Convert text flowcharts to Mermaid (for WebView rendering) ===
        processed = convertFlowchartsToMermaid(processed)
        
        // === STEP 7: Fix blockquotes ===
        
        // Ensure blockquotes have space after >
        processed = processed.replace(Regex("^>([^>\\s\\n])"), "> $1")
        processed = processed.replace(Regex("\\n>([^>\\s\\n])"), "\n> $1")
        
        // === STEP 8: Clean up whitespace issues ===
        
        // Fix multiple consecutive blank lines (max 2)
        processed = processed.replace(Regex("\\n{4,}"), "\n\n\n")
        
        // Ensure content doesn't start with blank lines
        processed = processed.trimStart('\n', ' ')
        
        // Remove trailing whitespace from lines (except in code blocks)
        val finalLines = mutableListOf<String>()
        var inCodeBlock = false
        for (line in processed.lines()) {
            if (line.trim().startsWith("```")) {
                inCodeBlock = !inCodeBlock
                finalLines.add(line)
            } else if (inCodeBlock) {
                finalLines.add(line)  // Keep code block content as-is
            } else {
                finalLines.add(line.trimEnd())
            }
        }
        processed = finalLines.joinToString("\n")
        
        Log.d(TAG, "After processing, first 200 chars: ${processed.take(200)}")
        
        return processed
    }
    
    /**
     * Extracts plain text summary from markdown (for previews)
     */
    fun extractPlainText(markdown: String, maxLength: Int = 200): String {
        var text = process(markdown)  // First process to fix escapes
        
        // Remove code blocks
        text = text.replace(Regex("```[\\s\\S]*?```"), " ")
        
        // Remove inline code
        text = text.replace(Regex("`[^`]+`"), " ")
        
        // Remove headers markers
        text = text.replace(Regex("^#{1,6}\\s*", RegexOption.MULTILINE), "")
        
        // Remove bold/italic markers
        text = text.replace(Regex("\\*\\*([^*]+)\\*\\*"), "$1")
        text = text.replace(Regex("\\*([^*]+)\\*"), "$1")
        text = text.replace(Regex("__([^_]+)__"), "$1")
        text = text.replace(Regex("_([^_]+)_"), "$1")
        
        // Remove links, keep text
        text = text.replace(Regex("\\[([^]]+)]\\([^)]+\\)"), "$1")
        
        // Remove blockquote markers
        text = text.replace(Regex("^>\\s*", RegexOption.MULTILINE), "")
        
        // Remove list markers
        text = text.replace(Regex("^[\\-*+]\\s+", RegexOption.MULTILINE), "")
        text = text.replace(Regex("^\\d+\\.\\s+", RegexOption.MULTILINE), "")
        
        // Remove table formatting
        text = text.replace(Regex("\\|"), " ")
        text = text.replace(Regex("-{3,}"), " ")
        
        // Collapse whitespace
        text = text.replace(Regex("\\s+"), " ").trim()
        
        return if (text.length > maxLength) {
            text.take(maxLength - 3) + "..."
        } else {
            text
        }
    }
    
    /**
     * Fixes table formatting to ensure proper GFM table rendering.
     * GFM tables require:
     * 1. Header row with | separators
     * 2. Separator row with |---|---| pattern
     * 3. Data rows with | separators
     */
    private fun fixTables(content: String): String {
        val lines = content.lines().toMutableList()
        var inCodeBlock = false
        val result = mutableListOf<String>()
        var i = 0
        
        while (i < lines.size) {
            val line = lines[i]
            
            // Track code blocks to avoid modifying table-like content inside them
            if (line.trim().startsWith("```")) {
                inCodeBlock = !inCodeBlock
                result.add(line)
                i++
                continue
            }
            
            if (inCodeBlock) {
                result.add(line)
                i++
                continue
            }
            
            // Check if this line looks like a table row
            val trimmedLine = line.trim()
            if (trimmedLine.contains("|") && trimmedLine.count { it == '|' } >= 2) {
                // This might be a table - collect consecutive table-like lines
                val tableLines = mutableListOf<String>()
                var j = i
                while (j < lines.size) {
                    val tableLine = lines[j].trim()
                    if (tableLine.contains("|") && tableLine.count { it == '|' } >= 2) {
                        tableLines.add(tableLine)
                        j++
                    } else if (tableLine.isEmpty() && tableLines.isNotEmpty()) {
                        // Allow one empty line within table detection
                        break
                    } else {
                        break
                    }
                }
                
                if (tableLines.size >= 2) {
                    // We have a potential table
                    val fixedTable = fixTableStructure(tableLines)
                    result.addAll(fixedTable)
                    i = j
                    continue
                }
            }
            
            result.add(line)
            i++
        }
        
        return result.joinToString("\n")
    }
    
    /**
     * Ensures a table has proper structure with header separator row
     */
    private fun fixTableStructure(tableLines: List<String>): List<String> {
        if (tableLines.isEmpty()) return tableLines
        
        val result = mutableListOf<String>()
        
        // Fix spacing in each line
        val fixedLines = tableLines.map { line ->
            var fixed = line.trim()
            
            // Ensure line starts and ends with |
            if (!fixed.startsWith("|")) fixed = "| $fixed"
            if (!fixed.endsWith("|")) fixed = "$fixed |"
            
            // Add spaces around pipe separators for readability
            fixed = fixed.replace(Regex("\\|([^|\\s-])"), "| $1")
            fixed = fixed.replace(Regex("([^|\\s-])\\|"), "$1 |")
            
            fixed
        }
        
        // Check if second line is a separator row (contains only |, -, :, spaces)
        val hasSeparator = fixedLines.size >= 2 && 
            fixedLines[1].replace(Regex("[|:\\-\\s]"), "").isEmpty()
        
        if (hasSeparator) {
            // Table already has separator, just return fixed lines
            return fixedLines
        }
        
        // Need to add separator row after header
        if (fixedLines.isNotEmpty()) {
            val headerLine = fixedLines[0]
            val columnCount = headerLine.count { it == '|' } - 1
            
            // Create separator row
            val separator = "|" + " --- |".repeat(columnCount.coerceAtLeast(1))
            
            result.add(fixedLines[0]) // Header
            result.add(separator)      // Separator
            result.addAll(fixedLines.drop(1)) // Data rows
        }
        
        return result
    }
    
    /**
     * Converts text-based flowcharts to Mermaid syntax for WebView rendering
     * Detects patterns like:
     * - Input → Processing → Output
     * - Step 1 --> Step 2 --> Step 3
     */
    private fun convertFlowchartsToMermaid(content: String): String {
        val lines = content.lines().toMutableList()
        val result = mutableListOf<String>()
        var inCodeBlock = false
        
        for (line in lines) {
            // Track code blocks
            if (line.trim().startsWith("```")) {
                inCodeBlock = !inCodeBlock
                result.add(line)
                continue
            }
            
            if (inCodeBlock) {
                result.add(line)
                continue
            }
            
            // Check for flowchart patterns (arrow-based flows)
            val trimmed = line.trim()
            if (isFlowchartLine(trimmed)) {
                // Convert to Mermaid flowchart
                val mermaidDiagram = convertToMermaid(trimmed)
                result.add("```mermaid")
                result.add(mermaidDiagram)
                result.add("```")
            } else {
                result.add(line)
            }
        }
        
        return result.joinToString("\n")
    }
    
    /**
     * Detects if a line is a text-based flowchart
     */
    private fun isFlowchartLine(line: String): Boolean {
        // Must have multiple arrow-like separators and be standalone
        val arrowPatterns = listOf("→", "-->", "->", "=>", "⟶")
        val arrowCount = arrowPatterns.sumOf { arrow -> 
            line.split(arrow).size - 1 
        }
        
        // Only convert if it looks like a flow (2+ arrows, not in a sentence context)
        return arrowCount >= 2 && 
               !line.startsWith("-") && // Not a list item
               !line.startsWith("*") && // Not a list item
               !line.contains("http") && // Not a URL
               line.split(Regex("[→>=-]+")).all { it.trim().length < 30 } // Short segments
    }
    
    /**
     * Converts a text flowchart line to Mermaid syntax
     */
    private fun convertToMermaid(line: String): String {
        // Split by arrow patterns
        val parts = line.split(Regex("\\s*(?:→|-->|->|=>|⟶)\\s*"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        
        if (parts.size < 2) return "flowchart LR\n    A[$line]"
        
        val nodes = StringBuilder("flowchart LR\n")
        
        parts.forEachIndexed { index, part ->
            val nodeId = ('A'.code + index).toChar()
            val cleanPart = part.replace("**", "").trim() // Remove bold markers
            
            if (index < parts.size - 1) {
                val nextId = ('A'.code + index + 1).toChar()
                nodes.append("    $nodeId[$cleanPart] --> $nextId\n")
            } else {
                // Last node - just define it
                nodes.append("    $nodeId[$cleanPart]\n")
            }
        }
        
        return nodes.toString().trimEnd()
    }
    
    /**
     * Checks if content has rich elements that benefit from WebView rendering
     */
    fun hasRichContent(content: String): Boolean {
        return content.contains("|") || // tables
               content.contains("```") || // code blocks
               content.contains("mermaid") || // diagrams
               content.contains("→") || // flowcharts
               content.contains("-->") || // flowcharts
               content.contains("flowchart") // explicit mermaid
    }
}
