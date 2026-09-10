package admc;

// Holds one cache entry for LruCache: its key, value, and pointers to
// its neighbors (the previous and next nodes) in the recency-ordered
// doubly linked list.
class Node<K, V> {
    K key;
    V value;
    Node<K, V> prev;
    Node<K, V> next;

    Node(K key, V value) {
        this.key = key;
        this.value = value;
    }
}
