package com.stralgo.analysis;

import java.util.Objects;

/**
 * Maintains a fixed-size, time-ordered rolling window of tick volumes and provides
 * the rolling sum / average. Designed for low-latency hot-path usage:
 * - No allocations in the hot path (array allocated once at construction)
 * - Deterministic, constant-time update per tick
 * - Uses primitive arrays only
 *
 * Not thread-safe. Callers should ensure single-threaded access or provide their own
 * synchronization if shared between threads.
 */
public final class RollingVolumeTracker {
    private final int windowSize;
    private final long[] ring;
    // index of next slot to write (oldest element)
    private int head;
    // number of valid elements currently stored (<= windowSize)
    private int count;
    // rolling sum of elements currently in the window
    private long sum;

    /**
     * Create a tracker for a fixed number of most-recent ticks.
     *
     * @param windowSize number of entries in the fixed window (must be >= 1)
     * @throws IllegalArgumentException when windowSize <= 0
     */
    public RollingVolumeTracker(int windowSize) {
        if (windowSize <= 0) {
            throw new IllegalArgumentException("windowSize must be >= 1");
        }
        this.windowSize = windowSize;
        this.ring = new long[windowSize];
        this.head = 0;
        this.count = 0;
        this.sum = 0L;
    }

    /**
     * Add a new tick volume into the rolling window. Deterministic constant-time operation.
     * This method performs no heap allocations.
     *
     * @param volume tick volume to add (can be zero)
     */
    public void add(long volume) {
        long old = ring[head];
        sum -= old;

        ring[head] = volume;
        sum += volume;

        head = head + 1;
        if (head >= windowSize) {
            head = 0;
        }

        if (count < windowSize) {
            count++;
        }
    }

    /**
     * Convenience: add a tick and return the current rolling average.
     *
     * @param volume new tick volume
     * @return current rolling average after the add (0.0 if no elements)
     */
    public double addAndGetAverage(long volume) {
        add(volume);
        return getAverage();
    }

    /**
     * Current rolling sum of the values in the window.
     *
     * @return sum (0 if no elements)
     */
    public long getSum() {
        return sum;
    }

    /**
     * Current rolling average of the values in the window.
     *
     * @return average as double; returns 0.0 if there are no elements yet
     */
    public double getAverage() {
        return count == 0 ? 0.0 : (sum / (double) count);
    }

    /**
     * Number of values currently stored in the window (grows up to {@link #getWindowSize()}).
     *
     * @return current element count
     */
    public int getCount() {
        return count;
    }

    /**
     * Fixed size of the rolling window.
     *
     * @return configured window size
     */
    public int getWindowSize() {
        return windowSize;
    }

    /**
     * Clear the tracker. This zeroes out the internal array and resets state.
     * This method does touch every slot (O(windowSize)) but is not on the hot path.
     */
    public void reset() {
        for (int i = 0; i < ring.length; i++) {
            ring[i] = 0L;
        }
        head = 0;
        count = 0;
        sum = 0L;
    }

    /**
     * Copy current window values (oldest-first) into the provided destination array.
     * The destination must have length at least {@link #getCount()}. This method
     * does allocate nothing but writes into caller-provided array.
     *
     * @param dst destination array to receive values; must not be null
     * @throws IllegalArgumentException if dst.length < count
     * @throws NullPointerException     if dst is null
     */
    public void copyInto(long[] dst) {
        Objects.requireNonNull(dst, "dst");
        if (dst.length < count) {
            throw new IllegalArgumentException("dst length < current count");
        }
        int idx;
        int copied = 0;
        if (count == windowSize) {
            idx = head; // head points to next write => oldest is at head
            for (int i = 0; i < windowSize; i++) {
                dst[copied++] = ring[idx++];
                if (idx == windowSize) idx = 0;
            }
        } else {
            idx = 0;
            for (int i = 0; i < count; i++) {
                dst[copied++] = ring[idx++];
            }
        }
    }

    @Override
    public String toString() {
        return "RollingVolumeTracker{" +
                "windowSize=" + windowSize +
                ", count=" + count +
                ", sum=" + sum +
                ", average=" + getAverage() +
                '}';
    }
}

