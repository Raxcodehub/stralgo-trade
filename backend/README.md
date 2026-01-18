# Market Backend

A Java 25 backend application for processing intraday market tick data, focusing on latency measurement and replay simulation.

## Features
- Synchronous tick processing with latency metrics
- Candle aggregation and CSV persistence
- Rolling window analysis for market insights
- Deterministic replay from stored data

## Quick Start
```bash
./gradlew run
```

## Architecture
- Simple Java implementation without unnecessary frameworks
- Event-driven pipeline for tick processing
- Comprehensive latency statistics collection

## Documentation
See [CHANGES.md](CHANGES.md) for detailed implementation history and current state.
