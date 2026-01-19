package robot.behaviours;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import robot.RobotConstants;
import robot.RobotContext;
import robot.hardware.MotorHardware;
import shared.YawPidResultDTO;
import shared.dto.old.PositionDTO;

import static org.locationtech.jts.math.MathUtil.clamp;

public class MotorActuatorBehaviour extends CyclicBehaviour {
    private final RobotContext ctx;
    private long prevTimestamp = System.currentTimeMillis();

    private enum CruiseState {
        STOPPED,
        ROTATING,
        DRIVING,
        ARRIVED
    }

    private static final double POS_TOLERANCE = 50.0; // mm
    private static final double ROTATE_ENTER = 15.0; // deg
    private static final double ROTATE_EXIT = 5.0; // deg

    private static final double DRIVE_SPEED = 600.0; // deg/sec
    private static final double KP_TURN = 1.0 / 90.0; // maps 90deg -> turn=1
    private static final double MIN_TURN = 0.15; // EV3 deadband fix

    private CruiseState cruiseState = CruiseState.ROTATING;

    private double latchedTargetAngle = 0.0;
    private long lastTimeMs = System.currentTimeMillis();
    private long lastPosMs = -1;

    // this class is currently not in use to avoid another bug caused by the delay
    // between behaviours. it causing the robot to turn right and left wildly
    @Deprecated
    public MotorActuatorBehaviour(Agent a, RobotContext ctx) {
        super(a);
        this.ctx = ctx;
    }

    @Override
    public void action() {
        PositionDTO pos = ctx.getLastPosition();
        if (pos == null) {
            block(50);
            return;
        }

        long now = System.currentTimeMillis();
        double dt = (now - lastTimeMs);
        lastTimeMs = now;
        if (dt <= 0)
            dt = 0.05;

        Double tx = ctx.getTargetX();
        Double ty = ctx.getTargetY();

        // 1. Check if we have a target
        if (tx == null || ty == null) {
            MotorHardware.apply(0, 0, dt);
            block(50);
            return;
        }

        double currentX = pos.getX();
        double currentY = pos.getY();
        double currentAngle = pos.getAngleDeg();

        double dx = tx - currentX;
        double dy = ty - currentY;
        double distance = Math.hypot(dx, dy);

        // 2. Check Arrival
        if (distance < POS_TOLERANCE) {
            MotorHardware.apply(0, 0, dt);
            block(50);
            return;
        }

        // 3. Calculate Heading Error
        double targetAngle = Math.toDegrees(Math.atan2(dy, dx));
        double angleError = normalizeDeg(targetAngle - currentAngle);

        // 4. Calculate Turn (Proportional)
        // KP: Degrees/Sec per Degree of Error.
        // e.g. Error=10deg -> Turn=50deg/s difference.
        double KP = 5.0;
        double applyTurn = angleError * KP;

        // Limiter for turn speed to prevent crazy values?
        // Motor max is ~1000. Let's clamp turn effect to 400.
        applyTurn = Math.max(-400.0, Math.min(400.0, applyTurn));

        // 5. Calculate Speed
        double applySpeed = DRIVE_SPEED;

        if (Math.abs(angleError) > 60) {
            applySpeed = 100.0;
        }

        // 6. Apply
        MotorHardware.applyCommand((long) dt, 400, 0);

        System.out.println(String.format(
                "DirectDrive: T(%.0f,%.0f) Curr:(%.0f,%.0f) H:%.1f TgtH:%.0f Err:%.1f -> SPD:%.0f TRN:%.2f",
                tx, ty, currentX, currentY, currentAngle, targetAngle, angleError, applySpeed, applyTurn));

        lastPosMs = pos.getTimestamp();
        block(50);
    }

    static double normalizeDeg(double a) {
        return ((a + 180) % 360 + 360) % 360 - 180;
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

}
