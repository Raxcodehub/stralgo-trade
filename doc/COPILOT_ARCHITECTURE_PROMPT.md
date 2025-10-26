# GitHub Copilot Architecture Prompt for Stralgo-Trade

## Project Overview

You are tasked with architecting and implementing a high-performance automated trading system that supports both intraday and long-term trading strategies using Zerodha and Upstox broker APIs.

## Project Goals

Design and implement a robust, scalable, and maintainable trading automation platform that:
- Executes trades automatically based on configurable strategies
- Supports both intraday (day trading) and long-term (positional) trading
- Integrates with Zerodha and Upstox broker APIs for market data and order execution
- Processes market data and events with minimal latency
- Handles concurrent operations efficiently
- Provides real-time monitoring and alerting capabilities
- Ensures fault tolerance and graceful error handling
- Maintains comprehensive audit logs for all trading activities

## Tech Stack

### Core Technologies
- **JDK 25**: Latest Java Development Kit for modern language features
- **Spring Boot 3.5.7**: Application framework with dependency injection and configuration management
- **Project Reactor**: Reactive programming for non-blocking, event-driven architecture
- **LMAX Disruptor**: High-performance inter-thread messaging library for ultra-low latency event processing

### Additional Technology Recommendations
- **Spring WebFlux**: For reactive REST APIs and WebSocket connections
- **Spring Security**: For authentication and authorization
- **Spring Data R2DBC**: For reactive database access
- **PostgreSQL** or **TimescaleDB**: For time-series market data storage
- **Redis**: For caching and session management
- **Micrometer**: For metrics and monitoring
- **Logback/SLF4J**: For structured logging
- **JUnit 5 + Mockito**: For unit testing
- **TestContainers**: For integration testing
- **Flutter**: Cross-platform UI framework for mobile and web interfaces
- **Gradle**: Build automation and dependency management

## Architecture Requirements

### 1. System Architecture Pattern
Design a **Reactive Microservices Architecture** with the following characteristics:
- Event-driven design using LMAX Disruptor for critical data paths
- Reactive streams (Project Reactor) for non-blocking I/O operations
- Clear separation of concerns with modular components
- Domain-Driven Design (DDD) principles where applicable

### 2. Core Components

#### a) Market Data Service
- **Responsibility**: Subscribe to and process real-time market data from Zerodha and Upstox
- **Features**:
  - WebSocket connections for live data feeds
  - Market depth (Level 2 data) processing
  - Tick data aggregation and normalization
  - OHLC (Open-High-Low-Close) candle generation
  - Support for multiple instruments simultaneously
- **Integration**: Disruptor-based event processing pipeline

#### b) Strategy Engine
- **Responsibility**: Execute trading strategies based on market conditions
- **Features**:
  - Pluggable strategy interface for custom implementations
  - Support for technical indicators (SMA, EMA, RSI, MACD, Bollinger Bands, etc.)
  - Backtesting capabilities
  - Strategy state management
  - Risk parameters per strategy
- **Integration**: Consume events from Market Data Service via Disruptor

#### c) Order Management System (OMS)
- **Responsibility**: Manage order lifecycle and execution
- **Features**:
  - Order creation, modification, and cancellation
  - Support for different order types (Market, Limit, Stop-Loss, Bracket, Cover)
  - Position tracking and P&L calculation
  - Order validation and pre-trade risk checks
  - Retry mechanism for failed orders
  - Order status synchronization with brokers
- **Integration**: Reactive communication with broker APIs

#### d) Risk Management Module
- **Responsibility**: Enforce risk limits and position controls
- **Features**:
  - Per-strategy position limits
  - Daily loss limits
  - Maximum drawdown protection
  - Exposure limits per instrument/sector
  - Circuit breakers for abnormal conditions
  - Real-time P&L monitoring

#### e) Broker Integration Layer
- **Responsibility**: Abstract broker-specific APIs
- **Features**:
  - Unified interface for Zerodha and Upstox
  - API authentication and session management
  - Rate limiting and request throttling
  - Automatic reconnection for WebSocket connections
  - Error handling and retry logic
  - Support for sandbox/testing environments

#### f) Data Persistence Layer
- **Responsibility**: Store historical data and application state
- **Features**:
  - Time-series storage for OHLC data
  - Order and trade history
  - Strategy configurations
  - Audit logs
  - Performance metrics
- **Integration**: Use R2DBC for reactive database access

#### g) User Management & Security
- **Responsibility**: Handle user authentication, authorization, and account management
- **Features**:
  - JWT-based authentication with refresh tokens
  - User registration and login/logout
  - Role-based access control (Admin, Trader, Viewer)
  - Password encryption and validation
  - Session management and timeout handling
  - Multi-factor authentication support
  - User profile management
  - Audit logging for security events

#### h) API Gateway
- **Responsibility**: External interface for monitoring and control
- **Features**:
  - RESTful APIs for strategy management
  - WebSocket for real-time updates
  - Authentication and authorization
  - API rate limiting
  - Request validation

#### i) Notification Service
- **Responsibility**: Alert users of important events
- **Features**:
  - Email notifications
  - SMS alerts (via Twilio or similar)
  - Webhook integrations
  - Configurable alert rules
  - Alert priority levels

#### j) User Interface (Flutter)
- **Responsibility**: Provide cross-platform mobile and web interface
- **Features**:
  - Login and registration screens
  - Real-time market data visualization
  - Strategy configuration and monitoring
  - Order placement and management
  - Portfolio tracking and P&L display
  - Risk management dashboard
  - Alert notifications
  - Responsive design for mobile and web

### 3. Event Processing Architecture

Design a high-performance event processing pipeline using LMAX Disruptor:

```
Market Data Feed → Disruptor Ring Buffer → Event Handlers
                                         ├─ Market Data Processor
                                         ├─ Strategy Engine
                                         ├─ Risk Manager
                                         └─ Order Manager
```

**Key Considerations**:
- Use `MultiProducerSequencer` for multiple data sources
- Implement `WorkHandler` for parallel processing where applicable
- Configure appropriate buffer size (power of 2) based on throughput requirements
- Use `YieldingWaitStrategy` or `BusySpinWaitStrategy` for ultra-low latency

### 4. Reactive Patterns

Implement Project Reactor patterns throughout:
- **Mono**: For single-value async operations (e.g., place order)
- **Flux**: For streams of data (e.g., market ticks, order updates)
- **Schedulers**: Use bounded elastic scheduler for blocking I/O, parallel for CPU-intensive tasks
- **Backpressure**: Implement proper backpressure strategies for data streams
- **Error Handling**: Use `onErrorResume`, `retry`, and `timeout` operators appropriately

### 5. Configuration Management

- Use Spring Boot's externalized configuration
- Support for multiple profiles (dev, test, prod)
- Environment-specific configurations for broker credentials
- Feature flags for enabling/disabling strategies
- Hot-reload capability for strategy parameters where safe

### 6. Security Requirements

- **Authentication**: JWT-based authentication with refresh token support
- **Authorization**: Role-based access control (Admin, Trader, Viewer roles)
- **User Management**: Secure user registration, login, and profile management
- **Password Security**: BCrypt password hashing with strength validation
- **Session Management**: Configurable session timeouts and concurrent session limits
- **API Security**: Secure storage of broker API credentials (encrypted properties)
- **Audit Logging**: Comprehensive logging of authentication and authorization events
- **Rate Limiting**: API rate limiting and brute force protection
- **Input Validation**: Comprehensive input sanitization and validation
- **HTTPS Enforcement**: SSL/TLS encryption for all communications
- **CORS Configuration**: Proper cross-origin resource sharing setup
- **Security Headers**: Implementation of security headers (CSP, HSTS, etc.)

### 7. Monitoring and Observability

- **Metrics**: Expose metrics via Micrometer
  - Order execution latency
  - Strategy performance (win rate, P&L)
  - API response times
  - Event processing throughput
  - System resource usage
- **Logging**: Structured logging with correlation IDs
- **Health Checks**: Spring Boot Actuator endpoints
- **Tracing**: Distributed tracing for request flows

### 8. Error Handling and Resilience

- Circuit breaker pattern for external API calls (using Resilience4j)
- Retry strategies with exponential backoff
- Graceful degradation when services are unavailable
- Dead letter queue for failed events
- Comprehensive exception handling with meaningful error messages
- Automatic recovery mechanisms where possible

## Implementation Guidelines

### Phase 1: Foundation Setup
1. Initialize Spring Boot 3.5.7 project with JDK 25
2. Set up Gradle with all dependencies
3. Configure Project Reactor and LMAX Disruptor
4. Establish project structure following DDD principles
5. Set up database schema and migration scripts
6. Implement basic configuration management

### Phase 2: Core Infrastructure
1. Implement Disruptor event bus
2. Create domain models (User, Order, Position, Instrument, Strategy, Trade, etc.)
3. Set up reactive repository layer
4. Implement logging and monitoring infrastructure
5. Create base exception hierarchy
6. Set up Spring Security with JWT authentication

### Phase 2.5: User Management & Authentication
1. Implement User entity and repository
2. Create JWT token service
3. Implement authentication endpoints (register, login, logout)
4. Set up role-based authorization
5. Create user profile management
6. Implement password encryption and validation
7. Add security audit logging

### Phase 3: Broker Integration
1. Implement Zerodha API client
   - Authentication and session management
   - Market data WebSocket client
   - Order placement REST API
   - Historical data API
2. Implement Upstox API client (similar structure)
3. Create unified broker interface abstraction
4. Implement connection pooling and rate limiting
5. Add comprehensive error handling

### Phase 4: Market Data Pipeline
1. Create market data event model
2. Implement WebSocket handlers for real-time data
3. Set up Disruptor pipeline for data processing
4. Implement tick-to-candle aggregation
5. Create market data repository
6. Add data quality checks and validation

### Phase 5: Strategy Engine
1. Define strategy interface and base class
2. Implement technical indicator library
3. Create sample strategies (SMA crossover, RSI, etc.)
4. Implement strategy state machine
5. Add strategy parameter validation
6. Create strategy backtesting framework

### Phase 6: Order Management
1. Implement order creation and validation
2. Create order execution service
3. Implement position tracking
4. Add P&L calculation engine
5. Create order status reconciliation
6. Implement order history persistence

### Phase 7: Risk Management
1. Implement position limit checks
2. Create daily loss limit enforcement
3. Add exposure monitoring
4. Implement circuit breaker logic
5. Create risk reporting

### Phase 8: API and UI
1. Create RESTful API endpoints
2. Implement WebSocket for real-time updates
3. Add API documentation (Swagger/OpenAPI)
4. Initialize Flutter project for cross-platform UI
5. Implement authentication screens (login, register, profile)
6. Implement core UI screens (dashboard, strategy management, order placement)
7. Integrate with backend APIs and authentication
8. Implement real-time data updates
8. Add notification handling in UI

### Phase 9: Testing and Quality
1. Write unit tests for all core components
2. Create integration tests with TestContainers
3. Implement end-to-end tests
4. Perform load testing
5. Conduct security testing
6. Add performance benchmarks

### Phase 10: Documentation and Deployment
1. Write comprehensive README
2. Create API documentation
3. Document architecture decisions (ADR)
4. Create deployment guides
5. Set up CI/CD pipeline
6. Prepare production checklist

## Code Quality Standards

- Follow Java coding conventions and best practices
- Maintain test coverage above 80%
- Use meaningful variable and method names
- Write self-documenting code with JavaDoc for public APIs
- Apply SOLID principles
- Keep classes focused and methods concise
- Use immutable objects where appropriate
- Prefer composition over inheritance
- Handle resources properly (try-with-resources, reactive cleanup)

## Sample Code Structures

### Domain Model Example
```java
@Value
@Builder
public class Order {
    String orderId;
    String instrumentToken;
    OrderType orderType;
    TransactionType transactionType;
    BigDecimal quantity;
    BigDecimal price;
    OrderStatus status;
    Instant createdAt;
    String strategyId;
    BrokerType broker;
}
```

### Strategy Interface Example
```java
public interface TradingStrategy {
    Mono<Signal> analyze(MarketData marketData);
    Mono<Order> generateOrder(Signal signal);
    StrategyConfig getConfig();
    void updateConfig(StrategyConfig config);
}
```

### Disruptor Event Example
```java
public class MarketDataEvent {
    private String instrumentToken;
    private BigDecimal lastPrice;
    private BigDecimal volume;
    private Instant timestamp;
    private EventType eventType;
    
    public enum EventType {
        TICK, CANDLE, DEPTH
    }
}
```

### Reactive Service Example
```java
@Service
public class OrderService {
    private final BrokerClient brokerClient;
    private final OrderRepository orderRepository;
    
    public Mono<Order> placeOrder(Order order) {
        return validateOrder(order)
            .flatMap(this::checkRiskLimits)
            .flatMap(brokerClient::submitOrder)
            .flatMap(orderRepository::save)
            .doOnSuccess(this::publishOrderEvent)
            .onErrorResume(this::handleOrderError);
    }
}
```

## Zerodha API Integration Notes

- **Authentication**: Use Kite Connect API with API key and access token
- **WebSocket**: Use KiteTicker for real-time market data
- **Rate Limits**: Respect API rate limits (typically 3 requests/second)
- **Instrument Tokens**: Maintain a local cache of instrument tokens
- **Session Management**: Handle session expiry and re-authentication

## Upstox API Integration Notes

- **Authentication**: OAuth 2.0 based authentication
- **WebSocket**: Use Upstox WebSocket API for real-time data
- **Rate Limits**: Be aware of API call limits
- **Market Data**: Support for both Level 1 and Level 2 data
- **Order Types**: Map Upstox order types to internal model

## Testing Strategy

1. **Unit Tests**: Test individual components in isolation
2. **Integration Tests**: Test component interactions with TestContainers
3. **Contract Tests**: Verify API contracts with broker APIs (use WireMock)
4. **Performance Tests**: Use JMH for microbenchmarks
5. **End-to-End Tests**: Test complete trading workflows
6. **Chaos Engineering**: Test resilience under failure conditions

## Documentation Requirements

Create the following documentation:
1. **README.md**: Project overview, setup instructions, quick start guide
2. **ARCHITECTURE.md**: Detailed architecture documentation with diagrams
3. **API.md**: API documentation and examples
4. **DEPLOYMENT.md**: Deployment and configuration guide
5. **DEVELOPMENT.md**: Developer onboarding and contribution guidelines
6. **BROKER_INTEGRATION.md**: Broker-specific integration details
7. **STRATEGY_GUIDE.md**: Guide for creating custom strategies

## Success Criteria

The implementation is successful when:
- ✅ System can connect to both Zerodha and Upstox APIs
- ✅ Real-time market data is received and processed with <100ms latency
- ✅ Orders can be placed, modified, and cancelled programmatically
- ✅ At least 2 working sample strategies are implemented
- ✅ Risk management rules are enforced correctly
- ✅ System handles API failures gracefully with retries
- ✅ All critical components have >80% test coverage
- ✅ System can process >10,000 events/second via Disruptor
- ✅ API gateway provides secure access to system functions
- ✅ Comprehensive logging and monitoring are in place
- ✅ Documentation is complete and clear

## Next Steps

As GitHub Copilot, you should:
1. **Confirm Understanding**: Validate the requirements and ask clarifying questions
2. **Create Project Structure**: Initialize the Gradle project with proper module structure
3. **Generate Boilerplate**: Create Spring Boot configuration, application properties, and main classes
4. **Implement Core Models**: Generate domain models and DTOs
5. **Build Services**: Implement services following the phases outlined above
6. **Write Tests**: Generate comprehensive test suites
7. **Document**: Create all required documentation
8. **Iterate**: Refine implementation based on testing and review

## Important Considerations

- **Market Hours**: Consider market trading hours and handle pre-market, post-market scenarios
- **Holidays**: Implement holiday calendar for Indian stock exchanges (NSE, BSE)
- **Position Squaring**: Auto-square-off intraday positions before market close
- **Margin Requirements**: Check margin availability before placing orders
- **Corporate Actions**: Handle stock splits, dividends, etc.
- **Data Accuracy**: Validate data quality and handle bad ticks
- **Disaster Recovery**: Implement backup and recovery mechanisms
- **Regulatory Compliance**: Ensure compliance with SEBI regulations

## Resources

- Zerodha Kite Connect API: https://kite.trade/docs/connect/v3/
- Upstox API Documentation: https://upstox.com/developer/api-documentation/
- LMAX Disruptor: https://lmax-exchange.github.io/disruptor/
- Project Reactor Reference: https://projectreactor.io/docs/core/release/reference/
- Spring Boot Documentation: https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/

---

**Start by acknowledging this prompt and outlining your implementation approach. Then proceed with creating the project structure and implementing components incrementally.**
