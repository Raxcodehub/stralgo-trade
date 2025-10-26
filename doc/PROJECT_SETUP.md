# Stralgo-Trade: Project Setup Guide

This document provides quick setup instructions for developers and architects working on the Stralgo-Trade automated trading platform.

## Quick Start for GitHub Copilot

To begin implementation with GitHub Copilot:

1. **Review the main prompt**: Read `COPILOT_ARCHITECTURE_PROMPT.md` thoroughly
2. **Initialize the project**: Start with Maven/Gradle project setup
3. **Follow the phases**: Implement features incrementally as outlined
4. **Test continuously**: Write tests alongside implementation
5. **Document decisions**: Keep architecture decision records (ADRs)

## Initial Project Structure

```
stralgo-trade/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── stralgo/
│   │   │           └── trade/
│   │   │               ├── StralgoTradeApplication.java
│   │   │               ├── config/
│   │   │               │   ├── DisruptorConfig.java
│   │   │               │   ├── ReactorConfig.java
│   │   │               │   ├── SecurityConfig.java
│   │   │               │   └── BrokerConfig.java
│   │   │               ├── domain/
│   │   │               │   ├── model/
│   │   │               │   │   ├── Order.java
│   │   │               │   │   ├── Position.java
│   │   │               │   │   ├── Instrument.java
│   │   │               │   │   ├── Strategy.java
│   │   │               │   │   └── Trade.java
│   │   │               │   ├── event/
│   │   │               │   │   ├── MarketDataEvent.java
│   │   │               │   │   ├── OrderEvent.java
│   │   │               │   │   └── SignalEvent.java
│   │   │               │   └── enums/
│   │   │               │       ├── OrderType.java
│   │   │               │       ├── OrderStatus.java
│   │   │               │       └── TransactionType.java
│   │   │               ├── broker/
│   │   │               │   ├── api/
│   │   │               │   │   └── BrokerClient.java
│   │   │               │   ├── zerodha/
│   │   │               │   │   ├── ZerodhaClient.java
│   │   │               │   │   ├── ZerodhaWebSocket.java
│   │   │               │   │   └── ZerodhaAuthService.java
│   │   │               │   └── upstox/
│   │   │               │       ├── UpstoxClient.java
│   │   │               │       ├── UpstoxWebSocket.java
│   │   │               │       └── UpstoxAuthService.java
│   │   │               ├── marketdata/
│   │   │               │   ├── MarketDataService.java
│   │   │               │   ├── MarketDataProcessor.java
│   │   │               │   ├── CandleAggregator.java
│   │   │               │   └── TickNormalizer.java
│   │   │               ├── strategy/
│   │   │               │   ├── api/
│   │   │               │   │   ├── TradingStrategy.java
│   │   │               │   │   └── StrategyConfig.java
│   │   │               │   ├── engine/
│   │   │               │   │   └── StrategyEngine.java
│   │   │               │   ├── indicators/
│   │   │               │   │   ├── SMA.java
│   │   │               │   │   ├── EMA.java
│   │   │               │   │   ├── RSI.java
│   │   │               │   │   └── MACD.java
│   │   │               │   └── impl/
│   │   │               │       ├── SMACrossoverStrategy.java
│   │   │               │       └── RSIStrategy.java
│   │   │               ├── order/
│   │   │               │   ├── OrderService.java
│   │   │               │   ├── OrderValidator.java
│   │   │               │   ├── PositionTracker.java
│   │   │               │   └── PnLCalculator.java
│   │   │               ├── risk/
│   │   │               │   ├── RiskManager.java
│   │   │               │   ├── PositionLimitChecker.java
│   │   │               │   └── CircuitBreaker.java
│   │   │               ├── event/
│   │   │               │   ├── DisruptorEventBus.java
│   │   │               │   └── handlers/
│   │   │               │       ├── MarketDataHandler.java
│   │   │               │       ├── StrategyHandler.java
│   │   │               │       └── OrderHandler.java
│   │   │               ├── repository/
│   │   │               │   ├── OrderRepository.java
│   │   │               │   ├── PositionRepository.java
│   │   │               │   ├── TradeRepository.java
│   │   │               │   └── MarketDataRepository.java
│   │   │               ├── api/
│   │   │               │   ├── controller/
│   │   │               │   │   ├── StrategyController.java
│   │   │               │   │   ├── OrderController.java
│   │   │               │   │   └── PositionController.java
│   │   │               │   └── dto/
│   │   │               │       ├── OrderRequest.java
│   │   │               │       ├── OrderResponse.java
│   │   │               │       └── StrategyRequest.java
│   │   │               └── notification/
│   │   │                   ├── NotificationService.java
│   │   │                   └── AlertManager.java
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       ├── application-prod.yml
│   │       ├── logback-spring.xml
│   │       └── db/
│   │           └── migration/
│   │               └── V1__initial_schema.sql
│   └── test/
│       └── java/
│           └── com/
│               └── stralgo/
│                   └── trade/
│                       ├── integration/
│                       ├── unit/
│                       └── e2e/
├── pom.xml (or build.gradle)
├── .gitignore
├── docker-compose.yml
├── Dockerfile
├── README.md
├── ARCHITECTURE.md
├── COPILOT_ARCHITECTURE_PROMPT.md
└── PROJECT_SETUP.md
```

## Essential Dependencies (Maven)

```xml
<properties>
    <java.version>25</java.version>
    <spring-boot.version>3.5.7</spring-boot.version>
    <disruptor.version>4.0.0</disruptor.version>
    <reactor.version>2023.0.0</reactor.version>
</properties>

<dependencies>
    <!-- Spring Boot -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-webflux</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>
    
    <!-- Reactive Data -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-r2dbc</artifactId>
    </dependency>
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>r2dbc-postgresql</artifactId>
    </dependency>
    
    <!-- LMAX Disruptor -->
    <dependency>
        <groupId>com.lmax</groupId>
        <artifactId>disruptor</artifactId>
        <version>${disruptor.version}</version>
    </dependency>
    
    <!-- Project Reactor -->
    <dependency>
        <groupId>io.projectreactor</groupId>
        <artifactId>reactor-core</artifactId>
    </dependency>
    
    <!-- Redis -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis-reactive</artifactId>
    </dependency>
    
    <!-- Resilience4j -->
    <dependency>
        <groupId>io.github.resilience4j</groupId>
        <artifactId>resilience4j-spring-boot3</artifactId>
    </dependency>
    
    <!-- Monitoring -->
    <dependency>
        <groupId>io.micrometer</groupId>
        <artifactId>micrometer-registry-prometheus</artifactId>
    </dependency>
    
    <!-- Testing -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>io.projectreactor</groupId>
        <artifactId>reactor-test</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>testcontainers</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

## Environment Variables

Create a `.env` file (do not commit to version control):

```bash
# Zerodha Configuration
ZERODHA_API_KEY=your_api_key
ZERODHA_API_SECRET=your_api_secret
ZERODHA_ACCESS_TOKEN=your_access_token

# Upstox Configuration
UPSTOX_API_KEY=your_api_key
UPSTOX_API_SECRET=your_api_secret
UPSTOX_ACCESS_TOKEN=your_access_token

# Database
DB_HOST=localhost
DB_PORT=5432
DB_NAME=stralgo_trade
DB_USERNAME=postgres
DB_PASSWORD=postgres

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# Application
SERVER_PORT=8080
LOG_LEVEL=INFO
```

## Sample application.yml

```yaml
spring:
  application:
    name: stralgo-trade
  
  r2dbc:
    url: r2dbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  
  redis:
    host: ${REDIS_HOST}
    port: ${REDIS_PORT}

server:
  port: ${SERVER_PORT:8080}

logging:
  level:
    root: ${LOG_LEVEL:INFO}
    com.stralgo.trade: DEBUG

disruptor:
  ring-buffer-size: 8192
  wait-strategy: yielding

broker:
  zerodha:
    api-key: ${ZERODHA_API_KEY}
    api-secret: ${ZERODHA_API_SECRET}
    base-url: https://api.kite.trade
    websocket-url: wss://ws.kite.trade
  
  upstox:
    api-key: ${UPSTOX_API_KEY}
    api-secret: ${UPSTOX_API_SECRET}
    base-url: https://api.upstox.com/v2
    websocket-url: wss://ws.upstox.com

risk:
  max-daily-loss: 10000
  max-position-size: 100000
  max-drawdown-percent: 5.0

management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
```

## Docker Compose for Local Development

```yaml
version: '3.8'

services:
  postgres:
    image: postgres:16
    environment:
      POSTGRES_DB: stralgo_trade
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data

  prometheus:
    image: prom/prometheus:latest
    ports:
      - "9090:9090"
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml

  grafana:
    image: grafana/grafana:latest
    ports:
      - "3000:3000"
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=admin
    volumes:
      - grafana_data:/var/lib/grafana

volumes:
  postgres_data:
  redis_data:
  grafana_data:
```

## Development Workflow

1. **Setup Environment**: Install JDK 25, Maven, Docker
2. **Clone Repository**: `git clone <repo-url>`
3. **Start Dependencies**: `docker-compose up -d`
4. **Configure Environment**: Copy `.env.example` to `.env` and fill in values
5. **Build Project**: `mvn clean install`
6. **Run Tests**: `mvn test`
7. **Run Application**: `mvn spring-boot:run`
8. **Access APIs**: http://localhost:8080
9. **View Metrics**: http://localhost:8080/actuator/prometheus

## Key Implementation Tips

### 1. Start with the Event Model
Define your events first as they drive the architecture:
- MarketDataEvent
- SignalEvent
- OrderEvent
- RiskEvent

### 2. Configure Disruptor Early
Set up the Disruptor ring buffer and handlers as the backbone:
```java
@Configuration
public class DisruptorConfig {
    @Bean
    public Disruptor<MarketDataEvent> disruptor() {
        EventFactory<MarketDataEvent> eventFactory = MarketDataEvent::new;
        int bufferSize = 8192;
        
        Disruptor<MarketDataEvent> disruptor = new Disruptor<>(
            eventFactory,
            bufferSize,
            DaemonThreadFactory.INSTANCE,
            ProducerType.MULTI,
            new YieldingWaitStrategy()
        );
        
        disruptor.handleEventsWith(marketDataHandler())
                 .then(strategyHandler())
                 .then(orderHandler());
        
        return disruptor;
    }
}
```

### 3. Use Reactive Patterns Consistently
All I/O operations should be non-blocking:
```java
public Mono<Order> placeOrder(OrderRequest request) {
    return Mono.just(request)
        .map(this::validateRequest)
        .flatMap(brokerClient::submitOrder)
        .flatMap(orderRepository::save)
        .subscribeOn(Schedulers.boundedElastic());
}
```

### 4. Implement Comprehensive Error Handling
```java
public Mono<MarketData> fetchMarketData(String symbol) {
    return brokerClient.getMarketData(symbol)
        .timeout(Duration.ofSeconds(5))
        .retry(3)
        .onErrorResume(TimeoutException.class, e -> 
            fallbackMarketData(symbol))
        .onErrorResume(e -> {
            log.error("Failed to fetch market data", e);
            return Mono.empty();
        });
}
```

### 5. Test Reactively
```java
@Test
void testOrderPlacement() {
    StepVerifier.create(orderService.placeOrder(orderRequest))
        .expectNextMatches(order -> 
            order.getStatus() == OrderStatus.PLACED)
        .verifyComplete();
}
```

## Common Pitfalls to Avoid

1. **Blocking in Reactive Chains**: Never use blocking calls in reactive streams
2. **Large Ring Buffer**: Don't make Disruptor buffer too large (wastes memory)
3. **No Backpressure**: Always handle backpressure in Flux streams
4. **Ignoring Market Hours**: Check if market is open before trading
5. **Missing Rate Limits**: Respect broker API rate limits
6. **No Position Squaring**: Auto-square-off intraday positions
7. **Poor Error Handling**: Don't let exceptions crash the system
8. **No Audit Trail**: Log all critical operations

## Performance Targets

- **Market Data Latency**: < 100ms from broker to strategy
- **Order Execution**: < 500ms from signal to order placement
- **Event Throughput**: > 10,000 events/second via Disruptor
- **API Response Time**: < 200ms for p95
- **Database Queries**: < 50ms for p95

## Security Checklist

- [ ] API credentials stored securely (not in code)
- [ ] All endpoints require authentication
- [ ] Input validation on all API requests
- [ ] Rate limiting enabled
- [ ] Audit logging for sensitive operations
- [ ] HTTPS enforced in production
- [ ] SQL injection prevention (use parameterized queries)
- [ ] XSS prevention (sanitize inputs)

## Go-Live Checklist

- [ ] All unit tests passing
- [ ] All integration tests passing
- [ ] Load testing completed
- [ ] Security audit completed
- [ ] Documentation complete
- [ ] Monitoring and alerting configured
- [ ] Backup and recovery tested
- [ ] Broker API credentials verified
- [ ] Risk limits configured
- [ ] Circuit breakers tested
- [ ] Logging levels appropriate
- [ ] Database migrations successful
- [ ] Performance benchmarks met

---

**For questions or issues, refer to the main architecture prompt in `COPILOT_ARCHITECTURE_PROMPT.md`**
