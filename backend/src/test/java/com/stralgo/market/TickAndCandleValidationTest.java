package com.stralgo.market;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class TickAndCandleValidationTest {

    @Test
    void tickValidationRejectsEmptySymbolAndNegativeValues() {
        Instant ts = Instant.parse("2024-01-01T00:00:00Z");
        assertThrows(IllegalArgumentException.class, () -> Tick.of("  ", BigDecimal.ONE, 1, ts));
        assertThrows(IllegalArgumentException.class, () -> Tick.of("T", BigDecimal.valueOf(-1), 1, ts));
        assertThrows(IllegalArgumentException.class, () -> Tick.of("T", BigDecimal.ONE, -1, ts));
    }

    @Test
    void candleValidationRejectsBadTimesAndValues() {
        Instant badStart = Instant.parse("2024-01-01T00:00:30Z"); // not minute-aligned
        assertThrows(IllegalArgumentException.class, () -> Candle.of("SYM", badStart, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, 0));

        Instant good = Instant.parse("2024-01-01T00:00:00Z");
        assertThrows(IllegalArgumentException.class, () -> Candle.of("  ", good, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, 0));
        assertThrows(IllegalArgumentException.class, () -> Candle.of("SYM", good, BigDecimal.valueOf(-1), BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, 0));
        assertThrows(IllegalArgumentException.class, () -> Candle.of("SYM", good, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.valueOf(2), BigDecimal.ONE, 0)); // high < low
        assertThrows(IllegalArgumentException.class, () -> Candle.of("SYM", good, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, -1));
    }
}

