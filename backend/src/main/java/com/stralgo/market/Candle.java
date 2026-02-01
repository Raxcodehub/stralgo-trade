package com.stralgo.market;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * Represents a completed OHLCV candle.
 *
 * <p>A {@code Candle} is a derived market fact produced by aggregating
 * multiple {@link Tick} instances over a fixed time interval.</p>
 *
 * <p>This class contains no aggregation logic; it is a pure data holder.</p>
 *
 * <p>Each candle includes:
 * <ul>
 *   <li>Symbol - instrument</li>
 *   <li>Open price (first tick)</li>
 *   <li>High price</li>
 *   <li>Low price</li>
 *   <li>Close price (last tick)</li>
 *   <li>Total traded volume</li>
 * </ul>
 *
 * <p>Candles are immutable once created.</p>
 */
public record Candle(
        String symbol,
        Instant startTime,
        BigDecimal open,
        BigDecimal high,
        BigDecimal low,
        BigDecimal close,
        long volume
) {
    // Compact constructor for validation
    public Candle {
        Objects.requireNonNull(symbol, "symbol");
        Objects.requireNonNull(startTime, "startTime");
        Objects.requireNonNull(open, "open");
        Objects.requireNonNull(high, "high");
        Objects.requireNonNull(low, "low");
        Objects.requireNonNull(close, "close");

        symbol = symbol.trim();
        if (symbol.isEmpty()) {
            throw new IllegalArgumentException("symbol must not be empty");
        }

        // startTime must be aligned to minute boundary
        if (startTime.getEpochSecond() % 60 != 0 || startTime.getNano() != 0) {
            throw new IllegalArgumentException("startTime must be aligned to minute boundary (seconds and nanos == 0)");
        }

        if (open.compareTo(BigDecimal.ZERO) < 0
                || high.compareTo(BigDecimal.ZERO) < 0
                || low.compareTo(BigDecimal.ZERO) < 0
                || close.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("prices must be >= 0");
        }

        if (high.compareTo(low) < 0) {
            throw new IllegalArgumentException("high must be >= low");
        }

        if (volume < 0) {
            throw new IllegalArgumentException("volume must be >= 0");
        }
    }

    /**
     * Static factory for clarity and future extensibility.
     */
    public static Candle of(String symbol,
                            Instant startTime,
                            BigDecimal open,
                            BigDecimal high,
                            BigDecimal low,
                            BigDecimal close,
                            long volume) {
        return new Candle(symbol, startTime, open, high, low, close, volume);
    }
}
