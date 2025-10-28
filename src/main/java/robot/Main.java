package robot;

import ev3dev.actuators.lego.motors.EV3LargeRegulatedMotor;
import ev3dev.sensors.ev3.EV3UltrasonicSensor;
import lejos.hardware.port.MotorPort;
import lejos.hardware.port.SensorPort;
import lejos.robotics.SampleProvider;

import robot.utils.MotorMixer;
import robot.utils.UltrasonicReadingUtil;

import java.util.Random;

public class Main {

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
            while (true) {
//                long now = System.nanoTime();
//                double dt = Math.max(1.0 / LOOP_HZ, (now - prevTick) / 1e9);
//                prevTick = now;
//
//                // 1) Distance read from ultrasonic sensor
//                double d = ultrasonic.readRaw();
//
//                // 2) Halt / Resume
//                if (d > 0 && d <= STOP_CM) {
//                    halted = true;
//                } else if (halted && d >= RESUME_CM) {
//                    halted = false;
//                }
//
//                // 3) Avoidance band detection (non-deterministic side selection)
//                if (!halted && d > STOP_CM && d <= AVOID_CM) {
//                    if (!inAvoid) {
//                        // choose a side randomly when entering avoidance
//                        avoidSide = (rng.nextBoolean() ? +1 : -1);
//                        inAvoid = true;
//                    }
//                } else {
//                    inAvoid = false;
//                    avoidSide = 0;
//                }
//
//                // 4) Map distance -> targets (forward & turn)
//                if (halted || d == 0.0) {
//                    // d==0 means "no echo" from the filter: be safe and stop
//                    forwardTarget = 0.0;
//                    turnFrac = 0.0;
//                } else {
//                    // Forward: linear between SPEED_MIN..SPEED_MAX over [RESUME_CM..FAR_CM]
//                    double dForSpeed = clamp(d, RESUME_CM, FAR_CM);
//                    double alpha = (dForSpeed - RESUME_CM) / (FAR_CM - RESUME_CM); // 0..1
//                    forwardTarget = SPEED_MIN + alpha * (SPEED_MAX - SPEED_MIN) * 2;
//
//                    // Turn: only inside avoidance band, proportional to "closeness"
//                    if (inAvoid) {
//                        // closeness grows from 0 at AVOID_CM to 1 near STOP_CM
//                        double closeness = (AVOID_CM - clamp(d, STOP_CM, AVOID_CM)) / (AVOID_CM - STOP_CM);
//                        turnFrac = avoidSide * (closeness * TURN_MAX);
//                    } else {
//                        turnFrac = 0.0;
//                    }
//                }
//
//                // 5) Apply to motors (slew handled by mixer)
//                if (halted) {
//                    mixer.apply(0.0, 0.0, dt);
//                } else {
//                    mixer.apply(forwardTarget, turnFrac, dt);
//                }
//
//                // 6) Keep ~20 Hz
//                next += PERIOD_NS;
//                long sleepNs = next - System.nanoTime();
//                if (sleepNs > 1_000_000) {
//                    try { Thread.sleep(sleepNs / 1_000_000, (int)(sleepNs % 1_000_000)); }
//                    catch (InterruptedException ignored) {}
//                } else if (sleepNs > 0) {
//                    Thread.yield();
//                } else {
//                    next = System.nanoTime();
//                }
                left.setSpeed(300);
                right.setSpeed(300);

                left.backward();
                right.backward();
            }
        } finally {
            try { left.stop(true); right.stop(true); } catch (Exception ignored) {}
        }
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
