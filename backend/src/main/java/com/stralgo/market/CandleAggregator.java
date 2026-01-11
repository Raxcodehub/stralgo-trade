package com.stralgo.market;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Aggregates incoming ticks into 1-minute candles.
 * Assumes ticks arrive in time order per symbol.
 * Emits a completed candle when the minute boundary changes.
 * Does not modify past candles.
 */
public final class CandleAggregator {
    // current in-progress candle builders keyed by symbol
    private final Map<String, CandleBuilder> current = new HashMap<>();

    /**
     * Process a tick. If the tick closes the previous minute for its symbol,
     * returns an Optional containing the completed Candle. Otherwise, returns empty.
     */
    public synchronized Optional<Candle> onTick(Tick tick) {
        Objects.requireNonNull(tick, "tick");

        String symbol = Objects.requireNonNull(tick.symbol(), "tick.symbol").trim();
        if (symbol.isEmpty()) {
            throw new IllegalArgumentException("tick.symbol must not be empty");
        }

        Instant tickStart = tick.timestamp().truncatedTo(ChronoUnit.MINUTES);

        CandleBuilder builder = current.get(symbol);
        if (builder == null) {
            // first tick for this symbol
            CandleBuilder b = new CandleBuilder(tickStart);
            b.apply(tick);
            current.put(symbol, b);
            return Optional.empty();
        }

        if (tickStart.equals(builder.startTime)) {
            // same minute, update and continue
            builder.apply(tick);
            return Optional.empty();
        }

        if (tickStart.isBefore(builder.startTime)) {
            // Out-of-order tick (older minute) — caller promised in-order per symbol
            throw new IllegalArgumentException("received out-of-order tick for symbol " + symbol);
        }

        // minute boundary advanced -> emit completed candle for previous minute
        Candle completed = builder.build(symbol);
        // start new builder for the current tick's minute
        CandleBuilder next = new CandleBuilder(tickStart);
        next.apply(tick);
        current.put(symbol, next);
        return Optional.of(completed);
    }

    // Simple mutable builder used internally to accumulate values for a minute.
    private static final class CandleBuilder {
        final Instant startTime;
        BigDecimal open;
        BigDecimal high;
        BigDecimal low;
        BigDecimal close;
        long volume;
        Instant lastTickTimestamp;

        CandleBuilder(Instant startTime) {
            this.startTime = Objects.requireNonNull(startTime, "startTime");
        }

        void apply(Tick tick) {
            Objects.requireNonNull(tick, "tick");
            BigDecimal price = Objects.requireNonNull(tick.price(), "tick.price");
            Instant ts = Objects.requireNonNull(tick.timestamp(), "tick.timestamp");

            // Ensure tick belongs to this builder's minute
            Instant tsStart = ts.truncatedTo(ChronoUnit.MINUTES);
            if (!tsStart.equals(startTime)) {
                throw new IllegalArgumentException("tick timestamp does not match builder minute");
            }

            // Ensure in-order within minute
            if (lastTickTimestamp != null && ts.isBefore(lastTickTimestamp)) {
                System.out.printf("Current tick: %s Last tick time: %s, current tick time: %s%n", tick, lastTickTimestamp, ts);
                throw new IllegalArgumentException("ticks must arrive in non-decreasing timestamp order");
            }
            lastTickTimestamp = ts;

            if (open == null) {
                open = price;
                high = price;
                low = price;
                close = price;
                volume = tick.quantity();
                return;
            }

            if (price.compareTo(high) > 0) {
                high = price;
            }
            if (price.compareTo(low) < 0) {
                low = price;
            }
            close = price;
            volume += tick.quantity();
        }

        Candle build(String symbol) {
            if (open == null) {
                throw new IllegalStateException("cannot build candle with no ticks");
            }
            return Candle.of(symbol, startTime, open, high, low, close, volume);
        }
    }
}
