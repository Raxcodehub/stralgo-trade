package com.stralgo.intraday.event;

import com.stralgo.market.Tick;

import java.time.Instant;
import java.util.Objects;

/**
 * Wraps a {@link Tick} with system timing metadata.
 *
 * <p>{@code TickEvent} separates market time from system processing time,
 * enabling accurate latency measurement without modifying domain objects.</p>
 *
 * <p>Timestamps captured include:
 * <ul>
 *   <li>Market time – when the event occurred in the market</li>
 *   <li>Receive time – when the system received the event</li>
 *   <li>Processed time – when processing completed</li>
 * </ul>
 *
 * <p>This class is used as the unit of measurement for latency analysis.</p>
 */
public final class TickEvent {
    private final Tick tick;
    private final Instant marketTime;
    private final Instant receiveTime;
    private final Instant processedTime; // may be null until set at pipeline exit

    private TickEvent(Tick tick, Instant marketTime, Instant receiveTime, Instant processedTime) {
        this.tick = Objects.requireNonNull(tick, "tick");
        this.marketTime = Objects.requireNonNull(marketTime, "marketTime");
        this.receiveTime = Objects.requireNonNull(receiveTime, "receiveTime");
        this.processedTime = processedTime; // nullable until pipeline exit
    }

    /**
     * Create a TickEvent at ingestion time. marketTime is derived from the tick.
     * Assumes Tick exposes a `time()` method returning Instant.
     */
    public static TickEvent ingest(Tick tick, Instant receiveTime) {
        // marketTime comes from the tick
        Instant marketTime = tick.timestamp(); // expected method on Tick
        return new TickEvent(tick, marketTime, receiveTime, null);
    }

    /**
     * Return a new TickEvent with processedTime set (pipeline exit).
     */
    public TickEvent withProcessedTime(Instant processedTime) {
        return new TickEvent(this.tick, this.marketTime, this.receiveTime, Objects.requireNonNull(processedTime));
    }

    public Tick tick() {
        return tick;
    }

    public Instant marketTime() {
        return marketTime;
    }

    public Instant receiveTime() {
        return receiveTime;
    }

    /**
     * May be null if processing hasn't completed yet.
     */
    public Instant processedTime() {
        return processedTime;
    }

    @Override
    public String toString() {
        return "TickEvent{" +
                "tick=" + tick +
                ", marketTime=" + marketTime +
                ", receiveTime=" + receiveTime +
                ", processedTime=" + processedTime +
                '}';
    }
}

