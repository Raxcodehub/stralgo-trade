package com.stralgo.market;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class CandleAggregatorTest {

    @Test
    void aggregatesTicksIntoMinuteCandle() {
        CandleAggregator agg = new CandleAggregator();

        Instant t1 = Instant.parse("2024-01-01T10:15:00Z");
        Instant t2 = Instant.parse("2024-01-01T10:15:10Z");
        Instant t3 = Instant.parse("2024-01-01T10:15:59Z");
        Instant t4 = Instant.parse("2024-01-01T10:16:00Z");

        Optional<Candle> maybe = agg.onTick(Tick.of("SYM", BigDecimal.valueOf(100), 1, t1));
        assertTrue(maybe.isEmpty());

        maybe = agg.onTick(Tick.of("SYM", BigDecimal.valueOf(101), 2, t2));
        assertTrue(maybe.isEmpty());

        maybe = agg.onTick(Tick.of("SYM", BigDecimal.valueOf(99), 3, t3));
        assertTrue(maybe.isEmpty());

        // tick on next minute should emit completed candle for previous minute
        maybe = agg.onTick(Tick.of("SYM", BigDecimal.valueOf(102), 4, t4));
        assertTrue(maybe.isPresent());
        Candle c = maybe.get();
        assertEquals("SYM", c.symbol());
        assertEquals(Instant.parse("2024-01-01T10:15:00Z"), c.startTime());
        assertEquals(BigDecimal.valueOf(100), c.open());
        assertEquals(BigDecimal.valueOf(101), c.high());
        assertEquals(BigDecimal.valueOf(99), c.low());
        assertEquals(BigDecimal.valueOf(99), c.close());
        assertEquals(6, c.volume());
    }

    @Test
    void outOfOrderTicksThrow() {
        CandleAggregator agg = new CandleAggregator();
        Instant t1 = Instant.parse("2024-01-01T10:15:30Z");
        Instant t2 = Instant.parse("2024-01-01T10:15:10Z");

        agg.onTick(Tick.of("SYM", BigDecimal.valueOf(100), 1, t1));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                agg.onTick(Tick.of("SYM", BigDecimal.valueOf(101), 1, t2)));
        assertTrue(ex.getMessage().contains("non-decreasing timestamp"));
    }
}

