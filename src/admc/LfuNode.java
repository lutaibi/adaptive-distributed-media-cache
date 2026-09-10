package admc;

public class LfuNode<K, V> {
    K key;
    V value;
    int freq;

    LfuNode(K key, V value) {
        this.key = key;
        this.value = value;
        this.freq = 1; // starts at 1 the moment it's first added
    }
}
