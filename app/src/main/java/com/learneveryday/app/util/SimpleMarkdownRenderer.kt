package com.learneveryday.app.util

import android.content.Context
import android.text.method.LinkMovementMethod
import android.util.Log
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.learneveryday.app.R
import io.noties.markwon.Markwon
import io.noties.markwon.core.MarkwonTheme
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.ext.tasklist.TaskListPlugin
import io.noties.markwon.html.HtmlPlugin
import io.noties.markwon.linkify.LinkifyPlugin
import io.noties.markwon.AbstractMarkwonPlugin

/**
 * Simple, reliable markdown renderer using Markwon.
 * This replaces the complex custom ContentParser with a battle-tested library
 * that handles all markdown formatting correctly on mobile.
 */
object SimpleMarkdownRenderer {
    
    private const val TAG = "SimpleMarkdownRenderer"
    
    @Volatile
    private var markwonInstance: Markwon? = null
    
    /**
     * Get or create the singleton Markwon instance
     */
    fun getInstance(context: Context): Markwon {
        return markwonInstance ?: synchronized(this) {
            markwonInstance ?: createMarkwon(context.applicationContext).also {
                markwonInstance = it
            }
        }
    }
    
    /**
     * Create a configured Markwon instance with all necessary plugins
     */
    private fun createMarkwon(context: Context): Markwon {
        val primaryColor = ContextCompat.getColor(context, R.color.primary)
        val surfaceVariantColor = ContextCompat.getColor(context, R.color.surface_variant)
        val textPrimaryColor = ContextCompat.getColor(context, R.color.text_primary)
        
        return Markwon.builder(context)
            // Theme configuration
            .usePlugin(object : AbstractMarkwonPlugin() {
                override fun configureTheme(builder: MarkwonTheme.Builder) {
                    builder
                        // Links
                        .linkColor(primaryColor)
                        // Code styling
                        .codeBackgroundColor(surfaceVariantColor)
                        .codeTextColor(textPrimaryColor)
                        .codeTextSize(14)
                        // Code block styling
                        .codeBlockBackgroundColor(surfaceVariantColor)
                        .codeBlockTextColor(textPrimaryColor)
                        .codeBlockTextSize(13)
                        // Quote styling
                        .blockQuoteColor(primaryColor)
                        .blockQuoteWidth(4)
                        // Headings
                        .headingBreakHeight(0)
                        // Lists
                        .bulletListItemStrokeWidth(2)
                        .bulletWidth(8)
                        // Horizontal rule
                        .thematicBreakColor(surfaceVariantColor)
                        .thematicBreakHeight(2)
                }
            })
            // Table support
            .usePlugin(TablePlugin.create { builder ->
                builder
                    .tableBorderColor(surfaceVariantColor)
                    .tableHeaderRowBackgroundColor(surfaceVariantColor)
                    .tableCellPadding(16)
                    .tableBorderWidth(1)
            })
            // Strikethrough support
            .usePlugin(StrikethroughPlugin.create())
            // Task list support (checkboxes)
            .usePlugin(TaskListPlugin.create(context))
            // HTML support for any HTML in markdown
            .usePlugin(HtmlPlugin.create())
            // Auto-linkify URLs, emails, phones
            .usePlugin(LinkifyPlugin.create())
            .build()
    }
    
    /**
     * Render markdown content to a TextView
     */
    fun render(textView: TextView, content: String) {
        if (content.isBlank()) {
            textView.text = ""
            return
        }
        
        try {
            val markwon = getInstance(textView.context)
            
            // Pre-process content to fix common issues
            val processedContent = preprocessContent(content)
            
            // Render markdown
            markwon.setMarkdown(textView, processedContent)
            
            // Enable link clicking
            textView.movementMethod = LinkMovementMethod.getInstance()
            
            Log.d(TAG, "Successfully rendered ${content.length} chars of markdown")
        } catch (e: Exception) {
            Log.e(TAG, "Error rendering markdown, falling back to plain text", e)
            // Fallback to plain text if rendering fails
            textView.text = stripMarkdown(content)
        }
    }
    
    /**
     * Pre-process content to fix common AI-generated markdown issues
     */
    private fun preprocessContent(content: String): String {
        var processed = content
        
        // Fix escaped characters that may have been double-escaped
        processed = processed
            .replace("\\\\n", "\n")
            .replace("\\n", "\n")
            .replace("\\\\t", "\t")
            .replace("\\t", "\t")
            .replace("\\\\\"", "\"")
            .replace("\\\"", "\"")
        
        // Fix escaped markdown characters
        processed = processed
            .replace("\\*\\*", "**")
            .replace("\\*", "*")
            .replace("\\`\\`\\`", "```")
            .replace("\\`", "`")
            .replace("\\|", "|")
            .replace("\\#", "#")
            .replace("\\_", "_")
        
        // Ensure proper header spacing
        processed = processed.replace(Regex("^(#{1,6})([^#\\s])"), "$1 $2")
        processed = processed.replace(Regex("\n(#{1,6})([^#\\s])"), "\n$1 $2")
        
        // Ensure proper list spacing
        processed = processed.replace(Regex("^([*\\-+])([^\\s*\\-+])"), "$1 $2")
        processed = processed.replace(Regex("\n([*\\-+])([^\\s*\\-+])"), "\n$1 $2")
        
        // Fix multiple consecutive blank lines
        processed = processed.replace(Regex("\n{4,}"), "\n\n\n")
        
        // Remove Mermaid diagrams (not supported by Markwon, convert to text)
        processed = convertMermaidToText(processed)
        
        // Ensure tables have proper formatting
        processed = fixTableFormatting(processed)
        
        return processed.trim()
    }
    
    /**
     * Convert Mermaid diagrams to readable text since Markwon doesn't support them
     */
    private fun convertMermaidToText(content: String): String {
        val mermaidPattern = Regex("```mermaid\\s*\\n([\\s\\S]*?)\\n```", RegexOption.MULTILINE)
        
        return content.replace(mermaidPattern) { match ->
            val diagramContent = match.groupValues[1]
            
            // Extract nodes and flow from Mermaid syntax
            val nodes = mutableListOf<String>()
            val lines = diagramContent.lines()
            
            for (line in lines) {
                // Match node definitions like A[Label], B{Decision}, etc.
                val nodePattern = Regex("[A-Z]\\[([^\\]]+)]")
                nodePattern.findAll(line).forEach { nodes.add(it.groupValues[1]) }
                
                // Also match diamond decision nodes
                val decisionPattern = Regex("[A-Z]\\{([^}]+)}")
                decisionPattern.findAll(line).forEach { nodes.add(it.groupValues[1]) }
            }
            
            if (nodes.isNotEmpty()) {
                "> **Flow:** ${nodes.joinToString(" → ")}"
            } else {
                // If we can't parse it, just show as a note
                "> _Diagram: See detailed steps above_"
            }
        }
    }
    
    /**
     * Fix table formatting to ensure proper rendering
     */
    private fun fixTableFormatting(content: String): String {
        val lines = content.lines().toMutableList()
        val result = mutableListOf<String>()
        var i = 0
        var inCodeBlock = false
        
        while (i < lines.size) {
            val line = lines[i]
            
            // Track code blocks
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
            
            // Check for table row
            if (line.contains("|") && line.count { it == '|' } >= 2) {
                // Check if next line is separator
                val nextLine = lines.getOrNull(i + 1)?.trim() ?: ""
                val hasSeparator = nextLine.matches(Regex("^\\|?[\\s|:-]+\\|?$"))
                
                // Check if it's a header row (no separator yet)
                if (!hasSeparator && nextLine.contains("|")) {
                    // This is likely a header row without separator
                    result.add(line)
                    val columnCount = line.count { it == '|' } - 1
                    if (columnCount > 0) {
                        result.add("|" + " --- |".repeat(columnCount.coerceAtLeast(1)))
                    }
                } else {
                    result.add(line)
                }
            } else {
                result.add(line)
            }
            
            i++
        }
        
        return result.joinToString("\n")
    }
    
    /**
     * Strip all markdown formatting and return plain text
     */
    fun stripMarkdown(content: String): String {
        var text = content
        
        // Remove code blocks
        text = text.replace(Regex("```[\\s\\S]*?```"), "")
        
        // Remove inline code
        text = text.replace(Regex("`[^`]+`"), "")
        
        // Remove headers
        text = text.replace(Regex("^#{1,6}\\s*", RegexOption.MULTILINE), "")
        
        // Remove bold/italic
        text = text.replace(Regex("\\*\\*([^*]+)\\*\\*"), "$1")
        text = text.replace(Regex("\\*([^*]+)\\*"), "$1")
        text = text.replace(Regex("__([^_]+)__"), "$1")
        text = text.replace(Regex("_([^_]+)_"), "$1")
        
        // Remove links, keep text
        text = text.replace(Regex("\\[([^]]+)]\\([^)]+\\)"), "$1")
        
        // Remove blockquotes
        text = text.replace(Regex("^>\\s*", RegexOption.MULTILINE), "")
        
        // Remove list markers
        text = text.replace(Regex("^[\\-*+]\\s+", RegexOption.MULTILINE), "• ")
        text = text.replace(Regex("^\\d+\\.\\s+", RegexOption.MULTILINE), "")
        
        // Remove table formatting
        text = text.replace("|", " ")
        text = text.replace(Regex("-{3,}"), "")
        
        // Collapse whitespace
        text = text.replace(Regex("\\s+"), " ")
        
        return text.trim()
    }
    
    /**
     * Get a plain text preview of markdown content
     */
    fun getPreview(content: String, maxLength: Int = 150): String {
        val plainText = stripMarkdown(content)
        return if (plainText.length > maxLength) {
            plainText.take(maxLength - 3) + "..."
        } else {
            plainText
        }
    }
}
