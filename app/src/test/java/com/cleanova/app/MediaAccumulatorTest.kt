package com.cleanova.app

import com.cleanova.app.domain.MediaAccumulator
import com.cleanova.app.domain.MediaTally
import org.junit.Assert.assertEquals
import org.junit.Test

class MediaAccumulatorTest {
    @Test fun empty() { assertEquals(MediaTally(0, 0, 0, false), MediaAccumulator().snapshot()) }
    @Test fun unknownSizesAreCountedSeparately() {
        val tally = MediaAccumulator()
        listOf<Long?>(12L, null, -1L, 0L, 8L).forEach(tally::record)
        assertEquals(MediaTally(5, 20, 2, false), tally.snapshot())
    }
    @Test fun largeSyntheticLibrary() {
        val tally = MediaAccumulator()
        repeat(100_000) { tally.record(1_000_000L) }
        assertEquals(MediaTally(100_000, 100_000_000_000, 0, false), tally.snapshot())
    }
    @Test fun overflowDoesNotWrap() {
        val tally = MediaAccumulator()
        tally.record(Long.MAX_VALUE)
        tally.record(10)
        assertEquals(MediaTally(2, Long.MAX_VALUE, 0, true), tally.snapshot())
    }
}
