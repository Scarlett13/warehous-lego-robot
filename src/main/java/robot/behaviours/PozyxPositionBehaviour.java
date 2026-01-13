package robot.behaviours;

import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import robot.RobotContext;
import shared.dto.old.PositionDTO;
import shared.dto.old.PozyxPointDTO;
import robot.sims.DummyPositioningSim;
import robot.utils.*;
import shared.utils.MqttUtil;

import static robot.RobotConstants.IS_SIMS;

public class PozyxPositionBehaviour extends TickerBehaviour {
    private final RobotContext ctx;
    private final MqttUtil mqttUtil;
    private final DummyPositioningSim  dummyPositioningSim;

    public PozyxPositionBehaviour(Agent a, long period, RobotContext ctx, MqttUtil mqttUtil) {
        super(a, period);
        this.ctx = ctx;
        this.mqttUtil = mqttUtil;
        this.dummyPositioningSim = new DummyPositioningSim();
    }

    @Override
    protected void onTick() {
        if (ctx.getState() == RobotState.CHARGING || ctx.getState() == RobotState.STANDBY) return;

        long currentTimestamp = System.currentTimeMillis();
        PositionDTO positionDto = ctx.getLastPosition();

        long lastContextTimestamp = positionDto.getTimestamp();
        long dtsec = currentTimestamp - lastContextTimestamp;

        if(dtsec <= 0) return;

        PozyxPointDTO currentPosition = IS_SIMS ? dummyPositioningSim.getPositionSim() : mqttUtil.getLocation();

        if (currentPosition == null || (currentPosition.x == 0 || currentPosition.y == 0)) {
           return;
        }

        float angle = mqttUtil.getAngle();

        PositionDTO newPositionDto = new PositionDTO(
                currentTimestamp,
                currentPosition.x,
                currentPosition.y,
                angle,
                dtsec
        );

        ctx.setLastPosition(newPositionDto);
//        System.out.println("New position: " + newPositionDto.toString());
    }
}
