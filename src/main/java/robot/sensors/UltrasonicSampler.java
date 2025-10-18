package robot.sensors;

import lejos.robotics.SampleProvider;
import robot.Main;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Owns a single ultrasonic distance mode (meters).
 * Runs a fixed-rate thread that continuously samples and publishes the latest filtered distance in cm.
 */
@Deprecated
public class UltrasonicSampler implements Runnable {
    private final SampleProvider mode; // distance mode (meters)
    private final double farCm;
    private final double stopCm;
    private final int medianN;
    private final int hz;
    private final AtomicBoolean running;
    private final float[] buf;

    private volatile double latestCm = 200.0; // safe default

    public UltrasonicSampler(SampleProvider distanceMode,
                             double farCm, double stopCm,
                             int medianN, int hz,
                             AtomicBoolean runningFlag) {
        this.mode = distanceMode;
        this.farCm = farCm;
        this.stopCm = stopCm;
        this.medianN = (medianN % 2 == 1) ? medianN : medianN + 1; // ensure odd
        this.hz = hz;
        this.running = runningFlag;
        this.buf = new float[distanceMode.sampleSize()];
    }

    /** Latest filtered distance in centimeters (non-blocking). */
    public double getDistanceCm() { return latestCm; }

    @Override
    public void run() {
        final long periodNs = 1_000_000_000L / hz;
        long tNext = System.nanoTime();

        while (running.get()) {
            // burst: median of N samples, with small sleeps to be nice to CPU
            double[] w = new double[medianN];
            for (int i = 0; i < medianN; i++) {
                mode.fetchSample(buf, 0);             // meters
                double cm = metersToCm(buf[0]);
                w[i] = sanitize(cm);
                try { Thread.sleep(1); } catch (InterruptedException ignored) {}
            }
            java.util.Arrays.sort(w);
            latestCm = w[w.length / 2];

            // fixed-rate pacing
            tNext = Main.getNext(tNext, periodNs);
        }
    }

    private double metersToCm(float m) {
        if (Float.isNaN(m) || Float.isInfinite(m)) return farCm;
        return m * 100.0;
    }

    private double sanitize(double cm) {
        if (cm <= 0) return stopCm;
        if (cm > farCm) return farCm;
        return cm;
    }
}
