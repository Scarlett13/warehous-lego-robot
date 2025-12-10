package server.control;

import shared.dto.PositionSpeedPidDTO;
import shared.utils.CommonPid;

import static robot.RobotConstants.*;
import static robot.utils.PidUtil.*;

public class LocalisationPidControl {
    public static PositionSpeedPidDTO computeControl(
            long millis,
            int posX,
            int posY,
            int destX,
            int destY,
            double previousSpeed,
            double dtSec,
            CommonPid positionPid,
            boolean wasReachedGoal
    ) {
        final double dx = destX - posX;
        final double dy = destY - posY;
        final double goalDistanceMm = Math.hypot(dx, dy);

        // --- logic to control the robot to stop ---
        final boolean reachedGoal = (Math.abs(dx) < DEST_GOAL_STOP && Math.abs(dy) < DEST_GOAL_STOP);
        final double goalFactor = toUnitInterval(goalDistanceMm, DEST_GOAL_SLOW, DEST_GOAL_FAST);
        double desiredSpeed = lerp(SPEED_MIN, SPEED_MAX, goalFactor);

        positionPid.setSetpoint(desiredSpeed);
        double pidDeltaPerTick = positionPid.getOutput(previousSpeed);

        // rate to accelerate slower, brake
        final double accelPerSec = 100.0;
        final double decelPerSec = 250.0;
        final double maxUpStep   = Math.max(0.0, accelPerSec * dtSec);
        final double maxDownStep = (wasReachedGoal ? 0.0 : decelPerSec) * dtSec;

        double limitedStep = (pidDeltaPerTick >= 0)
                ? Math.min(pidDeltaPerTick, maxUpStep)
                : Math.max(pidDeltaPerTick, -maxDownStep);

        double newSpeedCmd = previousSpeed + limitedStep;

        newSpeedCmd = clamp(newSpeedCmd, Math.min(SPEED_MIN, SPEED_MAX), Math.max(SPEED_MIN, SPEED_MAX));

        if (reachedGoal) {
            newSpeedCmd = 0.0;
            positionPid.reset();
        }

        return new PositionSpeedPidDTO(newSpeedCmd, millis);
    }
}
