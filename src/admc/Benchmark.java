package admc;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

/**
 * Runs the same sequence of Zipf-distributed requests against multiple
 * eviction policies and compares their hit rates, latency (p50/p99) and
 * throughput under a skewed, real-world-like access pattern.
 * <p>
 * Repeats the comparison across several seeds and reports the average,
 * so results don't depend on one lucky or unlucky run. A short legend
 * explaining each metric is printed before the results.
 */
public class Benchmark {
    // Simulated time to fetch an item from the origin on a cache miss, in ms.
    // Use 1 for latency runs, 0 for quick hit-rate-only runs.
    private static final int ORIGIN_DELAY_MS = 1;

    /**
     * Results of one benchmark run against one cache.
     */
    private record Result(double hitRate, double p50Micros, double p99Micros, double throughput) {
    }

    public static void main(String[] args) {
        int numItems = 1000;      // number of distinct keys
        int numRequests = 200_000; // total requests to simulate
        double skew = 1.0;
        int cacheCapacity = 100; // max items each cache can hold before evicting
        long[] seeds = {1, 2, 3, 4, 5};

        List<Supplier<EvictionPolicy<Integer, String>>> policies = List.of(
                () -> new LruCache<>(cacheCapacity),
                () -> new LfuCache<>(cacheCapacity));

        // Warm-up: run each policy once on a short sequence so the JIT compiler
        // has optimized the code before measuring. Results are discarded.
        // A simple stand-in for JMH-style warm-up.
        Workload warmupWorkload = new Workload(numItems, skew, 0);
        int[] warmupRequests = new int[20_000];
        for (int i = 0; i < warmupRequests.length; i++) {
            warmupRequests[i] = warmupWorkload.nextRequest();
        }
        for (Supplier<EvictionPolicy<Integer, String>> policy : policies) {
            runBenchmark(policy.get(), warmupRequests);
        }

        int numPolicies = policies.size();
        String[] names = new String[numPolicies];
        double[] hitTotals = new double[numPolicies];
        double[] p50Totals = new double[numPolicies];
        double[] p99Totals = new double[numPolicies];
        double[] throughputTotals = new double[numPolicies];

        printLegend();

        for (long seed : seeds) {
            // Generate the request sequence once per seed, so every policy
            // sees identical requests.
            Workload workload = new Workload(numItems, skew, seed);
            int[] requests = new int[numRequests];
            for (int i = 0; i < numRequests; i++) {
                requests[i] = workload.nextRequest();
            }

            System.out.println("Seed " + seed + ":");
            for (int p = 0; p < numPolicies; p++) {
                EvictionPolicy<Integer, String> cache = policies.get(p).get();
                names[p] = cache.name();
                Result r = runBenchmark(cache, requests);

                hitTotals[p] += r.hitRate();
                p50Totals[p] += r.p50Micros();
                p99Totals[p] += r.p99Micros();
                throughputTotals[p] += r.throughput();

                printResult(names[p], r.hitRate(), r.p50Micros(), r.p99Micros(), r.throughput());
            }
        }
        System.out.println("Average:");
        int numSeeds = seeds.length;
        for (int p = 0; p < numPolicies; p++) {
            printResult(names[p], hitTotals[p] / numSeeds, p50Totals[p] / numSeeds,
                    p99Totals[p] / numSeeds, throughputTotals[p] / numSeeds);
        }
    }

    /**
     * Prints a short explanation of each metric in the output.
     */
    private static void printLegend() {
        System.out.println("""
                hit   = % of requests served from the cache (higher is better)
                p50   = time the typical request took (lower is better)
                p99   = time the slowest 1% of requests took (lower is better)
                req/s = requests handled per second (higher is better)
                us    = microseconds (millionths of a second)
                """);
    }

    /**
     * Prints one line of results for a policy, with fixed-width columns
     * so lines align.
     */
    private static void printResult(String name, double hitRate, double p50Micros,
                                    double p99Micros, double throughput) {
        System.out.printf("  %-4s hit %5.1f%%  p50 %7.2f us  p99 %8.2f us  %,13.0f req/s%n",
                name, hitRate * 100, p50Micros, p99Micros, throughput);
    }

    /**
     * Simulates fetching an item from a slower backing store (the origin)
     * on a cache miss, by sleeping for ORIGIN_DELAY_MS. Models the latency
     * gap between serving a hit from memory and fetching a miss from
     * disk or another server.
     *
     * @param key the key of the item to fetch
     * @return the item's value
     */
    private static String fetchFromOrigin(int key) {
        if (ORIGIN_DELAY_MS > 0) {
            try {
                Thread.sleep(ORIGIN_DELAY_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        return "song-" + key;
    }

    /**
     * Replays a fixed sequence of requests against a single cache
     * and measures its hit rate, per-request latency and throughput.
     * A miss fetches the item from the origin and caches it for future requests.
     *
     * @param cache    the eviction policy under test
     * @param requests the sequence of keys to request, in order
     * @return hit rate (0.0 to 1.0), p50 and p99 latency in microseconds,
     * and throughput in requests per second
     */
    private static Result runBenchmark(EvictionPolicy<Integer, String> cache, int[] requests) {
        int n = requests.length;
        long[] latencies = new long[n];
        int hits = 0;

        long start = System.nanoTime();
        for (int i = 0; i < n; i++) {
            int key = requests[i];
            long t0 = System.nanoTime();

            String value = cache.get(key);
            if (value != null) {
                hits++; // cache already had it
            } else {
                cache.put(key, fetchFromOrigin(key)); // miss: fetch from origin and cache it
            }
            latencies[i] = System.nanoTime() - t0;
        }

        long totalNs = System.nanoTime() - start;

        Arrays.sort(latencies);
        double p50Micros = latencies[(int) (n * 0.50)] / 1_000.0;
        double p99Micros = latencies[(int) (n * 0.99)] / 1_000.0;
        double throughput = n / (totalNs / 1e9);

        return new Result((double) hits / n, p50Micros, p99Micros, throughput);
    }
}