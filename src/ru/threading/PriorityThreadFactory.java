package ru.threading;

import lombok.Getter;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author : faint
 * @date : 02.05.2024
 * @time : 21:38
 */
public class PriorityThreadFactory implements ThreadFactory {
    private final int prio;
    private final String name;
    private final AtomicInteger threadNumber = new AtomicInteger(1);
    @Getter private final ThreadGroup group;

    public PriorityThreadFactory(String name, int prio) {
        this.prio = prio;
        this.name = name;
        this.group = new ThreadGroup(this.name);
    }

    @Override
    public Thread newThread(Runnable r) {
        Thread t = new Thread(this.group, r);
        t.setName(this.name + "-" + this.threadNumber.getAndIncrement());
        t.setPriority(this.prio);
        return t;
    }
}
