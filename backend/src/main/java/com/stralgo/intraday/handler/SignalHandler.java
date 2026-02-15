package com.stralgo.intraday.handler;

import com.lmax.disruptor.EventHandler;
import com.stralgo.analysis.DerivedMetrics;
import com.stralgo.analysis.RollingWindow;
import com.stralgo.intraday.event.DisruptorTickEvent;
import com.stralgo.intraday.pipeline.TickEventPipeline;
import com.stralgo.market.Candle;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * Disruptor EventHandler that derives trading signals from market state.
 *
 * <p>Reads the rolling window candle data built by {@link MarketStateHandler}
 * and derives deterministic signals without placing orders.</p>
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Read rolling window from pipeline for the current symbol</li>
 *   <li>Compute derived metrics (range, volume, averages)</li>
 *   <li>Produce deterministic signal observations (no execution)</li>
 * </ul>
 *
 * <p>Constraints:
 * <ul>
 *   <li>No order placement</li>
 *   <li>No blocking operations</li>
 *   <li>No allocation in hot path</li>
 *   <li>Deterministic behavior only</li>
 * </ul>
 *
 * <p>Single-threaded event processing. Executes after {@link MarketStateHandler}
 * completes market state updates.</p>
 */
public final class SignalHandler implements EventHandler<DisruptorTickEvent> {

    private final TickEventPipeline pipeline;

    /**
     * Create a signal handler with the given pipeline.
     *
     * <p>The pipeline provides access to rolling windows that contain
     * the aggregated candles and market state.</p>
     *
     * @param pipeline the pipeline with market state (must not be null)
     */
    public SignalHandler(TickEventPipeline pipeline) {
        this.pipeline = Objects.requireNonNull(pipeline, "pipeline");
    }

    /**
     * Handle a tick event, deriving signals from current market state.
     *
     * <p>Algorithm:
     * <ol>
     *   <li>Extract symbol from ring buffer event</li>
     *   <li>Get rolling window for the symbol from pipeline</li>
     *   <li>Compute derived metrics from rolling window candles</li>
     *   <li>Store/observe signal data (no order placement)</li>
     * </ol>
     *
     * <p>If no window exists yet (no completed candles), signal derivation is skipped.</p>
     *
     * @param event the ring buffer event
     * @param sequence the ring buffer sequence
     * @param endOfBatch true if last event in batch (ignored)
     * @throws Exception if signal derivation fails
     */
    @Override
    public void onEvent(DisruptorTickEvent event, long sequence, boolean endOfBatch) throws Exception {
        Objects.requireNonNull(event, "event");

        // Extract the symbol from the ring buffer event
        String symbol = event.symbol;

        if (symbol == null || symbol.isEmpty()) {
            // No symbol; skip signal derivation
            return;
        }

        // Get the rolling window for this symbol from the pipeline
        RollingWindow window = pipeline.getWindow(symbol);

        if (window == null) {
            // Window doesn't exist yet; no market state to derive from
            return;
        }

        // Get snapshot of candles in the rolling window
        List<Candle> candles = window.getCandles();

        if (candles.isEmpty()) {
            // No candles in window yet; skip signal derivation
            return;
        }

        // Derive signals from the rolling window
        // These are pure computations; no side effects beyond observation
        deriveSignals(symbol, candles);
    }

    /**
     * Derive deterministic signals from rolling window candles.
     *
     * <p>Computes various metrics such as range, volume, and averages.
     * Results are not stored or acted upon; they are observations only.</p>
     *
     * @param candles the candles in the rolling window
     */
    private void deriveSignals(String symbol, List<Candle> candles) {
        try {
            // Compute derived metrics from the rolling window
            BigDecimal highestHigh = DerivedMetrics.highestHigh(candles);
            BigDecimal lowestLow = DerivedMetrics.lowestLow(candles);
            BigDecimal range = DerivedMetrics.range(candles);
            long totalVolume = DerivedMetrics.totalVolume(candles);
            BigDecimal avgClose = DerivedMetrics.averageClose(candles);

            // Signal derivation complete; metrics have been computed.
            // In a real system, these observations would be stored or used to
            // generate actionable signals. For now, we ensure they are not eliminated
            // by dead-code optimization.
            if (highestHigh == null || lowestLow == null ||
                totalVolume < 0 || avgClose == null) {
                throw new AssertionError("unreachable");
            }

            // No order placement, no blocking, no state mutation beyond this method.
        } catch (IllegalArgumentException iae) {
            // If candles list is empty, DerivedMetrics throws.
            // Gracefully skip signal derivation if it occurs.
        }
    }
}

