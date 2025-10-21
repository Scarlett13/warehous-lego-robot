package robot;

public class Constants {

    public Constants() {
    }

    public double setpoint;

    // -------- Distances (cm) --------
    public static final double STOP_CM    = 8.0;
    public static final double RESUME_CM  = 12.0;
    public static final double AVOID_CM   = 35.0;
    public static final double FAR_CM     = 200.0;
    public static final double MAX_CM     = 250.0;
    public static final double TARGET_CM  = 40.0;

    // -------- Motor & control limits --------
    public static final double SPEED_MIN  = 320.0;
    public static final double SPEED_MAX  = 720.0;
    public static final double TURN_MAX   = 0.60;
    public static final double SLEW_RATE  = 180.0;

    // -------- CommonPid gains (base + trim pattern) --------
    public static final double PSPEED = 6;
    public static final double ISPEED = 0.02;
    public static final double DSPEED = 0.40;

    // Trim limits so PID can't dominate base speed
    public static final double TRIM_LIMIT = 400.0;

    public static final double PID_RAMP_PER_STEP = 9.0;
    public static final double PID_OUTPUT_FILTER = 0.10;

    public static final double SETPOINT_RANGE_CM = 20.0;

    // -------- Loop timing --------
    public static final int LOOP_HZ = 20;
    public static final long PERIOD_NS = 1_000_000_000L / LOOP_HZ;


}
