package com.learneveryday.app.ui.content

import android.content.Context
import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.StyleSpan
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.learneveryday.app.R

/**
 * RecyclerView Adapter for rendering structured content blocks with native Android views.
 * Each block type has its own ViewHolder for optimal rendering.
 */
class ContentBlockAdapter(
    private var blocks: List<ContentBlock> = emptyList()
) : RecyclerView.Adapter<ContentBlockAdapter.BlockViewHolder>() {

    companion object {
        private const val TYPE_HEADING = 0
        private const val TYPE_TEXT = 1
        private const val TYPE_LIST = 2
        private const val TYPE_CODE = 3
        private const val TYPE_TABLE = 4
        private const val TYPE_CALLOUT = 5
        private const val TYPE_FLOWCHART = 6
        private const val TYPE_DEFINITION = 7
        private const val TYPE_DIVIDER = 8
        private const val TYPE_IMAGE = 9
    }

    fun updateContent(newBlocks: List<ContentBlock>) {
        blocks = newBlocks
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return when (blocks[position]) {
            is HeadingBlock -> TYPE_HEADING
            is TextBlock -> TYPE_TEXT
            is ListBlock -> TYPE_LIST
            is CodeBlock -> TYPE_CODE
            is TableBlock -> TYPE_TABLE
            is CalloutBlock -> TYPE_CALLOUT
            is FlowchartBlock -> TYPE_FLOWCHART
            is DefinitionBlock -> TYPE_DEFINITION
            is DividerBlock -> TYPE_DIVIDER
            is ImageBlock -> TYPE_IMAGE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BlockViewHolder {
        val context = parent.context
        return when (viewType) {
            TYPE_HEADING -> HeadingViewHolder(createHeadingView(context))
            TYPE_TEXT -> TextViewHolder(createTextView(context))
            TYPE_LIST -> ListViewHolder(createListContainer(context))
            TYPE_CODE -> CodeViewHolder(createCodeView(context))
            TYPE_TABLE -> TableViewHolder(createTableContainer(context))
            TYPE_CALLOUT -> CalloutViewHolder(createCalloutView(context))
            TYPE_FLOWCHART -> FlowchartViewHolder(createFlowchartContainer(context))
            TYPE_DEFINITION -> DefinitionViewHolder(createDefinitionContainer(context))
            TYPE_DIVIDER -> DividerViewHolder(createDividerView(context))
            TYPE_IMAGE -> ImageViewHolder(createImageView(context))
            else -> TextViewHolder(createTextView(context))
        }
    }

    override fun onBindViewHolder(holder: BlockViewHolder, position: Int) {
        holder.bind(blocks[position])
    }

    override fun getItemCount(): Int = blocks.size

    // ============ View Creation Methods ============

    private fun createHeadingView(context: Context): TextView {
        return TextView(context).apply {
            layoutParams = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, dpToPx(context, 16), 0, dpToPx(context, 8))
            }
            setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            setTypeface(null, Typeface.BOLD)
        }
    }

    private fun createTextView(context: Context): TextView {
        return TextView(context).apply {
            layoutParams = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, dpToPx(context, 8), 0, dpToPx(context, 8))
            }
            setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setLineSpacing(dpToPx(context, 4).toFloat(), 1.2f)
        }
    }

    private fun createListContainer(context: Context): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, dpToPx(context, 8), 0, dpToPx(context, 8))
            }
        }
    }

    private fun createCodeView(context: Context): HorizontalScrollView {
        return HorizontalScrollView(context).apply {
            layoutParams = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, dpToPx(context, 12), 0, dpToPx(context, 12))
            }
            isHorizontalScrollBarEnabled = true
            
            val codeCard = MaterialCardView(context).apply {
                radius = dpToPx(context, 8).toFloat()
                setCardBackgroundColor(ContextCompat.getColor(context, R.color.surface_variant))
                cardElevation = 0f
                
                val codeText = TextView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    setPadding(dpToPx(context, 16), dpToPx(context, 12), dpToPx(context, 16), dpToPx(context, 12))
                    setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                    typeface = Typeface.MONOSPACE
                    setTextIsSelectable(true)
                    tag = "codeText"
                }
                addView(codeText)
            }
            addView(codeCard)
        }
    }

    private fun createTableContainer(context: Context): HorizontalScrollView {
        return HorizontalScrollView(context).apply {
            layoutParams = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, dpToPx(context, 12), 0, dpToPx(context, 12))
            }
            isHorizontalScrollBarEnabled = true
            
            val tableLayout = TableLayout(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                setBackgroundResource(R.color.surface)
                tag = "tableLayout"
            }
            addView(tableLayout)
        }
    }

    private fun createCalloutView(context: Context): MaterialCardView {
        return MaterialCardView(context).apply {
            layoutParams = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, dpToPx(context, 12), 0, dpToPx(context, 12))
            }
            radius = dpToPx(context, 12).toFloat()
            cardElevation = dpToPx(context, 2).toFloat()
            
            val container = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dpToPx(context, 16), dpToPx(context, 12), dpToPx(context, 16), dpToPx(context, 12))
                
                val titleView = TextView(context).apply {
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                    setTypeface(null, Typeface.BOLD)
                    tag = "calloutTitle"
                }
                
                val messageView = TextView(context).apply {
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                    setLineSpacing(dpToPx(context, 2).toFloat(), 1.1f)
                    tag = "calloutMessage"
                }
                
                addView(titleView)
                addView(messageView)
            }
            addView(container)
        }
    }

    private fun createFlowchartContainer(context: Context): MaterialCardView {
        return MaterialCardView(context).apply {
            layoutParams = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, dpToPx(context, 12), 0, dpToPx(context, 12))
            }
            radius = dpToPx(context, 12).toFloat()
            setCardBackgroundColor(ContextCompat.getColor(context, R.color.surface_variant))
            cardElevation = 0f
            
            val scrollView = HorizontalScrollView(context).apply {
                val flowContainer = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(dpToPx(context, 16), dpToPx(context, 16), dpToPx(context, 16), dpToPx(context, 16))
                    tag = "flowContainer"
                }
                addView(flowContainer)
            }
            addView(scrollView)
        }
    }

    private fun createDefinitionContainer(context: Context): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, dpToPx(context, 8), 0, dpToPx(context, 8))
            }
        }
    }

    private fun createDividerView(context: Context): View {
        return View(context).apply {
            layoutParams = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dpToPx(context, 1)
            ).apply {
                setMargins(0, dpToPx(context, 16), 0, dpToPx(context, 16))
            }
            setBackgroundColor(ContextCompat.getColor(context, R.color.divider))
        }
    }

    private fun createImageView(context: Context): ImageView {
        return ImageView(context).apply {
            layoutParams = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, dpToPx(context, 12), 0, dpToPx(context, 12))
            }
            adjustViewBounds = true
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
    }

    // ============ ViewHolder Classes ============

    abstract class BlockViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        abstract fun bind(block: ContentBlock)
    }

    inner class HeadingViewHolder(private val textView: TextView) : BlockViewHolder(textView) {
        override fun bind(block: ContentBlock) {
            val heading = block as HeadingBlock
            textView.text = heading.text
            val textSize = when (heading.level) {
                1 -> 28f
                2 -> 24f
                3 -> 20f
                4 -> 18f
                5 -> 16f
                else -> 14f
            }
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize)
        }
    }

    inner class TextViewHolder(private val textView: TextView) : BlockViewHolder(textView) {
        override fun bind(block: ContentBlock) {
            val text = block as TextBlock
            textView.text = text.text
            
            when (text.style) {
                TextStyle.EMPHASIS -> textView.setTypeface(null, Typeface.ITALIC)
                TextStyle.STRONG -> textView.setTypeface(null, Typeface.BOLD)
                TextStyle.QUOTE -> {
                    textView.setTypeface(null, Typeface.ITALIC)
                    textView.setPadding(dpToPx(textView.context, 16), 0, 0, 0)
                    textView.setTextColor(ContextCompat.getColor(textView.context, R.color.text_secondary))
                }
                TextStyle.NORMAL -> textView.setTypeface(null, Typeface.NORMAL)
            }
        }
    }

    inner class ListViewHolder(private val container: LinearLayout) : BlockViewHolder(container) {
        override fun bind(block: ContentBlock) {
            val list = block as ListBlock
            container.removeAllViews()
            
            list.items.forEachIndexed { index, item ->
                val itemView = LinearLayout(container.context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.TOP
                    setPadding(0, dpToPx(context, 4), 0, dpToPx(context, 4))
                    
                    // Bullet or number
                    val marker = TextView(context).apply {
                        text = if (list.ordered) "${index + 1}." else "•"
                        setTextColor(ContextCompat.getColor(context, R.color.primary))
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                        layoutParams = LinearLayout.LayoutParams(
                            dpToPx(context, 24),
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                    }
                    
                    // Item text
                    val textView = TextView(context).apply {
                        text = item
                        setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                    }
                    
                    addView(marker)
                    addView(textView)
                }
                container.addView(itemView)
            }
        }
    }

    inner class CodeViewHolder(private val scrollView: HorizontalScrollView) : BlockViewHolder(scrollView) {
        override fun bind(block: ContentBlock) {
            val code = block as CodeBlock
            val codeText = scrollView.findViewWithTag<TextView>("codeText")
            codeText?.text = code.code
        }
    }

    inner class TableViewHolder(private val scrollView: HorizontalScrollView) : BlockViewHolder(scrollView) {
        override fun bind(block: ContentBlock) {
            val table = block as TableBlock
            val tableLayout = scrollView.findViewWithTag<TableLayout>("tableLayout")
            tableLayout?.removeAllViews()
            
            val context = scrollView.context
            
            // Header row
            val headerRow = TableRow(context).apply {
                setBackgroundColor(ContextCompat.getColor(context, R.color.surface_variant))
            }
            table.headers.forEach { header ->
                val cell = createTableCell(context, header, isHeader = true)
                headerRow.addView(cell)
            }
            tableLayout?.addView(headerRow)
            
            // Data rows
            table.rows.forEachIndexed { rowIndex, row ->
                val dataRow = TableRow(context).apply {
                    if (rowIndex % 2 == 1) {
                        setBackgroundColor(ContextCompat.getColor(context, R.color.surface))
                    }
                }
                row.forEach { cellText ->
                    val cell = createTableCell(context, cellText, isHeader = false)
                    dataRow.addView(cell)
                }
                tableLayout?.addView(dataRow)
            }
        }
        
        private fun createTableCell(context: Context, text: String, isHeader: Boolean): TextView {
            return TextView(context).apply {
                this.text = text
                setPadding(dpToPx(context, 12), dpToPx(context, 10), dpToPx(context, 12), dpToPx(context, 10))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                if (isHeader) {
                    setTypeface(null, Typeface.BOLD)
                }
                background = ContextCompat.getDrawable(context, R.drawable.table_cell_border)
                minWidth = dpToPx(context, 80)
            }
        }
    }

    inner class CalloutViewHolder(private val card: MaterialCardView) : BlockViewHolder(card) {
        override fun bind(block: ContentBlock) {
            val callout = block as CalloutBlock
            val context = card.context
            
            val (bgColor, borderColor, textColor, icon) = when (callout.style) {
                CalloutStyle.TIP -> Quadruple(
                    R.color.callout_tip_bg,
                    R.color.callout_tip_border,
                    R.color.callout_tip_text,
                    "💡"
                )
                CalloutStyle.WARNING -> Quadruple(
                    R.color.callout_warning_bg,
                    R.color.callout_warning_border,
                    R.color.callout_warning_text,
                    "⚠️"
                )
                CalloutStyle.ERROR -> Quadruple(
                    R.color.callout_error_bg,
                    R.color.callout_error_border,
                    R.color.callout_error_text,
                    "❌"
                )
                CalloutStyle.SUCCESS -> Quadruple(
                    R.color.callout_success_bg,
                    R.color.callout_success_border,
                    R.color.callout_success_text,
                    "✅"
                )
                CalloutStyle.INFO -> Quadruple(
                    R.color.callout_info_bg,
                    R.color.callout_info_border,
                    R.color.callout_info_text,
                    "ℹ️"
                )
            }
            
            card.setCardBackgroundColor(ContextCompat.getColor(context, bgColor))
            card.strokeColor = ContextCompat.getColor(context, borderColor)
            card.strokeWidth = dpToPx(context, 1)
            
            val titleView = card.findViewWithTag<TextView>("calloutTitle")
            val messageView = card.findViewWithTag<TextView>("calloutMessage")
            
            titleView?.apply {
                text = "$icon ${callout.title}"
                setTextColor(ContextCompat.getColor(context, textColor))
                visibility = if (callout.title.isNotEmpty()) View.VISIBLE else View.GONE
            }
            
            messageView?.apply {
                text = callout.message
                setTextColor(ContextCompat.getColor(context, textColor))
            }
        }
    }

    inner class FlowchartViewHolder(private val card: MaterialCardView) : BlockViewHolder(card) {
        override fun bind(block: ContentBlock) {
            val flowchart = block as FlowchartBlock
            val flowContainer = card.findViewWithTag<LinearLayout>("flowContainer")
            flowContainer?.removeAllViews()
            
            val context = card.context
            
            flowchart.steps.forEachIndexed { index, step ->
                // Step box
                val stepCard = MaterialCardView(context).apply {
                    radius = dpToPx(context, 8).toFloat()
                    setCardBackgroundColor(ContextCompat.getColor(context, R.color.primary))
                    cardElevation = dpToPx(context, 2).toFloat()
                    
                    val stepText = TextView(context).apply {
                        text = step.label
                        setTextColor(ContextCompat.getColor(context, R.color.white))
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                        setTypeface(null, Typeface.BOLD)
                        setPadding(dpToPx(context, 12), dpToPx(context, 8), dpToPx(context, 12), dpToPx(context, 8))
                        gravity = Gravity.CENTER
                    }
                    addView(stepText)
                }
                flowContainer?.addView(stepCard)
                
                // Arrow (except for last step)
                if (index < flowchart.steps.size - 1) {
                    val arrow = TextView(context).apply {
                        text = "→"
                        setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
                        setPadding(dpToPx(context, 8), 0, dpToPx(context, 8), 0)
                        gravity = Gravity.CENTER
                    }
                    flowContainer?.addView(arrow)
                }
            }
        }
    }

    inner class DefinitionViewHolder(private val container: LinearLayout) : BlockViewHolder(container) {
        override fun bind(block: ContentBlock) {
            val definition = block as DefinitionBlock
            container.removeAllViews()
            
            val context = container.context
            
            // Title
            if (!definition.title.isNullOrEmpty()) {
                val titleView = TextView(context).apply {
                    text = definition.title
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                    setTypeface(null, Typeface.BOLD)
                    setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                    setPadding(0, 0, 0, dpToPx(context, 8))
                }
                container.addView(titleView)
            }
            
            // Items
            definition.items.forEach { item ->
                val itemContainer = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(0, dpToPx(context, 4), 0, dpToPx(context, 4))
                    
                    val termView = TextView(context).apply {
                        text = item.term
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                        setTypeface(null, Typeface.BOLD)
                        setTextColor(ContextCompat.getColor(context, R.color.primary))
                    }
                    
                    val defView = TextView(context).apply {
                        text = item.definition
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                        setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                        setPadding(dpToPx(context, 16), dpToPx(context, 2), 0, 0)
                    }
                    
                    addView(termView)
                    addView(defView)
                }
                container.addView(itemContainer)
            }
        }
    }

    inner class DividerViewHolder(view: View) : BlockViewHolder(view) {
        override fun bind(block: ContentBlock) {
            // Divider doesn't need binding
        }
    }

    inner class ImageViewHolder(private val imageView: ImageView) : BlockViewHolder(imageView) {
        override fun bind(block: ContentBlock) {
            @Suppress("UNUSED_VARIABLE")
            val image = block as ImageBlock
            // TODO: Load image with Glide/Coil using image.url and image.altText
            // For now, show placeholder
            imageView.setImageResource(R.drawable.ic_image_placeholder)
        }
    }

    // ============ Utility ============

    private fun dpToPx(context: Context, dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            context.resources.displayMetrics
        ).toInt()
    }

    data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
}
