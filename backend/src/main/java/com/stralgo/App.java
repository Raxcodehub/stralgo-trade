package com.stralgo;

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

        Path dataDir = Path.of("data");
        CandleCsvWriters.init(dataDir);

        CandleAggregator aggregator = new CandleAggregator();

        String symbol = "TEST";

        Tick[] ticks = new Tick[] {
                Tick.of(symbol, new BigDecimal("100.0"), 10, addSeconds( 5)),
                Tick.of(symbol, new BigDecimal("101.5"), 5, addSeconds(10)),
                Tick.of(symbol, new BigDecimal("99.75"), 2, addSeconds(11)),
                // this tick is in the next minute and should cause the previous candle to be emitted
                Tick.of(symbol, new BigDecimal("102.0"), 7, addSeconds(55)),
                Tick.of(symbol, new BigDecimal("100.9"), 10, addSeconds(13)),
                Tick.of(symbol, new BigDecimal("102.5"), 5, addSeconds(14)),
                Tick.of(symbol, new BigDecimal("100.5"), 2, addSeconds(15)),
                Tick.of(symbol, new BigDecimal("101.7"), 7, addSeconds(16))
        };

        for (Tick tick : ticks) {
            Optional<Candle> emitted = aggregator.onTick(tick);
            emitted.ifPresent(c -> {
                System.out.println("Emitted candle: " + c);
                // write asynchronously (fire-and-forget)
                CandleCsvWriters.writeCompletedCandle(c);
            });
        }

        System.out.println("Done feeding ticks.");

        // flush and close writers so files are consistent for reading
        CandleCsvWriters.close();

        // Read candles back from CSV and print them (replay)
        CandleCsvReader reader = new CandleCsvReader();
        Path file = dataDir.resolve(symbol).resolve(String.format("%s.csv", ticks[0].timestamp().toString().substring(0,10)));

        try (Stream<Candle> s = reader.read(file)) {
            s.forEach(c -> System.out.println("Read candle: " + c));
        }
    }
}