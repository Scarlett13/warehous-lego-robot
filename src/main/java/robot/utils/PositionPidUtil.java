package robot.utils;

import shared.utils.CommonPid;
import shared.dto.old.PidResultDTO;

import static robot.RobotConstants.DEST_GOAL_TURN_ANGLE;
import static robot.RobotConstants.SPEED_MAX;
import static robot.RobotConstants.SPEED_MIN;
import static robot.RobotConstants.TURN_MAX;
import static robot.utils.PidUtil.*;

public class PositionPidUtil {

    public static PidResultDTO computeControl(
            double robotXmm, double robotYmm,
            double goalXmm,  double goalYmm,
            double prevSpeedCmd,
            CommonPid positionPid,
            double dtSec,
            float angle
    ) {
        // --- Calculate position to the goal ---
        final double dx = goalXmm - robotXmm;
        final double dy = goalYmm - robotYmm;
        final double goalDistanceMm = Math.hypot(dx, dy);

        float target_angle = (float) Math.toDegrees(Math.atan2(dy, dx));
        float diff_angle   = wrap180(target_angle - angle);

        // --- logic to control the robot to stop ---
        final boolean reachedGoal = goalDistanceMm < 30;

        System.out.println("robotx: " + robotXmm+", roboty: " + robotYmm+", goalx: " + goalXmm+", goaly: " + goalYmm);
        System.out.println("goal distance: " + goalDistanceMm + ", reached goal: " + reachedGoal + ", dx: " + dx + ", dy: " + dy);


        boolean isHalted = reachedGoal;

        // --- logic to control the robot turn ---
//        boolean isAvoiding = false;
//        int avoidDirection = 0; // -1 = tu rn left, +1 = turn right, 0 = none

        boolean inAvoid = Math.abs(diff_angle) > DEST_GOAL_TURN_ANGLE;
        int avoidSide = 0;
        double turnFraction = 0.0;
//
        if (inAvoid) {
            avoidSide = (diff_angle > 0) ? -1 : +1;

            double mag = Math.min(Math.abs(diff_angle) / 45.0, 1.0); // 0..1 grows with error
            turnFraction = Math.copySign(mag * TURN_MAX, diff_angle); // +left / -right
        }
        System.out.println("angle: " + angle+", diff_angle: " + diff_angle+", target_angle: " + target_angle);

//        if (!isHalted && nearGoalTurnBand) {
//            isAvoiding = true;
//            avoidDirection = diff_angle < (DEST_GOAL_TURN_ANGLE * -1) ? -1 : +1;
//        }

        // --- logic to calculate the speed factor of the motor  ---
        // factor that cntrol the motor speed based on the goal distance: 0 when close (slow), 1 when far (fast)
        final double speedFactor = toUnitInterval(goalDistanceMm, /*near*/ 300, /*far*/ 1500);

        double desiredSpeed = lerp(SPEED_MIN, SPEED_MAX, speedFactor);

        // Hard stops at near edges
//        if (goalDistanceMm <= DEST_GOAL_STOP) desiredSpeed = 0.0;
//        if (isAvoiding) desiredSpeed = 0.0;

        // --- apply the speed factor to the PID to get desired speed---
        positionPid.setSetpoint(desiredSpeed);
        double pidDeltaPerTick = positionPid.getOutput(prevSpeedCmd);

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

        // --- if halted, speed is 0 and turn frac is 0 ---
        if (isHalted) {
            newSpeedCmd = 0.0;
//            turnFraction = 0.0;
            positionPid.reset();
        }

        return new PidResultDTO(newSpeedCmd, 0, isHalted, false, 0, reachedGoal);
    }

    static float wrap180(float a) {
        float r = (float) Math.IEEEremainder(a, 360.0);
        if (r <= -180f) r += 360f;
        if (r > 180f)   r -= 360f;
        return r;
    }
}
