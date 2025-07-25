package ru.workers;

/**
 * @author : faint
 * @date : 23.07.2025
 * @time : 11:12
 */
public interface IWorker {
    int getId();

    String getLog();

    void cancel();
}
