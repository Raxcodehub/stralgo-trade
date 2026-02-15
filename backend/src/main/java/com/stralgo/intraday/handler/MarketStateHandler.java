package com.stralgo.intraday.handler;

import com.lmax.disruptor.EventHandler;
import com.stralgo.intraday.event.DisruptorTickEvent;
import com.stralgo.intraday.event.TickEvent;
import com.stralgo.intraday.pipeline.TickEventPipeline;
import com.stralgo.market.Tick;

import java.time.Instant;
import java.util.Objects;

/**
 * Disruptor EventHandler that builds market state from tick events.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Converts mutable ring buffer events to immutable domain objects</li>
 *   <li>Feeds ticks through candle aggregation</li>
 *   <li>Updates rolling analysis windows with completed candles</li>
 *   <li>Marks processing completion time</li>
 * </ul>
 *
 * <p>The handler preserves all existing pipeline behavior without introducing
 * signal logic, allocation, or concurrency concerns.</p>
 *
 * <p>Single-threaded, deterministic, no side effects beyond state updates.</p>
 */
public final class MarketStateHandler implements EventHandler<DisruptorTickEvent> {

    private final TickEventPipeline pipeline;

    /**
     * Create a handler with the given pipeline for market state building.
     *
     * @param pipeline the pipeline that aggregates candles and updates rolling metrics
     */
    public MarketStateHandler(TickEventPipeline pipeline) {
        this.pipeline = Objects.requireNonNull(pipeline, "pipeline");
    }

    /**
     * Handle a tick event, building market state.
     *
     * <p>Algorithm:
     * <ol>
     *   <li>Extract tick from ring buffer event fields</li>
     *   <li>Reconstruct immutable TickEvent with timing metadata</li>
     *   <li>Delegate to pipeline (candles + rolling metrics)</li>
     *   <li>Update processed time in ring buffer event</li>
     * </ol>
     *
     * <p>No allocation occurs beyond tick/event reconstruction.</p>
     *
     * @param event the ring buffer event
     * @param sequence the ring buffer sequence
     * @param endOfBatch true if last event in batch (ignored)
     * @throws Exception if pipeline processing fails
     */
    @Override
    public void onEvent(DisruptorTickEvent event, long sequence, boolean endOfBatch) throws Exception {
        Objects.requireNonNull(event, "event");

        // Reconstruct the original Tick from ring buffer fields
        Tick tick = Tick.of(
            event.symbol,
            new java.math.BigDecimal(event.price),
            event.quantity,
            Instant.ofEpochSecond(0, event.marketTimeNanos)
        );

        // Reconstruct ingest time (when event was published)
        Instant receiveTime = Instant.ofEpochSecond(0, event.ingestTimeNanos);

        // Create immutable TickEvent for the pipeline
        TickEvent tickEvent = TickEvent.ingest(tick, receiveTime);

        // Delegate to pipeline:
        // - Feeds tick into CandleAggregator
        // - Updates rolling windows with completed candles
        // - Derives metrics from rolling windows
        pipeline.onEvent(tickEvent);

        // Update processed time in ring buffer event for latency measurement
        event.setProcessedTime(System.nanoTime());
    }
}

