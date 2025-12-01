package robot;

import shared.DistancePidResultDTO;
import shared.dto.*;
import robot.sims.UltrasonicReadingUtilDummy;
import shared.dto.old.DestinationDTO;
import shared.dto.old.PositionDTO;
import shared.utils.CommonPid;
import robot.utils.RobotState;

import static robot.RobotConstants.*;


public class RobotContext {
    private volatile RobotState state                            = RobotState.STANDBY;

    private volatile DistanceDTO lastDistance                    = new DistanceDTO(0,0);
    private volatile DistancePidResultDTO lastDistancePidResult  = new DistancePidResultDTO(0, 0, false, false);
    private volatile PositionDTO lastPosition                    = new PositionDTO(0, 0,0,0);

    private volatile DestinationDTO lastDestination              = new DestinationDTO(0, 0, 0);

    private static final CommonPid distancePid                   = new CommonPid(PSPEED, ISPEED, DSPEED);

    private final UltrasonicReadingUtilDummy dummyUltrasonic = new UltrasonicReadingUtilDummy();

    public void instantiatePid(){
        distancePid.setOutputLimits(200);
        distancePid.setSetpoint(0);
        distancePid.setSetpoint(SPEED_MAX);
        distancePid.setSetpointRange(SPEED_MIN);

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

    public CommonPid getDistancePid() {
        return distancePid;
    }

    public UltrasonicReadingUtilDummy getDummyUltrasonic() {
        return this.dummyUltrasonic;
    }



    public DistancePidResultDTO getLastDistancePidResult() {
        return lastDistancePidResult;
    }

    public void setLastDistancePidResult(DistancePidResultDTO lastDistancePidResult) {
        this.lastDistancePidResult = lastDistancePidResult;
    }

    public DistanceDTO getLastDistance() {
        return lastDistance;
    }

    public void setLastDistance(DistanceDTO lastDistance) {
        this.lastDistance = lastDistance;
    }

    public PositionDTO getLastPosition() {
        return lastPosition;
    }

    public void setLastPosition(PositionDTO lastPosition) {
        this.lastPosition = lastPosition;
    }

    public DestinationDTO getLastDestination() {
        return lastDestination;
    }

    public void setLastDestination(DestinationDTO lastDestination) {
        this.lastDestination = lastDestination;
    }
}