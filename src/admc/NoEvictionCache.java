package admc;

import java.util.HashMap;
import java.util.Map;

public class NoEvictionCache<K, V> implements EvictionPolicy<K, V> {
    private final Map<K, V> map = new HashMap<>();

    @Override
    public V get(K key) {
        return map.get(key);
    }

    @Override
    public void put(K key, V value) {
        map.put(key, value);
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public String name() {
        return "NoEviction";
    }
}
