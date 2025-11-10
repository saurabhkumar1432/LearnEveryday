package com.learneveryday.app.util

/**
 * Utility for determining adaptive chunk size for outline generation.
 * Rough heuristic: assume ~250 tokens per outline.
 */
object ChunkSizingUtil {
    fun calculateAdaptiveChunkSize(totalTitles: Int, maxTokens: Int, baseChunk: Int): Int {
        if (totalTitles <= 0) return 0
        if (totalTitles <= baseChunk) return totalTitles
        val availableChunks = maxTokens / 250
        return availableChunks.coerceIn(1, baseChunk)
    }
}
