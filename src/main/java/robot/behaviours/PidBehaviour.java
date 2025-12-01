//package robot.behaviours;
//
//import jade.core.AID;
//import jade.core.Agent;
//import jade.core.behaviours.TickerBehaviour;
//import robot.RobotContext;
//import robot.control.DistancePidControl;
//import shared.dto.old.*;
//import shared.messaging.TopicConstants;
//import shared.messaging.acl.Acl;
//import shared.dto.*;
//import robot.utils.*;
//import shared.messaging.TopicHelper;
//import shared.utils.JsonUtil;
//
//public class PidBehaviour extends TickerBehaviour {
//    private final AID topicSpeed;
//    private final AID topicTurn;
//    private final AID topicState;
//    private final RobotContext ctx;
//    private long lastTimestamp = System.currentTimeMillis();
//
//    public PidBehaviour(Agent a, long period, RobotContext ctx) {
//        super(a, period);
//
//        this.ctx = ctx;
//        this.topicSpeed = TopicHelper.topic(a, TopicConstants.SPEED_PID_TOPIC);
//        this.topicTurn = TopicHelper.topic(a, TopicConstants.ROBOT_TURN_TOPIC);
//        this.topicState = TopicHelper.topic(a, TopicConstants.ROBOT_STATE_TOPIC);
//    }
//
//    @Override
//    protected void onTick() {
//        if (ctx.getState() != RobotState.WORKING) return;
//
//        long newTimestamp = System.currentTimeMillis();
//        double dtSec =  newTimestamp - lastTimestamp;
//
//        DistanceDTO distance = ctx.getDistance();
//        PositionDTO position = ctx.getPosition();
//        DestinationDTO destination = ctx.getDestination();
//        MotorStateDTO motorStateDTO = ctx.getLastMotorState();
//        SpeedDTO speedDTO = ctx.getLastMotorSpeed();
//
//        PidResultDTO distanceResult = DistancePidControl.computeControl(
//                distance.getDistance(), position.getX(), position.getY(), destination.getX(), destination.getY(),
//                speedDTO.getSpeed(), motorStateDTO.isHalted(), ctx.getDistancePid(),
//                dtSec, position.getAngleDeg()
//        );
//
//        PidResultDTO positionResult = PositionPidUtil.computeControl(
//               position.getX(), position.getY(), destination.getX(), destination.getY(),
//                speedDTO.getSpeed(), ctx.getPositionPid(),
//                dtSec, position.getAngleDeg()
//        );
//
//        if(positionResult.isGoalReached()) {
//            ctx.setLastMotorSpeed(new SpeedDTO(newTimestamp, 0));
//            ctx.setLastMotorState(new MotorStateDTO(newTimestamp, true, false));
//            ctx.setLastTurnSpeed(new TurnDTO(newTimestamp, 0));
//            ctx.setState(RobotState.STANDBY);
//
//            Acl.publish(myAgent, topicSpeed, JsonUtil.toJson(new SpeedDTO(newTimestamp, 0)));
//            Acl.publish(myAgent, topicTurn, JsonUtil.toJson(new MotorStateDTO(newTimestamp, true, false)));
//            Acl.publish(myAgent, topicState, JsonUtil.toJson(new TurnDTO(newTimestamp, 0)));
//
//            lastTimestamp = newTimestamp;
//
//            return;
//        }
//
//        double speed = Math.min(distanceResult.getForwardTarget(), positionResult.getForwardTarget());
//        double turnfrac = Math.max(distanceResult.getTurnFrac(), positionResult.getTurnFrac());
//        boolean ishalted = positionResult.isHalted() || distanceResult.isGoalReached();
//        boolean isavoid = positionResult.isInAvoid() || distanceResult.isInAvoid();
//
//        SpeedDTO newSpeed = new SpeedDTO(newTimestamp, speed);
//        MotorStateDTO newMotorState = new MotorStateDTO(newTimestamp,  ishalted,  isavoid);
//        TurnDTO newTurn = new TurnDTO(newTimestamp, turnfrac);
//
//        ctx.setLastMotorSpeed(newSpeed);
//        ctx.setLastMotorState(newMotorState);
//        ctx.setLastTurnSpeed(newTurn);
//
//        Acl.publish(myAgent, topicSpeed, JsonUtil.toJson(newSpeed));
//        Acl.publish(myAgent, topicTurn, JsonUtil.toJson(newTurn));
//        Acl.publish(myAgent, topicState, JsonUtil.toJson(newMotorState));
//
//        lastTimestamp = newTimestamp;
//
////        System.out.println(r);
////        System.out.println(ctx.getLastMotorSpeed().toString());
////        System.out.println(ctx.getLastMotorState().toString());
////        System.out.println(ctx.getLastTurnSpeed().toString());
//    }
//
//    private void calculateSpeedUltrasonic(){
//
//    }
//
//    private void calculateSpeedPozyx(){
//
//    }
//
//    private void calculateTurnPozyx(){
//    }
//
//}
