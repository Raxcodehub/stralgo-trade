package com.stralgo;

import com.stralgo.analysis.DerivedMetrics;
import com.stralgo.analysis.RollingWindow;
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

public class App {
    private static final Logger log = LogManager.getLogger(App.class);

    public static Instant base = Instant.now();

    public static Instant addSeconds(int seconds) {
        base = base.plusSeconds(seconds);
        return base;
    }

    static void main() throws Exception {
        log.info("Backend alive");
        String symbol = "TEST";

        Path dataDir = Path.of("data");
        /*
        CandleCsvWriters.init(dataDir);

        CandleAggregator aggregator = new CandleAggregator();

        List<Tick> ticks = new ArrayList<>();
        for (int i = 0; i < 500; i++) {
            BigDecimal price = BigDecimal.valueOf(95 + Math.random() * 10);
            long volume = (long)(1 + Math.random() * 19);
            ticks.add(Tick.of(symbol, price, volume, addSeconds(1)));
        }

        for (Tick tick : ticks) {
            Optional<Candle> emitted = aggregator.onTick(tick);
            emitted.ifPresent(c -> {
                System.out.println("Emitted candle: " + c);
                // write asynchronously (fire-and-forget)
                CandleCsvWriters.writeCompletedCandle(c);
            });
        }*/

        System.out.println("Done feeding ticks.");

        // flush and close writers so files are consistent for reading
        CandleCsvWriters.close();

        // Read candles back from CSV and print them (replay)
        CandleCsvReader reader = new CandleCsvReader();
        Path file = dataDir.resolve(symbol).resolve(String.format("%s.csv", "2026-01-11"));

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
        }
    }
}