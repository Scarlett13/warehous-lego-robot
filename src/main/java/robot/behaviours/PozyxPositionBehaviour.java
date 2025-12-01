package robot.behaviours;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import robot.RobotContext;
import shared.messaging.MessagingConstants;
import shared.messaging.acl.Acl;
import shared.dto.old.PositionDTO;
import shared.dto.old.PozyxPointDTO;
import robot.sims.DummyPositioningSim;
import robot.utils.*;
import shared.messaging.TopicHelper;
import shared.utils.JsonUtil;
import shared.utils.MqttUtil;

import static robot.RobotConstants.IS_SIMS;

public class PozyxPositionBehaviour extends TickerBehaviour {
    private final AID topic;
    private final RobotContext ctx;
    private final MqttUtil mqttUtil;
    private final DummyPositioningSim  dummyPositioningSim;

    public PozyxPositionBehaviour(Agent a, long period, RobotContext ctx, MqttUtil mqttUtil) {
        super(a, period);
        this.topic = TopicHelper.topic(a, MessagingConstants.ROBOT_POSITION_TOPIC);
        this.ctx = ctx;
        this.mqttUtil = mqttUtil;
        this.dummyPositioningSim = new DummyPositioningSim();
    }

    @Override
    protected void onTick() {
        if (ctx.getState() != RobotState.WORKING) return;

        long currentTimestamp = System.currentTimeMillis();
        PositionDTO positionDto = ctx.getLastPosition();
        long lastContextTimestamp = positionDto.getTimestamp();
        if(lastContextTimestamp >= currentTimestamp) return;
        long dtsec = currentTimestamp - lastContextTimestamp;

        PozyxPointDTO currentPosition = IS_SIMS ? dummyPositioningSim.getPositionSim() : mqttUtil.getLocation();
        if (currentPosition != null && (currentPosition.x != 0 && currentPosition.y != 0)) {
            float angle = mqttUtil.getAngle();

            positionDto = new PositionDTO(
                    currentTimestamp,
                    currentPosition.x,
                    currentPosition.y,
                    angle
            );
        }

//        PositionSpeedPidDTO positionSpeedPid = PositionSpeedPidControl.computeControl(
//                currentTimestamp, currentPosition.x, currentPosition.y, );

        ctx.setLastPosition(positionDto);
        Acl.publish(myAgent, topic, JsonUtil.toJson(positionDto));
    }
}
