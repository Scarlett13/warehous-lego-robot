package robot.behaviours.ultrasonic;

import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import lejos.robotics.SampleProvider;

public class RawReading extends TickerBehaviour {
    private final String mode;
    private final float[] buf;

    public RawReading(Agent a, long period, String distanceMode) {
        super(a, period);
        this.mode = distanceMode;
        this.buf  = new float[1];
    }

    @Override
    protected void onTick() {
//        readRaw();
        System.out.println("Reading Behaviour");
    }

    public float readRaw() {
//        mode.fetchSample(buf, 0);
//        return buf[0] > 300 ? 150: buf[0];
        return buf[0];
    }
}
