package robot.agents;
//

import jade.core.Agent;
import lejos.hardware.sensor.SensorMode;
import robot.behaviours.ultrasonic.Initalization;
import robot.behaviours.ultrasonic.RawReading;

public class UltrasonicAgent  extends Agent {
    @Override
    protected void setup() {
        super.setup();
        System.out.println("UltrasonicAgent setup");

        addBehaviour(new Initalization());
        addBehaviour(new RawReading(this, 1000, "Distance"));
    }

    @Override
    protected void takeDown() {
        super.takeDown();
        System.out.println("UltrasonicAgent takeDown");
    }
}
