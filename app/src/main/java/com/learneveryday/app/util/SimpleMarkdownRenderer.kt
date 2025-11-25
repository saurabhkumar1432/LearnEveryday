package com.learneveryday.app.util

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.util.Log
import android.util.LruCache
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
import java.util.concurrent.Executors

/**
 * Simple, reliable markdown renderer using Markwon.
 * This replaces the complex custom ContentParser with a battle-tested library
 * that handles all markdown formatting correctly on mobile.
 * 
 * Optimized for performance with:
 * - Async rendering on background thread
 * - LRU cache for rendered content
 * - Pre-processing optimizations
 */
object SimpleMarkdownRenderer {
    
    private const val TAG = "SimpleMarkdownRenderer"
    private const val CACHE_SIZE = 10 // Number of rendered documents to cache
    
    @Volatile
    private var markwonInstance: Markwon? = null
    
    // Main thread handler for posting results
    private val mainHandler = Handler(Looper.getMainLooper())
    
    // Background executor for markdown parsing
    private val renderExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "MarkdownRenderer").apply { 
            isDaemon = true 
            priority = Thread.NORM_PRIORITY - 1
        }
    }
    
    // LRU cache for rendered Spanned content (keyed by content hash)
    private val renderCache = object : LruCache<Int, Spanned>(CACHE_SIZE) {
        override fun sizeOf(key: Int, value: Spanned): Int = 1
    }
    
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
                        // Code styling (inline code) - same size as body text
                        .codeBackgroundColor(surfaceVariantColor)
                        .codeTextColor(textPrimaryColor)
                        .codeTextSize(17)
                        // Code block styling - same size as body text
                        .codeBlockBackgroundColor(surfaceVariantColor)
                        .codeBlockTextColor(textPrimaryColor)
                        .codeBlockTextSize(17)
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
     * Render markdown content to a TextView (synchronous - use for small content)
     */
    fun render(textView: TextView, content: String) {
        if (content.isBlank()) {
            textView.text = ""
            return
        }
        
        try {
            val contentHash = content.hashCode()
            
            // Check cache first
            val cached = renderCache.get(contentHash)
            if (cached != null) {
                textView.setText(cached, TextView.BufferType.SPANNABLE)
                textView.movementMethod = LinkMovementMethod.getInstance()
                Log.d(TAG, "Served ${content.length} chars from cache")
                return
            }
            
            val markwon = getInstance(textView.context)
            
            // Pre-process content to fix common issues
            val processedContent = preprocessContent(content)
            
            // Render and cache
            val spanned = markwon.toMarkdown(processedContent)
            renderCache.put(contentHash, spanned)
            
            textView.setText(spanned, TextView.BufferType.SPANNABLE)
            
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
     * Render markdown content asynchronously (preferred for large content)
     * Parses on background thread, applies result on main thread
     */
    fun renderAsync(textView: TextView, content: String, onComplete: (() -> Unit)? = null) {
        if (content.isBlank()) {
            textView.text = ""
            onComplete?.invoke()
            return
        }
        
        val contentHash = content.hashCode()
        
        // Check cache first on main thread
        val cached = renderCache.get(contentHash)
        if (cached != null) {
            textView.setText(cached, TextView.BufferType.SPANNABLE)
            textView.movementMethod = LinkMovementMethod.getInstance()
            Log.d(TAG, "Served ${content.length} chars from cache (async)")
            onComplete?.invoke()
            return
        }
        
        // Store context reference before going to background
        val context = textView.context.applicationContext
        
        renderExecutor.execute {
            try {
                val markwon = getInstance(context)
                val processedContent = preprocessContent(content)
                val spanned = markwon.toMarkdown(processedContent)
                
                // Cache the result
                renderCache.put(contentHash, spanned)
                
                // Post to main thread
                mainHandler.post {
                    try {
                        textView.setText(spanned, TextView.BufferType.SPANNABLE)
                        textView.movementMethod = LinkMovementMethod.getInstance()
                        Log.d(TAG, "Async rendered ${content.length} chars of markdown")
                        onComplete?.invoke()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error applying rendered markdown", e)
                        textView.text = stripMarkdown(content)
                        onComplete?.invoke()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error rendering markdown async", e)
                mainHandler.post {
                    textView.text = stripMarkdown(content)
                    onComplete?.invoke()
                }
            }
        }
    }
    
    /**
     * Clear the render cache (call when memory is low)
     */
    fun clearCache() {
        renderCache.evictAll()
        Log.d(TAG, "Render cache cleared")
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
     * Fix table formatting to ensure proper rendering.
     * Removes duplicate separator rows and ensures only one separator after header.
     */
    private fun fixTableFormatting(content: String): String {
        val lines = content.lines().toMutableList()
        val result = mutableListOf<String>()
        var i = 0
        var inCodeBlock = false
        var inTable = false
        var hasSeparator = false
        
        while (i < lines.size) {
            val line = lines[i]
            val trimmedLine = line.trim()
            
            // Track code blocks
            if (trimmedLine.startsWith("```")) {
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
            
            // Check if this is a table separator row (contains only |, -, :, spaces)
            val isSeparatorRow = trimmedLine.contains("|") && 
                                 trimmedLine.replace(Regex("[|:\\-\\s]"), "").isEmpty() &&
                                 trimmedLine.contains("-")
            
            // Check if this is a table data row
            val isTableRow = trimmedLine.contains("|") && trimmedLine.count { it == '|' } >= 2 && !isSeparatorRow
            
            when {
                isSeparatorRow -> {
                    // Only add separator if we're in a table and haven't added one yet
                    if (inTable && !hasSeparator) {
                        result.add(line)
                        hasSeparator = true
                    }
                    // Skip duplicate separator rows
                }
                
                isTableRow -> {
                    if (!inTable) {
                        // Starting a new table
                        inTable = true
                        hasSeparator = false
                        result.add(line)
                        
                        // Check if next line is a separator
                        val nextLine = lines.getOrNull(i + 1)?.trim() ?: ""
                        val nextIsSeparator = nextLine.contains("|") && 
                                             nextLine.replace(Regex("[|:\\-\\s]"), "").isEmpty() &&
                                             nextLine.contains("-")
                        
                        // If no separator follows, add one
                        if (!nextIsSeparator) {
                            val columnCount = (trimmedLine.count { it == '|' } - 1).coerceAtLeast(1)
                            result.add("|" + " --- |".repeat(columnCount))
                            hasSeparator = true
                        }
                    } else {
                        // Continue table
                        result.add(line)
                    }
                }
                
                else -> {
                    // Not a table row - reset table state
                    if (inTable) {
                        inTable = false
                        hasSeparator = false
                    }
                    result.add(line)
                }
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
