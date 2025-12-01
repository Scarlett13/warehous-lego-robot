package robot.utils;

import shared.utils.CommonPid;

import static robot.RobotConstants.*;

public final class PidUtil {
    public static final class ControlResult {
        public final double forwardTarget; // deg/s
        public final double turnFrac;      // [-TURN_MAX..+TURN_MAX]
        public final boolean halted;
        public final boolean inAvoid;
        public final int avoidSide;        // -1 left, +1 right, 0 none
        public final boolean goalReached;

        public ControlResult(double f, double t, boolean h, boolean a, int s, boolean g) {
            this.forwardTarget = f;
            this.turnFrac = t;
            this.halted = h;
            this.inAvoid = a;
            this.avoidSide = s;
            this.goalReached = g;
        }

        @Override
        public String toString() {
            return String.format(
                    "ControlResult{forwardTarget=%.2f, turnFrac=%.3f, halted=%s, inAvoid=%s, avoidSide=%d}",
                    forwardTarget, turnFrac, halted, inAvoid, avoidSide
            );
        }
    }

    /**
     * Decide the speed and turn each control tick.
     *
     * Simple idea:
     *  1) Stop if destination is very close or an obstacle is too close (with hysteresis).
     *  2) If not stopping, compute a desired speed from two proportional factors:
     *     - "How far to the goal?"   (closer -> slower)
     *     - "How far is the obstacle?" (closer -> slower)
     *     We take the conservative MIN of those two factors.
     *  3) Use your PID to move the speed command toward the desired speed.
     *     Then apply asymmetric rate limits (accelerate slower, brake faster).
     *  4) If in avoidance mode, add a turn fraction (away from obstacle / toward goal band).
     */
    public static ControlResult computeControl(
            double obstacleDistCm,
            double robotXmm, double robotYmm,
            double goalXmm,  double goalYmm,
            double prevSpeedCmd,
            boolean wasHalted,
            boolean wasAvoiding,
            CommonPid speedPid,
            double dtSec,
            float angle
    ) {
        // --- Calculate position to the goal ---
        final double dx = goalXmm - robotXmm;
        final double dy = goalYmm - robotYmm;
        final double goalDistanceMm = Math.hypot(dx, dy);

        float target_angle = (float) Math.toDegrees(Math.atan2((double)(dy), (double)(dx)));
        float diff_angle = target_angle - angle;
        diff_angle = (float) (((diff_angle + 540.0) % 360.0) - 180.0);

        // --- logic to control the robot to stop ---
        final boolean mustStopForObstacle = (obstacleDistCm <= STOP_DISTANCE);
        final boolean reachedGoal = (Math.abs(dx) < DEST_GOAL_STOP && Math.abs(dy) < DEST_GOAL_STOP);
        final boolean canResumeObstacle = (obstacleDistCm >= RESUME_DISTANCE);

        System.out.println("robotx: " + robotXmm+", roboty: " + robotYmm+", goalx: " + goalXmm+", goaly: " + goalYmm);
        System.out.println("goal distance: " + goalDistanceMm + ", reached goal: " + reachedGoal + ", dx: " + dx + ", dy: " + dy);
        System.out.println("mustStopForObstacle: " + mustStopForObstacle + ", washalted: "+ wasHalted +", canResumeObstacle: " + canResumeObstacle);


        boolean isHalted = reachedGoal
                || mustStopForObstacle
                || (wasHalted && !canResumeObstacle);

        // --- logic to control the robot turn ---
        boolean isAvoiding = false;
        int avoidDirection = 0; // -1 = tu rn left, +1 = turn right, 0 = none

        final boolean inObstacleBand =  (obstacleDistCm >= STOP_DISTANCE && obstacleDistCm <= AVOID_DISTANCE);
        final boolean nearGoalTurnBand = Math.abs(diff_angle) > DEST_GOAL_TURN_ANGLE;
        System.out.println("inObstacleBand: " + inObstacleBand+", nearGoalTurnBand: " + nearGoalTurnBand);
        System.out.println("angle: " + angle+", diff_angle: " + diff_angle+", target_angle: " + target_angle);

        if (!isHalted && (inObstacleBand || nearGoalTurnBand)) {
            isAvoiding = true;
            avoidDirection = diff_angle < (DEST_GOAL_TURN_ANGLE * -1) ? -1 : +1;
        }

        // --- logic to calculate the speed factor of the motor  ---
        // factor that cntrol the motor speed based on the goal distance: 0 when close (slow), 1 when far (fast)
        final double goalFactor = toUnitInterval(goalDistanceMm, /*near*/ 300, /*far*/ 1500);

        // factor that cntrol the motor speed based on the obstacle distance
        final double obstacleFactor = toUnitInterval(obstacleDistCm, STOP_DISTANCE, FAR_DISTANCE);

        // choose the worst factor
        final double speedFactor = Math.min(goalFactor, obstacleFactor);

        double desiredSpeed = lerp(SPEED_MIN, SPEED_MAX, speedFactor);

        // Hard stops at near edges
//        if (goalDistanceMm <= DEST_GOAL_STOP) desiredSpeed = 0.0;
//        if (isAvoiding) desiredSpeed = 0.0;

        // --- apply the speed factor to the PID to get desired speed---
        speedPid.setSetpoint(desiredSpeed);
        double pidDeltaPerTick = speedPid.getOutput(prevSpeedCmd);

        // rate to accelerate slower, brake
        final double accelPerSec = 100.0;
        final double decelPerSec = 250.0;
        final double maxUpStep   = Math.max(0.0, accelPerSec * dtSec);
        final double maxDownStep = (isHalted ? 0.0 : decelPerSec) * dtSec;

        double limitedStep = (pidDeltaPerTick >= 0)
                ? Math.min(pidDeltaPerTick, maxUpStep)
                : Math.max(pidDeltaPerTick, -maxDownStep);

        double newSpeedCmd = prevSpeedCmd + limitedStep;

        newSpeedCmd = clamp(newSpeedCmd, Math.min(SPEED_MIN, SPEED_MAX), Math.max(SPEED_MIN, SPEED_MAX));

        // clear the pid integral error when the robot stopped
        if (isHalted) speedPid.reset();

        // --- Turn fraction logic ---
        double turnFraction = 0.0;
        if (isAvoiding) {
            double intensity;
            if (nearGoalTurnBand) {
                // turn when near the goal
                intensity = clamp01(1.0 - Math.abs((diff_angle / DEST_GOAL_TURN_ANGLE)));
            } else {
                // turn when near obstacle
                double d = clamp(obstacleDistCm, STOP_DISTANCE, AVOID_DISTANCE);
                intensity = (AVOID_DISTANCE - d) / (AVOID_DISTANCE - STOP_DISTANCE);
            }
            turnFraction = avoidDirection * (intensity * TURN_MAX);
        }

        // --- if halted, speed is 0 and turn frac is 0 ---
        if (isHalted) {
            newSpeedCmd = 0.0;
            turnFraction = 0.0;
            speedPid.reset();
        }

        return new ControlResult(newSpeedCmd, turnFraction, isHalted, isAvoiding, avoidDirection, reachedGoal);
    }

    // ----------------- small helpers -----------------

    public static double toUnitInterval(double value, double near, double far) {
        if (Double.isNaN(value)) return 0.5; // neutral if unknown
        if (far <= near) return 0.0;
        double t = (value - near) / (far - near);
        return clamp01(t);
    }

    public static double clamp01(double v) {
        return (v < 0.0) ? 0.0 : Math.min(v, 1.0);
    }

    private static boolean destinationIsLeft(double rx, double ry, double dx, double dy) {
        // determine the robot position,
        // If robot is facing +X (no heading available), "left" means destY > robotY
        return (dy - ry) > 0.0;
    }

    public static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    public static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

}

