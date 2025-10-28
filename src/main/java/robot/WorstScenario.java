package robot;

import ev3dev.actuators.lego.motors.EV3LargeRegulatedMotor;
import ev3dev.sensors.ev3.EV3UltrasonicSensor;
import lejos.hardware.port.MotorPort;
import lejos.hardware.port.SensorPort;
import lejos.robotics.SampleProvider;
import org.eclipse.paho.client.mqttv3.MqttException;
import robot.dto.PozyxPointDTO;
import robot.utils.MotorMixer;
import robot.utils.MqttUtil;
import robot.utils.PidUtil;
import robot.utils.UltrasonicReadingUtil;

import java.util.Random;

public class WorstScenario {
    // -------- Distances (cm) --------
    public static final double STOP_CM    = 8.0;
    public static final double RESUME_CM  = 12.0;
    public static final double AVOID_CM   = 35.0;
    public static final double FAR_CM     = 200.0;
    public static final double MAX_CM     = 250.0;

    // -------- Motor & control limits --------
    public static final double SPEED_MIN  = 320.0;
    public static final double SPEED_MAX  = 720.0;
    public static final double TURN_MAX   = 0.60;
    public static final double SLEW_RATE  = 180.0;

    // -------- Loop timing --------
    public static final int LOOP_HZ = 20;
    public static final long PERIOD_NS = 1_000_000_000L / LOOP_HZ;

    public static final double destinationx = 9000;
    public static final double destinationy = 14700;

    public static MqttUtil mqttUtil;

    static {
        try {
            mqttUtil = new MqttUtil(Constants.MQTT_TAG);
        } catch (MqttException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
    // --- Hardware ---
    EV3LargeRegulatedMotor left  = new EV3LargeRegulatedMotor(MotorPort.D);
    EV3LargeRegulatedMotor right = new EV3LargeRegulatedMotor(MotorPort.A);
    EV3UltrasonicSensor usFront  = new EV3UltrasonicSensor(SensorPort.S2);
    SampleProvider distMode = usFront.getDistanceMode(); // returns cm in your setup

    // --- Median filter (cm) ---
    UltrasonicReadingUtil ultrasonic = new UltrasonicReadingUtil(distMode);

    // --- Mixer (forward, turn) -> (left, right) with slew limiting ---
    MotorMixer mixer = new MotorMixer(SPEED_MIN, SPEED_MAX, TURN_MAX, SLEW_RATE, left, right);

    // motor comfort
        left.setAcceleration(300);
        right.setAcceleration(300);

    // --- State ---
    boolean halted = false;
    boolean inAvoid = false;
    int avoidSide = 0; // -1 = left, +1 = right
    final Random rng = new Random();

    double forwardTarget = 0.0; // deg/s
    double turnFrac = 0.0;      // [-TURN_MAX..+TURN_MAX]

    long next = System.nanoTime();
    long prevTick = next;

        try {
            mqttUtil = new MqttUtil(Constants.MQTT_TAG);
        while (true) {
            long now = System.nanoTime();
            double dt = Math.max(1.0 / LOOP_HZ, (now - prevTick) / 1e9);
            prevTick = now;

            // 1) Distance read from ultrasonic sensor
            double d = ultrasonic.readRaw();
            PozyxPointDTO currentPosition = mqttUtil.getLocation();

//            PidUtil.ControlResult r = PidUtil.computeControl(
//                    d, currentPosition.x, currentPosition.y, destinationx, destinationy,
//                    speedDTO.getSpeed(), motorStateDTO.isHalted(), motorStateDTO.isInTurn(), ctx.getSpeedPid(), dt
//            );

            // 5) Apply to motors (slew handled by mixer)
            if (halted) {
                mixer.apply(0.0, 0.0, dt);
            } else {
                mixer.apply(forwardTarget, turnFrac, dt);
            }

            // 6) Keep ~20 Hz
            next += PERIOD_NS;
            long sleepNs = next - System.nanoTime();
            if (sleepNs > 1_000_000) {
                try { Thread.sleep(sleepNs / 1_000_000, (int)(sleepNs % 1_000_000)); }
                catch (InterruptedException ignored) {}
            } else if (sleepNs > 0) {
                Thread.yield();
            } else {
                next = System.nanoTime();
            }
        }
    } catch (MqttException e) {
            throw new RuntimeException(e);
        } finally {
        try { left.stop(true); right.stop(true); } catch (Exception ignored) {}
    }
}

private static double clamp(double v, double lo, double hi) {
    return Math.max(lo, Math.min(hi, v));
}
}
