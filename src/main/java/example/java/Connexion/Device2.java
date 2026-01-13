package example.java.Connexion;

import ev3dev.actuators.lego.motors.EV3LargeRegulatedMotor;
import ev3dev.sensors.ev3.EV3ColorSensor;
import ev3dev.sensors.ev3.EV3UltrasonicSensor;
import lejos.hardware.port.MotorPort;
import lejos.hardware.port.SensorPort;

import lejos.robotics.SampleProvider;
import lejos.robotics.filter.MaximumFilter;
import lejos.utility.Delay;

import java.io.File;

public class Device2 {
    static EV3LargeRegulatedMotor motorB = null;
    static EV3LargeRegulatedMotor motorA = null;
    static EV3UltrasonicSensor ultrasonicSensor = null;
    static EV3ColorSensor color1 = null;


    public static void init() {
        System.out.println("Init starts");
        // Delay.msDelay(6000);
        ultrasonicSensor = new EV3UltrasonicSensor(SensorPort.S2);
        motorA = new EV3LargeRegulatedMotor(MotorPort.D);
        motorB = new EV3LargeRegulatedMotor(MotorPort.A);
        color1 = new EV3ColorSensor(SensorPort.S4);
        System.out.println("Init finishes");

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            public void run() {
                System.out.println("Stop motors Ctrl+C");
                motorA.stop();
                motorB.stop();
            }
        }));

    }

    public static void stop() {
        motorA.stop();
        motorB.stop();
    }

    public static void backward() {
        motorA.forward();
        motorB.forward();
    }

    public static void forward() {
        motorA.backward();
        motorB.backward();
    }

    public static void turnLeft() {
        motorA.stop();
        motorB.backward();
    }

    public static void turnRight() {
        motorA.backward();
        motorB.stop();
    }

    public static void setSpeed(int speed) {
        motorA.setSpeed(speed);
        motorB.setSpeed(speed);
    }

    public static int check_Emergency() {


        Delay.msDelay(1);
        SampleProvider sp = ultrasonicSensor.getDistanceMode();
        int distanceValue = 0;

        float[] sample = new float[sp.sampleSize()];

        sp.fetchSample(sample, 0);
        distanceValue = (int) sample[0];

        System.out.println("Distance: " + distanceValue);

        return distanceValue;
    }

}
