package MonteCarloMini;

import java.util.Random;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.ForkJoinPool;

public class MonteCarloMinimizationParallel{
    static final boolean DEBUG = false;

    static long startTime = 0;
    static long endTime = 0;

    private static void tick() {
        startTime = System.currentTimeMillis();
    }

    private static void tock() {
        endTime = System.currentTimeMillis();
    }

    public static void main(String[] args) {

        int rows, columns;
        double xmin, xmax, ymin, ymax;
        double searches_density;

        int num_searches;
        Search[] searches;
        Random rand = new Random();

        // Set your values here
        rows = 10;
        columns = 10;

        xmin =1;
        xmax = 100000;
        ymin = 1;
        ymax = 200000;
        searches_density = 5000.44;

        if (DEBUG) {
            System.out.printf("Arguments, Rows: %d, Columns: %d\n", rows, columns);
            System.out.printf("Arguments, x_range: ( %f, %f ), y_range( %f, %f )\n", xmin, xmax, ymin, ymax);
            System.out.printf("Arguments, searches_density: %f\n", searches_density);
            System.out.printf("\n");
        }



        TerrainArea terrain = new TerrainArea(rows, columns, xmin, xmax, ymin, ymax);
        num_searches = (int) (rows * columns * searches_density);
        searches = new Search[num_searches];
        for (int i = 0; i < num_searches; i++)
            searches[i] = new Search(i + 1, rand.nextInt(rows), rand.nextInt(columns), terrain);

        if (DEBUG) {
            System.out.printf("Number searches: %d\n", num_searches);
        }
        System.out.println("exe"+searches.length);
        tick();

        ForkJoinPool pool = new ForkJoinPool();
        SearchTask searchTask = new SearchTask(searches, 0, num_searches);

        int min = pool.invoke(searchTask);

        tock();

        if (DEBUG) {
            terrain.print_heights();
            terrain.print_visited();
        }

        System.out.printf("Run parameters\n");
        System.out.printf("\t Rows: %d, Columns: %d\n", rows, columns);
        System.out.printf("\t x: [%f, %f], y: [%f, %f]\n", xmin, xmax, ymin, ymax);
        System.out.printf("\t Search density: %f (%d searches)\n", searches_density, num_searches);

        System.out.printf("Time: %d ms\n", endTime - startTime);
        int tmp = terrain.getGrid_points_visited();
        System.out.printf("Grid points visited: %d  (%2.0f%s)\n", tmp, (tmp / (rows * columns * 1.0)) * 100.0, "%");
        tmp = terrain.getGrid_points_evaluated();
        System.out.printf("Grid points evaluated: %d  (%2.0f%s)\n", tmp, (tmp / (rows * columns * 1.0)) * 100.0, "%");

        System.out.printf("Global minimum: %d at x=%.1f y=%.1f\n\n", min,
                terrain.getXcoord(searches[searchTask.getFinder()].getPos_row()),
                terrain.getYcoord(searches[searchTask.getFinder()].getPos_col()));
    }
}

class SearchTask extends RecursiveTask<Integer> {
    private static final int THRESHOLD = 551000;
    private Search[] searches;
    private int start;
    private int end;
    private int finder;

    public SearchTask(Search[] searches, int start, int end) {
        this.searches = searches;
        this.start = start;
        this.end = end;
        this.finder = start;
    }

    @Override
    protected Integer compute() {
        if (end - start <= THRESHOLD) {
            int min = Integer.MAX_VALUE;
            for (int i = start; i < end; i++) {
                int localMin = searches[i].find_valleys();
                if ((!searches[i].isStopped()) && (localMin < min)) {
                    min = localMin;
                    // finder = i;
                }
            }
            return min;
        } else {
            int mid = (end + start) / 2;
            SearchTask leftTask = new SearchTask(searches, start, mid);
            SearchTask rightTask = new SearchTask(searches, mid, end);

            leftTask.fork();
            int rightResult = rightTask.compute();
            int leftResult = leftTask.join();
            return Math.min(leftResult, rightResult);
        }
    }

    public int getFinder() {
        return finder;
    }}