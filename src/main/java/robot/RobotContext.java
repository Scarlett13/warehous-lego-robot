package robot;

import robot.dto.*;
import robot.sims.UltrasonicReadingUtilDummy;
import robot.utils.CommonPid;
import robot.utils.RobotState;

import static robot.Constants.*;


public class RobotContext {
    private volatile RobotState state               = RobotState.STANDBY;

    private volatile DistanceDTO lastDistance       = new DistanceDTO(0,0);
    private volatile PositionDTO lastPosition       = new PositionDTO(0, 0,0,0);
    private volatile DestinationDTO lastDestination = new DestinationDTO(0, 0, 0);
    private volatile SpeedDTO lastMotorSpeed        = new SpeedDTO(0, 0);
    private volatile TurnDTO lastTurnSpeed          = new TurnDTO(0, 0);
    private volatile MotorStateDTO lastMotorState   = new MotorStateDTO(0, true, false);
    private static final CommonPid speedPid         = new CommonPid(PSPEED, ISPEED, DSPEED);
    private final UltrasonicReadingUtilDummy dummyUltrasonic = new UltrasonicReadingUtilDummy();

    public void instantiatePid(){
        speedPid.setOutputLimits(200);
        speedPid.setSetpoint(0);
        speedPid.setSetpoint(SPEED_MAX);
        speedPid.setSetpointRange(SPEED_MIN);

    }

    public RobotState getState() {
        return state;
    }

    public void setState(RobotState s) {
        state = s;
    }

    public DistanceDTO getDistance() {
        return lastDistance;
    }

    public void setDistance(DistanceDTO d) {
        lastDistance = d;
    }

    public PositionDTO getPosition() {
        return lastPosition;
    }

    public void setPosition(PositionDTO p) {
        lastPosition = p;
    }

    public DestinationDTO getDestination() {
        return lastDestination;
    }

    public void setDestination(DestinationDTO d) {
        lastDestination = d;
    }

    public MotorStateDTO getLastMotorState() {
        return lastMotorState;
    }

    public void setLastMotorState(MotorStateDTO lastMotorState) {
        this.lastMotorState = lastMotorState;
    }

    public TurnDTO getLastTurnSpeed() {
        return lastTurnSpeed;
    }

    public void setLastTurnSpeed(TurnDTO lastTurnSpeed) {
        this.lastTurnSpeed = lastTurnSpeed;
    }

    public SpeedDTO getLastMotorSpeed() {
        return lastMotorSpeed;
    }

    public void setLastMotorSpeed(SpeedDTO lastMotorSpeed) {
        this.lastMotorSpeed = lastMotorSpeed;
    }

    public CommonPid getSpeedPid() {
        return speedPid;
    }

    public UltrasonicReadingUtilDummy getDummyUltrasonic() {
        return this.dummyUltrasonic;
    }
}