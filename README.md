# Stralgo-Trade

A high-performance automated trading platform for Indian stock markets, supporting both intraday and long-term trading strategies using Zerodha and Upstox broker APIs.

## Overview

Stralgo-Trade is designed to automate trading decisions and execution with minimal latency, leveraging modern reactive programming paradigms and high-performance event processing.

## Key Features

- **Multi-Broker Support**: Seamlessly integrate with Zerodha and Upstox APIs
- **Real-time Market Data**: WebSocket-based live market data feeds
- **Strategy Engine**: Pluggable architecture for custom trading strategies
- **Low Latency**: LMAX Disruptor for ultra-fast event processing
- **Reactive Architecture**: Non-blocking I/O with Project Reactor
- **Risk Management**: Comprehensive risk controls and position limits
- **Order Management**: Full lifecycle management of orders and positions
- **Monitoring**: Built-in metrics, health checks, and observability

## Tech Stack

- **JDK 25**: Latest Java features and performance improvements
- **Spring Boot 3.5.7**: Enterprise-grade application framework
- **Project Reactor**: Reactive, non-blocking programming model
- **LMAX Disruptor**: High-performance inter-thread messaging
- **PostgreSQL/TimescaleDB**: Time-series data storage
- **Redis**: Caching and session management
- **Docker**: Containerized deployment

## Getting Started

### For Developers

If you're setting up the project for development:

1. Review the [Project Setup Guide](PROJECT_SETUP.md)
2. Follow the quick start instructions
3. Refer to architecture documentation for design details

### For GitHub Copilot / AI Architects

If you're using GitHub Copilot or an AI assistant to implement this project:

1. **Start here**: Read [COPILOT_ARCHITECTURE_PROMPT.md](COPILOT_ARCHITECTURE_PROMPT.md)
2. Follow the implementation phases outlined in the prompt
3. Use [PROJECT_SETUP.md](PROJECT_SETUP.md) as a quick reference

The architecture prompt contains comprehensive requirements, design patterns, and implementation guidelines to build the entire system from scratch.

## Project Structure

```
stralgo-trade/
├── COPILOT_ARCHITECTURE_PROMPT.md  # Main architecture and implementation guide
├── PROJECT_SETUP.md                # Quick setup reference
├── README.md                        # This file
└── (Implementation will be added here)
```

## Documentation

- **[Architecture Prompt](COPILOT_ARCHITECTURE_PROMPT.md)**: Complete architecture and implementation guide for AI assistants
- **[Project Setup](PROJECT_SETUP.md)**: Quick reference for project structure, dependencies, and setup

Additional documentation will be created during implementation:
- ARCHITECTURE.md: Detailed architecture documentation
- API.md: API reference and examples
- DEPLOYMENT.md: Deployment and operations guide
- STRATEGY_GUIDE.md: Guide for creating trading strategies

## Goals

This platform aims to:

1. Automate intraday and long-term trading strategies
2. Process market data with <100ms latency
3. Execute orders within 500ms of signal generation
4. Handle >10,000 events/second
5. Provide robust risk management
6. Ensure high availability and fault tolerance

## License

[To be determined]

## Contributing

[Contribution guidelines to be added]

## Disclaimer

This software is for educational and research purposes. Trading in financial markets involves risk. Use at your own discretion and always test thoroughly before live trading.