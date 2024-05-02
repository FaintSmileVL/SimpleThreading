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
    private ThreadPoolExecutor _executor = new ThreadPoolExecutor(2, 4, 5, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
    private final ScheduledThreadPoolExecutor _scheduledExecutor = new ScheduledThreadPoolExecutor(4, new PriorityThreadFactory("ScheduledThreadPool", 5), new LoggingRejectedExecutionHandler());
    private boolean _shutdown;

    private ThreadPoolManager() {
        scheduleAtFixedRate(new RunnableImpl() {

            @Override
            public void runImpl() {
                ThreadPoolManager.this._scheduledExecutor.purge();
                ThreadPoolManager.this._executor.purge();
            }
        }, 300000, 300000);
    }

    public ScheduledFuture<?> scheduleAtFixedRate(Runnable r, long initial, long delay) {
        return this._scheduledExecutor.scheduleAtFixedRate(this.wrap(r), this.validate(initial), this.validate(delay), TimeUnit.MILLISECONDS);
    }

    public void execute(Runnable r)
    {
        this._executor.execute(this.wrap(r));
    }

    private long validate(long delay) {
        return Math.max(0, Math.min(MAX_DELAY, delay));
    }

    public boolean isShutdown() {
        return this._shutdown;
    }

    public Runnable wrap(Runnable r) {
        return r;
    }

    public void shutdown() throws InterruptedException {
        this._shutdown = true;
        try {
            this._scheduledExecutor.shutdown();
            this._scheduledExecutor.awaitTermination(10, TimeUnit.SECONDS);
        } finally {
            this._executor.shutdown();
            this._executor.awaitTermination(1, TimeUnit.MINUTES);
        }
    }
}
