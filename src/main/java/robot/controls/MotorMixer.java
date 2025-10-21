package robot.controls;

import ev3dev.actuators.lego.motors.EV3LargeRegulatedMotor;

public class MotorMixer {
    private final double speedMin, speedMax, turnMax, slewRate;
    private final EV3LargeRegulatedMotor left, right;

    private double prevLeft = 0.0, prevRight = 0.0;

    public MotorMixer(double speedMin, double speedMax, double turnMax, double slewRate,
                      EV3LargeRegulatedMotor left, EV3LargeRegulatedMotor right) {
        this.speedMin = speedMin;
        this.speedMax = speedMax;
        this.turnMax  = turnMax;
        this.slewRate = slewRate;
        this.left  = left;
        this.right = right;
    }

    public void apply(double forwardDegPerSec, double turnFrac, double dt) {
        // clamp inputs
        double fwd  = clamp(forwardDegPerSec, 0.0, speedMax);
        double turn = clamp(turnFrac, -turnMax, +turnMax);

        // floor only if moving
        if (fwd > 0 && fwd < speedMin) fwd = speedMin;

        // mix
        double leftCmd  = fwd * (1.0 - turn);
        double rightCmd = fwd * (1.0 + turn);

        // slew limit
        leftCmd  = slew(prevLeft,  leftCmd,  slewRate, dt);
        rightCmd = slew(prevRight, rightCmd, slewRate, dt);
        prevLeft = leftCmd; prevRight = rightCmd;

        // apply
        applyMotor(left,  leftCmd);
        applyMotor(right, rightCmd);
    }

    private static double slew(double prev, double target, double ratePerSec, double dtSec) {
        double maxStep = Math.max(0, ratePerSec) * Math.max(0.0, dtSec);
        double delta = target - prev;
        if (Math.abs(delta) <= maxStep) return target;
        return prev + Math.signum(delta) * maxStep;
    }

    private static void applyMotor(EV3LargeRegulatedMotor m, double dps) {
        dps = Math.max(0.0, dps);
        m.setSpeed((int) dps);
        if (dps == 0.0) m.stop(true);
        else m.backward();
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
