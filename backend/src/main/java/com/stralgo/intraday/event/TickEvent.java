package com.stralgo.intraday.event;

// Wraps a market Tick with timing metadata
// marketTime: when the event occurred in the market
// receiveTime: when the system received it
// processedTime: when the system finished processing it
//
// Fields:
// Tick tick
// Instant marketTime
// Instant receiveTime
// Instant processedTime
//
// Rules:
// marketTime comes from the tick
// receiveTime is set at ingestion
// processedTime is set at pipeline exit
//
// This class exists only so latency becomes measurable.

import com.stralgo.market.Tick;

import java.time.Instant;
import java.util.Objects;

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

