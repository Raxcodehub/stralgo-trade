package com.stralgo.analysis;

import com.stralgo.market.Candle;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Computes simple derived metrics from collections of {@link Candle}s.
 *
 * <p>{@code DerivedMetrics} provides stateless, pure functions that calculate
 * commonly used market measurements such as range, volume, and averages.</p>
 *
 * <p>This class:
 * <ul>
 *   <li>Maintains no internal state</li>
 *   <li>Performs no trading decisions</li>
 *   <li>Is safe for replay and backtesting</li>
 * </ul>
 */

public final class DerivedMetrics {

    private DerivedMetrics() {
        // Utility class
    }

    /**
     * Returns the highest high from the list of candles.
     */
    public static BigDecimal highestHigh(List<Candle> candles) {
        if (candles.isEmpty()) {
            throw new IllegalArgumentException("candles list must not be empty");
        }
        return candles.stream()
                .map(Candle::high)
                .max(BigDecimal::compareTo)
                .orElseThrow();
    }

    /**
     * Returns the lowest low from the list of candles.
     */
    public static BigDecimal lowestLow(List<Candle> candles) {
        if (candles.isEmpty()) {
            throw new IllegalArgumentException("candles list must not be empty");
        }
        return candles.stream()
                .map(Candle::low)
                .min(BigDecimal::compareTo)
                .orElseThrow();
    }

    /**
     * Returns the total volume from the list of candles.
     */
    public static long totalVolume(List<Candle> candles) {
        return candles.stream()
                .mapToLong(Candle::volume)
                .sum();
    }

    /**
     * Returns the average close from the list of candles.
     */
    public static BigDecimal averageClose(List<Candle> candles) {
        if (candles.isEmpty()) {
            throw new IllegalArgumentException("candles list must not be empty");
        }
        BigDecimal sum = candles.stream()
                .map(Candle::close)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(candles.size()), 8, RoundingMode.HALF_UP);
    }

    /**
     * Returns the range (highest high - lowest low) from the list of candles.
     */
    public static BigDecimal range(List<Candle> candles) {
        if (candles.isEmpty()) {
            throw new IllegalArgumentException("candles list must not be empty");
        }
        BigDecimal high = highestHigh(candles);
        BigDecimal low = lowestLow(candles);
        return high.subtract(low);
    }
}
