package robot.utils;

import lejos.robotics.SampleProvider;

public class UltrasonicReadingUtil {
    private final SampleProvider mode;
    private final float[] buf;

    public UltrasonicReadingUtil(SampleProvider distanceMode) {
        this.mode = distanceMode;
        this.buf  = new float[distanceMode.sampleSize()];
    }

    public float readRaw() {
        mode.fetchSample(buf, 0);
        return buf[0] > 300 ? 150: buf[0];
    }

}
