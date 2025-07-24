package ru.workers;

import lombok.Getter;

import java.util.concurrent.ScheduledFuture;

/**
 * @author : faint
 * @date : 23.07.2025
 * @time : 11:12
 */
public abstract class Worker implements IWorker {
    @Getter protected long minTimeWork = 0;
    @Getter protected long maxTimeWork = 0;
    @Getter protected long maxObject = 0;
    @Getter protected long minObject = 0;
    protected int id;
    protected ScheduledFuture<?> worker;

    @Override
    public int getId() {
        return id;
    }

    @Override
    public void cancel() {
        if (worker != null) {
            worker.cancel(false);
            worker = null;
        }
    }

    @Override
    public String getLog() {
        StringBuilder sb = new StringBuilder();
        var className = getClass().getSimpleName();
        sb.append(className).append(": WorkerId = ").append(id).append("\n");
        sb.append(className).append(": minTimeWork = ").append(minTimeWork).append("\n");
        sb.append(className).append(": maxTimeWork = ").append(maxTimeWork).append("\n");
        sb.append(className).append(": minObject = ").append(minObject).append("\n");
        sb.append(className).append(": maxObject = ").append(maxObject).append("\n");
        return sb.toString();
    }
}
