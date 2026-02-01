package com.stralgo.persistence;

import com.stralgo.market.Candle;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Reads {@link Candle} instances from CSV files for replay and analysis.
 *
 * <p>The reader emits candles in time order and reconstructs domain objects
 * exactly as they were originally persisted.</p>
 *
 * <p>This class enables deterministic replay and reproducible experiments.</p>
 */

public final class CandleCsvReader {
    public Stream<Candle> read(Path file) throws IOException {
        Objects.requireNonNull(file, "file");

        String symbol = Optional.ofNullable(file.getParent())
                .map(Path::getFileName)
                .map(Path::toString)
                .orElse("unknown");

        return Files.lines(file)
                .map(line -> parseLine(symbol, line));
    }

    private Candle parseLine(String symbol, String line) {
        String[] parts = line.split(",", -1);
        if (parts.length < 6) {
            throw new IllegalArgumentException("invalid csv line: " + line);
        }
        try {
            java.time.Instant ts = java.time.Instant.parse(parts[0]);
            BigDecimal open = new BigDecimal(parts[1]);
            BigDecimal high = new BigDecimal(parts[2]);
            BigDecimal low = new BigDecimal(parts[3]);
            BigDecimal close = new BigDecimal(parts[4]);
            // volume was written as a floating value; convert to long
            long volume = new BigDecimal(parts[5]).longValue();
            return Candle.of(symbol, ts, open, high, low, close, volume);
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("failed to parse csv line: " + line, ex);
        }
    }
}

