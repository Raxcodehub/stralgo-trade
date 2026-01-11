package com.stralgo;

import com.stralgo.market.Candle;
import com.stralgo.market.CandleAggregator;
import com.stralgo.market.Tick;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

public class App {
    
    public static Instant base = Instant.now();
    
    public static Instant addSeconds(int seconds) {
        base = base.plusSeconds(seconds);
        return base;
    }
    static void main() {
        System.out.println("Backend alive");

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
            emitted.ifPresent(c -> System.out.println("Emitted candle: " + c));
        }

        System.out.println("Done feeding ticks.");
    }
}