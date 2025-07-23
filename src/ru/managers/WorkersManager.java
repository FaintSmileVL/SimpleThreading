package ru.managers;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import ru.workers.IWorker;
import ru.workers.IWorkerType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Centralized manager for worker thread lifecycle and allocation.
 *
 * <p>This class provides thread-safe management of worker instances across different
 * worker types. Key responsibilities include:</p>
 *
 * <ul>
 *   <li>Initialization and configuration of worker pools by type</li>
 *   <li>Automatic worker allocation with load balancing</li>
 *   <li>Clean termination of worker instances</li>
 *   <li>Thread-safe access to worker collections</li>
 * </ul>
 *
 * <p><b>Implementation Notes:</b>
 * <ul>
 *   <li>Singleton pattern with lazy initialization</li>
 *   <li>Uses {@link ConcurrentHashMap} for thread-safe operations</li>
 *   <li>Worker types must implement {@link IWorkerType} interface</li>
 * </ul>
 *
 * <p><b>Usage Example:</b>
 * <pre>{@code
 * // Initialize
 * WorkersManager.getInstance().load(EnumSet.allOf(MyWorkerTypes.class));
 *
 * // Get worker
 * IWorker worker = WorkersManager.getInstance().getFreeWorker(manager);
 *
 * // Cleanup
 * WorkersManager.getInstance().purgeByType(manager);
 * }</pre>
 *
 * @author faint
 * @since 23.07.2025
 * @see IWorker
 * @see IWorkerType
 * @see IParallelManager
 */
@Slf4j
public class WorkersManager {
    @Getter(lazy = true)
    private static final WorkersManager instance = new WorkersManager();
    private final Map<IWorkerType, Map<Integer, IWorker>> workers = new ConcurrentHashMap<>();    // managerType | <workerId | worker>

    /**
     * Initializes the WorkerManager with the specified set of worker types.
     *
     * <p>This method prepares the manager to handle workers of the given types by creating
     * dedicated storage for each type. Must be called before using other manager functionality.</p>
     *
     * <p><b>Usage Example:</b>
     * <pre>{@code
     * public enum MyWorkerTypes implements IWorkerType {
     *     DB_WORKER,
     *     NETWORK_WORKER,
     *     ...
     *     PROCESSING_WORKER
     * }
     *
     * // Initialize with all enum values
     * workerManager.load(EnumSet.allOf(MyWorkerTypes.class));
     * }</pre>
     *
     * @param workerTypes a set of worker type identifiers that implement {@link IWorkerType}.
     *                    Typically an enum implementing this interface.
     * @throws IllegalArgumentException if workerTypes is null or empty
     * @throws IllegalStateException if the manager is already initialized
     * @see IWorkerType
     */
    public void load(Set<IWorkerType> workerTypes) {
        Objects.requireNonNull(workerTypes, "Worker types set cannot be null");
        if (workerTypes.isEmpty()) {
            throw new IllegalArgumentException("At least one worker type must be specified");
        }

        log.info("Initializing WorkerManager with {} worker types", workerTypes.size());

        for (IWorkerType type : workerTypes) {
            workers.putIfAbsent(type, new ConcurrentHashMap<>());
        }

        log.info("WorkerManager successfully initialized with types: {}", workerTypes);
    }

    /**
     * Terminates and removes all workers of the specified manager type.
     *
     * <p>This method performs the following operations:
     * <ol>
     *   <li>Cancels all active workers of the given type</li>
     *   <li>Clears the worker map for this type</li>
     * </ol>
     *
     * @param manager the manager whose workers should be purged
     * @throws NullPointerException if the manager is null or no workers exist for this type
     */
    public void purgeByType(IParallelManager manager) {
        Objects.requireNonNull(manager, "Manager cannot be null");
        Map<Integer, IWorker> workerMap = getWorkerMap(manager);

        workerMap.values().forEach(worker -> {
            if (worker != null) {
                worker.cancel();
            }
        });
        workerMap.clear();
    }

    /**
     * Retrieves all workers associated with the specified manager.
     *
     * @param manager the manager whose workers should be returned
     * @return a collection of workers for this manager type
     * @throws NullPointerException if the manager is null or no workers exist for this type
     */
    public Collection<IWorker> getWorkers(IParallelManager manager) {
        return getWorkerMap(manager).values();
    }

    /**
     * Gets the worker map for the specified manager type.
     *
     * @param manager the manager whose worker map should be returned
     * @return the map of worker IDs to worker instances
     * @throws NullPointerException if the manager is null or no workers exist for this type
     */
    private Map<Integer, IWorker> getWorkerMap(IParallelManager manager) {
        Objects.requireNonNull(manager, "Manager cannot be null");
        IWorkerType type = manager.getType();
        Map<Integer, IWorker> workerMap = workers.get(type);
        if (workerMap == null) {
            throw new NullPointerException("No workers registered for type: " + type);
        }
        return workerMap;
    }

    /**
     * Finds or creates a worker with available capacity.
     *
     * <p>The method performs the following logic:
     * <ol>
     *   <li>Searches existing workers for one with available capacity</li>
     *   <li>If none found, creates a new worker with incremented ID</li>
     * </ol>
     *
     * @param manager the manager requesting the worker
     * @return an available worker instance (existing or newly created)
     * @throws NullPointerException if the manager is null
     * @throws IllegalStateException if worker creation fails
     */
    public IWorker getFreeWorker(IParallelManager manager) {
        Objects.requireNonNull(manager, "Manager cannot be null");
        Map<Integer, IWorker> workers = getWorkerMap(manager);
        // Search for existing worker with capacity
        for (Map.Entry<Integer, IWorker> entry : workers.entrySet()) {
            int objectCount = manager.getObjectsCountByWorkerId(entry.getKey());
            if (objectCount < manager.getMaximumObjects()) {
                return entry.getValue();
            }
        }
        // Create new worker if none available
        int newWorkerId = workers.isEmpty() ? 0 : Collections.max(workers.keySet()) + 1;
        IWorker newWorker = manager.getFactory().createWorker(newWorkerId);
        workers.put(newWorkerId, newWorker);
        return newWorker;
    }
}
