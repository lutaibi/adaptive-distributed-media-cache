package admc;

public class Main {
    public static void main(String[] args) {
        EvictionPolicy<Integer, String> cache = new NoEvictionCache<>();

        cache.put(1, "a");
        cache.put(2, "b");

        System.out.println(cache.get(1));
        System.out.println(cache.get(3));
        System.out.println("Size: " +cache.size());
    }
}