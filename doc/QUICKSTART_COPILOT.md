# Quick Start Guide for Copilot

This is a condensed guide to get GitHub Copilot or AI assistants started on implementing Stralgo-Trade immediately.

## Step 1: Acknowledge and Plan (5 minutes)

Review these documents in order:
1. `README.md` - Project overview
2. `COPILOT_ARCHITECTURE_PROMPT.md` - Full architecture specification
3. `PROJECT_SETUP.md` - Implementation reference

Confirm understanding of:
- Project goal: Automate trading using Zerodha/Upstox APIs
- Tech stack: JDK 25, Spring Boot 3.5.7, Project Reactor, LMAX Disruptor
- Architecture: Reactive, event-driven with Disruptor for low latency

## Step 2: Initialize Project (10 minutes)

```bash
# Create Gradle multi-module project structure
mkdir stralgo-trade
cd stralgo-trade

# Initialize root build files
gradle init --type basic --dsl groovy

# Create backend module
mkdir stralgo-backend
cd stralgo-backend
gradle init --type java-application --dsl groovy

# Create UI module
cd ..
mkdir stralgo-ui
cd stralgo-ui
flutter create .

# Configure settings.gradle for multi-module
echo "include 'stralgo-backend', 'stralgo-ui'" > ../settings.gradle
```

Add dependencies from `PROJECT_SETUP.md` sections "Essential Dependencies (Gradle)" and "Flutter Dependencies"

## Step 3: Create Base Structure (15 minutes)

Follow the directory structure in `PROJECT_SETUP.md`:

```
src/main/java/com/stralgo/trade/
├── StralgoTradeApplication.java
├── config/
├── domain/
├── broker/
├── marketdata/
├── strategy/
├── order/
├── risk/
├── event/
├── repository/
├── api/
└── notification/
```

## Step 4: Implement Phase 1 - Foundation (1-2 hours)

Create these files first:

### 1. Main Application Class
```java
@SpringBootApplication
@EnableR2dbcRepositories
public class StralgoTradeApplication {
    public static void main(String[] args) {
        SpringApplication.run(StralgoTradeApplication.class, args);
    }
}
```

### 2. Application Configuration
Create `application.yml` from template in `PROJECT_SETUP.md`

### 3. Domain Models
```java
// Order.java, Position.java, Instrument.java, etc.
```

### 4. Disruptor Configuration
```java
@Configuration
public class DisruptorConfig {
    // Configure LMAX Disruptor
}
```

## Step 5: Implement Phase 2 - Core Infrastructure (2-3 hours)

1. Event Bus with Disruptor
2. Domain models
3. Repository interfaces
4. Exception hierarchy
5. **Security configuration (JWT + Spring Security)**

### Security Implementation (Priority)
```java
// 1. Add User model and UserRole enum
// 2. Create JWT service for token generation/validation
// 3. Configure Spring Security with JWT filter
// 4. Add authentication endpoints (/api/auth/login, /api/auth/register)
// 5. Secure all API endpoints except auth
```

**Security Checklist:**
- [ ] JWT configuration in application.yml
- [ ] User entity with roles
- [ ] Authentication service
- [ ] JWT filter for request validation
- [ ] Password encoding (BCrypt)
- [ ] Role-based access control

## Step 6: Implement Phase 3 - Broker Integration (3-4 hours)

Priority: Zerodha first, then Upstox

1. Authentication service
2. REST API client (using WebClient)
3. WebSocket client for market data
4. Rate limiting
5. Error handling

## Step 7: Continue with Remaining Phases

Follow phases 4-10 from `COPILOT_ARCHITECTURE_PROMPT.md`:
- Phase 4: Market Data Pipeline
- Phase 5: Strategy Engine
- Phase 6: Order Management
- Phase 7: Risk Management
- Phase 8: API and UI
- Phase 9: Testing
- Phase 10: Documentation

## Key Code Patterns to Follow

### Reactive Service Pattern
```java
@Service
@RequiredArgsConstructor
public class OrderService {
    private final BrokerClient brokerClient;
    private final OrderRepository repository;
    
    public Mono<Order> placeOrder(Order order) {
        return validateOrder(order)
            .flatMap(brokerClient::submitOrder)
            .flatMap(repository::save)
            .doOnSuccess(this::publishOrderEvent);
    }
}
```

### Disruptor Event Handler Pattern
```java
@Component
public class MarketDataHandler implements EventHandler<MarketDataEvent> {
    @Override
    public void onEvent(MarketDataEvent event, long sequence, boolean endOfBatch) {
        // Process market data event
        processTickData(event);
    }
}
```

### Strategy Interface Pattern
```java
public interface TradingStrategy {
    Mono<Signal> analyze(MarketData data);
    StrategyConfig getConfig();
}
```

## Testing Approach

For each component:
1. Write unit test
2. Write integration test
3. Test with mock broker data
4. Test with real broker sandbox API

## Checkpoints

After each phase:
- ✅ All tests passing
- ✅ Code compiles without warnings
- ✅ No security vulnerabilities
- ✅ Documentation updated

## Time Estimates

- **Foundation Setup**: 2-3 hours
- **Core Infrastructure**: 3-4 hours
- **Broker Integration**: 4-5 hours
- **Market Data Pipeline**: 3-4 hours
- **Strategy Engine**: 4-5 hours
- **Order Management**: 3-4 hours
- **Risk Management**: 2-3 hours
- **API Layer**: 2-3 hours
- **UI Development**: 4-5 hours
- **Testing**: 4-5 hours
- **Documentation**: 2-3 hours

**Total Estimated Time**: 35-45 hours for complete implementation

## Critical Success Factors

1. **Start with events**: Define event model first
2. **Test continuously**: Don't write large chunks without testing
3. **Use reactive patterns**: No blocking code in reactive chains
4. **Handle errors**: Comprehensive error handling from day 1
5. **Document as you go**: Don't leave documentation for the end
6. **Security first**: Never commit credentials, always validate inputs

## Common Questions

**Q: Which broker should I implement first?**
A: Start with Zerodha - it has better documentation and wider adoption.

**Q: Should I use Maven or Gradle?**
A: Gradle is specified for this project. Use Gradle for the backend module.

**Q: How do I set up the Flutter UI?**
A: Follow the Flutter setup in `PROJECT_SETUP.md`. Run `flutter create .` in the stralgo-ui directory.

**Q: Do I need all the technologies mentioned?**
A: Core ones (Spring Boot, Reactor, Disruptor) are mandatory. Others can be added incrementally.

**Q: How do I test without real broker accounts?**
A: Use WireMock to mock broker APIs, and TestContainers for infrastructure.

**Q: What's the minimum viable product?**
A: Connection to one broker + basic market data + simple SMA strategy + manual order placement.

## Next Steps

1. Read `COPILOT_ARCHITECTURE_PROMPT.md` thoroughly
2. Initialize the project structure
3. Start with Phase 1 implementation
4. Test each component as you build
5. Iterate through all phases
6. Deploy and validate

## Support

If you get stuck:
1. Re-read the architecture prompt for that specific component
2. Check the sample code in `PROJECT_SETUP.md`
3. Refer to Spring Boot and Project Reactor documentation
4. Test with simpler scenarios first

---

**Remember**: The goal is a production-ready, high-performance trading platform. Take time to do it right, test thoroughly, and document well.

Good luck! 🚀
