package admc;

public class Main {
    public static void main(String[] args) {
        Workload workload = new Workload(10, 1.0);
        int[] counts = new int[11]; // index 0 unused, keys are 1-10

        for (int i = 0; i < 100000; i++) {
            int key = workload.nextRequest();
            counts[key]++;
        }

        for (int key = 1; key <= 10; key++) {
            System.out.println("Key " + key + ": " + counts[key]);
        }

    }
}