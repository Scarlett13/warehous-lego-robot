package robot.sensors;

import lejos.robotics.SampleProvider;
import java.util.Arrays;

public class UltrasonicRaw {
    private final SampleProvider mode;
    private final float[] buf;

    public UltrasonicRaw(SampleProvider distanceMode) {
        this.mode = distanceMode;
        this.buf  = new float[distanceMode.sampleSize()];
    }

    public int sampleSize() {
        return mode.sampleSize();
    }


    public double readMedianCm(int n, double maxCm) {
        if (n < 3) n = 3; // small odd count
        double[] vals = new double[n];
        int k = 0;

        for (int i = 0; i < n; i++) {
            mode.fetchSample(buf, 0);   // raw cm
            float v = buf[0];

            if (!Float.isNaN(v) && !Float.isInfinite(v) && v <= (float)maxCm && v >= 0f) {
                vals[k++] = v;
            }

            // tiny pause
            try { Thread.sleep(2); } catch (InterruptedException ignored) {}
        }

        if (k == 0) return 0.0;           // no valid readings this round

        Arrays.sort(vals, 0, k);          // median of valid slice [0..k)
        return vals[k / 2];
    }

    public float readRaw() {
        mode.fetchSample(buf, 0);
        return buf[0] > 300 ? 150: buf[0];
    }
}
