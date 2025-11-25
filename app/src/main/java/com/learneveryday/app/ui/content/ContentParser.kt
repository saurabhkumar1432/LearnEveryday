package com.learneveryday.app.ui.content

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken

/**
 * Parses AI-generated structured content JSON into ContentBlock objects.
 * Also handles legacy markdown content by converting it to blocks.
 */
object ContentParser {
    
    private const val TAG = "ContentParser"
    private val gson = Gson()
    
    /**
     * Parse content string - tries structured JSON first, falls back to markdown conversion
     */
    fun parse(content: String): List<ContentBlock> {
        if (content.isBlank()) return emptyList()
        
        val trimmed = content.trim()
        
        // Try parsing as structured JSON first
        if (trimmed.startsWith("[") || trimmed.startsWith("{")) {
            try {
                val blocks = parseStructuredJson(trimmed)
                if (blocks.isNotEmpty()) {
                    Log.d(TAG, "Parsed ${blocks.size} structured content blocks")
                    return blocks
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to parse as structured JSON, falling back to markdown", e)
            }
        }
        
        // Fall back to markdown conversion
        return convertMarkdownToBlocks(content)
    }
    
    /**
     * Parse structured JSON content
     */
    private fun parseStructuredJson(json: String): List<ContentBlock> {
        val blocks = mutableListOf<ContentBlock>()
        
        try {
            val element = JsonParser.parseString(json)
            
            val jsonArray = when {
                element.isJsonArray -> element.asJsonArray
                element.isJsonObject -> {
                    val obj = element.asJsonObject
                    obj.getAsJsonArray("blocks") ?: obj.getAsJsonArray("content") ?: return emptyList()
                }
                else -> return emptyList()
            }
            
            for (item in jsonArray) {
                if (item.isJsonObject) {
                    parseBlock(item.asJsonObject)?.let { blocks.add(it) }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing structured JSON", e)
        }
        
        return blocks
    }
    
    /**
     * Parse a single block from JSON object
     */
    private fun parseBlock(obj: JsonObject): ContentBlock? {
        val type = obj.get("type")?.asString ?: return null
        
        return try {
            when (type) {
                "heading" -> HeadingBlock(
                    level = obj.get("level")?.asInt ?: 2,
                    text = obj.get("text")?.asString ?: ""
                )
                
                "text", "paragraph" -> TextBlock(
                    text = obj.get("text")?.asString ?: "",
                    style = parseTextStyle(obj.get("style")?.asString)
                )
                
                "list" -> {
                    val items = obj.getAsJsonArray("items")?.map { it.asString } ?: emptyList()
                    ListBlock(
                        items = items,
                        ordered = obj.get("ordered")?.asBoolean ?: false
                    )
                }
                
                "code" -> CodeBlock(
                    code = obj.get("code")?.asString ?: "",
                    language = obj.get("language")?.asString ?: "plaintext"
                )
                
                "table" -> {
                    val headers = obj.getAsJsonArray("headers")?.map { it.asString } ?: emptyList()
                    val rows = obj.getAsJsonArray("rows")?.map { row ->
                        row.asJsonArray.map { it.asString }
                    } ?: emptyList()
                    TableBlock(headers = headers, rows = rows)
                }
                
                "callout", "alert", "note" -> CalloutBlock(
                    title = obj.get("title")?.asString ?: "",
                    message = obj.get("message")?.asString ?: obj.get("text")?.asString ?: "",
                    style = parseCalloutStyle(obj.get("style")?.asString)
                )
                
                "flowchart", "flow", "diagram" -> {
                    val steps = obj.getAsJsonArray("steps")?.map { step ->
                        val stepObj = step.asJsonObject
                        FlowStep(
                            label = stepObj.get("label")?.asString ?: "",
                            description = stepObj.get("description")?.asString
                        )
                    } ?: emptyList()
                    FlowchartBlock(
                        title = obj.get("title")?.asString,
                        steps = steps
                    )
                }
                
                "definition", "definitions" -> {
                    val items = obj.getAsJsonArray("items")?.map { item ->
                        val itemObj = item.asJsonObject
                        DefinitionItem(
                            term = itemObj.get("term")?.asString ?: "",
                            definition = itemObj.get("definition")?.asString ?: ""
                        )
                    } ?: emptyList()
                    DefinitionBlock(
                        title = obj.get("title")?.asString,
                        items = items
                    )
                }
                
                "divider", "separator", "hr" -> DividerBlock(
                    style = parseDividerStyle(obj.get("style")?.asString)
                )
                
                "image" -> ImageBlock(
                    url = obj.get("url")?.asString ?: "",
                    caption = obj.get("caption")?.asString,
                    alt = obj.get("alt")?.asString
                )
                
                else -> {
                    Log.w(TAG, "Unknown block type: $type")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing block of type $type", e)
            null
        }
    }
    
    private fun parseTextStyle(style: String?): TextStyle {
        return when (style?.lowercase()) {
            "emphasis", "italic", "em" -> TextStyle.EMPHASIS
            "strong", "bold" -> TextStyle.STRONG
            "quote", "blockquote" -> TextStyle.QUOTE
            else -> TextStyle.NORMAL
        }
    }
    
    private fun parseCalloutStyle(style: String?): CalloutStyle {
        return when (style?.lowercase()) {
            "tip", "green" -> CalloutStyle.TIP
            "warning", "warn", "orange", "caution" -> CalloutStyle.WARNING
            "error", "danger", "red" -> CalloutStyle.ERROR
            "success", "done", "complete" -> CalloutStyle.SUCCESS
            else -> CalloutStyle.INFO
        }
    }
    
    private fun parseDividerStyle(style: String?): DividerStyle {
        return when (style?.lowercase()) {
            "dots", "dotted" -> DividerStyle.DOTS
            "space", "blank" -> DividerStyle.SPACE
            else -> DividerStyle.LINE
        }
    }
    
    /**
     * Convert legacy markdown content to structured blocks.
     * This handles existing lessons that use markdown format.
     */
    fun convertMarkdownToBlocks(markdown: String): List<ContentBlock> {
        val blocks = mutableListOf<ContentBlock>()
        val lines = markdown.lines()
        var i = 0
        
        while (i < lines.size) {
            val line = lines[i]
            val trimmed = line.trim()
            
            when {
                // Empty line - skip
                trimmed.isEmpty() -> {
                    i++
                }
                
                // Heading
                trimmed.startsWith("#") -> {
                    val level = trimmed.takeWhile { it == '#' }.length.coerceIn(1, 6)
                    val text = trimmed.dropWhile { it == '#' }.trim()
                    if (text.isNotEmpty()) {
                        blocks.add(HeadingBlock(level, cleanInlineFormatting(text)))
                    }
                    i++
                }
                
                // Code block
                trimmed.startsWith("```") -> {
                    val language = trimmed.removePrefix("```").trim().ifEmpty { "plaintext" }
                    val codeLines = mutableListOf<String>()
                    i++
                    while (i < lines.size && !lines[i].trim().startsWith("```")) {
                        codeLines.add(lines[i])
                        i++
                    }
                    blocks.add(CodeBlock(codeLines.joinToString("\n"), language))
                    i++ // Skip closing ```
                }
                
                // Table (starts with |)
                trimmed.startsWith("|") -> {
                    val tableLines = mutableListOf<String>()
                    while (i < lines.size && lines[i].trim().let { it.startsWith("|") || it.contains("|") }) {
                        val tableLine = lines[i].trim()
                        // Skip separator row
                        if (!tableLine.matches(Regex("^\\|?[\\s|:-]+\\|?$"))) {
                            tableLines.add(tableLine)
                        }
                        i++
                    }
                    if (tableLines.isNotEmpty()) {
                        val table = parseMarkdownTable(tableLines)
                        if (table != null) blocks.add(table)
                    }
                }
                
                // Blockquote / Callout
                trimmed.startsWith(">") -> {
                    val quoteLines = mutableListOf<String>()
                    while (i < lines.size && lines[i].trim().startsWith(">")) {
                        quoteLines.add(lines[i].trim().removePrefix(">").trim())
                        i++
                    }
                    val quoteText = quoteLines.joinToString(" ")
                    val callout = parseCalloutFromQuote(quoteText)
                    blocks.add(callout)
                }
                
                // Unordered list
                trimmed.matches(Regex("^[*\\-+]\\s+.*")) -> {
                    val items = mutableListOf<String>()
                    while (i < lines.size && lines[i].trim().matches(Regex("^[*\\-+]\\s+.*"))) {
                        val item = lines[i].trim().replaceFirst(Regex("^[*\\-+]\\s+"), "")
                        items.add(cleanInlineFormatting(item))
                        i++
                    }
                    blocks.add(ListBlock(items, ordered = false))
                }
                
                // Ordered list
                trimmed.matches(Regex("^\\d+\\.\\s+.*")) -> {
                    val items = mutableListOf<String>()
                    while (i < lines.size && lines[i].trim().matches(Regex("^\\d+\\.\\s+.*"))) {
                        val item = lines[i].trim().replaceFirst(Regex("^\\d+\\.\\s+"), "")
                        items.add(cleanInlineFormatting(item))
                        i++
                    }
                    blocks.add(ListBlock(items, ordered = true))
                }
                
                // Horizontal rule
                trimmed.matches(Regex("^[-*_]{3,}$")) -> {
                    blocks.add(DividerBlock())
                    i++
                }
                
                // Flowchart pattern (arrows)
                trimmed.contains("→") || trimmed.contains("-->") || trimmed.contains("->") -> {
                    val flowchart = parseFlowchartFromText(trimmed)
                    if (flowchart != null) {
                        blocks.add(flowchart)
                    } else {
                        blocks.add(TextBlock(cleanInlineFormatting(trimmed)))
                    }
                    i++
                }
                
                // Regular paragraph
                else -> {
                    val paragraphLines = mutableListOf<String>()
                    while (i < lines.size) {
                        val nextLine = lines[i].trim()
                        if (nextLine.isEmpty() || 
                            nextLine.startsWith("#") || 
                            nextLine.startsWith("```") ||
                            nextLine.startsWith("|") ||
                            nextLine.startsWith(">") ||
                            nextLine.matches(Regex("^[*\\-+]\\s+.*")) ||
                            nextLine.matches(Regex("^\\d+\\.\\s+.*"))) {
                            break
                        }
                        paragraphLines.add(nextLine)
                        i++
                    }
                    val text = paragraphLines.joinToString(" ")
                    if (text.isNotEmpty()) {
                        blocks.add(TextBlock(cleanInlineFormatting(text)))
                    }
                }
            }
        }
        
        Log.d(TAG, "Converted markdown to ${blocks.size} blocks")
        return blocks
    }
    
    /**
     * Parse markdown table lines into TableBlock
     */
    private fun parseMarkdownTable(lines: List<String>): TableBlock? {
        if (lines.isEmpty()) return null
        
        val parseRow = { line: String ->
            line.trim()
                .removePrefix("|")
                .removeSuffix("|")
                .split("|")
                .map { it.trim() }
        }
        
        val headers = parseRow(lines[0])
        val rows = lines.drop(1).map { parseRow(it) }
        
        return if (headers.isNotEmpty()) {
            TableBlock(headers, rows)
        } else null
    }
    
    /**
     * Parse blockquote into callout if it has a special format
     */
    private fun parseCalloutFromQuote(text: String): ContentBlock {
        // Check for patterns like "**Tip:** message" or "**Warning:** message"
        val calloutPattern = Regex("^\\*\\*([^*]+)\\*\\*:?\\s*(.*)", RegexOption.DOT_MATCHES_ALL)
        val match = calloutPattern.find(text)
        
        return if (match != null) {
            val title = match.groupValues[1].trim()
            val message = match.groupValues[2].trim()
            val style = when {
                title.contains("tip", ignoreCase = true) -> CalloutStyle.TIP
                title.contains("warning", ignoreCase = true) -> CalloutStyle.WARNING
                title.contains("caution", ignoreCase = true) -> CalloutStyle.WARNING
                title.contains("error", ignoreCase = true) -> CalloutStyle.ERROR
                title.contains("danger", ignoreCase = true) -> CalloutStyle.ERROR
                title.contains("note", ignoreCase = true) -> CalloutStyle.INFO
                title.contains("info", ignoreCase = true) -> CalloutStyle.INFO
                title.contains("success", ignoreCase = true) -> CalloutStyle.SUCCESS
                else -> CalloutStyle.INFO
            }
            CalloutBlock(title, cleanInlineFormatting(message), style)
        } else {
            TextBlock(cleanInlineFormatting(text), TextStyle.QUOTE)
        }
    }
    
    /**
     * Parse text-based flowchart into FlowchartBlock
     */
    private fun parseFlowchartFromText(text: String): FlowchartBlock? {
        // Split by arrow patterns
        val parts = text.split(Regex("\\s*(?:→|-->|->|=>)\\s*"))
            .map { it.trim().removePrefix("**").removeSuffix("**") }
            .filter { it.isNotEmpty() }
        
        if (parts.size < 2) return null
        
        // Check if it looks like a flow description vs regular text
        val avgWordCount = parts.map { it.split(" ").size }.average()
        if (avgWordCount > 4) return null // Probably regular text, not a flow
        
        val steps = parts.map { FlowStep(it) }
        return FlowchartBlock(null, steps)
    }
    
    /**
     * Clean inline markdown formatting and convert to plain text
     */
    private fun cleanInlineFormatting(text: String): String {
        return text
            .replace(Regex("\\*\\*([^*]+)\\*\\*"), "$1")  // Bold
            .replace(Regex("\\*([^*]+)\\*"), "$1")        // Italic
            .replace(Regex("__([^_]+)__"), "$1")          // Bold
            .replace(Regex("_([^_]+)_"), "$1")            // Italic
            .replace(Regex("`([^`]+)`"), "$1")            // Inline code
            .replace(Regex("\\[([^]]+)]\\([^)]+\\)"), "$1") // Links
            .trim()
    }
}
