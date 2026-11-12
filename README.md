# Adaptive Distributed Media Cache

A Java caching engine for comparing cache eviction policies under realistic, skewed workloads. The long-term goal is a cache that adapts its eviction strategy to changing traffic patterns and partitions media across multiple nodes.

**Status:** early stage. Single-node eviction policies and benchmarking are implemented. The adaptive policy and the distributed layer are being designed.

## What's implemented

- **Interchangeable eviction policies** behind a common `EvictionPolicy` interface, so a new policy can be benchmarked by adding one line to the benchmark.
- **LRU (Least Recently Used):** a hash map plus a doubly linked list with sentinel nodes, giving O(1) `get` and `put`.
- **LFU (Least Frequently Used):** a hash map plus frequency buckets (`LinkedHashSet` per frequency) with an incrementally tracked minimum frequency, giving O(1) `get` and `put`. Ties are broken by least recently used.
- **Zipf workload generator:** models real-world popularity, where a few items receive most requests. It precomputes a cumulative distribution and uses binary search to draw each request in O(log n).
- **Benchmark harness:** replays identical request sequences against each policy across several random seeds and reports the hit rate for each run and the average.

## Results

200,000 requests over 1,000 distinct items, Zipf skew 1.0, cache capacity 100 (10% of items), 5 seeds:

| Seed | LRU | LFU |
|---|---|---|
| 1 | 57.6% | 65.5% |
| 2 | 57.9% | 66.4% |
| 3 | 57.5% | 64.8% |
| 4 | 57.6% | 65.1% |
| 5 | 57.8% | 66.5% |
| **Average** | **57.7%** | **65.7%** |

Under a stable Zipf workload, LFU outperforms LRU by about 8 percentage points. Item popularity never changes in this workload, so frequency is a reliable signal. LRU only tracks recency, so a moderately popular item can be evicted once enough other items are requested in between its accesses.

## Running it

Requires Java 11 or later.

```bash
javac -d out src/admc/*.java
java -cp out admc.Benchmark
```

## Next steps

- **Adaptive policy:** track workload signals, such as the share of one-off requests and shifts in popularity, and adjust the balance between recency and frequency as traffic changes.
- **More workloads:** add shifting-popularity and scan-heavy (crawler-style) workloads, where LFU's advantage is expected to shrink or reverse.
- **More policies:** implement newer policies such as SIEVE and S3-FIFO and compare them against LRU and LFU.
- **Distributed layer:** partition media chunks across multiple nodes using consistent hashing, minimising key redistribution when nodes join or leave.
- **Tests:** JUnit tests for eviction order and edge cases.