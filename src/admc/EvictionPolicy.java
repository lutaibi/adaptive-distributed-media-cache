package admc;

/**
 * Defines the contract every cache eviction policy in this project follows
 * so different implementations (e.g., LRU, LFU, ARC) can be used and benchmarked
 * interchangeably without callers needing to know which one they're using.
 *
 * @param <K> the type of keys stored in the cache
 * @param <V> the type of values stored in the cache
 */
public interface EvictionPolicy<K, V> {
    /**
     * Retrieves the value associated with the given key, if present.
     * Accessing a key through this method counts as "using" it, and may
     * affect its position under the eviction policy's ordering (e.g.,
     * marking it as recently or frequently used, depending on the policy).
     *
     * @param key the key to look up
     * @return the associated value, or {@code null} if the key is not
     *         currently in the cache
     */
    V get(K key);

    /**
     * Inserts or updates the value associated with the given key. If the
     * cache is full and the key is new, an existing entry may be
     * evicted according to the policy's rules.
     *
     * @param key   the key to store
     * @param value the value to associate with the key
     */
    void put(K key, V value);

    /**
     * @return the number of entries currently held in the cache
     */
    int size();

    /**
     * @return a short, human-readable label identifying this policy
     *         (e.g., "LRU", "LFU"), used for benchmark output.
     */
    String name();
}
