package admc;

import java.util.List;
import java.util.function.Supplier;

/**
 * Runs the same sequence of Zipf-distributed requests against multiple
 * eviction policies and compares their hit rates, to evaluate how each
 * policy performs under a skewed, real-world-like access pattern.
 * Repeats the comparison across several seeds and reports the average,
 * so results don't depend on one lucky or unlucky run
 */
public class Benchmark {
    public static void main(String[] args) {
        int numItems = 1000;      // number of distinct keys
        int numRequests = 200000; // total requests to simulate
        double skew = 1.0;
        int cacheCapacity = 100; // max items each cache can hold before evicting
        long[] seeds = {1, 2, 3, 4, 5};

        List<Supplier<EvictionPolicy<Integer, String>>> policies = List.of(() -> new LruCache<>(cacheCapacity), () -> new LfuCache<>(cacheCapacity));

        double[] totals = new double[policies.size()];

        for (long seed : seeds) {
            // Generate the request sequence once per seed, so every policy
            // sees identical requests.
            Workload workload = new Workload(numItems, skew, seed);
            int[] requests = new int[numRequests];
            for (int i = 0; i < numRequests; i++) {
                requests[i] = workload.nextRequest();
            }
            StringBuilder line = new StringBuilder("Seed " + seed + ":");
            for (int p = 0; p < policies.size(); p++) {
                EvictionPolicy<Integer, String> cache = policies.get(p).get();
                double hitRate = runBenchmark(cache, requests);
                totals[p] += hitRate;
                line.append(String.format(" %s %.1f%%", cache.name(), hitRate * 100));
            }
            System.out.println(line);
        }

        StringBuilder average = new StringBuilder("Average:");
        for (int p = 0; p < policies.size(); p++) {
            String name = policies.get(p).get().name();
            average.append(String.format(" %s %.1f%%", name, totals[p] / seeds.length * 100));
        }
        System.out.println(average);
    }

    /**
     * Replays a fixed sequence of requests against a single cache
     * and measures its hit rate. A miss simulates fetching the item from
     * some slower backing store and caching it for future requests.
     *
     * @param cache    the eviction policy under test
     * @param requests the sequence of keys to request, in order
     * @return the fraction of requests that were served from the cache
     * (a value between 0.0 and 1.0)
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