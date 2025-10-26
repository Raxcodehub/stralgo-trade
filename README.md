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
- **Cross-Platform UI**: Flutter-based mobile and web interface
- **Monitoring**: Built-in metrics, health checks, and observability

## Tech Stack

- **JDK 25**: Latest Java features and performance improvements
- **Spring Boot 3.5.7**: Enterprise-grade application framework
- **Project Reactor**: Reactive, non-blocking programming model
- **LMAX Disruptor**: High-performance inter-thread messaging
- **PostgreSQL/TimescaleDB**: Time-series data storage
- **Redis**: Caching and session management
- **Flutter**: Cross-platform UI framework for mobile and web
- **Gradle**: Build automation and dependency management
- **Docker**: Containerized deployment

## Getting Started

### Prerequisites

- JDK 25
- Flutter SDK (for UI development)
- Gradle 8.x
- Docker and Docker Compose
- PostgreSQL and Redis (via Docker)

### For Developers

If you're setting up the project for development:

1. Clone the repository
2. Set up the backend: `cd stralgo-backend && ./gradlew build`
3. Set up the UI: `cd stralgo-ui && flutter pub get`
4. Start infrastructure: `docker-compose up -d`
5. Configure environment variables (copy `.env.example` to `.env`)
6. Run the backend: `./gradlew bootRun` (from backend directory)
7. Run the UI: `flutter run` (from UI directory)

### For GitHub Copilot / AI Architects

If you're using GitHub Copilot or an AI assistant to implement this project:

1. **Start here**: Read [COPILOT_ARCHITECTURE_PROMPT.md](doc/COPILOT_ARCHITECTURE_PROMPT.md)
2. Follow the implementation phases outlined in the prompt
3. Use [PROJECT_SETUP.md](doc/PROJECT_SETUP.md) as a quick reference

The architecture prompt contains comprehensive requirements, design patterns, and implementation guidelines to build the entire system from scratch.

## Project Structure

```
stralgo-trade/
├── stralgo-backend/                 # Spring Boot backend module
│   ├── src/main/java/...           # Backend source code
│   ├── src/main/resources/...      # Backend resources
│   ├── src/test/...                # Backend tests
│   └── build.gradle                # Backend build script
├── stralgo-ui/                     # Flutter UI module
│   ├── lib/...                     # Flutter source code
│   ├── test/...                    # Flutter tests
│   ├── pubspec.yaml                # Flutter dependencies
│   └── android/ios/...             # Platform-specific code
├── doc/                            # Documentation folder
│   ├── COPILOT_ARCHITECTURE_PROMPT.md  # Main architecture and implementation guide
│   ├── PROJECT_SETUP.md            # Quick setup reference
│   ├── QUICKSTART_COPILOT.md       # Quick start guide for Copilot
│   ├── IMPLEMENTATION_CHECKLIST.md # Validation checklist
│   ├── PACKAGE_SUMMARY.md          # Complete package overview
│   ├── copilot-instructions.md     # Copilot instructions
│   └── .env.example                # Environment variables template
├── build.gradle                    # Root build script
├── settings.gradle                 # Gradle settings
├── README.md                       # This file
└── .gitignore                      # Git ignore rules
```

## Documentation

### For Implementation (Current)
- **[📦 Package Summary](doc/PACKAGE_SUMMARY.md)**: Overview of the complete documentation package
- **[🤖 Architecture Prompt](doc/COPILOT_ARCHITECTURE_PROMPT.md)**: Complete architecture and implementation guide for AI assistants (436 lines)
- **[🚀 Quick Start](doc/QUICKSTART_COPILOT.md)**: Condensed guide to get started immediately (235 lines)
- **[⚙️ Project Setup](doc/PROJECT_SETUP.md)**: Quick reference for structure, dependencies, and setup (487 lines)
- **[✅ Implementation Checklist](doc/IMPLEMENTATION_CHECKLIST.md)**: Comprehensive validation checklist (325 lines)
- **[🔧 Environment Template](doc/.env.example)**: Configuration template for environment variables

### To Be Created During Implementation
- ARCHITECTURE.md: Detailed architecture documentation with diagrams
- API.md: API reference and examples
- DEPLOYMENT.md: Deployment and operations guide
- STRATEGY_GUIDE.md: Guide for creating trading strategies
- BROKER_INTEGRATION.md: Broker-specific integration details
- DEVELOPMENT.md: Developer onboarding guide

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