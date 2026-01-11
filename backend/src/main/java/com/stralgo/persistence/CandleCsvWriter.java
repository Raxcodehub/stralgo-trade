package com.stralgo.persistence;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Writes completed candles to CSV files.
 * Appends data (never rewrites history).
 * File pattern: data/<symbol>/<YYYY-MM-DD>.csv
 * CSV row: timestamp,open,high,low,close,volume
 * </p>
 * This writer uses a single background writer thread that drains a
 * BlockingQueue of write tasks. This keeps the implementation very simple and
 * reliable: writes are performed sequentially by the background thread and no
 * per-file locks are required. Callers may use the async API to enqueue writes
 * without blocking, or the synchronous API which waits for the write to finish.
 */
public class CandleCsvWriter {
    private final Path baseDir;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    private final LinkedBlockingQueue<CandleWriteTask> queue = new LinkedBlockingQueue<>();
    private final Thread worker;
    private volatile boolean running = true;

    private CandleCsvWriter() {
        this(Paths.get("data"));
    }

    public CandleCsvWriter(Path baseDir) {
        this.baseDir = Objects.requireNonNull(baseDir, "baseDir");
        this.worker = new Thread(this::runWorker, "candle-csv-writer-thread");
        this.worker.setDaemon(true);
        this.worker.start();
    }

    /**
     * Non-blocking convenience method: create a write task and enqueue it.
     * The caller will not wait for the write to complete. If the enqueue fails
     * (queue full or writer closed) the method logs a debug message.
     */
    public void writeCompletedCandle(String symbol,
                                     Instant timestamp,
                                     double open,
                                     double high,
                                     double low,
                                     double close,
                                     double volume) {
        boolean ok = writeCompletedCandleAsync(symbol, timestamp, open, high, low, close, volume);
        if (!ok) {
            // Simple debug log - no logging framework is required here
            System.err.println("[debug] CandleCsvWriter: failed to enqueue write for symbol=" + symbol + " ts=" + timestamp);
        }
    }

    /**
     * Enqueue a completed candle write without returning a CompletableFuture.
     * Returns true if the task was successfully added to the queue, false otherwise.
     * This method does not block waiting for the write to finish.
     */
    public boolean writeCompletedCandleAsync(String symbol,
                                             Instant timestamp,
                                             double open,
                                             double high,
                                             double low,
                                             double close,
                                             double volume) {
        Objects.requireNonNull(symbol, "symbol");
        Objects.requireNonNull(timestamp, "timestamp");
        if (!running) {
            return false;
        }

        // Create a task without an attached CompletableFuture - nobody is waiting.
        CandleWriteTask task = new CandleWriteTask(symbol, timestamp, open, high, low, close, volume, null);
        // Use offer to avoid blocking producers; return whether it was accepted.
        return queue.offer(task);
    }

    // Keep an alternative API for callers that want a CompletableFuture (optional)
    /**
     * Enqueue a completed candle write and return a CompletableFuture that
     * completes when the write finishes or completes exceptionally on IO error.
     * This helper is provided for callers who want to wait for completion.
     */
    public CompletableFuture<Void> writeCompletedCandleWithFuture(String symbol,
                                                                   Instant timestamp,
                                                                   double open,
                                                                   double high,
                                                                   double low,
                                                                   double close,
                                                                   double volume) {
        Objects.requireNonNull(symbol, "symbol");
        Objects.requireNonNull(timestamp, "timestamp");
        if (!running) {
            CompletableFuture<Void> f = new CompletableFuture<>();
            f.completeExceptionally(new IllegalStateException("writer is closed"));
            return f;
        }

        CompletableFuture<Void> future = new CompletableFuture<>();
        CandleWriteTask task = new CandleWriteTask(symbol, timestamp, open, high, low, close, volume, future);
        // Use add (unbounded queue) to ensure task enqueued; if queue is bounded it may throw
        queue.add(task);
        return future;
    }

    private void runWorker() {
        while (true) {
            CandleWriteTask task;
            try {
                task = queue.take();
            } catch (InterruptedException e) {
                // If interrupted while waiting, exit if not running and queue empty
                if (!running && queue.isEmpty()) {
                    break;
                }
                continue;
            }

            // poison pill
            if (task.poison) {
                break;
            }

            try {
                doWriteCompletedCandle(task.symbol, task.timestamp, task.open, task.high, task.low, task.close, task.volume);
                if (task.future != null) {
                    task.future.complete(null);
                }
            } catch (Throwable t) {
                if (task.future != null) {
                    task.future.completeExceptionally(t);
                }
            }
        }

        // Drain remaining tasks and fail them (we're shutting down)
        CandleWriteTask remaining;
        while ((remaining = queue.poll()) != null) {
            if (remaining.poison) break;
            if (remaining.future != null) {
                remaining.future.completeExceptionally(new IOException("writer shutdown"));
            }
        }
    }

    // Core synchronous write logic used by the worker thread
    private void doWriteCompletedCandle(String symbol,
                                        Instant timestamp,
                                        double open,
                                        double high,
                                        double low,
                                        double close,
                                        double volume) throws IOException {
        String sanitized = sanitizeSymbol(symbol);
        LocalDate date = timestamp.atZone(ZoneOffset.UTC).toLocalDate();
        Path dir = baseDir.resolve(sanitized);
        Files.createDirectories(dir);
        Path file = dir.resolve(date.format(DATE_FMT) + ".csv");

        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            writer.write(formatCsvLine(timestamp, open, high, low, close, volume));
            writer.newLine();
            writer.flush();
        }
    }

    private static String formatCsvLine(Instant ts,
                                        double open,
                                        double high,
                                        double low,
                                        double close,
                                        double volume) {
        // timestamp in ISO-8601 (UTC), followed by numeric fields
        return String.join(",",
                ts.toString(),
                Double.toString(open),
                Double.toString(high),
                Double.toString(low),
                Double.toString(close),
                Double.toString(volume));
    }

    private static String sanitizeSymbol(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            return "unknown";
        }
        // allow common filename chars, replace others with underscore
        return symbol.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    /**
     * Shut down the writer. After calling close, no more async writes should be submitted.
     * This method enqueues a poison pill and waits briefly for the worker to finish.
     */
    public void close() {
        if (!running) return;

        running = false;

        // Poison pill
        queue.add(CandleWriteTask.poison());

        // Give worker time to drain queue
        try {
            // Wait longer - 10-15 seconds is usually enough for candles
            worker.join(TimeUnit.SECONDS.toMillis(12));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            // last resort
            worker.interrupt();
            try {
                worker.join(2000);
            } catch (InterruptedException ignored) {}
        }
    }

    // Simple task object shared between producer and consumer
    private static final class CandleWriteTask {
        final String symbol;
        final Instant timestamp;
        final double open;
        final double high;
        final double low;
        final double close;
        final double volume;
        final CompletableFuture<Void> future;
        final boolean poison;

        private CandleWriteTask(String symbol, Instant timestamp, double open, double high, double low, double close, double volume, CompletableFuture<Void> future) {
            this.symbol = symbol;
            this.timestamp = timestamp;
            this.open = open;
            this.high = high;
            this.low = low;
            this.close = close;
            this.volume = volume;
            this.future = future;
            this.poison = false;
        }

        private CandleWriteTask(boolean poison) {
            this.symbol = null;
            this.timestamp = null;
            this.open = 0;
            this.high = 0;
            this.low = 0;
            this.close = 0;
            this.volume = 0;
            this.future = null;
            this.poison = poison;
        }

        static CandleWriteTask poison() {
            return new CandleWriteTask(true);
        }
    }
}
