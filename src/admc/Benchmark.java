package admc;

/**
 * Runs the same sequence of Zipf-distributed requests against multiple
 * eviction policies and compares their hit rates, to evaluate how each
 * policy performs under a skewed, real-world-like access pattern.
 */
public class Benchmark {
    public static void main(String[] args) {
        int numItems = 1000;      // number of distinct keys
        int numRequests = 200000; // total requests to simulate
        double skew = 1.0;
        int cacheCapacity = 100; // max items each cache can hold before evicting

        Workload workload = new Workload(numItems, skew);

        // Generate the request sequence once, so both caches see identical requests.
        int[] requests = new int[numRequests];
        for (int i = 0; i < numRequests; i++) {
            requests[i] = workload.nextRequest();
        }

        double lruHitRate = runBenchmark(new LruCache<>(cacheCapacity), requests);
        double lfuHitRate = runBenchmark(new LfuCache<>(cacheCapacity), requests);

        System.out.println("LRU hit rate: " + lruHitRate);
        System.out.println("LFU hit rate: " + lfuHitRate);
    }

    /**
     * Replays a fixed sequence of requests against a single cache
     * and measures its hit rate. A miss simulates fetching the item from
     * some slower backing store and caching it for future requests.
     *
     * @param cache    the eviction policy under test
     * @param requests the sequence of keys to request, in order
     * @return the fraction of requests that were served from the cache
     *         (a value between 0.0 and 1.0)
     */
    private static double runBenchmark(EvictionPolicy<Integer, String> cache, int[] requests) {
        int hits = 0;

        for (int key : requests) {
            String value = cache.get(key);
            if (value != null) {
                hits++; // cache already had it
            } else {
                cache.put(key, "song-" + key); // miss: simulate fetching and caching it
            }
        }
        return (double) hits / requests.length;
    }
}
