package com.stralgo.intraday.ingest;

import com.lmax.disruptor.RingBuffer;
import com.stralgo.intraday.event.DisruptorTickEvent;
import com.stralgo.market.Tick;

import java.util.Objects;

/**
 * Publishes ticks into the Disruptor ring buffer with nanosecond-precision ingest time capture.
 *
 * <p>Zero-allocation publisher: no intermediate objects created on the hot path.
 * Ingest time is captured via {@link System#nanoTime()} immediately before publishing.</p>
 *
 * <p>Always publishes the ring buffer sequence to ensure event persistence.</p>
 */
public final class TickPublisher {

    private final RingBuffer<DisruptorTickEvent> ringBuffer;

    /**
     * Create a publisher for the given ring buffer.
     *
     * @param ringBuffer the Disruptor ring buffer (must not be null)
     */
    public TickPublisher(RingBuffer<DisruptorTickEvent> ringBuffer) {
        this.ringBuffer = Objects.requireNonNull(ringBuffer, "ringBuffer");
    }

    /**
     * Publish a tick into the ring buffer, capturing ingest time.
     *
     * <p>Algorithm:
     * <ol>
     *   <li>Claim the next sequence from the ring buffer</li>
     *   <li>Capture ingest time via {@link System#nanoTime()}</li>
     *   <li>Get the event at the claimed sequence</li>
     *   <li>Populate the event with tick data and ingest time</li>
     *   <li>Publish the sequence (signal handler)</li>
     * </ol>
     *
     * <p>No allocation occurs on the hot path: the ring buffer pre-allocates
     * all {@code DisruptorTickEvent} objects, and this method reuses them.</p>
     *
     * @param tick the tick to publish (must not be null)
     * @return the ring buffer sequence of the published event
     */
    public long publishTick(Tick tick) {
        Objects.requireNonNull(tick, "tick");

        // Claim the next available sequence in the ring buffer
        long sequence = ringBuffer.next();

        try {
            // Capture ingest time in nanoseconds immediately after claiming
            long ingestTimeNanos = System.nanoTime();

            // Get the event at the claimed sequence (no allocation; pre-allocated by factory)
            DisruptorTickEvent event = ringBuffer.get(sequence);

            // Populate the event with tick data and ingest time
            // processedTimeNanos is set to 0 initially; the handler will update it
            event.set(tick, ingestTimeNanos, 0L);

            return sequence;
        } finally {
            // Always publish the sequence to signal the handler
            ringBuffer.publish(sequence);
        }
    }
}

