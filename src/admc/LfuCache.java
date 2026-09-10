package admc;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;

public class LfuCache<K, V> implements EvictionPolicy<K, V> {
    private final int capacity;
    private int minFreq;
    private final Map<K, LfuNode<K, V>> cacheMap = new HashMap<>();
    private final Map<Integer, LinkedHashSet<LfuNode<K, V>>> freqMap = new HashMap<>();

    public LfuCache(int capacity) {
        this.capacity = capacity;
        this.minFreq = 0;
    }
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

    // Increases a node's frequency by 1, moves it to the correct frequency
    // bucket, and updates minFreq if its old bucket is now empty.
    // Used by both get() and put() whenever an existing node is accessed.
    private void touch(LfuNode<K, V> node) {
        int oldFreq = node.freq;
        freqMap.get(oldFreq).remove(node);

        // if that bucket is now empty and was the minimum, increment minFreq
        if (freqMap.get(oldFreq).isEmpty() && oldFreq == minFreq) {
            minFreq++;
        }

        node.freq++;
        freqMap.computeIfAbsent(node.freq, k -> new LinkedHashSet<>()).add(node);
    }

    @Override
    public int size() {
        return cacheMap.size();
    }

    @Override
    public String name() {
        return "LFU";
    }
}
