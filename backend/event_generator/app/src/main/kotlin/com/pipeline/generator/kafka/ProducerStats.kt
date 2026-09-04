package com.pipeline.generator.kafka

import java.util.concurrent.atomic.AtomicLong

data class Snapshot(
    val sent: Long,
    val failed: Long,
)

class ProducerStats {
    private val sent = AtomicLong(0)
    private val failed = AtomicLong(0)

    fun recordSent() {
        sent.incrementAndGet()
    }
    fun recordFailed() {
        failed.incrementAndGet()
    }
    fun snapshot() = Snapshot(sent = sent.get(), failed = failed.get())
}
