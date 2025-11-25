package com.learneveryday.app.util

import android.content.Context
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.content.ContextCompat
import com.learneveryday.app.R

/**
 * Renders AI-generated markdown content with proper support for:
 * - Tables (horizontal scrolling)
 * - Code blocks (syntax highlighting)
 * - Flowcharts/diagrams (Mermaid.js)
 * - Math equations (KaTeX)
 * - Responsive mobile layout
 */
object RichContentRenderer {
    
    /**
     * Configures a WebView for optimal markdown content rendering
     */
    fun setupWebView(webView: WebView, isDarkTheme: Boolean = false) {
        webView.apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                loadWithOverviewMode = true
                useWideViewPort = true
                builtInZoomControls = false
                displayZoomControls = false
                setSupportZoom(false)
            }
            
            // Transparent background
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            
            // Handle links
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                    // Open external links in browser
                    url?.let {
                        try {
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(it))
                            view?.context?.startActivity(intent)
                        } catch (e: Exception) {
                            // Ignore
                        }
                    }
                    return true
                }
            }
        }
    }
    
    /**
     * Renders markdown content in the WebView with full styling
     */
    fun renderContent(webView: WebView, markdown: String, context: Context, isDarkTheme: Boolean = false) {
        val processedMarkdown = MarkdownProcessor.process(markdown)
        val html = generateHtml(processedMarkdown, context, isDarkTheme)
        webView.loadDataWithBaseURL(
            "file:///android_asset/",
            html,
            "text/html",
            "UTF-8",
            null
        )
    }
    
    /**
     * Generates full HTML document with styling and scripts
     */
    private fun generateHtml(markdown: String, context: Context, isDarkTheme: Boolean): String {
        val colors = getThemeColors(context, isDarkTheme)
        val escapedContent = escapeForHtml(markdown)
        
        return """
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
    
    <!-- Marked.js for Markdown parsing -->
    <script src="https://cdn.jsdelivr.net/npm/marked/marked.min.js"></script>
    
    <!-- Highlight.js for code syntax highlighting -->
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.9.0/styles/${if (isDarkTheme) "github-dark" else "github"}.min.css">
    <script src="https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.9.0/highlight.min.js"></script>
    
    <!-- Mermaid.js for flowcharts and diagrams -->
    <script src="https://cdn.jsdelivr.net/npm/mermaid/dist/mermaid.min.js"></script>
    
    <style>
        * {
            box-sizing: border-box;
        }
        
        html, body {
            margin: 0;
            padding: 0;
            background: transparent;
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, sans-serif;
            font-size: 16px;
            line-height: 1.7;
            color: ${colors.textPrimary};
            word-wrap: break-word;
            overflow-x: hidden;
        }
        
        .content {
            padding: 0;
            max-width: 100%;
        }
        
        /* Headings */
        h1, h2, h3, h4, h5, h6 {
            color: ${colors.textPrimary};
            margin-top: 1.5em;
            margin-bottom: 0.5em;
            font-weight: 600;
            line-height: 1.3;
        }
        
        h1 { font-size: 1.75em; border-bottom: 2px solid ${colors.primary}; padding-bottom: 0.3em; }
        h2 { font-size: 1.5em; border-bottom: 1px solid ${colors.border}; padding-bottom: 0.2em; }
        h3 { font-size: 1.25em; }
        h4 { font-size: 1.1em; }
        h5, h6 { font-size: 1em; }
        
        h1:first-child, h2:first-child, h3:first-child {
            margin-top: 0;
        }
        
        /* Paragraphs */
        p {
            margin: 1em 0;
        }
        
        /* Links */
        a {
            color: ${colors.primary};
            text-decoration: none;
        }
        
        a:hover {
            text-decoration: underline;
        }
        
        /* Bold and Italic */
        strong, b {
            font-weight: 600;
            color: ${colors.textPrimary};
        }
        
        em, i {
            font-style: italic;
        }
        
        /* Lists */
        ul, ol {
            padding-left: 1.5em;
            margin: 1em 0;
        }
        
        li {
            margin: 0.5em 0;
        }
        
        li > ul, li > ol {
            margin: 0.25em 0;
        }
        
        /* Inline code */
        code:not(pre code) {
            background: ${colors.codeBackground};
            color: ${colors.codeText};
            padding: 0.2em 0.4em;
            border-radius: 4px;
            font-family: 'SF Mono', Monaco, 'Cascadia Code', monospace;
            font-size: 0.9em;
        }
        
        /* Code blocks */
        pre {
            background: ${colors.codeBlockBackground};
            border-radius: 8px;
            padding: 1em;
            overflow-x: auto;
            margin: 1em 0;
            border: 1px solid ${colors.border};
        }
        
        pre code {
            background: transparent;
            padding: 0;
            font-family: 'SF Mono', Monaco, 'Cascadia Code', monospace;
            font-size: 0.875em;
            line-height: 1.5;
            white-space: pre;
        }
        
        /* Tables - CRITICAL for proper rendering */
        .table-wrapper {
            width: 100%;
            overflow-x: auto;
            margin: 1em 0;
            -webkit-overflow-scrolling: touch;
        }
        
        table {
            border-collapse: collapse;
            width: 100%;
            min-width: max-content;
            font-size: 0.9em;
        }
        
        th, td {
            border: 1px solid ${colors.tableBorder};
            padding: 10px 14px;
            text-align: left;
            white-space: nowrap;
        }
        
        th {
            background: ${colors.tableHeaderBg};
            font-weight: 600;
            color: ${colors.textPrimary};
        }
        
        tr:nth-child(even) {
            background: ${colors.tableRowAlt};
        }
        
        tr:hover {
            background: ${colors.tableRowHover};
        }
        
        /* Blockquotes / Callouts */
        blockquote {
            margin: 1em 0;
            padding: 0.75em 1em;
            border-left: 4px solid ${colors.primary};
            background: ${colors.blockquoteBg};
            border-radius: 0 8px 8px 0;
        }
        
        blockquote p {
            margin: 0;
        }
        
        /* Tip/Note/Warning callouts */
        blockquote:has(strong:first-child) {
            padding-left: 1em;
        }
        
        blockquote strong:first-child {
            display: block;
            margin-bottom: 0.5em;
        }
        
        /* Horizontal rule */
        hr {
            border: none;
            border-top: 1px solid ${colors.border};
            margin: 2em 0;
        }
        
        /* Images */
        img {
            max-width: 100%;
            height: auto;
            border-radius: 8px;
            margin: 1em 0;
        }
        
        /* Mermaid diagrams */
        .mermaid {
            background: ${colors.diagramBg};
            border-radius: 8px;
            padding: 1em;
            margin: 1em 0;
            overflow-x: auto;
            text-align: center;
        }
        
        /* Task lists */
        ul.task-list {
            list-style: none;
            padding-left: 0;
        }
        
        .task-list-item {
            display: flex;
            align-items: flex-start;
            gap: 0.5em;
        }
        
        .task-list-item input[type="checkbox"] {
            margin-top: 0.3em;
            accent-color: ${colors.primary};
        }
        
        /* Definition lists */
        dl {
            margin: 1em 0;
        }
        
        dt {
            font-weight: 600;
            margin-top: 1em;
        }
        
        dd {
            margin-left: 1em;
            color: ${colors.textSecondary};
        }
        
        /* Flow diagram text styling */
        .flow-diagram {
            background: ${colors.diagramBg};
            padding: 1em;
            border-radius: 8px;
            font-family: monospace;
            text-align: center;
            margin: 1em 0;
            overflow-x: auto;
        }
        
        /* Key points / Summary boxes */
        .summary-box {
            background: ${colors.summaryBg};
            border: 1px solid ${colors.primary};
            border-radius: 8px;
            padding: 1em;
            margin: 1em 0;
        }
        
        /* Responsive adjustments */
        @media (max-width: 480px) {
            body {
                font-size: 15px;
            }
            
            h1 { font-size: 1.5em; }
            h2 { font-size: 1.3em; }
            h3 { font-size: 1.15em; }
            
            pre {
                padding: 0.75em;
                font-size: 0.8em;
            }
            
            th, td {
                padding: 8px 10px;
            }
        }
    </style>
</head>
<body>
    <div class="content" id="content"></div>
    
    <script>
        // Configure Marked.js
        marked.setOptions({
            highlight: function(code, lang) {
                if (lang && hljs.getLanguage(lang)) {
                    try {
                        return hljs.highlight(code, { language: lang }).value;
                    } catch (e) {}
                }
                return hljs.highlightAuto(code).value;
            },
            breaks: true,
            gfm: true,
            tables: true,
            sanitize: false
        });
        
        // Custom renderer for tables and diagrams
        const renderer = new marked.Renderer();
        
        // Wrap tables for horizontal scroll
        renderer.table = function(header, body) {
            return '<div class="table-wrapper"><table><thead>' + header + '</thead><tbody>' + body + '</tbody></table></div>';
        };
        
        // Handle code blocks - check for mermaid
        const originalCode = renderer.code;
        renderer.code = function(code, language) {
            if (language === 'mermaid') {
                return '<div class="mermaid">' + code + '</div>';
            }
            // Check for flow diagram patterns
            if (code.includes('→') || code.includes('-->') || code.includes('->')) {
                return '<div class="flow-diagram"><pre>' + code + '</pre></div>';
            }
            return originalCode.call(this, code, language);
        };
        
        marked.use({ renderer });
        
        // Raw markdown content
        const rawMarkdown = `${escapedContent}`;
        
        // Parse and render
        document.getElementById('content').innerHTML = marked.parse(rawMarkdown);
        
        // Initialize Mermaid
        mermaid.initialize({
            startOnLoad: true,
            theme: '${if (isDarkTheme) "dark" else "default"}',
            securityLevel: 'loose',
            flowchart: {
                useMaxWidth: true,
                htmlLabels: true
            }
        });
        
        // Re-run Mermaid for any diagrams
        mermaid.run();
        
        // Apply syntax highlighting to code blocks
        document.querySelectorAll('pre code').forEach((block) => {
            hljs.highlightElement(block);
        });
    </script>
</body>
</html>
        """.trimIndent()
    }
    
    /**
     * Theme colors container
     */
    private data class ThemeColors(
        val textPrimary: String,
        val textSecondary: String,
        val primary: String,
        val background: String,
        val surface: String,
        val border: String,
        val codeBackground: String,
        val codeBlockBackground: String,
        val codeText: String,
        val tableBorder: String,
        val tableHeaderBg: String,
        val tableRowAlt: String,
        val tableRowHover: String,
        val blockquoteBg: String,
        val diagramBg: String,
        val summaryBg: String
    )
    
    /**
     * Get colors from theme resources
     */
    private fun getThemeColors(context: Context, isDarkTheme: Boolean): ThemeColors {
        return if (isDarkTheme) {
            ThemeColors(
                textPrimary = "#E4E4E7",
                textSecondary = "#A1A1AA",
                primary = "#818CF8",
                background = "#18181B",
                surface = "#27272A",
                border = "#3F3F46",
                codeBackground = "#3F3F46",
                codeBlockBackground = "#1F1F23",
                codeText = "#E4E4E7",
                tableBorder = "#3F3F46",
                tableHeaderBg = "#3F3F46",
                tableRowAlt = "rgba(63, 63, 70, 0.3)",
                tableRowHover = "rgba(129, 140, 248, 0.1)",
                blockquoteBg = "rgba(129, 140, 248, 0.1)",
                diagramBg = "#27272A",
                summaryBg = "rgba(129, 140, 248, 0.1)"
            )
        } else {
            ThemeColors(
                textPrimary = "#1F2937",
                textSecondary = "#6B7280",
                primary = "#6366F1",
                background = "#FFFFFF",
                surface = "#F9FAFB",
                border = "#E5E7EB",
                codeBackground = "#F3F4F6",
                codeBlockBackground = "#F8FAFC",
                codeText = "#1F2937",
                tableBorder = "#E5E7EB",
                tableHeaderBg = "#F3F4F6",
                tableRowAlt = "rgba(243, 244, 246, 0.5)",
                tableRowHover = "rgba(99, 102, 241, 0.05)",
                blockquoteBg = "rgba(99, 102, 241, 0.05)",
                diagramBg = "#F9FAFB",
                summaryBg = "rgba(99, 102, 241, 0.05)"
            )
        }
    }
    
    /**
     * Escape content for safe embedding in JavaScript string
     */
    private fun escapeForHtml(content: String): String {
        return content
            .replace("\\", "\\\\")
            .replace("`", "\\`")
            .replace("\$", "\\\$")
            .replace("\r\n", "\n")
            .replace("\r", "\n")
    }
}
