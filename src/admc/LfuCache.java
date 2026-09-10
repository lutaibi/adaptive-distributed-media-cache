package admc;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;

/**
 * A cache that evicts the least frequently used entry when full, using
 * insertion order within each frequency bucket as a tiebreaker (the
 * least-recently-accessed entry at a given frequency is evicted first).
 *
 * @param <K> the type of keys stored in the cache
 * @param <V> the type of values stored in the cache
 */
public class LfuCache<K, V> implements EvictionPolicy<K, V> {
    private final int capacity;

    // Lowest access frequency currently present in the cache. Since frequencies
    // only increase by 1 per touch, the new minimum after a bucket empties
    // is always the old minimum + 1. Tracked incrementally instead of
    // recomputed from freqMap.
    private int minFreq;

    // Key -> node lookup. For O(1) access to a cache entry and its current
    // frequency.
    private final Map<K, LfuNode<K, V>> cacheMap = new HashMap<>();

    // Frequency -> the set of nodes currently at that frequency, in the
    // order they were last touched. Lets eviction find the least
    // frequently (and, within a tie, least recently) used entry in O(1).
    private final Map<Integer, LinkedHashSet<LfuNode<K, V>>> freqMap = new HashMap<>();

    /**
     * Creates an empty LFU cache with the given maximum capacity.
     *
     * @param capacity the maximum number of entries this cache can hold
     *                 before it begins evicting
     */
    public LfuCache(int capacity) {
        this.capacity = capacity;
        this.minFreq = 0;
    }

    /**
     * Retrieves the value associated with the given key, if present, and
     * increases the key's access frequency.
     *
     * @param key the key to look up
     * @return the associated value, or {@code null} if the key is not
     *         currently in the cache
     */
    @Override
    public V get(K key) {
        LfuNode<K, V> node = cacheMap.get(key);
        if (node == null) {
            return null;
        } else {
            touch(node);
            return node.value;
        }
    }

    /**
     *  Inserts or updates the value associated with the given key. If the key
     *  is new and the cache is full, the least frequently used entry
     *  (ties broken by least recently touched) is evicted first.
     *
     * @param key   the key to store
     * @param value the value to associate with the key
     */
    @Override
    public void put(K key, V value) {
        LfuNode<K, V> node = cacheMap.get(key);
        if (node == null) {
            if (cacheMap.size() >= capacity) {
                LfuNode<K, V> evictedNode = freqMap.get(minFreq).iterator().next();
                freqMap.get(minFreq).remove(evictedNode);
                cacheMap.remove(evictedNode.key);
            }
            LfuNode<K, V> newNode = new LfuNode<>(key, value);
            cacheMap.put(key, newNode);
            freqMap.computeIfAbsent(1, k -> new LinkedHashSet<>()).add(newNode);
            minFreq = 1;
        } else {
            node.value = value;
            touch(node);
        }
    }

    /**
     * Increases a node's frequency by 1 and moves it to the correct frequency
     * bucket, updating {@code minFreq} if its old bucket is now empty. Called
     * by both {@link #get} and {@link #put} whenever an existing entry is
     * accessed or "touched".
     *
     * @param node the node whose frequency should be increased
     */
    private void touch(LfuNode<K, V> node) {
        int oldFreq = node.freq;
        freqMap.get(oldFreq).remove(node);

        // If that bucket is now empty and was the minimum, increment minFreq.
        if (freqMap.get(oldFreq).isEmpty() && oldFreq == minFreq) {
            minFreq++;
        }

        node.freq++;
        freqMap.computeIfAbsent(node.freq, k -> new LinkedHashSet<>()).add(node);
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
        return "LFU";
    }
}
