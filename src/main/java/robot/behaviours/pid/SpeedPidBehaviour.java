package robot.behaviours.pid;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import robot.Constants;
import robot.RobotContext;
import robot.acl.Acl;
import robot.dto.*;
import robot.utils.JsonUtil;
import robot.utils.PidUtil;
import robot.utils.RobotState;
import robot.utils.TopicHelper;

public class SpeedPidBehaviour extends TickerBehaviour {
    private final AID topicSpeed;
    private final AID topicTurn;
    private final AID topicState;
    private final RobotContext ctx;
    private long lastTimestamp = System.currentTimeMillis();

    public SpeedPidBehaviour(Agent a, long period, RobotContext ctx) {
        super(a, period);

        this.ctx = ctx;
        this.topicSpeed = TopicHelper.topic(a, Constants.SPEED_PID_TOPIC);
        this.topicTurn = TopicHelper.topic(a, Constants.ROBOT_TURN_TOPIC);
        this.topicState = TopicHelper.topic(a, Constants.ROBOT_STATE_TOPIC);
    }

    @Override
    protected void onTick() {
        if (ctx.getState() != RobotState.WORKING) return;

        long newTimestamp = System.currentTimeMillis();
        double dtSec =  newTimestamp - lastTimestamp;

        DistanceDTO distance = ctx.getDistance();
        PositionDTO position = ctx.getPosition();
        DestinationDTO destination = ctx.getDestination();
        MotorStateDTO motorStateDTO = ctx.getLastMotorState();
        SpeedDTO speedDTO = ctx.getLastMotorSpeed();

        PidUtil.ControlResult r = PidUtil.computeControl(
                distance.getDistance(), position.getX(), position.getY(), destination.getX(), destination.getY(),
                speedDTO.getSpeed(), motorStateDTO.isHalted(), motorStateDTO.isInTurn(), ctx.getSpeedPid(),
                dtSec, position.getAngleDeg()
        );

        if(r.goalReached){
            ctx.setLastMotorSpeed(new SpeedDTO(newTimestamp, 0));
            ctx.setLastMotorState(new MotorStateDTO(newTimestamp, true, false));
            ctx.setLastTurnSpeed(new TurnDTO(newTimestamp, 0));
            ctx.setState(RobotState.STANDBY);

            return;
        }

        SpeedDTO newSpeed = new SpeedDTO(newTimestamp, r.forwardTarget);
        MotorStateDTO newMotorState = new MotorStateDTO(newTimestamp, r.halted, r.inAvoid);
        TurnDTO newTurn = new TurnDTO(newTimestamp, r.turnFrac);

        ctx.setLastMotorSpeed(newSpeed);
        ctx.setLastMotorState(newMotorState);
        ctx.setLastTurnSpeed(newTurn);

        Acl.publish(myAgent, topicSpeed, JsonUtil.toJson(newSpeed));
        Acl.publish(myAgent, topicTurn, JsonUtil.toJson(newTurn));
        Acl.publish(myAgent, topicState, JsonUtil.toJson(newMotorState));

        lastTimestamp = newTimestamp;

        System.out.println(r);
//        System.out.println(ctx.getLastMotorSpeed().toString());
//        System.out.println(ctx.getLastMotorState().toString());
//        System.out.println(ctx.getLastTurnSpeed().toString());
    }

}
