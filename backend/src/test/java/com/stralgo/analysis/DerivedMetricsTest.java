package com.stralgo.analysis;

import com.stralgo.market.Candle;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class DerivedMetricsTest {

    private static final Instant BASE = Instant.parse("2020-01-01T00:00:00Z");

    private static Candle createCandle(int minute, double open, double high, double low, double close, long volume) {
        return Candle.of("TEST", BASE.plusSeconds(minute * 60L), BigDecimal.valueOf(open), BigDecimal.valueOf(high), BigDecimal.valueOf(low), BigDecimal.valueOf(close), volume);
    }

    @Test
    public void highestHigh() {
        List<Candle> candles = List.of(
                createCandle(0, 100, 105, 95, 102, 100),
                createCandle(1, 102, 110, 100, 108, 200),
                createCandle(2, 108, 112, 105, 110, 150)
        );
        assertEquals(BigDecimal.valueOf(112.0), DerivedMetrics.highestHigh(candles));
    }

    @Test
    public void lowestLow() {
        List<Candle> candles = List.of(
                createCandle(0, 100, 105, 95, 102, 100),
                createCandle(1, 102, 110, 100, 108, 200),
                createCandle(2, 108, 112, 105, 110, 150)
        );
        assertEquals(BigDecimal.valueOf(95.0), DerivedMetrics.lowestLow(candles));
    }

    @Test
    public void totalVolume() {
        List<Candle> candles = List.of(
                createCandle(0, 100, 105, 95, 102, 100),
                createCandle(1, 102, 110, 100, 108, 200),
                createCandle(2, 108, 112, 105, 110, 150)
        );
        assertEquals(450L, DerivedMetrics.totalVolume(candles));
    }

    @Test
    public void averageClose() {
        List<Candle> candles = List.of(
                createCandle(0, 100, 105, 95, 102, 100),
                createCandle(1, 102, 110, 100, 108, 200),
                createCandle(2, 108, 112, 105, 110, 150)
        );
        BigDecimal expected = BigDecimal.valueOf(102).add(BigDecimal.valueOf(108)).add(BigDecimal.valueOf(110)).divide(BigDecimal.valueOf(3), 8, RoundingMode.HALF_UP);
        assertEquals(expected, DerivedMetrics.averageClose(candles));
    }

    @Test
    public void range() {
        List<Candle> candles = List.of(
                createCandle(0, 100, 105, 95, 102, 100),
                createCandle(1, 102, 110, 100, 108, 200),
                createCandle(2, 108, 112, 105, 110, 150)
        );
        assertEquals(BigDecimal.valueOf(17.0), DerivedMetrics.range(candles));
    }

    @Test
    public void emptyListThrows() {
        List<Candle> empty = List.of();
        assertThrows(IllegalArgumentException.class, () -> DerivedMetrics.highestHigh(empty));
        assertThrows(IllegalArgumentException.class, () -> DerivedMetrics.lowestLow(empty));
        assertThrows(IllegalArgumentException.class, () -> DerivedMetrics.averageClose(empty));
        assertThrows(IllegalArgumentException.class, () -> DerivedMetrics.range(empty));
    }
}
