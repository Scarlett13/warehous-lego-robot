package robot.hardware;

import ev3dev.actuators.lego.motors.EV3LargeRegulatedMotor;
import ev3dev.sensors.ev3.EV3ColorSensor;
import ev3dev.sensors.ev3.EV3UltrasonicSensor;
import lejos.hardware.port.MotorPort;
import lejos.hardware.port.SensorPort;
import lejos.robotics.SampleProvider;
import lejos.utility.Delay;

public class UltrasonicHardware {
    static EV3UltrasonicSensor ultrasonicSensor = null;

    public static void init() {
        System.out.println("Init ultrasonic starts");
        // Delay.msDelay(6000);
        ultrasonicSensor = new EV3UltrasonicSensor(SensorPort.S2);

        System.out.println("Init ultrasonic finishes");
    }

    public static int readRaw() {
        Delay.msDelay(1);
        SampleProvider sp = ultrasonicSensor.getDistanceMode();
        int distanceValue = 0;

        float[] sample = new float[sp.sampleSize()];

        sp.fetchSample(sample, 0);
        distanceValue = (int) sample[0];

//        System.out.println("Distance: " + sample);

        return Math.min(distanceValue, 300);
    }
}
