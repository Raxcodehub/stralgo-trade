package com.stralgo.intraday.event;

/**
 * Ring buffer event representing a trading signal derived from market state.
 *
 * <p>Optimized for low-latency, zero-allocation signal processing in the Disruptor.
 * All fields are mutable to enable ring buffer reuse without allocation.</p>
 *
 * <p>A signal event captures:
 * <ul>
 *   <li>symbol – instrument identifier</li>
 *   <li>signalType – type of signal (0=NONE, 1=BUY, 2=SELL, 3=HOLD)</li>
 *   <li>strength – signal strength in basis points (0-10000, where 10000 = 100%)</li>
 *   <li>price – reference price at signal generation (in smallest unit)</li>
 *   <li>timestamp – signal generation time in nanoseconds since epoch</li>
 *   <li>highestHigh – highest high from rolling window (in smallest unit)</li>
 *   <li>lowestLow – lowest low from rolling window (in smallest unit)</li>
 *   <li>range – price range (in smallest unit)</li>
 *   <li>totalVolume – total volume from rolling window</li>
 *   <li>avgClose – average close from rolling window (in smallest unit)</li>
 * </ul>
 *
 * <p>Fields are public mutable to eliminate allocation overhead in hot paths.
 * No defensive copying or wrapping – caller is responsible for correct usage.</p>
 *
 * <p>Signal types are represented as integers to avoid enum allocation:
 * <ul>
 *   <li>0 = NONE (no signal)</li>
 *   <li>1 = BUY</li>
 *   <li>2 = SELL</li>
 *   <li>3 = HOLD</li>
 * </ul>
 */
public final class SignalEvent {

    // Signal type constants
    public static final int SIGNAL_NONE = 0;
    public static final int SIGNAL_BUY = 1;
    public static final int SIGNAL_SELL = 2;
    public static final int SIGNAL_HOLD = 3;

    // Core signal fields
    public String symbol;
    public int signalType;        // 0=NONE, 1=BUY, 2=SELL, 3=HOLD
    public int strength;          // signal strength in basis points (0-10000)
    public long price;            // reference price in smallest unit
    public long timestampNanos;   // signal generation time

    // Market state snapshot (derived metrics that triggered the signal)
    public long highestHigh;      // highest high from rolling window
    public long lowestLow;        // lowest low from rolling window
    public long range;            // price range
    public long totalVolume;      // total volume from rolling window
    public long avgClose;         // average close from rolling window

    /**
     * Default constructor. All fields initialized to zero/null.
     * The Disruptor factory will instantiate this once per ring buffer slot.
     */
    public SignalEvent() {
        this.symbol = null;
        this.signalType = SIGNAL_NONE;
        this.strength = 0;
        this.price = 0L;
        this.timestampNanos = 0L;
        this.highestHigh = 0L;
        this.lowestLow = 0L;
        this.range = 0L;
        this.totalVolume = 0L;
        this.avgClose = 0L;
    }

    /**
     * Set all signal event fields.
     *
     * @param symbol the instrument symbol
     * @param signalType signal type (SIGNAL_NONE, SIGNAL_BUY, SIGNAL_SELL, SIGNAL_HOLD)
     * @param strength signal strength in basis points (0-10000)
     * @param price reference price in smallest unit
     * @param timestampNanos signal generation time in nanoseconds
     * @param highestHigh highest high from rolling window
     * @param lowestLow lowest low from rolling window
     * @param range price range
     * @param totalVolume total volume
     * @param avgClose average close
     */
    public void set(String symbol, int signalType, int strength, long price, long timestampNanos,
                    long highestHigh, long lowestLow, long range, long totalVolume, long avgClose) {
        this.symbol = symbol;
        this.signalType = signalType;
        this.strength = strength;
        this.price = price;
        this.timestampNanos = timestampNanos;
        this.highestHigh = highestHigh;
        this.lowestLow = lowestLow;
        this.range = range;
        this.totalVolume = totalVolume;
        this.avgClose = avgClose;
    }

    /**
     * Reset event to initial state for ring buffer reuse.
     * Called by Disruptor between publishing events.
     */
    public void reset() {
        this.symbol = null;
        this.signalType = SIGNAL_NONE;
        this.strength = 0;
        this.price = 0L;
        this.timestampNanos = 0L;
        this.highestHigh = 0L;
        this.lowestLow = 0L;
        this.range = 0L;
        this.totalVolume = 0L;
        this.avgClose = 0L;
    }

    /**
     * Check if this is a BUY signal.
     *
     * @return true if signal type is BUY
     */
    public boolean isBuy() {
        return signalType == SIGNAL_BUY;
    }

    /**
     * Check if this is a SELL signal.
     *
     * @return true if signal type is SELL
     */
    public boolean isSell() {
        return signalType == SIGNAL_SELL;
    }

    /**
     * Check if this is a HOLD signal.
     *
     * @return true if signal type is HOLD
     */
    public boolean isHold() {
        return signalType == SIGNAL_HOLD;
    }

    /**
     * Check if there is no signal.
     *
     * @return true if signal type is NONE
     */
    public boolean isNone() {
        return signalType == SIGNAL_NONE;
    }

    /**
     * Get signal type as string for debugging.
     *
     * @return signal type name
     */
    public String getSignalTypeName() {
        return switch (signalType) {
            case SIGNAL_NONE -> "NONE";
            case SIGNAL_BUY -> "BUY";
            case SIGNAL_SELL -> "SELL";
            case SIGNAL_HOLD -> "HOLD";
            default -> "UNKNOWN";
        };
    }

    @Override
    public String toString() {
        return "SignalEvent{" +
                "symbol='" + symbol + '\'' +
                ", signalType=" + getSignalTypeName() +
                ", strength=" + strength +
                ", price=" + price +
                ", timestampNanos=" + timestampNanos +
                ", highestHigh=" + highestHigh +
                ", lowestLow=" + lowestLow +
                ", range=" + range +
                ", totalVolume=" + totalVolume +
                ", avgClose=" + avgClose +
                '}';
    }
}

