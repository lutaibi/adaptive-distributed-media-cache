package admc;

public class Main {
    public static void main(String[] args) {
        EvictionPolicy<Integer, String> lfu = new LfuCache<>(2);
        EvictionPolicy<Integer, String> lru = new LruCache<>(2);

        lfu.put(1, "a");
        lfu.put(2, "b");
        lfu.get(1);
        lfu.get(1); // touch 1 twice, now freq(1) = 2 and freq(2) = 1
        lfu.put(3, "c"); // cache full, should evict 2 (lowest freq)

        System.out.println(lfu.get(1)); // expect a (survived, higher freq)
        System.out.println(lfu.get(2)); // expect null (evicted, lowest freq)
        System.out.println(lfu.get(3)); // expect c (newest)


        lru.put(1, "a");
        lru.put(2, "b");
        lru.get(1); // touch 1, moves it to front (most recently used)
        lru.put(3, "c"); // cache full, evicts 2 (least recently used, since 1 was touched more recently)

        System.out.println(lru.get(1)); // expect a (survived, was touched recently)
        System.out.println(lru.get(2)); // expect null (evicted, least recently used)
        System.out.println(lru.get(3)); // expect c (newest)
    }
}