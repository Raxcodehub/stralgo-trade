package com.stralgo.market;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * Represents a single market tick (atomic market event).
 * Note: This will be change heavily when it Zerodha integrated.
 *
 * <p>A {@code Tick} captures raw market information exactly as it occurred,
 * without any system-specific timing or processing metadata.</p>
 *
 * <p>This class is immutable and represents market truth.</p>
 *
 * <p>Fields typically include:
 * <ul>
 *   <li>Symbol / instrument identifier</li>
 *   <li>Traded price</li>
 *   <li>Traded quantity</li>
 *   <li>Market timestamp</li>
 * </ul>
 *
 * <p>This object must not be modified once created.</p>
 */

public record Tick(String symbol, BigDecimal price, long quantity, Instant timestamp) {
    // Compact canonical constructor for validation
    public Tick {
        Objects.requireNonNull(symbol, "symbol");
        Objects.requireNonNull(price, "price");
        Objects.requireNonNull(timestamp, "timestamp");

        symbol = symbol.trim();
        if (symbol.isEmpty()) {
            throw new IllegalArgumentException("symbol must not be empty");
        }
        if (price.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("price must be >= 0");
        }
        if (quantity < 0) {
            throw new IllegalArgumentException("quantity must be >= 0");
        }
    }

    /**
     * Static factory for clarity and future extensibility.
     */
    public static Tick of(String symbol, BigDecimal price, long quantity, Instant timestamp) {
        return new Tick(symbol, price, quantity, timestamp);
    }
}
