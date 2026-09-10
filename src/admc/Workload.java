package admc;

import java.util.Random;
import java.util.Arrays;

/**
 * Generates a stream of requests following a Zipf distribution where a
 * small number of "popular" keys receive a disproportionate share of requests.
 * Models real-world access patterns (e.g., a few songs getting
 * most plays, most songs rarely touched).
 */

public class Workload {
    // Prefix sum of Zipf probabilities: cumulativeDistribution[i]
    // holds the running total of probability through rank (i + 1).
    // Lets a single random draw be mapped to a specific key by finding
    // which prefix-sum range it falls into.
    private final double[] cumulativeDistribution;

    private final int numItems;

    // Reused across all draws. Creating a new Random()
    // per call is wasteful and can reduce randomness quality.
    private final Random random = new Random();

    /**
     * Creates a workload generator with a precomputed Zipf distribution.
     * 
     * @param numItems number of distinct keys to generate requests for.
     * @param s        skew parameter; higher values mean a more extreme
     *                 concentration of requests on the top-ranked keys
     *                 (s = 1.0 for Zipf distribution).
     */
    public Workload(int numItems, double s) {
        this.numItems = numItems;
        this.cumulativeDistribution = buildZipfDistribution(numItems, s);
    }

    /**
     * Builds the cumulative (prefix sum) Zipf distribution for the given
     * number of items, used to map a single random draw to a specific key.
     *
     * @param numItems number of distinct keys (ranks) to generate weights for.
     * @param s        skew parameter; higher values concentrate more requests
     *                 on the top-ranked keys (s = 1.0 for Zipf distribution).
     * @return cumulative probability array, where index i holds the running
     *         total of probability through rank (i + 1)
     */
    private double[] buildZipfDistribution(int numItems, double s) {
        double[] weights = new double[numItems];
        double total = 0.0;

        // Step 1: compute raw weight (1 / k^s) for each rank
        // and track the running total
        for (int k = 1; k <= numItems; k++) {
            double currentWeight = 1.0 / Math.pow(k, s);
            weights[k - 1] = currentWeight;
            total += currentWeight;
        }

        // Step 2: build the cumulative (prefix sum) array of normalized probabilities.
        double[] cumulative = new double[numItems];
        double runningSum = 0.0;
        for (int i = 0; i < numItems; i++) {
            runningSum += weights[i] / total;
            cumulative[i] = runningSum;
        }

        return cumulative;
    }

    /**
     * Draws a single random key (1-indexed rank) according to the Zipf
     * distribution built at construction time. Uses binary search over
     * the cumulative distribution array, since it's sorted (a prefix sum
     * is always non-decreasing). This avoids the O(n) linear scan
     * per request that a plain loop would require.
     *
     * @return the selected key, as a 1-indexed rank
     */
    public int nextRequest() {
        double r = random.nextDouble(); // uniform random value in [0.0, 1.0)

        int index = Arrays.binarySearch(cumulativeDistribution, r);
        if (index < 0) {
            // No exact match (the normal case), so decode the insertion point,
            // which is exactly the first index where cumulativeDistribution[i] >= r.
            index = -index - 1;
        }
        return index + 1; // convert back to 1-indexed rank
    }
}
