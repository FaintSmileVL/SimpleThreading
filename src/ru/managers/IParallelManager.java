package ru.managers;

import ru.factory.IWorkerFactory;
import ru.workers.IWorker;
import ru.workers.IWorkerType;

import java.util.Collection;
import java.util.Map;

/**
 * @author : faint
 * @date : 23.07.2025
 * @time : 11:14
 *
 * Manages parallel execution of tasks through worker-based architecture.
 *
 * <p>Implementations should provide worker factory, capacity management,
 * and actor tracking functionality.</p>
 */
public interface IParallelManager {
    /**
     * Gets the worker factory responsible for creating new worker instances.
     *
     * @return non-null worker factory implementation
     */
    IWorkerFactory getFactory();

    /**
     * Gets the maximum number of objects a single worker can handle.
     *
     * @return positive integer representing capacity limit
     */
    int getMaximumObjects();

    /**
     * Initializes resources for a new actor.
     *
     * @param id unique identifier for the actor
     * @throws IllegalArgumentException if id is invalid
     */
    void initNewActor(int id);

    /**
     * Gets the total count of active actors across all workers.
     *
     * @return non-negative actor count
     */
    int getActorsSize();

    /**
     * Gets the actor map for a specific worker.
     *
     * @param workerId the worker identifier
     * @return immutable view of actor map, or null if worker doesn't exist
     */
    Map<Integer, ?> getActors(int workerId);

    /**
     * Gets the manager type identifier.
     *
     * @return non-null worker type
     */
    IWorkerType getType();

    /**
     * Gets all workers associated with this manager.
     *
     * <p><b>Thread Safety:</b>
     * The returned collection is thread-safe for iteration, but changes to the underlying
     * worker set will be reflected.</p>
     *
     * @return live view of workers collection
     */
    default Collection<IWorker> getWorkers() {
        return WorkersManager.getInstance().getWorkers(this);
    }

    /**
     * Gets the current workload for a specific worker.
     *
     * @param workerId the worker identifier
     * @return number of assigned actors, or -1 if worker doesn't exist
     */
    default int getObjectsCountByWorkerId(int workerId) {
        Map<Integer, ?> objects = getActors(workerId);
        return objects != null ? objects.size() : -1;
    }

    /**
     * Generates diagnostic information about current state.
     *
     * <p>Format:
     * [Worker1 Log]
     * [Worker2 Log]
     * ...
     * Total actor count = X</p>
     *
     * @return formatted diagnostic string
     */
    default String getLog() {
        StringBuilder sb = new StringBuilder();
        getWorkers().forEach(worker ->
                sb.append(worker.getLog()).append("\n")
        );
        return sb.append("Total actor count = ")
                .append(getActorsSize())
                .toString();
    }
}
