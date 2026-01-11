package com.stralgo.analysis;

import com.stralgo.market.Candle;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

/// Maintains a rolling time window of candles.
/// Window size is time-based (Duration). Old candles are evicted when a new
/// candle is added and its timestamp moves the window forward.
///
/// Notes:
/// - Internally stores candles in time order (oldest first).
/// - Caller must add candles in non-decreasing time order; if an older candle
///   is added after a newer one an IllegalArgumentException is thrown. This
///   keeps the class simple and deterministic for live or replay feeds.
public final class RollingWindow {
    private final Duration windowSize;
    private final Deque<Candle> deque = new ArrayDeque<>();

    public RollingWindow(Duration windowSize) {
        this.windowSize = Objects.requireNonNull(windowSize, "windowSize");
        if (windowSize.isNegative() || windowSize.isZero()) {
            throw new IllegalArgumentException("windowSize must be positive");
        }
    }

    /// Add a candle to the rolling window. Candles must be provided in
    /// non-decreasing timestamp order (replay or live feed). After adding,
    /// evict any candles older than (c.startTime - windowSize).
    public synchronized void add(Candle c) {
        Objects.requireNonNull(c, "candle");
        Instant ts = c.startTime();
        // Enforce non-decreasing order to avoid state leaks across time
        if (!deque.isEmpty()) {
            Instant last = deque.getLast().startTime();
            if (ts.isBefore(last)) {
                throw new IllegalArgumentException("candles must be added in non-decreasing time order");
            }
        }

        deque.addLast(c);
        evictOlderThan(ts.minus(windowSize));
    }

    private void evictOlderThan(Instant thresholdInclusive) {
        while (!deque.isEmpty()) {
            Candle head = deque.getFirst();
            // remove while head.startTime < thresholdInclusive (strictly older)
            if (head.startTime().isBefore(thresholdInclusive)) {
                deque.removeFirst();
            } else {
                break;
            }
        }
    }

    /// Returns a snapshot list of candles in the window in time order (oldest first).
    public synchronized List<Candle> getCandles() {
        if (deque.isEmpty()) return Collections.emptyList();
        return new ArrayList<>(deque);
    }

    /// Convenience: clear window (useful for tests or replay resets).
    public synchronized void clear() {
        deque.clear();
    }
}

