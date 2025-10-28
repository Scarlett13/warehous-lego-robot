package robot.behaviours.command;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import robot.Constants;
import robot.RobotContext;
import robot.acl.Acl;
import robot.dto.PositionDTO;
import robot.dto.PozyxPointDTO;
import robot.utils.*;

public class PozyxPositionBehaviour extends TickerBehaviour {
    private final AID topic;
    private final RobotContext ctx;
    private final MqttUtil mqttUtil;

    public PozyxPositionBehaviour(Agent a, long period, RobotContext ctx, MqttUtil mqttUtil) {
        super(a, period);
        this.topic = TopicHelper.topic(a, Constants.POSITION_TOPIC);
        this.ctx = ctx;
        this.mqttUtil = mqttUtil;
    }

    @Override
    protected void onTick() {
        if (ctx.getState() != RobotState.WORKING) return;

        long currentTimestamp = System.currentTimeMillis();
        long lastContextTimestamp = ctx.getPosition().getTimestamp();
        if(lastContextTimestamp >= currentTimestamp) return;

        PositionDTO positionDto = ctx.getPosition();

        PozyxPointDTO currentPosition = mqttUtil.getSmoothenedLocation(10);
        if (currentPosition != null && (currentPosition.x != 0 && currentPosition.y != 0)) {
            float angle = mqttUtil.getAngle();

            positionDto = new PositionDTO(
                    currentTimestamp,
                    currentPosition.x,
                    currentPosition.y,
                    angle
            );
        }

        ctx.setPosition(positionDto);
        Acl.publish(myAgent, topic, JsonUtil.toJson(positionDto));
    }
}
