package com.learneveryday.app.util

import org.junit.Assert.assertEquals
import org.junit.Test

class ChunkSizingUtilTest {

    @Test
    fun `returns 0 when no titles`() {
        val actual = ChunkSizingUtil.calculateAdaptiveChunkSize(totalTitles = 0, maxTokens = 1000, baseChunk = 8)
        assertEquals(0, actual)
    }

    @Test
    fun `returns totalTitles when less than baseChunk`() {
        val actual = ChunkSizingUtil.calculateAdaptiveChunkSize(totalTitles = 3, maxTokens = 1000, baseChunk = 8)
        assertEquals(3, actual)
    }

    @Test
    fun `coerces availableChunks within bounds`() {
        // maxTokens small => availableChunks = floor(200/250)=0 -> coerced to 1
        val minCoerced = ChunkSizingUtil.calculateAdaptiveChunkSize(totalTitles = 20, maxTokens = 200, baseChunk = 8)
        assertEquals(1, minCoerced)

        // maxTokens large => availableChunks = floor(5000/250)=20 -> coerced to baseChunk=8
        val maxCoerced = ChunkSizingUtil.calculateAdaptiveChunkSize(totalTitles = 20, maxTokens = 5000, baseChunk = 8)
        assertEquals(8, maxCoerced)
    }

    @Test
    fun `respects baseChunk when titles exceed it`() {
        // totalTitles > baseChunk, enough tokens for 4
        val actual = ChunkSizingUtil.calculateAdaptiveChunkSize(totalTitles = 50, maxTokens = 1000, baseChunk = 6)
        // 1000/250 = 4, within 1..6
        assertEquals(4, actual)
    }
}
