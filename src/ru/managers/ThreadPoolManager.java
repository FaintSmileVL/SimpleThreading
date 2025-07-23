package ru.managers;

import lombok.Getter;
import ru.threading.LoggingRejectedExecutionHandler;
import ru.threading.PriorityThreadFactory;
import ru.threading.RunnableImpl;

import java.util.concurrent.*;

/**
 * Manages thread pools for asynchronous task execution with scheduling capabilities.
 *
 * <p>Provides two-tier thread pool management:
 * <ul>
 *   <li>Standard ThreadPoolExecutor for immediate task execution</li>
 *   <li>ScheduledThreadPoolExecutor for delayed and periodic tasks</li>
 * </ul>
 *
 * <p><b>Key features:</b>
 * <ul>
 *   <li>Singleton pattern with lazy initialization</li>
 *   <li>Automatic pool maintenance (purge every 5 minutes)</li>
 *   <li>Thread priority control via PriorityThreadFactory</li>
 *   <li>Graceful shutdown handling</li>
 *   <li>Input validation for schedule delays</li>
 * </ul>
 *
 * @author faint
 * @since 02.05.2024
 */
public class ThreadPoolManager {
    @Getter(lazy = true)
    private static final ThreadPoolManager instance = new ThreadPoolManager();
    private static final long MAX_DELAY = TimeUnit.NANOSECONDS.toMillis(Long.MAX_VALUE - System.nanoTime()) / 2;
    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(2, 4, 5, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
    private final ScheduledThreadPoolExecutor scheduledExecutor = new ScheduledThreadPoolExecutor(4, new PriorityThreadFactory("ScheduledThreadPool", 5), new LoggingRejectedExecutionHandler());
    @Getter private boolean shutdown;

    private ThreadPoolManager() {
        scheduleAtFixedRate(new RunnableImpl() {

            @Override
            public void runImpl() {
                ThreadPoolManager.this.scheduledExecutor.purge();
                ThreadPoolManager.this.executor.purge();
            }
        }, 300000, 300000);
    }

    /**
     * Schedules a periodic task that becomes enabled first after the given initial delay,
     * and subsequently with the given period between executions.
     *
     * @param r the task to execute
     * @param initial the time to delay first execution (milliseconds)
     * @param delay the period between successive executions (milliseconds)
     * @return ScheduledFuture representing pending completion of the task
     * @throws IllegalArgumentException if delay <= 0 after validation
     * @throws RejectedExecutionException if task cannot be scheduled
     */
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable r, long initial, long delay) {
        return scheduledExecutor.scheduleAtFixedRate(this.wrap(r), this.validate(initial), this.validate(delay), TimeUnit.MILLISECONDS);
    }

    /**
     * Executes the given command at some time in the future.
     *
     * @param r the runnable task
     * @throws RejectedExecutionException if task cannot be accepted
     * @throws NullPointerException if command is null
     */
    public void execute(Runnable r)
    {
        this.executor.execute(this.wrap(r));
    }

    /**
     * Creates and executes a one-shot action that becomes enabled after the given delay.
     *
     * @param r the task to execute
     * @param delay the time from now to delay execution (milliseconds)
     * @return ScheduledFuture representing pending completion of the task
     * @throws RejectedExecutionException if task cannot be scheduled
     */
    public ScheduledFuture<?> schedule(Runnable r, long delay)
    {
        return scheduledExecutor.schedule(wrap(r), delay, TimeUnit.MILLISECONDS);
    }

    /**
     * Validates and normalizes delay values to prevent overflow.
     *
     * @param delay the proposed delay in milliseconds
     * @return normalized delay value between 0 and MAX_DELAY
     */
    private long validate(long delay) {
        return Math.max(0, Math.min(MAX_DELAY, delay));
    }

    /**
     * Wraps the runnable for additional processing (hook method).
     *
     * @param r the original runnable
     * @return wrapped runnable (default implementation returns original)
     */
    public Runnable wrap(Runnable r) {
        return r;
    }

    /**
     * Initiates an orderly shutdown of all thread pools.
     *
     * <p>Execution steps:
     * <ol>
     *   <li>First shuts down scheduled executor (waiting up to 10 seconds)</li>
     *   <li>Then shuts down main executor (waiting up to 1 minute)</li>
     * </ol>
     *
     * @throws InterruptedException if interrupted while waiting
     */
    public void shutdown() throws InterruptedException {
        shutdown = true;
        try {
            scheduledExecutor.shutdown();
            scheduledExecutor.awaitTermination(10, TimeUnit.SECONDS);
        } finally {
            executor.shutdown();
            executor.awaitTermination(1, TimeUnit.MINUTES);
        }
    }
}
