package com.stralgo.persistence;

import com.stralgo.market.Candle;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Simple static wrapper around {@link CandleCsvWriter} that provides
 * convenience static methods so callers don't need to manage the writer
 * instance or its thread. The wrapper lazily initializes a single shared
 * CandleCsvWriter and registers a JVM shutdown hook to close it.
 * </p>
 * Initialization notes:
 * - Callers may optionally call {@link #init(Path)} before the first write to
 *   set a custom base directory. The first init wins; subsequent inits are
 *   ignored.
 * - Calling {@link #close()} will close the underlying writer and mark the
 *   wrapper as closed; calling {@link #init(Path)} or performing a write will
 *   re-open the writer.
 */
public final class CandleCsvWriters {
    private CandleCsvWriters() {
        // utility
    }

    private static volatile CandleCsvWriter INSTANCE;
    private static final Object LOCK = new Object();
    private static volatile boolean closed = false;
    private static volatile Path configuredBaseDir = null;

    /**
     * Optionally configure the base directory for CSV files before the writer
     * is created. The first call to init (or the first write) determines the
     * base directory. Subsequent calls are ignored. If the wrapper was closed
     * previously, init will re-open it.
     */
    public static void init(Path baseDir) {
        Objects.requireNonNull(baseDir, "baseDir");
        // allow re-initialization after a previous close
        synchronized (LOCK) {
            if (INSTANCE == null) {
                configuredBaseDir = baseDir;
                closed = false;
                INSTANCE = new CandleCsvWriter(baseDir);
                registerShutdownHook();
            }
        }
    }

    private static CandleCsvWriter instance() {
        CandleCsvWriter w = INSTANCE;
        if (w == null) {
            synchronized (LOCK) {
                if (INSTANCE == null) {
                    // if previously closed, opening again
                    Path base = configuredBaseDir == null ? Paths.get("data") : configuredBaseDir;
                    closed = false;
                    INSTANCE = new CandleCsvWriter(base);
                    registerShutdownHook();
                }
                w = INSTANCE;
            }
        }
        return w;
    }

    private static void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                close();
            } catch (Throwable t) {
                // swallow - we must not throw from shutdown hook
            }
        }, "candle-csv-writers-shutdown"));
    }

    /**
     * Enqueue-only async write: returns true if the task was accepted. This
     * method never blocks the caller.
     */
    public static boolean writeCompletedCandleAsync(String symbol,
                                                                    Instant timestamp,
                                                                    double open,
                                                                    double high,
                                                                    double low,
                                                                    double close,
                                                                    double volume) {
        Objects.requireNonNull(symbol, "symbol");
        Objects.requireNonNull(timestamp, "timestamp");
        if (closed) {
            return false;
        }
        CandleCsvWriter w = instance();
        return w.writeCompletedCandleAsync(symbol, timestamp, open, high, low, close, volume);
    }

    /** Convenience overload that accepts the domain Candle record and enqueues without blocking. */
    public static boolean writeCompletedCandleAsync(Candle candle) {
        Objects.requireNonNull(candle, "candle");
        return writeCompletedCandleAsync(candle.symbol(), candle.startTime(),
                candle.open().doubleValue(), candle.high().doubleValue(), candle.low().doubleValue(), candle.close().doubleValue(), (double) candle.volume());
    }

    /**
     * Async write that returns a CompletableFuture which completes when the
     * write finishes (or completes exceptionally). This delegates to the
     * underlying writer's future-returning API.
     */
    public static CompletableFuture<Void> writeCompletedCandleFuture(String symbol,
                                                                    Instant timestamp,
                                                                    double open,
                                                                    double high,
                                                                    double low,
                                                                    double close,
                                                                    double volume) {
        Objects.requireNonNull(symbol, "symbol");
        Objects.requireNonNull(timestamp, "timestamp");
        if (closed) {
            CompletableFuture<Void> f = new CompletableFuture<>();
            f.completeExceptionally(new IllegalStateException("CandleCsvWriters is closed"));
            return f;
        }
        CandleCsvWriter w = instance();
        return w.writeCompletedCandleWithFuture(symbol, timestamp, open, high, low, close, volume);
    }

    public static CompletableFuture<Void> writeCompletedCandleFuture(Candle candle) {
        Objects.requireNonNull(candle, "candle");
        return writeCompletedCandleFuture(candle.symbol(), candle.startTime(),
                candle.open().doubleValue(), candle.high().doubleValue(), candle.low().doubleValue(), candle.close().doubleValue(), (double) candle.volume());
    }

    /**
     * Non-blocking convenience method: create a write task and enqueue it.
     * The caller will not wait for the write to complete. If the enqueue fails
     * (queue full or writer closed) the method logs a debug message.
     */
    public static void writeCompletedCandle(String symbol,
                                            Instant timestamp,
                                            double open,
                                            double high,
                                            double low,
                                            double close,
                                            double volume) {
        boolean ok = writeCompletedCandleAsync(symbol, timestamp, open, high, low, close, volume);
        if (!ok) {
            // Simple debug log - no logging framework is required here
            System.err.println("[debug] CandleCsvWriters: failed to enqueue write for symbol=" + symbol + " ts=" + timestamp);
        }
    }

    public static void writeCompletedCandle(Candle candle) {
        Objects.requireNonNull(candle, "candle");
        writeCompletedCandle(candle.symbol(), candle.startTime(),
                candle.open().doubleValue(), candle.high().doubleValue(), candle.low().doubleValue(), candle.close().doubleValue(), (double) candle.volume());
    }

    /**
     * Close the shared writer. This is idempotent. After close, writes will fail.
     */
    public static synchronized void close() {
        if (closed) return;
        closed = true;

        CandleCsvWriter w = INSTANCE;
        INSTANCE = null;
        configuredBaseDir = null;

        if (w != null) {
            try {
                w.close();  // now waits longer for drain
            } catch (Exception e) {
                // In production: log.warn("Failed to cleanly close candle writer", e);
                System.err.println("[warn] CandleCsvWriters: failed to close cleanly");
            }
        }
    }
}
