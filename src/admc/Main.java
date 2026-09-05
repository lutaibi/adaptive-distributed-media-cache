package admc;

public class Main {
    public static void main(String[] args) {
        EvictionPolicy<Integer, String> lru = new LruCache<>(2); // capacity 2
        lru.put(1, "a");
        lru.put(2, "b");
        lru.get(1);          // touch 1, so 2 becomes least recently used
        lru.put(3, "c");     // should evict 2, not 1

        System.out.println(lru.get(1)); // expect "a" (survived)
        System.out.println(lru.get(2)); // expect null (evicted)
        System.out.println(lru.get(3)); // expect "c" (newest)
    }
}