package server.control;

import shared.YawPidResultDTO;
import shared.utils.CommonPid;

import static robot.RobotConstants.TURN_MAX;

public class YawPidControl {

    public static YawPidResultDTO computeControl(
            long millis,
            double targetAngle,
            double currentAngle,
            CommonPid yawPid) {
        // Calculate error wrapped to [-180, 180]
        double error = targetAngle - currentAngle;
        while (error > 180)
            error -= 360;
        while (error < -180)
            error += 360;

        // Deadband: If error is small, don't turn. Prevents "hunting" at 1Hz.
        if (Math.abs(error) < 5.0) {
            return new YawPidResultDTO(millis, 0.0);
        }

        // Configure PID
        // We want to drive the error to 0.
        // Setpoint = 0.
        // Input = -error.
        // PID Error = Setpoint - Input = 0 - (-error) = error.
        // This ensures P-term is P * error.

        yawPid.setSetpoint(0.0);

        // Use getOutput(actual) which compares to setpoint
        double output = yawPid.getOutput(-error);

        // Clamp output to TURN_MAX
        if (output > TURN_MAX)
            output = TURN_MAX;
        if (output < -TURN_MAX)
            output = -TURN_MAX;

        return new YawPidResultDTO(millis, output);
    }
}
