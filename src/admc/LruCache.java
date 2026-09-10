package admc;

import java.util.HashMap;
import java.util.Map;

/**
 * A cache that evicts the least recently used entry when full. Maintains entries
 * in a doubly linked list ordered by recency, with sentinel {@code head}/{@code tail}
 * so every real node has a valid neighbor on both sides, avoiding extra null checks
 * during insertion and removal.
 *
 * @param <K> the type of keys stored in the cache
 * @param <V> the type of values stored in the cache
 */
public class LruCache<K, V> implements EvictionPolicy<K, V> {
    private final int capacity;

    // Key -> node lookup, for O(1) access to a cache entry and its
    // position in the recency-ordered list.
    private final Map<K, Node<K, V>> cacheMap = new HashMap<>();

    // Sentinel nodes marking the two ends of the list. Never hold real
    // data; head.next is always the most recently used entry, tail.prev
    // is always the least recently used.
    private final Node<K, V> head = new Node<>(null, null);
    private final Node<K, V> tail = new Node<>(null, null);

    /**
     * Creates an empty LRU cache with the given maximum capacity.
     *
     * @param capacity the maximum number of entries this cache can hold
     *                 before it begins evicting
     */
    public LruCache(int capacity) {
        this.capacity = capacity;
        head.next = tail;
        tail.prev = head;
    }

    // Unlinks a node from wherever it currently sits in the list.
    private void remove(Node<K, V> node) {
        node.prev.next = node.next;
        node.next.prev = node.prev;
    }

    // Inserts a node immediately after head, marking it as most recently used.
    private void addToFront(Node<K, V> node) {
        node.next = head.next;
        node.prev = head;
        head.next.prev = node;
        head.next = node;
    }

    /**
     * Retrieves the value associated with the given key, if present,
     * and marks it as most recently used.
     *
     * @param key the key to look up
     * @return the associated value, or {@code null} if the key is not
     *         currently in the cache
     */
    @Override
    public V get(K key) {
        Node<K, V> node = cacheMap.get(key);
        if (node == null) {
            return null;
        } else {
            remove(node);
            addToFront(node);
            return node.value;
        }
    }

    /**
     * Inserts or updates the value associated with the given key. If the
     * key is new and the cache is full, the least recently used entry is
     * evicted first.
     *
     * @param key   the key to store
     * @param value the value to associate with the key
     */
    @Override
    public void put(K key, V value) {
        Node<K, V> node = cacheMap.get(key);
        if (node != null) {
            node.value = value;
            remove(node);
            addToFront(node);
        } else {
            if (cacheMap.size() >= capacity) {
                Node<K, V> lru = tail.prev;
                remove(lru);
                cacheMap.remove(lru.key);
            }
            Node<K, V> newNode = new Node<>(key, value);
            cacheMap.put(key, newNode);
            addToFront(newNode);
        }
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
        return "LRU";
    }
}
