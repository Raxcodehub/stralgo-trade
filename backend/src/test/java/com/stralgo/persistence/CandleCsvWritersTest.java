package com.stralgo.persistence;

import com.stralgo.market.Candle;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class CandleCsvWritersTest {
    private static final Logger log = LogManager.getLogger(CandleCsvWritersTest.class);

    private final Path tempDir = Path.of("build", "test-data", Long.toString(System.currentTimeMillis()));

    @AfterEach
    public void tearDown() throws IOException {
        log.info("Tear down test data directory: {}", tempDir);
        CandleCsvWriters.close();
        // best-effort cleanup
        if (Files.exists(tempDir)) {
            Files.walk(tempDir)
                    .sorted((a, b) -> b.compareTo(a))
                    .forEach(p -> p.toFile().delete());
        }
    }

    @Test
    public void enqueueOnlyReturnsTrueAndFileIsCreated() throws Exception {
        log.info("TEST: enqueueOnlyReturnsTrueAndFileIsCreated - init {}", tempDir);
        CandleCsvWriters.init(tempDir);

        Instant ts = Instant.parse("2020-01-01T00:00:00Z");
        boolean enqueued = CandleCsvWriters.writeCompletedCandleAsync("TEST", ts, 1.0, 2.0, 0.5, 1.5, 100.0);
        log.info("enqueue result: {}", enqueued);
        assertTrue(enqueued, "task should be accepted");

        Path file = tempDir.resolve("TEST").resolve("2020-01-01.csv");
        // wait (poll) for background writer to flush with a timeout
        waitForFile(file, 5_000);

        assertTrue(Files.exists(file), "csv file should exist");
        List<String> lines = Files.readAllLines(file);
        assertEquals(1, lines.size());
        assertTrue(lines.get(0).startsWith("2020-01-01T00:00:00Z"));
        log.info("Wrote and verified file {} with {} lines", file, lines.size());
    }

    @Test
    public void futureCompletesSuccessfully() throws Exception {
        log.info("TEST: futureCompletesSuccessfully - init {}", tempDir);
        CandleCsvWriters.init(tempDir);

        Instant ts = Instant.parse("2020-01-02T00:00:00Z");
        var future = CandleCsvWriters.writeCompletedCandleFuture("FUT", ts, 10.0, 12.0, 9.0, 11.0, 50.0);
        future.get(); // should complete normally
        log.info("future completed for FUT {}", ts);

        Path file = tempDir.resolve("FUT").resolve("2020-01-02.csv");
        assertTrue(Files.exists(file));
        List<String> lines = Files.readAllLines(file);
        assertEquals(1, lines.size());
        log.info("Wrote and verified file {} with {} lines", file, lines.size());
    }

    @Test
    public void overloadsAcceptCandleRecord() throws Exception {
        log.info("TEST: overloadsAcceptCandleRecord - init {}", tempDir);
        CandleCsvWriters.init(tempDir);
        Instant ts = Instant.parse("2020-01-03T00:00:00Z");
        Candle c = Candle.of("REC", ts, BigDecimal.valueOf(1.0), BigDecimal.valueOf(2.0), BigDecimal.valueOf(0.5), BigDecimal.valueOf(1.5), 10L);
        boolean enqueued = CandleCsvWriters.writeCompletedCandleAsync(c);
        log.info("enqueue record result: {}", enqueued);
        assertTrue(enqueued);
        CandleCsvWriters.writeCompletedCandleFuture(c).get();

        Path file = tempDir.resolve("REC").resolve("2020-01-03.csv");
        assertTrue(Files.exists(file));
        List<String> lines = Files.readAllLines(file);
        assertEquals(2, lines.size()); // one from async enqueue, one from future write
        log.info("Wrote and verified file {} with {} lines", file, lines.size());
    }

    @Test
    public void closePreventsFurtherEnqueue() throws Exception {
        log.info("TEST: closePreventsFurtherEnqueue - init {}", tempDir);
        CandleCsvWriters.init(tempDir);
        CandleCsvWriters.close();
        boolean enqueued = CandleCsvWriters.writeCompletedCandleAsync("X", Instant.parse("2020-01-04T00:00:00Z"), 1,1,1,1,1);
        log.info("enqueue after close result: {}", enqueued);
        assertFalse(enqueued);
        var f = CandleCsvWriters.writeCompletedCandleFuture("X", Instant.parse("2020-01-04T00:00:00Z"), 1,1,1,1,1);
        assertThrows(ExecutionException.class, f::get);
    }

    // --- New, more intense tests ---

    @Test
    public void highThroughputManyFutures() throws Exception {
        log.info("TEST: highThroughputManyFutures - init {}", tempDir);
        CandleCsvWriters.init(tempDir);

        final int N = 1000; // number of writes
        Instant base = Instant.parse("2020-02-01T00:00:00Z");
        List<CompletableFuture<Void>> futures = new ArrayList<>(N);
        for (int i = 0; i < N; i++) {
            Instant ts = base.plusSeconds((long) i * 60);
            futures.add(CandleCsvWriters.writeCompletedCandleFuture("HT", ts, i, i + 1, Math.max(0, i - 1), i + 0.5, i * 10.0));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(30, TimeUnit.SECONDS);
        log.info("Completed all futures for HT, N={}", N);

        Path file = tempDir.resolve("HT").resolve("2020-02-01.csv");
        assertTrue(Files.exists(file), "csv file should exist for high-throughput test");
        List<String> lines = Files.readAllLines(file);
        assertEquals(N, lines.size(), "expected number of lines written");
        log.info("Wrote and verified file {} with {} lines", file, lines.size());
    }

    @Test
    public void concurrentThreadsManySymbols() throws Exception {
        log.info("TEST: concurrentThreadsManySymbols - init {}", tempDir);
        CandleCsvWriters.init(tempDir);

        final int threads = 8;
        final int perThread = 250; // total 2000 writes
        ExecutorService ex = Executors.newFixedThreadPool(threads);

        List<Callable<CompletableFuture<Void>>> callables = new ArrayList<>();
        for (int t = 0; t < threads; t++) {
            final String sym = "SYM" + t;
            callables.add(() -> {
                List<CompletableFuture<Void>> list = new ArrayList<>(perThread);
                Instant base = Instant.parse("2020-03-01T00:00:00Z");
                for (int i = 0; i < perThread; i++) {
                    Instant ts = base.plusSeconds((long) i * 60);
                    list.add(CandleCsvWriters.writeCompletedCandleFuture(sym, ts, 1.0, 2.0, 0.5, 1.5, 1.0));
                }
                return CompletableFuture.allOf(list.toArray(new CompletableFuture[0]));
            });
        }

        List<Future<CompletableFuture<Void>>> submitted = new ArrayList<>();
        for (Callable<CompletableFuture<Void>> c : callables) {
            submitted.add(ex.submit(c));
        }

        // wait for each group's combined future to finish
        for (Future<CompletableFuture<Void>> f : submitted) {
            CompletableFuture<Void> cf = f.get(60, TimeUnit.SECONDS);
            cf.get(60, TimeUnit.SECONDS);
        }

        ex.shutdown();
        boolean b = ex.awaitTermination(5, TimeUnit.SECONDS);
        if(!b) {
            ex.shutdownNow();
        }

        for (int t = 0; t < threads; t++) {
            Path file = tempDir.resolve("SYM" + t).resolve("2020-03-01.csv");
            assertTrue(Files.exists(file), "csv file should exist for symbol " + t);
            List<String> lines = Files.readAllLines(file);
            log.info("Total lines for SYM{}: {}", t, lines.size());
            assertEquals(perThread, lines.size(), "expected per-thread lines for symbol " + t);
        }
        log.info("Completed concurrentThreadsManySymbols");
    }

    private static void waitForFile(Path file, long timeoutMillis) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        while (!Files.exists(file) && System.currentTimeMillis() < deadline) {
            Thread.sleep(10);
        }
    }
}
