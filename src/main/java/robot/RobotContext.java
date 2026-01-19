package robot;

import shared.DistancePidResultDTO;
import shared.YawPidResultDTO;
import shared.dto.*;
import robot.sims.UltrasonicReadingUtilDummy;
import shared.dto.old.PositionDTO;
import shared.utils.CommonPid;
import robot.utils.RobotState;

import static robot.RobotConstants.*;
import static shared.SharedConstants.MAX_BATTERY;

public class RobotContext {
    private volatile RobotState state = RobotState.STANDBY;
    private volatile String workId = null;
    private volatile int robotBatteryPercentage = MAX_BATTERY;

    private volatile UltrasonicReadingDTO lastUltrasonicReading = new UltrasonicReadingDTO(0, 0, 0);
    private volatile DistancePidResultDTO ultrasonicPidResult = new DistancePidResultDTO(0, 0, false, false);
    private volatile YawPidResultDTO yawPidResult = new YawPidResultDTO(0, 0);
    private volatile PositionDTO lastPosition = new PositionDTO(0, 0, 0, 0, 0);

    // Simulation Fields
    private volatile double targetSpeed = 0.0;
    private volatile double turnCorrection = 0.0;

    private static final CommonPid ultrasonicPid = new CommonPid(ULTRASONIC_P, ULTRASONIC_I, ULTRASONIC_D);
    private static final CommonPid yawPid = new CommonPid(0.01, 0.0, 0.0);

    private final UltrasonicReadingUtilDummy dummyUltrasonic = new UltrasonicReadingUtilDummy();

    public void instantiatePid() {
        ultrasonicPid.setOutputLimits(200);
        ultrasonicPid.setSetpoint(0);
        ultrasonicPid.setSetpoint(SPEED_MAX);
        ultrasonicPid.setSetpointRange(SPEED_MIN);

        yawPid.setOutputLimits(1.0); // Turn is [-1.0, 1.0]
    }

    public CommonPid getYawPid() {
        return yawPid;
    }

    public YawPidResultDTO getYawPidResult() {
        return yawPidResult;
    }

    public void setYawPidResult(YawPidResultDTO yawPidResult) {
        this.yawPidResult = yawPidResult;
    }

    public RobotState getState() {
        return state;
    }

    public void setState(RobotState s) {
        state = s;
    }

    public CommonPid getDistancePid() {
        return ultrasonicPid;
    }

    public UltrasonicReadingUtilDummy getDummyUltrasonic() {
        return this.dummyUltrasonic;
    }

    public DistancePidResultDTO getUltrasonicPidResult() {
        return ultrasonicPidResult;
    }

    public void setUltrasonicPidResult(DistancePidResultDTO ultrasonicPidResult) {
        this.ultrasonicPidResult = ultrasonicPidResult;
    }

    public UltrasonicReadingDTO getLastUltrasonicReading() {
        return lastUltrasonicReading;
    }

    public void setLastUltrasonicReading(UltrasonicReadingDTO lastUltrasonicReading) {
        this.lastUltrasonicReading = lastUltrasonicReading;
    }

    public PositionDTO getLastPosition() {
        return lastPosition;
    }

    public void setLastPosition(PositionDTO lastPosition) {
        this.lastPosition = lastPosition;
    }

    public String getWorkId() {
        return workId;
    }

    public void setWorkId(String workId) {
        this.workId = workId;
    }

    public int getRobotBatteryPercentage() {
        return robotBatteryPercentage;
    }

    public void setRobotBatteryPercentage(int robotBatteryPercentage) {
        this.robotBatteryPercentage = robotBatteryPercentage;
    }

    public double getTargetSpeed() {
        return targetSpeed;
    }

    public void setTargetSpeed(double targetSpeed) {
        this.targetSpeed = targetSpeed;
    }

    public double getTurnCorrection() {
        return turnCorrection;
    }

    private volatile Double targetX = null;
    private volatile Double targetY = null;

    public void setTurnCorrection(double turnCorrection) {
        this.turnCorrection = turnCorrection;
    }

    public Double getTargetX() {
        return targetX;
    }

    public void setTargetX(Double targetX) {
        this.targetX = targetX;
    }

    public Double getTargetY() {
        return targetY;
    }

    public void setTargetY(Double targetY) {
        this.targetY = targetY;
    }
}