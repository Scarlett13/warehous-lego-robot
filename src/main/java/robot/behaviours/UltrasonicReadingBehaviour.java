package robot.behaviours;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import robot.RobotConstants;
import robot.RobotContext;
import robot.control.DistancePidControl;
import shared.DistancePidResultDTO;
import shared.messaging.MessagingConstants;
import shared.messaging.acl.Acl;
import shared.dto.DistanceDTO;
import robot.hardware.UltrasonicHardware;
import robot.utils.*;
import shared.messaging.TopicHelper;
import shared.utils.JsonUtil;

public class UltrasonicReadingBehaviour extends TickerBehaviour {
    private final AID topic;
    private final RobotContext ctx;
    private long lastTimeStamp = System.currentTimeMillis();

    public UltrasonicReadingBehaviour(Agent a, long period, RobotContext ctx) {
        super(a, period);
        this.topic = TopicHelper.topic(a, MessagingConstants.ROBOT_ULTRASONIC_PID_TOPIC);
        this.ctx = ctx;
    }

    @Override
    protected void onTick() {
        if (ctx.getState() != RobotState.WORKING) return;

        long currentTimestamp = System.currentTimeMillis();
        long lastContextTimestamp = ctx.getDistance().getTimestamp();
        long dt =  currentTimestamp - lastTimeStamp;

        if(lastContextTimestamp >= currentTimestamp) return;

        int distance = RobotConstants.IS_SIMS ?
                Hardware.get().ultrasonicDummy().readRawSim() :
                UltrasonicHardware.readRaw();

        DistanceDTO distanceDto = new DistanceDTO(currentTimestamp, Math.min(distance, 200));
        DistancePidResultDTO lastDistancePidResult = ctx.getLastDistancePidResult();

        lastDistancePidResult = DistancePidControl.computeControl(currentTimestamp, distanceDto.getDistance(), lastDistancePidResult.getForwardTarget(), lastDistancePidResult.isHalted(), ctx.getDistancePid(), dt);

        ctx.setDistance(distanceDto);
        Acl.publish(myAgent, topic, JsonUtil.toJson(lastDistancePidResult));
        lastTimeStamp = currentTimestamp;
        System.out.println(distanceDto);
    }

}
