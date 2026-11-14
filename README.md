# Adaptive Distributed Media Cache

A Java caching engine for comparing cache eviction policies under realistic, skewed workloads. The long-term goal is a cache that adapts its eviction strategy to changing traffic patterns and partitions media across multiple nodes.

**Status:** early stage. Single-node eviction policies and benchmarking (hit rate, latency and throughput) are implemented. The adaptive policy and the distributed layer are being designed.

## What's implemented

- **Interchangeable eviction policies** behind a common `EvictionPolicy` interface, so a new policy can be benchmarked by adding one line to the benchmark.
- **LRU (Least Recently Used):** a hash map + a doubly linked list with sentinel nodes, giving O(1) `get` and `put`.
- **LFU (Least Frequently Used):** a hash map + frequency buckets (`LinkedHashSet` per frequency) with an incrementally tracked minimum frequency, giving O(1) `get` and `put`. Ties are broken by least recently used.
- **Zipf workload generator:** models real-world popularity, where a few items receive most requests. It precomputes a cumulative distribution and uses binary search to draw each request in O(log n).
- **Benchmark harness:** replays identical request sequences against each policy across several random seeds and reports, for each run and on average:
    - **hit rate**
    - **p50 and p99 latency** per request, timed with `System.nanoTime()`
    - **throughput** in requests per second
- **Simulated origin fetch:** each cache miss waits 1 ms (configurable) to imitate fetching from a slower backing store, so a policy's hit rate shows up in its latency and throughput.

## Results

200,000 requests over 1,000 distinct items, Zipf skew 1.0, cache capacity 100 (10% of items), 5 seeds.

### Hit rate

| Seed | LRU | LFU |
|---|---|---|
| 1 | 57.6% | 65.5% |
| 2 | 57.9% | 66.4% |
| 3 | 57.5% | 64.8% |
| 4 | 57.6% | 65.1% |
| 5 | 57.8% | 66.5% |
| **Average** | **57.7%** | **65.7%** |

Under a stable Zipf workload, LFU outperforms LRU by about 8 percentage points. Item popularity never changes in this workload, so frequency is a reliable signal. LRU only tracks recency, so a moderately popular item can be evicted once enough other items are requested in between its accesses.

### Latency and throughput

Averages across 5 seeds. Measured on a MacBook Pro (M2 Pro), macOS, Java 17.

**With a 1 ms origin fetch on each miss** (realistic miss cost):

| Policy | p50 | p99 | Throughput |
|---|---|---|---|
| LRU | 0.65 µs | 1,329 µs | 1,859 req/s |
| LFU | 2.03 µs | 1,314 µs | **2,285 req/s** |

**With no fetch delay** (cache overhead only):

| Policy | p50 | p99 | Throughput |
|---|---|---|---|
| LRU | 0.04 µs | 0.20 µs | **14.3M req/s** |
| LFU | 0.13 µs | 0.79 µs | 5.1M req/s |

**What this shows:** LFU does more work per request than LRU. It moves each item between frequency buckets on every access, so its p50 is about 3x higher, and with no fetch delay its throughput is about a third of LRU's.

When misses cost a realistic amount of time, the result reverses: LFU handles about 23% more requests per second (2,285 vs 1,859). Every miss means waiting over a millisecond for the origin, while LFU's extra work on each request (updating how many times an item has been used) takes about a microsecond. Each miss LFU avoids saves roughly 1,000 times more time than it spends keeping count.

Because almost all of the run time is spent waiting on misses, the throughput difference should match the difference in miss rates. LRU misses 42.3% of requests and LFU 34.3%, so LRU should take about 42.3 ÷ 34.3 ≈ 1.23 times as long. The measured ratio is 2,285 ÷ 1,859 ≈ 1.23, so nearly all of LFU's advantage comes from going to the origin less often.

p99 is about the same for both policies with the delay on, because the slowest 1% of requests are misses for both, and a miss takes as long as the fetch.

## Reading the output

The benchmark prints a short legend, then one line per policy:

```
  LRU   hit  57.6%  p50    0.67 us  p99  1302.50 us          1,860 req/s
```

- **hit:** the percentage of requests the cache answered itself, without going to the slower origin. Higher is better.
- **p50:** how long the typical request took. Half of all requests were faster than this. Lower is better.
- **p99:** how long the slowest requests took. 99% of requests were faster than this, so it shows the worst 1%. Large services watch this closely, because 1% of millions of users is still a lot of people. Lower is better.
- **req/s:** requests per second, how many lookups the cache handled each second. Higher is better.
- **us:** microseconds, millionths of a second (normally written µs).

**Seeds:** the workload uses a random number generator, and a seed is its starting value. The same seed always produces the same sequence of requests. Each seed gives a different sequence with the same popularity pattern, and every policy gets the same requests for a given seed, so the comparison is fair and repeatable.

## Running it

Requires **Java 16 or later** (the code uses records and text blocks).

```bash
javac -d out src/admc/*.java
java -cp out admc.Benchmark
```

With the default 1 ms fetch delay, a full run takes about 15–20 minutes, because every miss waits. For a quick hit-rate-only run, set `ORIGIN_DELAY_MS` to 0 at the top of `Benchmark.java`; the run then takes a few seconds.

**IntelliJ:** if the project doesn't build after cloning, right-click `src` and choose **Mark Directory as → Sources Root**.

## Design notes and limitations

- **Integer keys.** Keys are integer track IDs ranked by popularity (1 = most popular). A real system would use string IDs, but the key format doesn't affect eviction behaviour.
- **Fresh cache per run.** Each policy gets a new, empty cache for every seed, so no run starts with items left over from the previous one.
- **Simulated fetch timing.** `Thread.sleep(1)` on macOS typically sleeps about 1.3 ms, which is why p99 is above 1,000 µs.
- **JIT warm-up.** The JVM compiles frequently used code to faster machine code while the program runs, so early measurements are slower. The benchmark runs a short throwaway warm-up before measuring. Experiments showed this warm-up is too short: without it, throughput kept rising over roughly the first million requests. This mainly affects the no-delay numbers; with the 1 ms delay, waiting on misses dominates. JMH, the standard Java microbenchmarking tool, handles warm-up more rigorously (multiple rounds in separate JVM processes) and would be the next step for more reliable numbers.
- **Timer resolution.** LRU's p50 with no fetch delay (~40 ns) is close to the resolution of `System.nanoTime()` on Apple Silicon (~42 ns per tick), so it really means "40 ns or less". Throughput, timed over the whole run, is more reliable for very fast operations.

## Next steps

- **Media chunks:** cache pieces of a track (equivalent to the segments used in HLS/DASH streaming) instead of whole tracks, since listeners skip and the opening chunks are played most.
- **LFU allocation:** remove empty frequency buckets and reduce `Integer` boxing, which add garbage-collection work and are a likely cause of LFU's more variable p99.
- **Adaptive policy:** track workload signals, such as the share of one-off requests and shifts in popularity, and adjust the balance between recency and frequency as traffic changes.
- **More workloads:** add shifting-popularity and scan-heavy (crawler-style) workloads, where LFU's advantage is expected to shrink or reverse.
- **More policies:** implement newer policies such as SIEVE and S3-FIFO and compare them against LRU and LFU.
- **Distributed layer:** partition media chunks across multiple nodes using consistent hashing, minimising key redistribution when nodes join or leave.
- **Tests:** JUnit tests for eviction order and edge cases.