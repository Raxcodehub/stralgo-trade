package com.stralgo.intraday;

import com.lmax.disruptor.BusySpinWaitStrategy;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.stralgo.intraday.event.DisruptorTickEvent;
import com.stralgo.intraday.handler.MarketStateHandler;
import com.stralgo.intraday.pipeline.TickEventPipeline;

import java.util.Objects;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Initializes, starts, and manages the Disruptor event loop.
 *
 * <p>The bootstrap handles all Disruptor lifecycle concerns:
 * <ul>
 *   <li>Creating the ring buffer with pre-allocated events</li>
 *   <li>Attaching the consumer handler</li>
 *   <li>Starting the event loop</li>
 *   <li>Graceful shutdown</li>
 * </ul>
 *
 * <p>Configuration:
 * <ul>
 *   <li>Ring size: 65536 (2^16)</li>
 *   <li>Wait strategy: BusySpinWaitStrategy (lowest latency)</li>
 *   <li>Consumer thread: named for debugging</li>
 * </ul>
 *
 * <p>No business logic is contained here; the Disruptor is purely a transport mechanism.</p>
 */
public final class DisruptorBootstrap {

    private static final int RING_BUFFER_SIZE = 65536;  // 2^16
    private static final String CONSUMER_THREAD_NAME = "disruptor-consumer";

    private final Disruptor<DisruptorTickEvent> disruptor;
    private final RingBuffer<DisruptorTickEvent> ringBuffer;

    /**
     * Create and initialize a Disruptor with the given pipeline.
     *
     * <p>The Disruptor is created but not started; call {@link #start()} to begin
     * accepting and processing events.</p>
     *
     * @param pipeline the tick event pipeline (must not be null)
     */
    public DisruptorBootstrap(TickEventPipeline pipeline) {
        Objects.requireNonNull(pipeline, "pipeline");

        // Thread factory for the consumer thread
        ThreadFactory consumerThreadFactory = new ThreadFactory() {
            private final AtomicInteger threadCount = new AtomicInteger(0);

            @Override
            public Thread newThread(java.lang.Runnable r) {
                Thread t = new Thread(r, CONSUMER_THREAD_NAME + "-" + threadCount.getAndIncrement());
                t.setDaemon(false);
                return t;
            }
        };

        // Create the Disruptor with:
        // - Event factory: creates DisruptorTickEvent instances for the ring buffer
        // - Ring buffer size: 65536
        // - Thread factory: named consumer thread
        // - Single producer, single consumer
        // - BusySpinWaitStrategy for lowest latency
        this.disruptor = new Disruptor<>(
            DisruptorTickEvent::new,
            RING_BUFFER_SIZE,
            consumerThreadFactory,
            ProducerType.SINGLE,
            new BusySpinWaitStrategy()
        );

        // Attach handler:
        // MarketStateHandler - builds candles and rolling windows from ticks
        disruptor.handleEventsWith(new MarketStateHandler(pipeline));

        // Expose the ring buffer for the publisher
        this.ringBuffer = disruptor.getRingBuffer();
    }

    /**
     * Start the Disruptor event loop.
     *
     * <p>After this call, the consumer thread is running and ready to process events.</p>
     *
     * @return this for chaining
     */
    public DisruptorBootstrap start() {
        RingBuffer<DisruptorTickEvent> start = disruptor.start();
        return this;
    }

    /**
     * Shut down the Disruptor gracefully.
     *
     * <p>Waits for all pending events to be processed before terminating the consumer thread.</p>
     */
    public void shutdown() {
        disruptor.shutdown();
    }

    /**
     * Get the ring buffer for publishing events.
     *
     * <p>The returned ring buffer should be wrapped in a {@code TickPublisher}
     * for convenient event publishing with ingest time capture.</p>
     *
     * @return the ring buffer
     */
    public RingBuffer<DisruptorTickEvent> getRingBuffer() {
        return ringBuffer;
    }

    /**
     * Get the underlying Disruptor instance for advanced operations.
     *
     * @return the Disruptor
     */
    public Disruptor<DisruptorTickEvent> getDisruptor() {
        return disruptor;
    }
}









