package com.stralgo.intraday.handler;

import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;
import com.stralgo.analysis.DerivedMetrics;
import com.stralgo.analysis.RollingWindow;
import com.stralgo.intraday.event.DisruptorTickEvent;
import com.stralgo.intraday.event.SignalEvent;
import com.stralgo.intraday.pipeline.TickEventPipeline;
import com.stralgo.market.Candle;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * Disruptor EventHandler that detects trading signals from market state
 * and publishes them into a signal ring buffer.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Reads rolling window market state from pipeline</li>
 *   <li>Derives signal conditions from market metrics</li>
 *   <li>Publishes SignalEvent into second Disruptor ring buffer</li>
 * </ul>
 *
 * <p>Signal detection is deterministic and allocation-free in the hot path.
 * Uses pre-allocated ring buffer events to avoid GC pressure.</p>
 *
 * <p>Constraints:
 * <ul>
 *   <li>No blocking operations</li>
 *   <li>No allocation in hot path</li>
 *   <li>Deterministic behavior only</li>
 * </ul>
 *
 * <p>Single-threaded event processing. Executes after {@link MarketStateHandler}
 * completes market state updates.</p>
 */
public final class SignalDetector implements EventHandler<DisruptorTickEvent> {

    private final TickEventPipeline pipeline;
    private final RingBuffer<SignalEvent> signalRingBuffer;

    /**
     * Create a signal detector with the given pipeline and signal ring buffer.
     *
     * @param pipeline the pipeline with market state (must not be null)
     * @param signalRingBuffer the ring buffer for publishing signals (must not be null)
     */
    public SignalDetector(TickEventPipeline pipeline, RingBuffer<SignalEvent> signalRingBuffer) {
        this.pipeline = Objects.requireNonNull(pipeline, "pipeline");
        this.signalRingBuffer = Objects.requireNonNull(signalRingBuffer, "signalRingBuffer");
    }

    /**
     * Handle a tick event, detecting signals and publishing to signal ring buffer.
     *
     * <p>Algorithm:
     * <ol>
     *   <li>Extract symbol from ring buffer event</li>
     *   <li>Get rolling window for the symbol from pipeline</li>
     *   <li>Compute derived metrics from rolling window candles</li>
     *   <li>Detect signal conditions</li>
     *   <li>Publish SignalEvent to signal ring buffer if condition met</li>
     * </ol>
     *
     * <p>No allocation occurs in hot path; ring buffer events are pre-allocated.</p>
     *
     * @param event the ring buffer event
     * @param sequence the ring buffer sequence
     * @param endOfBatch true if last event in batch (ignored)
     * @throws Exception if signal detection or publishing fails
     */
    @Override
    public void onEvent(DisruptorTickEvent event, long sequence, boolean endOfBatch) throws Exception {
        Objects.requireNonNull(event, "event");

        // Extract the symbol from the ring buffer event
        String symbol = event.symbol;

        if (symbol == null || symbol.isEmpty()) {
            // No symbol; skip signal detection
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
            // No candles in window yet; skip signal detection
            return;
        }

        // Detect signals from the rolling window
        detectAndPublishSignal(symbol, candles, event.price, System.nanoTime());
    }

    /**
     * Detect signal conditions and publish SignalEvent if triggered.
     *
     * <p>Signal detection logic:
     * <ul>
     *   <li>Computes derived metrics from rolling window</li>
     *   <li>Evaluates signal conditions (deterministic)</li>
     *   <li>Publishes to signal ring buffer if condition met</li>
     * </ul>
     *
     * <p>This method demonstrates signal detection without actual trading strategy.
     * Real strategies would be plugged in here.</p>
     *
     * @param symbol the instrument symbol
     * @param candles the candles in the rolling window
     * @param currentPrice the current price from the tick
     * @param timestampNanos the signal generation timestamp
     */
    private void detectAndPublishSignal(String symbol, List<Candle> candles, long currentPrice, long timestampNanos) {
        try {
            // Compute derived metrics from the rolling window
            BigDecimal highestHigh = DerivedMetrics.highestHigh(candles);
            BigDecimal lowestLow = DerivedMetrics.lowestLow(candles);
            BigDecimal range = DerivedMetrics.range(candles);
            long totalVolume = DerivedMetrics.totalVolume(candles);
            BigDecimal avgClose = DerivedMetrics.averageClose(candles);

                // Determine signal type based on simple conditions
            // NOTE: This is placeholder logic for demonstration
            // Real signal strategies would be implemented here
            int signalType = determineSignalType(currentPrice, avgClose, range);

            // Only publish if there's an actual signal (not NONE)
            if (signalType != SignalEvent.SIGNAL_NONE) {
                // Calculate signal strength (placeholder: always 5000 basis points = 50%)
                int strength = 5000;

                // Read rolling average volume from pipeline's volume tracker (if available)
                long rollingAvg = 0L;
                try {
                    var vt = pipeline.getVolumeTracker(symbol);
                    if (vt != null) {
                        rollingAvg = Math.round(vt.getAverage());
                    }
                } catch (Exception ex) {
                    // Defensive: signal detection should not fail due to missing tracker
                    rollingAvg = 0L;
                }

                // Publish signal to the ring buffer (zero allocation)
                publishSignal(symbol, signalType, strength, currentPrice, timestampNanos,
                        highestHigh.longValue(), lowestLow.longValue(), range.longValue(),
                        totalVolume, rollingAvg, avgClose.longValue());
            }

        } catch (IllegalArgumentException iae) {
            // If candles list is empty, DerivedMetrics throws.
            // Gracefully skip signal detection if it occurs.
        }
    }

    /**
     * Determine signal type based on market conditions.
     *
     * <p>Placeholder logic for demonstration:
     * <ul>
     *   <li>BUY if price is below average close</li>
     *   <li>SELL if price is above average close</li>
     *   <li>NONE otherwise</li>
     * </ul>
     *
     * <p>Real trading strategies would replace this with sophisticated analysis.</p>
     *
     * @param currentPrice current tick price
     * @param avgClose average close from rolling window
     * @param range price range
     * @return signal type (SIGNAL_NONE, SIGNAL_BUY, SIGNAL_SELL)
     */
    private int determineSignalType(long currentPrice, BigDecimal avgClose, BigDecimal range) {
        // Simple mean reversion strategy (demonstration only)
        BigDecimal priceBD = new BigDecimal(currentPrice);

        // Only signal if we have meaningful range (avoid division by zero scenarios)
        if (range.compareTo(BigDecimal.ZERO) <= 0) {
            return SignalEvent.SIGNAL_NONE;
        }

        // If current price is significantly below average, consider BUY
        // If current price is significantly above average, consider SELL
        if (priceBD.compareTo(avgClose) < 0) {
            return SignalEvent.SIGNAL_BUY;
        } else if (priceBD.compareTo(avgClose) > 0) {
            return SignalEvent.SIGNAL_SELL;
        }

        return SignalEvent.SIGNAL_NONE;
    }

    /**
     * Publish signal to the signal ring buffer (zero allocation).
     *
     * <p>Uses Disruptor's publish pattern:
     * <ol>
     *   <li>Claim next sequence</li>
     *   <li>Get pre-allocated event</li>
     *   <li>Populate event fields</li>
     *   <li>Publish sequence</li>
     * </ol>
     *
     * @param symbol instrument symbol
     * @param signalType signal type
     * @param strength signal strength
     * @param price reference price
     * @param timestampNanos generation timestamp
     * @param highestHigh highest high
     * @param lowestLow lowest low
     * @param range price range
     * @param totalVolume total volume
     * @param avgClose average close
     */
    private void publishSignal(String symbol, int signalType, int strength, long price, long timestampNanos,
                                long highestHigh, long lowestLow, long range, long totalVolume, long rollingAvgVolume, long avgClose) {
        // Claim the next available sequence in the signal ring buffer
        long sequence = signalRingBuffer.next();

        try {
            // Get the pre-allocated event at the claimed sequence (no allocation)
            SignalEvent signalEvent = signalRingBuffer.get(sequence);

            // Populate the signal event with detected signal data
            signalEvent.set(symbol, signalType, strength, price, timestampNanos,
                    highestHigh, lowestLow, range, totalVolume, rollingAvgVolume, avgClose);

        } finally {
            // Always publish the sequence to signal the handler
            signalRingBuffer.publish(sequence);
        }
    }
}

