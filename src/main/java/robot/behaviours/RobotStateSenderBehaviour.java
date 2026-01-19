package robot.behaviours;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
import robot.RobotContext;
import shared.dto.RobotStatusDTO;
import shared.messaging.MessagingConstants;
import shared.messaging.TopicHelper;
import shared.messaging.acl.Acl;
import shared.utils.JsonUtil;

import static robot.RobotConstants.ROBOT_NAME;

public class RobotStateSenderBehaviour extends TickerBehaviour {
    private final AID topic;
    private final RobotContext ctx;

    public RobotStateSenderBehaviour(Agent a, long period, RobotContext ctx) {
        super(a, period);
        this.topic = TopicHelper.topic(a, MessagingConstants.ROBOT_STATE_TOPICS);
        this.ctx = ctx;
    }

    @Override
    protected void onTick() {
        long currentTimestamp = System.currentTimeMillis();

        // String.format("%s_%s", MessagingConstants.ROBOT_COMMAND, ROBOT_NAME)
        RobotStatusDTO currentStatus = new RobotStatusDTO(
                currentTimestamp,
                ctx.getRobotBatteryPercentage(),
                ctx.getLastPosition(),
                ctx.getWorkId(),
                ctx.getLastUltrasonicReading(),
                ctx.getState());

        // String test_json = JsonUtil.toJson("{'test': true}");

        // Acl.publish(this.getAgent(), topic, ROBOT_NAME,
        // JsonUtil.toJson(currentStatus), "json", ACLMessage.INFORM);

        if (!robot.RobotConstants.IS_SIMS) {
            System.out.println("State: " + ctx.getState() +
                    " | Pos: " + ctx.getLastPosition().toString() +
                    " | Work: " + ctx.getWorkId() +
                    " | Speed: " + ctx.getTargetSpeed() +
                    " | Turn: " + ctx.getTurnCorrection());
        }

        Acl.publish(this.getAgent(), topic, ROBOT_NAME, JsonUtil.toJson(currentStatus), "json", ACLMessage.INFORM);
    }
}
