package robot.sensors;

import lejos.robotics.SampleProvider;
import java.util.Arrays;

@Deprecated
public class UltrasonicSync {
    private final SampleProvider mode;
    private final double farCm;
    private final double minCm;
    private final int n;
    private final float[] buf;

    public UltrasonicSync(SampleProvider distanceMode, double farCm, double stopCm, int medianN) {
        this.mode = distanceMode;
        this.farCm = farCm;
        this.minCm = Math.max(1.0, Math.min(stopCm, farCm)); // simple lower bound
        this.n = (medianN % 2 == 1) ? medianN : medianN + 1; // ensure odd
        this.buf = new float[distanceMode.sampleSize()];
    }

    public double readMedianCm() {
        double[] w = new double[n];

        for (int i = 0; i < n; i++) {
            mode.fetchSample(buf, 0);                 // raw float, meters
            double cm = metersToCm(buf[0]);           // convert to cm
            cm = clamp(cm, minCm, farCm);             // clamp instead of dropping
            w[i] = cm;

            // tiny delay to avoid repeated same-timestamp reads (adjust if needed)
            try { Thread.sleep(2); } catch (InterruptedException ignored) {}
        }

        Arrays.sort(w);
        return w[0]; // median
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private static double metersToCm(float m) {
        if (Float.isNaN(m) || Float.isInfinite(m)) return 0.0f; // will be clamped to minCm
        return m * 100.0;
    }
}
