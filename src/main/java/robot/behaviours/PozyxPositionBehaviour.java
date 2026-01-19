package robot.behaviours;

import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import robot.RobotContext;
import server.control.YawPidControl;
import shared.YawPidResultDTO;
import shared.dto.old.PositionDTO;
import shared.dto.old.PozyxPointDTO;
import robot.sims.DummyPositioningSim;
import robot.utils.*;
import shared.utils.MqttUtil;

import static robot.RobotConstants.IS_SIMS;

public class PozyxPositionBehaviour extends TickerBehaviour {
    private final RobotContext ctx;
    private final MqttUtil mqttUtil;
    private final DummyPositioningSim dummyPositioningSim;

    public PozyxPositionBehaviour(Agent a, long period, RobotContext ctx, MqttUtil mqttUtil) {
        super(a, period);
        this.ctx = ctx;
        this.mqttUtil = mqttUtil;
        this.dummyPositioningSim = new DummyPositioningSim();
    }

    @Override
    protected void onTick() {
        if (ctx.getState() == RobotState.CHARGING || ctx.getState() == RobotState.STANDBY)
            return;

        long currentTimestamp = System.currentTimeMillis();
        PositionDTO positionDto = ctx.getLastPosition();

        long lastContextTimestamp = positionDto.getTimestamp();
        long dtsec = currentTimestamp - lastContextTimestamp;

        if (dtsec <= 0)
            return;

        PozyxPointDTO currentPosition = IS_SIMS ? dummyPositioningSim.getPositionSim() : mqttUtil.getLocation();

        if (currentPosition == null || (currentPosition.x == 0 || currentPosition.y == 0)) {
            return;
        }

        float angle = mqttUtil.getAngle();
        angle = ((angle + 180) % 360 + 360) % 360 - 180;

        PositionDTO newPositionDto = new PositionDTO(
                currentTimestamp,
                currentPosition.x,
                currentPosition.y,
                angle,
                dtsec);

        ctx.setLastPosition(newPositionDto);
        // System.out.println("New position: " + newPositionDto.toString());

        // --- NAVIGATION LOGIC (Direct Drive) ---
        Double tx = ctx.getTargetX();
        Double ty = ctx.getTargetY();

        // 1. Check Target
        if (tx == null || ty == null) {
            robot.hardware.MotorHardware.applyCommand(dtsec * 1000, 0, 0);
            return;
        }

        double currentX = newPositionDto.getX();
        double currentY = newPositionDto.getY();
        double currentAngle = newPositionDto.getAngleDeg();

        double dx = tx - currentX;
        double dy = ty - currentY;
        double distance = Math.hypot(dx, dy);

        // 2. Check Arrival (50mm tolerance)
        if (distance < 100.0) {
            robot.hardware.MotorHardware.applyCommand(dtsec * 1000, 0, 0);
            return;
        }

        // 3. Compute Heading Error
        double targetAngle = Math.toDegrees(Math.atan2(dy, dx));
        double angleError = normalizeDeg(targetAngle - currentAngle);

        // 4. P-Control Turn
        // KP = 5.0 (deg/s per degree error)
        // INVERTED: Positive Error (Left Turn Needed) -> Needs Right Wheel Faster ->
        // Needs Turn < 0.
        double KP = -5.0;
        double applyTurn = angleError * KP;
        // Clamp Turn Speed (+/- 400 deg/s)
        applyTurn = Math.max(-400.0, Math.min(400.0, applyTurn));

        // 5. Speed
        double applySpeed = 400.0;
        // Safety: Slow down if facing wrong way (>60 deg error)
        if (Math.abs(angleError) > 60) {
            applySpeed = 100.0;
        }

        // 6. Apply
        // robot.hardware.MotorHardware.applyCommand(dtsec * 1000, 400, 0); // Reverted
        robot.hardware.MotorHardware.applyCommand(dtsec * 1000,
                Math.min(Math.min(ctx.getTargetSpeed(), applySpeed), ctx.getUltrasonicPidResult().getForwardTarget()),
                applyTurn);

        System.out.println(String.format(
                "DirectDrive: T(%.0f,%.0f) Curr:(%.0f,%.0f) H:%.1f TgtH:%.0f Err:%.1f -> SPD:%.0f TRN:%.2f",
                tx, ty, currentX, currentY, currentAngle, targetAngle, angleError, applySpeed, applyTurn));
    }

    private double normalizeDeg(double a) {
        return ((a + 180) % 360 + 360) % 360 - 180;
    }
}
