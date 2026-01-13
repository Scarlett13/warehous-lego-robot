package robot.sims;

import java.util.concurrent.ThreadLocalRandom;

public class UltrasonicReadingUtilDummy {
    private static final int MAX = 200;     // start & max
    private static final int HOLD_SEC = 5;  // 0..4 → 200
    private static final int DOWN_SEC = 28; // then 18 s decreasing by 10/s
    private static final int UP_SEC   = 25; // then 25 s increasing by 10/s
    private final long startMillis = System.currentTimeMillis();


    public UltrasonicReadingUtilDummy() {
    }

    public int distanceAt() {
        long now = System.currentTimeMillis();
        double tSec = Math.max(0.0, (now - startMillis) / 1000.0);

        // Phase 1: hold at 200 for first 5 seconds
        if (tSec < HOLD_SEC) {
            return MAX;
        }

        // Phase 2: next 18 seconds, decrease by 10 each full second after t=5
        if (tSec < HOLD_SEC + DOWN_SEC) {
            int decSteps = (int) Math.floor(tSec - HOLD_SEC); // 0..17
            int distance = MAX - 10 * decSteps;
            return clamp(distance, 0, MAX);
        }

        // Phase 3: next 25 seconds, increase by 10 each full second after t=23
        int baseAtPhase3 = MAX - 10 * DOWN_SEC; // value at t = 5 + 18 = 23s
        int incSteps = (int) Math.floor(tSec - (HOLD_SEC + DOWN_SEC)); // 0..24+
        int distance = baseAtPhase3 + 10 * Math.min(incSteps, UP_SEC);
        return clamp(distance, 0, MAX);
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }


    public int readRawSim(){
        double r = ThreadLocalRandom.current().nextDouble();
        if (r < 0.85) return 200;
        else if (r < 0.95) return 15;
        else return 20;
//        return 20;
//        return distanceSim;
//        return distanceAt();
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
