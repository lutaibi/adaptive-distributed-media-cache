package admc;

// Holds one cache entry for LfuCache: its key, value, and
// current access frequency. Frequency starts at 1 and increases
// each time the entry is touched via get() or put().
class LfuNode<K, V> {
    K key;
    V value;
    int freq;

    LfuNode(K key, V value) {
        this.key = key;
        this.value = value;
        this.freq = 1;
    }
}
