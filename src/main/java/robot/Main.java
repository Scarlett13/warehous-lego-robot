package robot;

import ev3dev.actuators.lego.motors.EV3LargeRegulatedMotor;
import ev3dev.sensors.ev3.EV3UltrasonicSensor;
import lejos.hardware.port.MotorPort;
import lejos.hardware.port.SensorPort;
import lejos.robotics.SampleProvider;
import robot.control.MotorMixer;
import robot.control.UltrasonicPid;
import robot.sensors.UltrasonicSync;

public class Main {

    // -------- Distances (cm) --------
    public static final double TARGET_CM = 70.0;   // distance target
    public static final double AVOID_CM  = 35.0;   // begin steering below this
    public static final double STOP_CM   = 12.0;   // emergency stop
    public static final double FAR_CM    = 200.0;  // treat above as "no obstacle"

    // Stop→Resume hysteresis
    public static final double RESUME_CM = STOP_CM + 8.0;
    public static final long   RELEASE_DELAY_MS = 250;

    // -------- Motor & control limits --------
    public static final double SPEED_MIN = 80.0;   // deg/s
    public static final double SPEED_MAX = 520.0;  // deg/s
    public static final double TURN_MAX  = 0.65;   // |turn| fraction of fwd
    public static final double SLEW_RATE = 180.0;  // deg/s per second (slew limit)

    // -------- Loop timing --------
    public static final int LOOP_HZ = 20;          // 20 Hz main loop
    public static final long PERIOD_NS = 1_000_000_000L / LOOP_HZ;

    // -------- PID gains & clamps --------
    // Speed PID (distance -> forward speed)
    public static final double KP_SPEED = 5.0, KI_SPEED = 0.10, KD_SPEED = 2.5, I_MAX_SPEED = 200.0;
    // Turn PID (distance -> turn fraction); gentle
    public static final double KP_TURN = 0.015, KI_TURN = 0.0, KD_TURN = 0.010, I_MAX_TURN = 1.0;

    public static void main(String[] args) {
        // --- Hardware ---
        EV3LargeRegulatedMotor left  = new EV3LargeRegulatedMotor(MotorPort.D);
        EV3LargeRegulatedMotor right = new EV3LargeRegulatedMotor(MotorPort.A);
        EV3UltrasonicSensor usFront  = new EV3UltrasonicSensor(SensorPort.S2);
        SampleProvider distMode = usFront.getDistanceMode();

        // --- Helpers / components  ---
        //TODO: separate it in another thread or behaviour
        UltrasonicSync ultrasonic = new UltrasonicSync(distMode, FAR_CM, STOP_CM, /*medianN=*/5);
        UltrasonicPid speedPid = new UltrasonicPid(KP_SPEED, KI_SPEED, KD_SPEED, I_MAX_SPEED);
        UltrasonicPid turnPid  = new UltrasonicPid(KP_TURN,  KI_TURN,  KD_TURN,  I_MAX_TURN);
        MotorMixer mixer = new MotorMixer(SPEED_MIN, SPEED_MAX, TURN_MAX, SLEW_RATE, left, right);

        // motor comfort
        left.setAcceleration(300); //default 600
        right.setAcceleration(300);

        // --- State ---
        boolean halted = false;
        long releaseAt = 0L;
        double forwardSet; // deg/s
        double turnFrac; // [-TURN_MAX..+TURN_MAX]
        Alternator alternator = new Alternator(800); // ms to alternate between turn left or right

        // --- Main fixed-rate loop ---
        long next = System.nanoTime();
        long prevTick = next;

        try {
            while (true) {
                long now = System.nanoTime();
                double dt = Math.max(1.0 / LOOP_HZ, (now - prevTick) / 1e9);
                prevTick = now;

                // 1) Sensor read (median filtered cm)
                double d = ultrasonic.readMedianCm();

                // 2) Safety gate: stop & resume hysteresis
                if (d <= STOP_CM) {
                    halted = true;
                    releaseAt = 0L;
                } else if (halted && d >= RESUME_CM) {
                    if (releaseAt == 0L) {
                        releaseAt = System.currentTimeMillis() + RELEASE_DELAY_MS;
                    } else if (System.currentTimeMillis() >= releaseAt) {
                        halted = false; // release
                        // also reset PIDs to avoid jump
                        speedPid.reset();
                        turnPid.reset();
                    }
                }

                // 3) Compute speed setpoint via PID (distance -> forward deg/s)
                double speedOut;
                if (halted) {
                    speedOut = 0.0;
                } else {
                    double speedErr = clamp(d - TARGET_CM, -TARGET_CM, FAR_CM);
                    speedOut = speedPid.update(speedErr, dt);
                    if (speedOut < 0.0) speedOut = 0.0; // no reverse
                }
                forwardSet = clamp(speedOut, 0.0, SPEED_MAX);

                // 4) Compute turn fraction via PID (distance -> +/- turn)
                double turnOut;
                if (halted || d > AVOID_CM) {
                    turnOut = 0.0;
                    // decay integral to center quickly when safe
                    turnPid.leakIntegral(0.5 * dt);
                } else {
                    double closeness = (AVOID_CM - d);            // 0..(AVOID-STOP)
                    double sign = alternator.side();              // pick left/right
                    double turnErr = sign * closeness;
                    turnOut = turnPid.update(turnErr, dt) * 0.01; // scale cm -> fraction
                }
                turnFrac = clamp(turnOut, -TURN_MAX, TURN_MAX);

                // 5) Apply to motors with slew limiting inside the mixer
                if (halted) {
                    mixer.apply(0.0, 0.0, dt);
                } else {
                    mixer.apply(forwardSet, turnFrac, dt);
                }

                // 6) Sleep to keep fixed rate
                next = getNext(next, PERIOD_NS);
            }
        } finally {
            // safe shutdown
            try { left.stop(true); right.stop(true); } catch (Exception ignored) {}

        }
    }

    public static long getNext(long next, long periodNs) {
        next += periodNs;
        long sleepNs = next - System.nanoTime();
        if (sleepNs > 1_000_000) {
            try { Thread.sleep(sleepNs / 1_000_000, (int)(sleepNs % 1_000_000)); } catch (InterruptedException ignored) {}
        } else if (sleepNs > 0) {
            Thread.yield();
        } else {
            // overrun; real-time best-effort
            next = System.nanoTime();
        }
        return next;
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    /** Alternates +1/-1 when in avoidance, with simple time hysteresis. */
    static final class Alternator {
        private final long hysteresisMs;
        private long lastFlip = 0L;
        private int side = 1;
        Alternator(long hysteresisMs) { this.hysteresisMs = hysteresisMs; }
        double side() {
            long now = System.currentTimeMillis();
            if (now - lastFlip > hysteresisMs) {
                side = -side;
                lastFlip = now;
            }
            return side;
        }
    }
}
