package shared.dto;

import java.io.Serializable;

public class RobotStatusDTO implements Serializable {

    public enum RobotMode {
        IDLE,
        WORKING,
        GOING_TO_CHARGE,
        ERROR
    }

    public enum MotionMode {
        STOPPED,
        STRAIGHT,
        TURNING,
        IN_AVOID
    }

    private String robotId;
    private long timestampMillis;

    private RobotMode robotMode;
    private MotionMode motionMode;

    private double batteryPct;      // [0..100]
    private String currentWorkId = null;   // null if none

    private Point2D robotPosition;
    private Point2D robotTarget;

    private Boolean inAvoid, isHalted;

    private double robotCurrentSpeed;

    private double distanceReading;

    private double angleDiff;

    public RobotStatusDTO() {
    }

    public RobotStatusDTO(String robotId,
                          long timestampMillis,
                          RobotMode robotMode,
                          MotionMode motionMode,
                          boolean inAvoid,
                          boolean isHalted,
                          double batteryPct,
                          Point2D robotPosition,
                          String currentWorkId,
                          double robotCurrentSpeed,
                          double distanceReading,
                          double angleDiff,
                          Point2D robotTarget) {
        this.robotId = robotId;
        this.timestampMillis = timestampMillis;
        this.robotMode = robotMode;
        this.motionMode = motionMode;
        this.batteryPct = batteryPct;
        this.currentWorkId = currentWorkId;
        this.robotPosition = robotPosition;
        this.inAvoid = inAvoid;
        this.isHalted = isHalted;
        this.robotCurrentSpeed = robotCurrentSpeed;
        this.distanceReading = distanceReading;
        this.angleDiff = angleDiff;
        this.robotTarget = robotTarget;
    }

    public Point2D getRobotTarget() {
        return robotTarget;
    }

    public void setRobotTarget(Point2D robotTarget) {
        this.robotTarget = robotTarget;
    }

    public String getRobotId() {
        return robotId;
    }

    public void setRobotId(String robotId) {
        this.robotId = robotId;
    }

    public long getTimestampMillis() {
        return timestampMillis;
    }

    public void setTimestampMillis(long timestampMillis) {
        this.timestampMillis = timestampMillis;
    }

    public RobotMode getRobotMode() {
        return robotMode;
    }

    public void setRobotMode(RobotMode robotMode) {
        this.robotMode = robotMode;
    }

    public MotionMode getMotionMode() {
        return motionMode;
    }

    public void setMotionMode(MotionMode motionMode) {
        this.motionMode = motionMode;
    }

    public double getBatteryPct() {
        return batteryPct;
    }

    public void setBatteryPct(double batteryPct) {
        this.batteryPct = batteryPct;
    }

    public String getCurrentWorkId() {
        return currentWorkId;
    }

    public void setCurrentWorkId(String currentWorkId) {
        this.currentWorkId = currentWorkId;
    }

    public Point2D getRobotPosition() {
        return robotPosition;
    }

    public void setRobotPosition(Point2D robotPosition) {
        this.robotPosition = robotPosition;
    }

    public Boolean getInAvoid() {
        return inAvoid;
    }

    public void setInAvoid(Boolean inAvoid) {
        this.inAvoid = inAvoid;
    }

    public Boolean getHalted() {
        return isHalted;
    }

    public void setHalted(Boolean halted) {
        isHalted = halted;
    }

    public double getRobotCurrentSpeed() {
        return robotCurrentSpeed;
    }

    public void setRobotCurrentSpeed(double robotCurrentSpeed) {
        this.robotCurrentSpeed = robotCurrentSpeed;
    }

    public double getDistanceReading() {
        return distanceReading;
    }

    public void setDistanceReading(double distanceReading) {
        this.distanceReading = distanceReading;
    }

    public double getAngleDiff() {
        return angleDiff;
    }

    public void setAngleDiff(double angleDiff) {
        this.angleDiff = angleDiff;
    }

    @Override
    public String toString() {
        return "RobotStatusDTO{" +
                "robotId='" + robotId + '\'' +
                ", timestampMillis=" + timestampMillis +
                ", robotMode=" + robotMode +
                ", motionMode=" + motionMode +
                ", batteryPct=" + batteryPct +
                ", currentWorkId='" + currentWorkId + '\'' +
                '}';
    }
}
