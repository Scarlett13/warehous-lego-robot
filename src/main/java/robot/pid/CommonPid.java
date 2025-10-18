package robot.pid;

/** Minimal PID with output limits and integral clamp. */
public class CommonPid {
    private final double kp, ki, kd;
    private final double outMin, outMax;
    private final double iMax;      // absolute clamp for integral term

    private double iTerm = 0.0;
    private double prevErr = 0.0;
    private boolean first = true;

    /**
     * @param kp proportional gain
     * @param ki integral gain
     * @param kd derivative gain
     * @param outMin minimum output (e.g., 0 for "no reverse")
     * @param outMax maximum output (e.g., SPEED_MAX)
     * @param iMax  absolute clamp for integral term (anti-windup)
     */
    public CommonPid(double kp, double ki, double kd, double outMin, double outMax, double iMax) {
        this.kp = kp;
        this.ki = ki;
        this.kd = kd;
        this.outMin = outMin;
        this.outMax = outMax;
        this.iMax = Math.abs(iMax);
    }

    public double update(double error, double dt) {
        if (dt <= 0) dt = 1e-3;

        // P
        double p = kp * error;

        // I (clamped)
        iTerm += ki * error * dt;
        if (iTerm >  iMax) iTerm =  iMax;
        if (iTerm < -iMax) iTerm = -iMax;

        // D (on error)
        double d;
        if (first) {
            d = 0.0;
            first = false;
        } else {
            d = kd * (error - prevErr) / dt;
        }
        prevErr = error;

        // Output and clamp
        double out = p + iTerm + d;
        if (out > outMax) out = outMax;
        if (out < outMin) out = outMin;
        return out;
    }

    /** Optional: slowly bleed integral (e.g., during halt). */
    public void leakIntegral(double ratePerSec, double dt) {
        if (ratePerSec <= 0) return;
        double decay = ratePerSec * dt;
        if (iTerm > 0) iTerm = Math.max(0, iTerm - decay);
        else if (iTerm < 0) iTerm = Math.min(0, iTerm + decay);
    }

    public void reset() {
        iTerm = 0.0;
        prevErr = 0.0;
        first = true;
    }
}
