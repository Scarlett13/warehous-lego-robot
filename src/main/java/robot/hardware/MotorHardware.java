package robot.hardware;

import ev3dev.actuators.lego.motors.EV3LargeRegulatedMotor;
import lejos.hardware.port.MotorPort;
import robot.RobotConstants;

public class MotorHardware {
    static EV3LargeRegulatedMotor motorRight = null;
    static EV3LargeRegulatedMotor motorLeft = null;

    private static double speedMin, speedMax, turnMax, slewRate;

    private static double prevLeft = 0.0, prevRight = 0.0;

    public static void init() {
        System.out.println("Init motor starts");
        motorRight = new EV3LargeRegulatedMotor(MotorPort.D);
        motorLeft = new EV3LargeRegulatedMotor(MotorPort.A);
        System.out.println("Init motor finishes");

        speedMin = RobotConstants.SPEED_MIN;
        speedMax = RobotConstants.SPEED_MAX;
        turnMax = RobotConstants.TURN_MAX;
        slewRate = RobotConstants.SLEW_RATE;

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            public void run() {
                System.out.println("Stop motors Ctrl+C");
                motorRight.stop();
                motorLeft.stop();
            }
        }));
    }

    public static void applyCommand(long dt, double targetSpeed, double turnCorrection){
        double appliedSpeed =clamp(targetSpeed, speedMin, speedMax);

        // mix
        double leftCmd  = appliedSpeed + turnCorrection;
        double rightCmd = appliedSpeed - turnCorrection;

        // slew limit
        leftCmd  = slew(prevLeft,  leftCmd,  slewRate, dt);
        rightCmd = slew(prevRight, rightCmd, slewRate, dt);
        prevLeft = leftCmd;
        prevRight = rightCmd;

        // apply
        applyMotor(motorLeft,  leftCmd);
        applyMotor(motorRight, rightCmd);
    }

    public static void apply(double forwardDegPerSec, double turnFrac, double dt) {
        // clamp inputs
        double fwd  = clamp(forwardDegPerSec, 0.0, speedMax);
        double turn = clamp(turnFrac, -turnMax, +turnMax);

        // floor only if moving
        if (fwd > 0 && fwd < speedMin) fwd = speedMin;

        // mix
        double leftCmd  = fwd * (1.0 - turn);
        double rightCmd = fwd * (1.0 + turn);

        // slew limit
//        leftCmd  = slew(prevLeft,  leftCmd,  slewRate, dt);
//        rightCmd = slew(prevRight, rightCmd, slewRate, dt);
        prevLeft = leftCmd;
        prevRight = rightCmd;

        // apply
        applyMotor(motorLeft, leftCmd);
        applyMotor(motorRight, rightCmd);
    }

    private static void applyMotor(EV3LargeRegulatedMotor m, double dps) {
        dps = Math.max(0.0, dps);
        m.setSpeed((int) dps);
        if (dps == 0.0) stop(m);
        else m.backward();
    }

    public static void backward() {
        motorRight.forward();
        motorLeft.forward();
    }

    public static void forward() {
        motorRight.backward();
        motorLeft.backward();
    }

    public static void stop(EV3LargeRegulatedMotor motor) {
        motor.stop();
    }

//    public static void turnLeft() {
//        motorA.stop();
//        motorB.backward();
//    }
//
//    public static void turnRight() {
//        motorA.backward();
//        motorB.stop();
//    }

    public static void setSpeed(int rightSpeed, int leftSpeed) {
        motorRight.setSpeed(rightSpeed);
        motorLeft.setSpeed(leftSpeed);
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private static double slew(double prev, double target, double ratePerSec, double dtSec) {
        double maxStep = Math.max(0, ratePerSec) * Math.max(0.0, dtSec);
        double delta = target - prev;
        if (Math.abs(delta) <= maxStep) return target;
        return prev + Math.signum(delta) * maxStep;
    }
}
