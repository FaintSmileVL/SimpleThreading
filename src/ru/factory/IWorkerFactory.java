package ru.factory;

import ru.workers.IWorker;

/**
 * @author : faint
 * @date : 23.07.2025
 * @time : 11:15
 */
public interface IWorkerFactory {
    IWorker createWorker(int id);
}
