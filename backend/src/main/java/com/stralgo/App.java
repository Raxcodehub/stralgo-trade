package com.stralgo;

import com.lmax.disruptor.BusySpinWaitStrategy;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.stralgo.analysis.DerivedMetrics;
import com.stralgo.analysis.RollingWindow;
import com.stralgo.intraday.DisruptorBootstrap;
import com.stralgo.intraday.event.SignalEvent;
import com.stralgo.intraday.event.TickEvent;
import com.stralgo.intraday.handler.MarketStateHandler;
import com.stralgo.intraday.handler.SignalDetector;
import com.stralgo.intraday.ingest.TickPublisher;
import com.stralgo.intraday.metrics.LatencyMetrics;
import com.stralgo.intraday.pipeline.TickEventPipeline;
import com.stralgo.market.Candle;
import com.stralgo.market.CandleAggregator;
import com.stralgo.market.Tick;
import com.stralgo.persistence.CandleCsvReader;
import com.stralgo.persistence.CandleCsvWriters;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

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

    // MAIN: run SignalDetector test with two-stage Disruptor
    void main() throws Exception {
        log.info("Starting application (SignalDetector test)");
        runSignalDetector();
    }

    // Test SignalDetector with two-stage Disruptor: tick → signal
    private static void runSignalDetector() throws Exception {
        log.info("Backend alive (SignalDetector mode)");
        String symbol = "TEST";

        Path dataDir = Path.of("data");
        CandleCsvWriters.init(dataDir);

        // Initialize pipeline
        TickEventPipeline pipeline = new TickEventPipeline();

        // Create signal Disruptor (second ring buffer)
        ThreadFactory signalThreadFactory = new ThreadFactory() {
            private final AtomicInteger threadCount = new AtomicInteger(0);
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "signal-consumer-" + threadCount.getAndIncrement());
                t.setDaemon(false);
                return t;
            }
        };

        Disruptor<SignalEvent> signalDisruptor = new Disruptor<>(
            SignalEvent::new,
            1024,  // smaller ring buffer for signals
            signalThreadFactory,
            ProducerType.SINGLE,
            new BusySpinWaitStrategy()
        );

        // Attach signal consumer that prints detected signals
        signalDisruptor.handleEventsWith((event, sequence, endOfBatch) -> {
            if (event.signalType != SignalEvent.SIGNAL_NONE) {
                System.out.println("🔔 SIGNAL: " + event.getSignalTypeName() +
                    " | " + event.symbol +
                    " | strength=" + event.strength +
                    " | price=" + event.price +
                    " | range=" + event.range +
                    " | volume=" + event.totalVolume);
            }
        });

        // Start signal Disruptor
        signalDisruptor.start();
        RingBuffer<SignalEvent> signalRingBuffer = signalDisruptor.getRingBuffer();

        // Create tick Disruptor with MarketStateHandler and SignalDetector
        ThreadFactory tickThreadFactory = new ThreadFactory() {
            private final AtomicInteger threadCount = new AtomicInteger(0);
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "tick-consumer-" + threadCount.getAndIncrement());
                t.setDaemon(false);
                return t;
            }
        };

        Disruptor<com.stralgo.intraday.event.DisruptorTickEvent> tickDisruptor = new Disruptor<>(
            com.stralgo.intraday.event.DisruptorTickEvent::new,
            65536,
            tickThreadFactory,
            ProducerType.SINGLE,
            new BusySpinWaitStrategy()
        );

        // Wire handlers: MarketStateHandler → SignalDetector
        MarketStateHandler marketStateHandler = new MarketStateHandler(pipeline);
        SignalDetector signalDetector = new SignalDetector(pipeline, signalRingBuffer);

        tickDisruptor
            .handleEventsWith(marketStateHandler)
            .then(signalDetector);

        // Start tick Disruptor
        tickDisruptor.start();

        // Create publisher
        TickPublisher publisher = new TickPublisher(tickDisruptor.getRingBuffer());

        // Metrics tracking
        LatencyMetrics metrics = new LatencyMetrics();
        final int PRINT_EVERY = 100;
        int tickCounter = 0;

        // Reset base time
        base = Instant.now();

        // Generate test ticks
        List<Tick> ticks = new ArrayList<>();
        for (int i = 0; i < 500; i++) {
            BigDecimal price = BigDecimal.valueOf(95 + Math.random() * 10);
            long volume = (long)(1 + Math.random() * 19);
            ticks.add(Tick.of(symbol, price, volume, addSeconds(1)));
        }

        System.out.println("Publishing " + ticks.size() + " ticks into Disruptor...\n");

        // Publish ticks into Disruptor
        for (Tick tick : ticks) {
            // Publish tick
            long sequence = publisher.publishTick(tick);

            // Track metrics
            Instant receiveTime = tick.timestamp();
            TickEvent event = TickEvent.ingest(tick, receiveTime);
            Instant processedTime = tick.timestamp().plusNanos(1000);
            TickEvent processed = event.withProcessedTime(processedTime);
            metrics.record(processed);

            tickCounter++;
            if (tickCounter % PRINT_EVERY == 0) {
                printMetrics(metrics);
            }

            // Small delay to see signals being generated
            Thread.sleep(2);
        }

        System.out.println("\nDone feeding ticks into Disruptor.");

        // Allow both Disruptors to drain
        Thread.sleep(200);

        // Shutdown both Disruptors gracefully
        tickDisruptor.shutdown();
        signalDisruptor.shutdown();

        // Flush CSV writers
        CandleCsvWriters.close();

        // Print final metrics
        System.out.println("\n=== Final Metrics ===");
        printMetrics(metrics);
    }

    // Original main body moved here (minimal diff: keep original logic intact).
    private static void runDisruptor() throws Exception {
        log.info("Backend alive (Disruptor mode)");
        String symbol = "TEST";

        Path dataDir = Path.of("data");
        CandleCsvWriters.init(dataDir);

        // Initialize pipeline and Disruptor bootstrap
        TickEventPipeline pipeline = new TickEventPipeline();
        DisruptorBootstrap bootstrap = new DisruptorBootstrap(pipeline);
        bootstrap.start();

        // Create publisher for the ring buffer
        TickPublisher publisher = new TickPublisher(bootstrap.getRingBuffer());

        // Metrics tracking
        LatencyMetrics metrics = new LatencyMetrics();
        final int PRINT_EVERY = 100;
        int tickCounter = 0;

        // Reset base time to now so market time ≈ system time (accurate latency measurements)
        base = Instant.now();

        // Generate test ticks
        List<Tick> ticks = new ArrayList<>();
        for (int i = 0; i < 500; i++) {
            BigDecimal price = BigDecimal.valueOf(95 + Math.random() * 10);
            long volume = (long)(1 + Math.random() * 19);
            ticks.add(Tick.of(symbol, price, volume, addSeconds(1)));
        }

        // Publish ticks into Disruptor
        for (Tick tick : ticks) {
            // Publish tick with ingest time captured by publisher
            long sequence = publisher.publishTick(tick);

            // For latency measurement: use tick's timestamp as market time
            // Use a time after the tick as receive/processed time to simulate real processing
            Instant receiveTime = tick.timestamp();
            TickEvent event = TickEvent.ingest(tick, receiveTime);

            // Use tick timestamp + a small offset as processedTime to ensure monotonicity
            Instant processedTime = tick.timestamp().plusNanos(1000); // 1 microsecond later
            TickEvent processed = event.withProcessedTime(processedTime);
            metrics.record(processed);

            tickCounter++;
            if (tickCounter % PRINT_EVERY == 0) {
                printMetrics(metrics);
            }

            // Small delay to allow Disruptor to catch up
            Thread.sleep(1);
        }

        System.out.println("Done feeding ticks into Disruptor.");

        // Allow Disruptor to drain
        Thread.sleep(100);

        // Shutdown Disruptor gracefully
        bootstrap.shutdown();

        // Flush CSV writers
        CandleCsvWriters.close();

        // Print final metrics
        printMetrics(metrics);
    }

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

        // Reset base time to now so market time ≈ system time (accurate latency measurements)
        base = Instant.now();

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

            // For latency measurement: use tick's timestamp as market time
            // Use a time after the tick as receive/processed time to simulate real processing
            Instant receiveTime = tick.timestamp();
            TickEvent event = TickEvent.ingest(tick, receiveTime);

            // Pass through pipeline
            pipeline.onEvent(event);

            // Use tick timestamp + a small offset as processedTime to ensure monotonicity
            Instant processedTime = tick.timestamp().plusNanos(1000); // 1 microsecond later
            TickEvent processed = event.withProcessedTime(processedTime);
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

            // For replay: create TickEvent-like flow using candle's timestamp
            // NOTE: We do NOT record replay metrics since replay data is historical
            // and doesn't represent real latency. Only live simulation latency is meaningful.
            Instant ingestTime = c.startTime();
            Tick fakeTick = Tick.of(c.symbol(), c.close(), c.volume(), c.startTime());
            TickEvent event = TickEvent.ingest(fakeTick, ingestTime);
            pipeline.onEvent(event);
            // Intentionally not recording metrics for replay data
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
