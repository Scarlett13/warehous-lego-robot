package robot;

import ev3dev.actuators.lego.motors.EV3LargeRegulatedMotor;
import ev3dev.sensors.ev3.EV3UltrasonicSensor;
import lejos.hardware.port.MotorPort;
import lejos.hardware.port.SensorPort;
import lejos.robotics.SampleProvider;
import robot.controls.MotorMixer;
import robot.sensors.UltrasonicRaw;

import robot.pids.CommonPid;

import java.util.Random;

public class Main {

    public static void main(String[] args) {

        // --- Sensor ---
        EV3LargeRegulatedMotor left  = new EV3LargeRegulatedMotor(MotorPort.D);
        EV3LargeRegulatedMotor right = new EV3LargeRegulatedMotor(MotorPort.A);
        EV3UltrasonicSensor usFront  = new EV3UltrasonicSensor(SensorPort.S2);
        SampleProvider distMode      = usFront.getDistanceMode();

        UltrasonicRaw ultrasonic = new UltrasonicRaw(distMode);

        CommonPid speedPid = new CommonPid(Constants.PSPEED, Constants.ISPEED, Constants.DSPEED);
        speedPid.setOutputLimits(-Constants.TRIM_LIMIT, +Constants.TRIM_LIMIT);
        speedPid.setMaxIOutput(Constants.TRIM_LIMIT * 0.8);
        speedPid.setSetpointRange(Constants.SETPOINT_RANGE_CM);
        speedPid.setOutputRampRate(Constants.PID_RAMP_PER_STEP);
        speedPid.setOutputFilter(Constants.PID_OUTPUT_FILTER);
        // refer to: https://github.com/tekdemo/MiniPID-Java/blob/master/src/com/stormbots/MiniPID.java
        // speedPid.setDirection(true);

        // --- Mixer (forward, turn) ---
        MotorMixer mixer = new MotorMixer(Constants.SPEED_MIN, Constants.SPEED_MAX, Constants.TURN_MAX, Constants.SLEW_RATE, left, right);

        // Motor comfort
        left.setAcceleration(300);
        right.setAcceleration(300);

        // --- State ---
        boolean halted   = false;
        boolean inAvoid  = false;
        int avoidSide    = 0;
        final Random rng = new Random();

        double forwardTarget = 0.0;
        double turnFrac      = 0.0;

        long next = System.nanoTime();
        long prevTick = next;

        try {
            while (true) {
                long now = System.nanoTime();
                double dt = Math.max(1.0 / Constants.LOOP_HZ, (now - prevTick) / 1e9);
                prevTick = now;

                // 1) Distance
                double d = ultrasonic.readRaw();
                if (d == 0.0) d = Constants.FAR_CM;

                // 2) Halt / Resume (reset PID on transitions)
                if (d >= 0 && d <= Constants.STOP_CM) {
                    if (!halted) {
                        halted = true;
                        speedPid.reset(); // clear I/D, align internal state
                    }
                } else if (halted && d >= Constants.RESUME_CM) {
                    halted = false;
                    speedPid.reset(); // avoid kick on release
                }

                // 3) Avoidance band detection (random side, held while in band)
                if (!halted && d > Constants.STOP_CM && d <= Constants.AVOID_CM) {
                    if (!inAvoid) {
                        avoidSide = (rng.nextBoolean() ? +1 : -1);
                        inAvoid = true;
                    }
                } else {
                    inAvoid = false;
                    avoidSide = 0;
                }

                // 4) Forward speed = base (distance map) + PID trim; Turn = simple proportional (no PID)
                if (halted) {
                    forwardTarget = 0.0;
                    turnFrac = 0.0;
                } else {
                    // ---- Base speed
                    double alpha = (clamp(d, Constants.RESUME_CM, Constants.FAR_CM) - Constants.RESUME_CM) / (Constants.FAR_CM - Constants.RESUME_CM); // 0..1
                    double base  = Constants.SPEED_MIN + alpha * (Constants.SPEED_MAX - Constants.SPEED_MIN);

                    // CommonPid takes (sensor, target) = (d, TARGET_CM)
                    double trim = speedPid.getOutput(d, Constants.TARGET_CM);

                    forwardTarget = clamp(base + trim, 0.0, Constants.SPEED_MAX);

                    if (inAvoid) {
                        double closeness = (Constants.AVOID_CM - clamp(d, Constants.STOP_CM, Constants.AVOID_CM)) / (Constants.AVOID_CM - Constants.STOP_CM);
                        turnFrac = avoidSide * (closeness * Constants.TURN_MAX);

                        forwardTarget = Math.max(forwardTarget, Constants.SPEED_MIN);
                    } else {
                        turnFrac = 0.0;
                    }
                }

                // 5) Apply to motors
                if (halted) {
                    mixer.apply(0.0, 0.0, dt);
                } else {
                    mixer.apply(forwardTarget, turnFrac, dt);
                }

                // 6) Keep ~20 Hz
                next += Constants.PERIOD_NS;
                long sleepNs = next - System.nanoTime();
                if (sleepNs > 1_000_000) {
                    try { Thread.sleep(sleepNs / 1_000_000, (int)(sleepNs % 1_000_000)); }
                    catch (InterruptedException ignored) {}
                } else if (sleepNs > 0) {
                    Thread.yield();
                } else {
                    next = System.nanoTime(); // overrun; resync
                }
            }
        } finally {
            try { left.stop(true); right.stop(true); } catch (Exception ignored) {}
        }
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
