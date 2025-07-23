package ru.threading;

import lombok.Getter;

import java.util.concurrent.*;

/**
 * @author : faint
 * @date : 02.05.2024
 * @time : 21:38
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

    public ScheduledFuture<?> scheduleAtFixedRate(Runnable r, long initial, long delay) {
        return scheduledExecutor.scheduleAtFixedRate(this.wrap(r), this.validate(initial), this.validate(delay), TimeUnit.MILLISECONDS);
    }

    public void execute(Runnable r)
    {
        this.executor.execute(this.wrap(r));
    }

    public ScheduledFuture<?> schedule(Runnable r, long delay)
    {
        return scheduledExecutor.schedule(wrap(r), delay, TimeUnit.MILLISECONDS);
    }

    private long validate(long delay) {
        return Math.max(0, Math.min(MAX_DELAY, delay));
    }

    public Runnable wrap(Runnable r) {
        return r;
    }

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
