package com.cleanova.app.domain

data class MediaTally(val count: Long, val knownBytes: Long, val unknownSizes: Long, val overflow: Boolean)

/** Constant-memory aggregation of metadata. Unknown bytes are not represented as measured zero. */
class MediaAccumulator {
    private var count = 0L
    private var knownBytes = 0L
    private var unknownSizes = 0L
    private var overflow = false

    fun record(bytes: Long?) {
        count++
        if (bytes == null || bytes < 0L) {
            unknownSizes++
        } else if (Long.MAX_VALUE - knownBytes < bytes) {
            knownBytes = Long.MAX_VALUE
            overflow = true
        } else {
            knownBytes += bytes
        }
    }

    fun snapshot(): MediaTally = MediaTally(count, knownBytes, unknownSizes, overflow)
}
