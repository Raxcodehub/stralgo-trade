package com.stralgo;

import com.stralgo.analysis.DerivedMetrics;
import com.stralgo.analysis.RollingWindow;
import com.stralgo.intraday.event.TickEvent;
import com.stralgo.intraday.metrics.LatencyMetrics;
import com.stralgo.intraday.pipeline.TickEventPipeline;
import com.stralgo.market.Candle;
import com.stralgo.market.CandleAggregator;
import com.stralgo.market.Tick;
import com.stralgo.persistence.CandleCsvReader;
import com.stralgo.persistence.CandleCsvWriters;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Application entry point and composition root.
 *
 * <p>The {@code App} class wires together system components and provides
 * execution modes such as live simulation and replay.</p>
 *
 * <p>This class contains no business logic and exists solely to bootstrap
 * and orchestrate experiments.</p>
 */

public class App {
    private static final Logger log = LogManager.getLogger(App.class);

    public static Instant base = Instant.now();

    public static Instant addSeconds(int seconds) {
        base = base.plusSeconds(seconds);
        return base;
    }

    // MAIN: always run direct (non-reactive) ingestion
    void main() throws Exception {
        log.info("Starting application (non-reactive)");
        runDirect();
    }

    // Original main body moved here (minimal diff: keep original logic intact).
    private static void runDirect() throws Exception {
        log.info("Backend alive");
        String symbol = "TEST";

        Path dataDir = Path.of("data");
        CandleCsvWriters.init(dataDir);

        CandleAggregator aggregator = new CandleAggregator();

        // New: pipeline and metrics used by both live-sim and replay
        TickEventPipeline pipeline = new TickEventPipeline();
        LatencyMetrics metrics = new LatencyMetrics();
        final int PRINT_EVERY = 1000;
        int tickCounter = 0;

        // Live simulation: create ticks, wrap into TickEvent with receiveTime = Instant.now(), feed pipeline and metrics
        List<Tick> ticks = new ArrayList<>();
        for (int i = 0; i < 500; i++) {
            BigDecimal price = BigDecimal.valueOf(95 + Math.random() * 10);
            long volume = (long)(1 + Math.random() * 19);
            ticks.add(Tick.of(symbol, price, volume, addSeconds(1)));
        }

        for (Tick tick : ticks) {
            // preserve original aggregator behavior for CSV writers
            Optional<Candle> emitted = aggregator.onTick(tick);
            emitted.ifPresent(c -> {
                System.out.println("Emitted candle: " + c);
                CandleCsvWriters.writeCompletedCandle(c);
            });

            // New: create TickEvent at ingestion
            Instant receiveTime = Instant.now();
            // Ensure receiveTime is not before marketTime (simulation uses future-dated market timestamps)
            if (receiveTime.isBefore(tick.timestamp())) {
                receiveTime = tick.timestamp();
            }
            TickEvent event = TickEvent.ingest(tick, receiveTime);

            // Pass through pipeline (which will set processedTime)
            pipeline.onEvent(event);

            // Now record in metrics. Ensure processedTime >= receiveTime to avoid negative processing latency
            Instant processedInstant = Instant.now();
            if (processedInstant.isBefore(receiveTime)) {
                processedInstant = receiveTime;
            }
            TickEvent processed = event.withProcessedTime(processedInstant);
            metrics.record(processed);

            tickCounter++;
            if (tickCounter % PRINT_EVERY == 0) {
                printMetrics(metrics);
            }
        }

        System.out.println("Done feeding ticks.");

        // flush and close writers so files are consistent for reading
        CandleCsvWriters.close();

        // Reset pipeline state before replay so aggregator doesn't receive out-of-order ticks
        pipeline.reset();

        // Read candles back from CSV and print them (replay)
        CandleCsvReader reader = new CandleCsvReader();
        Path file = dataDir.resolve(symbol).resolve(String.format("%s.csv", "2026-01-12"));

        RollingWindow window5m = new RollingWindow(java.time.Duration.ofMinutes(5));
        RollingWindow window15m = new RollingWindow(java.time.Duration.ofMinutes(15));

        List<Candle> candles = new ArrayList<>();
        try (Stream<Candle> s = reader.read(file)) {
            s.forEach(c -> {
                System.out.println("Read candle: " + c);
                candles.add(c);
            });
        }

        // Sort candles by startTime to ensure non-decreasing order for RollingWindow
        candles.sort(java.util.Comparator.comparing(Candle::startTime));

        // Feed into windows and print market awareness after each candle
        for (Candle c : candles) {
            window5m.add(c);
            window15m.add(c);
            List<Candle> c5m = window5m.getCandles();
            List<Candle> c15m = window15m.getCandles();
            BigDecimal range5m = c5m.isEmpty() ? BigDecimal.ZERO : DerivedMetrics.range(c5m);
            long vol15m = DerivedMetrics.totalVolume(c15m);
            String time = c.startTime().toString().substring(11, 16); // HH:mm
            System.out.println(time + " | 5m range: " + range5m + " | 15m volume: " + vol15m);

            // Mirror live ingestion: create a TickEvent-like flow for replay so metrics see the same code paths
            // In replay mode market time is c.startTime(), we simulate receiveTime == now and processedTime == now
            Tick fakeTick = Tick.of(c.symbol(), c.close(), c.volume(), c.startTime());
            Instant receiveTime = Instant.now();
            if (receiveTime.isBefore(fakeTick.timestamp())) {
                receiveTime = fakeTick.timestamp();
            }
            TickEvent event = TickEvent.ingest(fakeTick, receiveTime);
            pipeline.onEvent(event);
            Instant processedInstant = Instant.now();
            if (processedInstant.isBefore(receiveTime)) {
                processedInstant = receiveTime;
            }
            TickEvent processed = event.withProcessedTime(processedInstant);
            metrics.record(processed);

            tickCounter++;
            if (tickCounter % PRINT_EVERY == 0) {
                printMetrics(metrics);
            }
        }

        // final metrics print
        printMetrics(metrics);
    }

    private static void printMetrics(LatencyMetrics metrics) {
        System.out.println("Ticks: " + metrics.ingestCount());
        System.out.println("Ingest latency avg: " + metrics.ingestAverage().map(App::formatDurationMillis).orElse("-") );
        System.out.println("Processing latency avg: " + metrics.processingAverage().map(App::formatDurationMillis).orElse("-") );
        System.out.println("End-to-end latency avg: " + metrics.endToEndAverage().map(App::formatDurationMillis).orElse("-") );
    }

    private static String formatDurationMillis(java.time.Duration d) {
        // format with 3 decimal places in milliseconds
        double ms = d.toNanos() / 1_000_000.0;
        return String.format("%.3f ms", ms);
    }
}
