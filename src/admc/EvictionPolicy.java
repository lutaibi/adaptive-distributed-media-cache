package admc;

public interface EvictionPolicy<K, V> {
    V get(K key);
    void put(K key, V value);
    int size();
    String name();
}
