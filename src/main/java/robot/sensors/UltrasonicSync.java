package robot.sensors;

import lejos.robotics.SampleProvider;
import java.util.Arrays;

/** Ultrasonic reader: drop invalid readings, return median in cm. */
public class UltrasonicSync {
    private final SampleProvider mode; // meters (float)
    private final double farCm;
    private final double stopCm;
    private final int n;               // odd count per read
    private final float[] buf;

    private double lastGood;           // fallback when all samples are bad

    public UltrasonicSync(SampleProvider distanceMode, double farCm, double stopCm, int medianN) {
        this.mode = distanceMode;
        this.farCm = farCm;
        this.stopCm = stopCm;
        this.n = (medianN % 2 == 1) ? medianN : medianN + 1; // ensure odd
        this.buf = new float[distanceMode.sampleSize()];
        this.lastGood = farCm; // start reasonable
    }

    /** Read N samples, drop anomaly, return median (cm). */
    public double readMedianCm() {
        double[] valid = new double[n];
        int k = 0;

        for (int i = 0; i < n; i++) {
            mode.fetchSample(buf, 0);         // raw
            double cm = metersToCm(buf[0]);   // -> converted

            // keep only sane values strictly within [stopCm..farCm]
            if (isValid(cm)) {
                valid[k++] = cm;
            }

            // tiny delay so we are not reading the same buffer timestamp
            try { Thread.sleep(2); } catch (InterruptedException ignored) {}
        }

        if (k == 0) {
            // nothing valid this round — return last known good (or far)
            return lastGood;
        }

        // compute median of the valid slice [0__k]
        Arrays.sort(valid, 0, k);
        double median = valid[k / 2];
        lastGood = median;
        return median;
    }

    private boolean isValid(double cm) {
        if (Double.isNaN(cm) || Double.isInfinite(cm)) return false;
        if (cm <= 0) return false;        // sensor glitch or impossible
        if (cm < stopCm) return false;    // treat too-close echoes as unreliable
        return !(cm > farCm);     // beyond reliable range
    }

    private double metersToCm(float raw) {
        // if raw is NaN/Inf, caller will drop it via isValid()
        return raw * 100.0;
    }
}
