package robot.control;

import shared.DistancePidResultDTO;
import shared.utils.CommonPid;

import static robot.RobotConstants.*;
import static robot.RobotConstants.AVOID_DISTANCE;
import static robot.RobotConstants.FAR_DISTANCE;
import static robot.RobotConstants.SPEED_MAX;
import static robot.RobotConstants.SPEED_MIN;
import static robot.RobotConstants.STOP_DISTANCE;
import static robot.utils.PidUtil.*;
import static robot.utils.PidUtil.clamp;

public class DistancePidControl {
    
    public static DistancePidResultDTO computeControl(
            long millis,
            double obstacleDistCm,
            double prevSpeedCmd,
            boolean wasHalted,
            CommonPid distancePid,
            double dtSec
    ) {
        // --- logic to control the robot to stop ---
        final boolean mustStopForObstacle = (obstacleDistCm <= STOP_DISTANCE);
//        final boolean reachedGoal = (Math.abs(dx) < DEST_GOAL_STOP && Math.abs(dy) < DEST_GOAL_STOP);
        final boolean canResumeObstacle = (obstacleDistCm >= RESUME_DISTANCE);

        System.out.println("mustStopForObstacle: " + mustStopForObstacle + ", washalted: "+ wasHalted +", canResumeObstacle: " + canResumeObstacle);

        boolean isHalted = mustStopForObstacle
                || (wasHalted && !canResumeObstacle);

        // --- logic to control the robot turn ---
        boolean isAvoiding = false;

        final boolean inObstacleBand =  (obstacleDistCm >= STOP_DISTANCE && obstacleDistCm <= AVOID_DISTANCE);

        System.out.println("inObstacleBand: " + inObstacleBand);

        if (!isHalted && (inObstacleBand)) {
            isAvoiding = true;
        }

        // --- logic to calculate the speed factor of the motor  ---
        // factor that cntrol the motor speed based on the obstacle distance
        final double speedFactor = toUnitInterval(obstacleDistCm, STOP_DISTANCE, FAR_DISTANCE);

        double desiredSpeed = lerp(SPEED_MIN, SPEED_MAX, speedFactor);

        // --- apply the speed factor to the PID to get desired speed---
        distancePid.setSetpoint(desiredSpeed);
        double pidDeltaPerTick = distancePid.getOutput(prevSpeedCmd);

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
        if (isHalted) distancePid.reset();


        // --- if halted, speed is 0 and turn frac is 0 ---
        if (isHalted) {
            newSpeedCmd = 0.0;
            distancePid.reset();
        }

        return new DistancePidResultDTO(millis, newSpeedCmd, isHalted, isAvoiding);
    }
}
