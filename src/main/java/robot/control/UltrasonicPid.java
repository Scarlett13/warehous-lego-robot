package robot.control;

/** Minimal PID with integral clamp and helper methods. */
public class UltrasonicPid {
    private final double kp, ki, kd, iClamp;
    private double integral = 0.0;
    private double prevErr = 0.0;
    private boolean first = true;

    public UltrasonicPid(double kp, double ki, double kd, double iClamp) {
        this.kp = kp; this.ki = ki; this.kd = kd; this.iClamp = Math.abs(iClamp);
    }

    public double update(double err, double dt) {
        if (dt <= 0) dt = 1e-3;

        integral += err * dt;
        if (integral > iClamp)  integral = iClamp;
        if (integral < -iClamp) integral = -iClamp;

        double deriv;
        if (first) { deriv = 0.0; first = false; }
        else       { deriv = (err - prevErr) / dt; }

        prevErr = err;
        return kp * err + ki * integral + kd * deriv;
    }

    /** Reduce stored integral gradually (helps re-centre when error ~0). */
    public void leakIntegral(double amount) {
        if (amount <= 0) return;
        if (integral > 0) integral = Math.max(0, integral - amount);
        else              integral = Math.min(0, integral + amount);
    }

    public void reset() {
        integral = 0.0;
        prevErr  = 0.0;
        first = true;
    }
}
