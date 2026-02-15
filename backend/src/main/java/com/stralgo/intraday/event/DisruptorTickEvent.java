package com.stralgo.intraday.event;

import com.stralgo.market.Tick;

/**
 * Ring buffer event for LMAX Disruptor.
 *
 * <p>Optimized for low-latency, zero-allocation tick processing.
 * All fields are mutable to enable ring buffer reuse without allocation.</p>
 *
 * <p>This event captures:
 * <ul>
 *   <li>symbol – instrument identifier</li>
 *   <li>price – traded price in smallest unit (long, e.g., cents or pips)</li>
 *   <li>quantity – trade volume</li>
 *   <li>marketTimeNanos – market timestamp in nanoseconds since epoch</li>
 *   <li>ingestTimeNanos – system ingestion time in nanoseconds since epoch</li>
 *   <li>processedTimeNanos – pipeline completion time in nanoseconds since epoch</li>
 * </ul>
 *
 * <p>Fields are public mutable to eliminate allocation overhead in hot paths.
 * No defensive copying or wrapping – caller is responsible for correct usage.</p>
 */
public final class DisruptorTickEvent {

    public String symbol;
    public long price;         // price in smallest unit (no BigDecimal allocation)
    public long quantity;
    public long marketTimeNanos;
    public long ingestTimeNanos;
    public long processedTimeNanos;

    /**
     * Default constructor. All fields initialized to zero/null.
     * The Disruptor factory will instantiate this once per ring buffer slot.
     */
    public DisruptorTickEvent() {
        this.symbol = null;
        this.price = 0L;
        this.quantity = 0L;
        this.marketTimeNanos = 0L;
        this.ingestTimeNanos = 0L;
        this.processedTimeNanos = 0L;
    }

    /**
     * Set all event data from a Tick and capture ingest/processed times.
     *
     * @param tick the market tick
     * @param ingestTimeNanos ingest time in nanoseconds
     * @param processedTimeNanos processed time in nanoseconds (may be 0 if not yet set)
     */
    public void set(Tick tick, long ingestTimeNanos, long processedTimeNanos) {
        this.symbol = tick.symbol();
        this.price = tick.price().longValue();  // assumes price stored as long unit
        this.quantity = tick.quantity();
        this.marketTimeNanos = tick.timestamp().getNano() + (tick.timestamp().getEpochSecond() * 1_000_000_000L);
        this.ingestTimeNanos = ingestTimeNanos;
        this.processedTimeNanos = processedTimeNanos;
    }

    /**
     * Reset event to initial state for ring buffer reuse.
     * Called by Disruptor between publishing events.
     */
    public void reset() {
        this.symbol = null;
        this.price = 0L;
        this.quantity = 0L;
        this.marketTimeNanos = 0L;
        this.ingestTimeNanos = 0L;
        this.processedTimeNanos = 0L;
    }

    /**
     * Update processed time (captured at pipeline exit).
     *
     * @param processedTimeNanos processed time in nanoseconds
     */
    public void setProcessedTime(long processedTimeNanos) {
        this.processedTimeNanos = processedTimeNanos;
    }

    /**
     * Compute ingest latency in nanoseconds.
     * Ingest latency = ingestTimeNanos - marketTimeNanos
     *
     * @return latency in nanoseconds
     */
    public long ingestLatencyNanos() {
        return ingestTimeNanos - marketTimeNanos;
    }

    /**
     * Compute processing latency in nanoseconds.
     * Processing latency = processedTimeNanos - ingestTimeNanos
     *
     * @return latency in nanoseconds
     */
    public long processingLatencyNanos() {
        return processedTimeNanos - ingestTimeNanos;
    }

    /**
     * Compute end-to-end latency in nanoseconds.
     * End-to-end latency = processedTimeNanos - marketTimeNanos
     *
     * @return latency in nanoseconds
     */
    public long endToEndLatencyNanos() {
        return processedTimeNanos - marketTimeNanos;
    }

    @Override
    public String toString() {
        return "DisruptorTickEvent{" +
                "symbol='" + symbol + '\'' +
                ", price=" + price +
                ", quantity=" + quantity +
                ", marketTimeNanos=" + marketTimeNanos +
                ", ingestTimeNanos=" + ingestTimeNanos +
                ", processedTimeNanos=" + processedTimeNanos +
                '}';
    }
}

