package com.learneveryday.app.ui.content

import com.google.gson.annotations.SerializedName

/**
 * Structured content blocks for lesson rendering.
 * AI generates these blocks instead of raw markdown, ensuring perfect mobile rendering.
 */
sealed class ContentBlock {
    abstract val type: String
}

/**
 * Heading block (h1, h2, h3, etc.)
 */
data class HeadingBlock(
    val level: Int, // 1-6
    val text: String
) : ContentBlock() {
    override val type: String = "heading"
}

/**
 * Paragraph of text with optional inline formatting
 */
data class TextBlock(
    val text: String,
    val style: TextStyle = TextStyle.NORMAL
) : ContentBlock() {
    override val type: String = "text"
}

enum class TextStyle {
    NORMAL,
    EMPHASIS,      // Italic style
    STRONG,        // Bold style  
    QUOTE          // Blockquote style
}

/**
 * Bullet or numbered list
 */
data class ListBlock(
    val items: List<String>,
    val ordered: Boolean = false // true = numbered, false = bullets
) : ContentBlock() {
    override val type: String = "list"
}

/**
 * Code block with syntax highlighting
 */
data class CodeBlock(
    val code: String,
    val language: String = "plaintext"
) : ContentBlock() {
    override val type: String = "code"
}

/**
 * Table with headers and rows
 */
data class TableBlock(
    val headers: List<String>,
    val rows: List<List<String>>
) : ContentBlock() {
    override val type: String = "table"
}

/**
 * Callout/alert box (tip, warning, note, info)
 */
data class CalloutBlock(
    val title: String,
    val message: String,
    val style: CalloutStyle = CalloutStyle.INFO
) : ContentBlock() {
    override val type: String = "callout"
}

enum class CalloutStyle {
    INFO,      // Blue - general information
    TIP,       // Green - helpful tips
    WARNING,   // Orange - caution
    ERROR,     // Red - important warnings
    SUCCESS    // Green - success messages
}

/**
 * Flowchart/Process diagram
 */
data class FlowchartBlock(
    val title: String?,
    val steps: List<FlowStep>
) : ContentBlock() {
    override val type: String = "flowchart"
}

data class FlowStep(
    val label: String,
    val description: String? = null
)

/**
 * Key-value pairs (for definitions, properties, etc.)
 */
data class DefinitionBlock(
    val title: String?,
    val items: List<DefinitionItem>
) : ContentBlock() {
    override val type: String = "definition"
}

data class DefinitionItem(
    val term: String,
    val definition: String
)

/**
 * Divider/separator line
 */
data class DividerBlock(
    val style: DividerStyle = DividerStyle.LINE
) : ContentBlock() {
    override val type: String = "divider"
}

enum class DividerStyle {
    LINE,
    DOTS,
    SPACE
}

/**
 * Image block (for future use)
 */
data class ImageBlock(
    val url: String,
    val caption: String? = null,
    val alt: String? = null
) : ContentBlock() {
    override val type: String = "image"
}

/**
 * Container for the full lesson content
 */
data class StructuredContent(
    val blocks: List<ContentBlock>
)

/**
 * Raw JSON block representation for Gson parsing
 */
data class RawContentBlock(
    val type: String,
    val level: Int? = null,
    val text: String? = null,
    val style: String? = null,
    val items: List<Any>? = null,
    val ordered: Boolean? = null,
    val code: String? = null,
    val language: String? = null,
    val headers: List<String>? = null,
    val rows: List<List<String>>? = null,
    val title: String? = null,
    val message: String? = null,
    val steps: List<Map<String, String>>? = null,
    val term: String? = null,
    val definition: String? = null,
    val url: String? = null,
    val caption: String? = null,
    val alt: String? = null
)
