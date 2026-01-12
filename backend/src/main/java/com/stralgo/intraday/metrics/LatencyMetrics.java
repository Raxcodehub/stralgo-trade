package com.stralgo.intraday.metrics;

// Collects latency measurements for TickEvents
// Provides simple aggregate statistics
//
// Tracks:
// ingest latency = receive − market
// processing latency = processed − receive
// end-to-end latency = processed − market
//
// Start stupid-simple:
// count
// min
// max
// average
//
// No histograms yet. No percentiles. We’re measuring existence, not excellence.

import com.stralgo.intraday.event.TickEvent;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Thread-safe, tiny collector of latency aggregates for TickEvent objects.
 *
 * Design notes:
 * - Stores values in nanoseconds for precision, exposes Duration to callers.
 * - Keeps count, min, max and total (for average) for three latency types.
 * - "Stupid-simple" API: record events and query aggregates as Optionals.
 */
public final class LatencyMetrics {
    private final Object lock = new Object();

    // ingest: receive - market
    private long ingestCount;
    private long ingestMinNanos = Long.MAX_VALUE;
    private long ingestMaxNanos;
    private long ingestTotalNanos;

    // processing: processed - receive
    private long processingCount;
    private long processingMinNanos = Long.MAX_VALUE;
    private long processingMaxNanos;
    private long processingTotalNanos;

    // end-to-end: processed - market
    private long endToEndCount;
    private long endToEndMinNanos = Long.MAX_VALUE;
    private long endToEndMaxNanos;
    private long endToEndTotalNanos;

    public LatencyMetrics() {
        // empty
    }

    /**
     * Record latencies for a TickEvent. Requires marketTime and receiveTime to be non-null.
     * processedTime is optional; if absent only the ingest latency is recorded.
     *
     * Throws IllegalArgumentException if receiveTime is before marketTime or processedTime is before receiveTime.
     */
    public void record(TickEvent event) {
        Objects.requireNonNull(event, "event");
        Instant market = Objects.requireNonNull(event.marketTime(), "event.marketTime");
        Instant receive = Objects.requireNonNull(event.receiveTime(), "event.receiveTime");

        long ingestNanos = Duration.between(market, receive).toNanos();
        if (ingestNanos < 0) {
            throw new IllegalArgumentException("receiveTime is before marketTime");
        }

        synchronized (lock) {
            ingestCount++;
            ingestTotalNanos += ingestNanos;
            if (ingestNanos < ingestMinNanos) ingestMinNanos = ingestNanos;
            if (ingestNanos > ingestMaxNanos) ingestMaxNanos = ingestNanos;
        }

        Instant processed = event.processedTime();
        if (processed != null) {
            long processingNanos = Duration.between(receive, processed).toNanos();
            if (processingNanos < 0) {
                throw new IllegalArgumentException("processedTime is before receiveTime");
            }

            long e2eNanos = Duration.between(market, processed).toNanos();
            if (e2eNanos < 0) {
                throw new IllegalArgumentException("processedTime is before marketTime");
            }

            synchronized (lock) {
                // processing
                processingCount++;
                processingTotalNanos += processingNanos;
                if (processingNanos < processingMinNanos) processingMinNanos = processingNanos;
                if (processingNanos > processingMaxNanos) processingMaxNanos = processingNanos;

                // end-to-end
                endToEndCount++;
                endToEndTotalNanos += e2eNanos;
                if (e2eNanos < endToEndMinNanos) endToEndMinNanos = e2eNanos;
                if (e2eNanos > endToEndMaxNanos) endToEndMaxNanos = e2eNanos;
            }
        }
    }

    // --- ingest queries ---

    public long ingestCount() {
        synchronized (lock) {
            return ingestCount;
        }
    }

    public Optional<Duration> ingestMin() {
        synchronized (lock) {
            return ingestCount == 0 ? Optional.empty() : Optional.of(Duration.ofNanos(ingestMinNanos));
        }
    }

    public Optional<Duration> ingestMax() {
        synchronized (lock) {
            return ingestCount == 0 ? Optional.empty() : Optional.of(Duration.ofNanos(ingestMaxNanos));
        }
    }

    public Optional<Duration> ingestAverage() {
        synchronized (lock) {
            return ingestCount == 0 ? Optional.empty() : Optional.of(Duration.ofNanos(ingestTotalNanos / ingestCount));
        }
    }

    // --- processing queries ---

    public long processingCount() {
        synchronized (lock) {
            return processingCount;
        }
    }

    public Optional<Duration> processingMin() {
        synchronized (lock) {
            return processingCount == 0 ? Optional.empty() : Optional.of(Duration.ofNanos(processingMinNanos));
        }
    }

    public Optional<Duration> processingMax() {
        synchronized (lock) {
            return processingCount == 0 ? Optional.empty() : Optional.of(Duration.ofNanos(processingMaxNanos));
        }
    }

    public Optional<Duration> processingAverage() {
        synchronized (lock) {
            return processingCount == 0 ? Optional.empty() : Optional.of(Duration.ofNanos(processingTotalNanos / processingCount));
        }
    }

    // --- end-to-end queries ---

    public long endToEndCount() {
        synchronized (lock) {
            return endToEndCount;
        }
    }

    public Optional<Duration> endToEndMin() {
        synchronized (lock) {
            return endToEndCount == 0 ? Optional.empty() : Optional.of(Duration.ofNanos(endToEndMinNanos));
        }
    }

    public Optional<Duration> endToEndMax() {
        synchronized (lock) {
            return endToEndCount == 0 ? Optional.empty() : Optional.of(Duration.ofNanos(endToEndMaxNanos));
        }
    }

    public Optional<Duration> endToEndAverage() {
        synchronized (lock) {
            return endToEndCount == 0 ? Optional.empty() : Optional.of(Duration.ofNanos(endToEndTotalNanos / endToEndCount));
        }
    }

    /**
     * Clear all recorded aggregates.
     */
    public void reset() {
        synchronized (lock) {
            ingestCount = 0;
            ingestMinNanos = Long.MAX_VALUE;
            ingestMaxNanos = 0;
            ingestTotalNanos = 0;

            processingCount = 0;
            processingMinNanos = Long.MAX_VALUE;
            processingMaxNanos = 0;
            processingTotalNanos = 0;

            endToEndCount = 0;
            endToEndMinNanos = Long.MAX_VALUE;
            endToEndMaxNanos = 0;
            endToEndTotalNanos = 0;
        }
    }

    @Override
    public String toString() {
        synchronized (lock) {
            return "LatencyMetrics{" +
                    "ingestCount=" + ingestCount +
                    ", ingestMin=" + (ingestCount == 0 ? "-" : Duration.ofNanos(ingestMinNanos)) +
                    ", ingestMax=" + (ingestCount == 0 ? "-" : Duration.ofNanos(ingestMaxNanos)) +
                    ", ingestAvg=" + (ingestCount == 0 ? "-" : Duration.ofNanos(ingestTotalNanos / ingestCount)) +
                    ", processingCount=" + processingCount +
                    ", processingMin=" + (processingCount == 0 ? "-" : Duration.ofNanos(processingMinNanos)) +
                    ", processingMax=" + (processingCount == 0 ? "-" : Duration.ofNanos(processingMaxNanos)) +
                    ", processingAvg=" + (processingCount == 0 ? "-" : Duration.ofNanos(processingTotalNanos / processingCount)) +
                    ", endToEndCount=" + endToEndCount +
                    ", endToEndMin=" + (endToEndCount == 0 ? "-" : Duration.ofNanos(endToEndMinNanos)) +
                    ", endToEndMax=" + (endToEndCount == 0 ? "-" : Duration.ofNanos(endToEndMaxNanos)) +
                    ", endToEndAvg=" + (endToEndCount == 0 ? "-" : Duration.ofNanos(endToEndTotalNanos / endToEndCount)) +
                    '}';
        }
    }
}

