# Market Backend Development Log

This document summarizes the implementation steps and changes made to the Java 25 backend project for market data processing.

## Project Overview
- **Language**: Java 25
- **Architecture**: Simple, readable Java without unnecessary frameworks
- **Focus**: Intraday market tick processing, candle aggregation, latency measurement, and replay simulation

## Implementation Steps

### Step 1: TickEvent (Measuring Instrument)
- **Package**: `com.stralgo.intraday.event`
- **File**: `TickEvent.java`
- **Purpose**: Wraps a `Tick` with timing metadata for latency measurement
- **Fields**: `Tick tick`, `Instant marketTime`, `Instant receiveTime`, `Instant processedTime`
- **Rules**: `marketTime` from tick, `receiveTime` at ingestion, `processedTime` at pipeline exit

### Step 2: Event Pipeline (Single-threaded)
- **Package**: `com.stralgo.intraday.pipeline`
- **File**: `TickEventPipeline.java`
- **Purpose**: End-to-end processing of `TickEvent`
- **Features**: No IO, no concurrency, updates `processedTime`
- **Method**: `onEvent(TickEvent event)` - extracts tick, feeds to aggregator and analysis windows

### Step 3: Latency Measurement
- **Package**: `com.stralgo.intraday.metrics`
- **File**: `LatencyMetrics.java`
- **Purpose**: Collects latency statistics for `TickEvent`s
- **Metrics Tracked**:
  - Ingest latency: `receive - market`
  - Processing latency: `processed - receive`
  - End-to-end latency: `processed - market`
- **Aggregates**: Count, min, max, average (no percentiles yet)

### Step 4: Wiring in App.java
- **Method**: `runDirect()`
- **Live Simulation**: Creates 500 ticks, feeds to aggregator (writes candles to CSV), wraps in `TickEvent`, processes through pipeline, records metrics
- **Replay**: Reads candles from CSV, reconstructs ticks, processes through same pipeline and metrics
- **Output**: Periodic metrics print every 1000 ticks, final metrics summary

### Step 5: Periodic Metrics Printing
- **Implementation**: Prints tick count and average latencies every 1000 ticks
- **Format**: "Ticks: X", "Ingest latency avg: Y ms", etc.

## Issues Encountered and Fixes

### Exception: "receiveTime is before marketTime"
- **Cause**: Simulation uses future-dated timestamps, causing `Instant.now()` to be before `tick.timestamp()`
- **Fix**: Clamp `receiveTime = max(Instant.now(), tick.timestamp())`

### Exception: "received out-of-order tick"
- **Cause**: Replay ticks not sorted by timestamp
- **Fix**: Sort candles by `startTime` before processing

### Tick Count Mismatch (549 vs 49)
- **Cause**: Reactor mode only counted replay ticks, not live simulation ticks
- **Fix**: Ensure live simulation also records metrics via `TickEvent` creation and `metrics.record()`

## Reactor Experiment and Removal

### Introduction of Reactor
- **Purpose**: Experiment with reactive programming for ingestion
- **Components**:
  - `ReactorTickIngestor`: Adapter to process `Flux<Tick>` reactively
  - `runWithReactor()`: Alternative mode using Reactor for replay
- **Features**: `Flux.create` to emit reconstructed ticks from CSV, synchronous blocking for metrics consistency

### Removal of Reactor
- **Reason**: User preference to avoid reactive programming
- **Changes**:
  - Removed `reactor-core` and `reactor-test` dependencies from `build.gradle`
  - Converted `ReactorTickIngestor` to synchronous `start(Iterable<Tick>)`
  - Removed `runWithReactor()` method and reactor imports from `App.java`
  - Simplified `main()` to always call `runDirect()`

## Current State

### Architecture
- **Ingestion**: Synchronous, single-threaded processing
- **Pipeline**: `TickEventPipeline` handles aggregation and analysis
- **Metrics**: `LatencyMetrics` collects and aggregates latencies
- **Persistence**: CSV-based candle storage and replay

### Key Classes
- `TickEvent`: Event wrapper with timing
- `TickEventPipeline`: Processing spine
- `LatencyMetrics`: Statistics collector
- `CandleAggregator`: Market data aggregation
- `RollingWindow`: Time-windowed analysis
- `CandleCsvReader/Writer`: Persistence layer

### Execution Flow
1. Live simulation: Generate ticks → Aggregate to candles → Write to CSV → Process through pipeline → Record metrics
2. Replay: Read candles from CSV → Reconstruct ticks → Process through same pipeline → Record metrics
3. Output: Periodic and final latency statistics

### Dependencies
- Java 25
- Log4j2 for logging
- JUnit 5 for testing

## Testing
- Unit tests pass for all components
- Integration via `runDirect()` ensures end-to-end functionality
- Metrics validation prevents negative latencies

## Future Considerations
- No trading strategies implemented (per rules)
- No broker APIs integrated
- Latency measurement is basic (count/min/max/avg)
- Potential for Disruptor or other frameworks later, but currently simple Java

This implementation provides a solid foundation for measuring and analyzing market data processing latencies in a controlled, replayable simulation environment.</content>
<filePath>CHANGES.md
