package robot.behaviours;

import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import robot.RobotContext;
import shared.dto.old.PositionDTO;

public class RobotSimPhysicsBehaviour extends TickerBehaviour {
    private final RobotContext ctx;

    // Simulation Constants
    // Assuming Speed comes in mm/s or similar units from DT
    // Assuming Turn is -1.0 to 1.0 factor of MAX_TURN_RATE
    private static final double MAX_TURN_RATE_DEG_PER_SEC = 60.0;

    public RobotSimPhysicsBehaviour(Agent a, long period, RobotContext ctx) {
        super(a, period);
        this.ctx = ctx;
    }

    @Override
    public void onTick() {
        double dt = getPeriod() / 1000.0; // split to seconds

        // 1. Get Control Inputs
        double speed = ctx.getTargetSpeed();
        double turnFactor = ctx.getTurnCorrection(); // [-1.0 ... 1.0]

        // 2. Get Current State
        PositionDTO pos = ctx.getLastPosition();
        double x = pos.getX();
        double y = pos.getY();
        double headingDeg = pos.getAngleDeg();

        // 3. Physics Integration (Kinematics)
        // Update Heading
        double turnRate = turnFactor * MAX_TURN_RATE_DEG_PER_SEC;
        double newHeading = headingDeg + (turnRate * dt);

        // Normalize Heading [0, 360]
        newHeading = newHeading % 360;
        if (newHeading < 0)
            newHeading += 360;

        // Update Position
        // Speed is linear velocity.
        // x_new = x + v * cos(theta) * dt
        // y_new = y + v * sin(theta) * dt

        double headingRad = Math.toRadians(newHeading);
        double dx = speed * Math.cos(headingRad) * dt;
        double dy = speed * Math.sin(headingRad) * dt;

        double newX = x + dx;
        double newY = y + dy;

        // 4. Update Context
        // We preserve other fields (confidence etc) or reset them
        pos.setX((int) newX);
        pos.setY((int) newY);
        pos.setAngleDeg((int) newHeading);

        // Log always for debugging purposes as requested
        // if (Math.abs(speed) > 0.1 || Math.abs(turnFactor) > 0.01) {
        System.out.println(
                String.format("[Teletubbies-Sim] State: %s | Pos: (%d, %d) Yaw: %.1f | Speed: %.1f Turn: %.2f",
                        ctx.getState(), (int) newX, (int) newY, newHeading, speed, turnFactor));
        // }
    }
}
