package admc;

import java.util.HashMap;
import java.util.Map;

/**
 * A cache with no eviction policy: entries are never removed, regardless
 * of capacity. Exists as a baseline to verify the {@link EvictionPolicy}
 * contract works correctly with the simplest possible implementation,
 * before adding the complexity of a real eviction strategy.
 *
 * @param <K> the type of keys stored in the cache
 * @param <V> the type of values stored in the cache
 */
public class NoEvictionCache<K, V> implements EvictionPolicy<K, V> {
    private final Map<K, V> cacheMap = new HashMap<>();

    /**
     * @param key the key to look up
     * @return the associated value, or {@code null} if the key is not
     *         currently in the cache
     */
    @Override
    public V get(K key) {
        return cacheMap.get(key);
    }

    /**
     * @param key   the key to store
     * @param value the value to associate with the key
     */
    @Override
    public void put(K key, V value) {
        cacheMap.put(key, value);
    }

    /**
     * @return the number of entries currently held in the cache
     */
    @Override
    public int size() {
        return cacheMap.size();
    }

    /**
     * @return a short, readable label identifying this policy.
     *         Used for benchmark output.
     */
    @Override
    public String name() {
        return "NoEviction";
    }
}
