package shared.dto;

import robot.utils.RobotState;
import shared.dto.old.PositionDTO;

import java.io.Serializable;


public class RobotStatusDTO implements Serializable {
    private long timestampMillis;

    private RobotState robotState;
    private double batteryPct;      // [0..100]
    private String currentWorkId = null;   // null if none

    private PositionDTO robotPosition;
    private UltrasonicReadingDTO ultrasonicReading;

    public RobotStatusDTO() {
    }

    public RobotStatusDTO(long timestampMillis,
                          double batteryPct,
                          PositionDTO robotPosition,
                          String currentWorkId,
                          UltrasonicReadingDTO ultrasonicReading,
                          RobotState robotState) {
        this.timestampMillis = timestampMillis;
        this.batteryPct = batteryPct;
        this.currentWorkId = currentWorkId;
        this.robotPosition = robotPosition;
        this.ultrasonicReading = ultrasonicReading;
        this.robotState = robotState;
    }

    public long getTimestampMillis() {
        return timestampMillis;
    }

    public void setTimestampMillis(long timestampMillis) {
        this.timestampMillis = timestampMillis;
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

    public PositionDTO getRobotPosition() {
        return robotPosition;
    }

    public void setRobotPosition(PositionDTO robotPosition) {
        this.robotPosition = robotPosition;
    }

    public UltrasonicReadingDTO getUltrasonicReading() {
        return ultrasonicReading;
    }

    public void setUltrasonicReading(UltrasonicReadingDTO ultrasonicReading) {
        this.ultrasonicReading = ultrasonicReading;
    }

    public RobotState getRobotState() {
        return robotState;
    }

    public void setRobotState(RobotState robotState) {
        this.robotState = robotState;
    }

    @Override
    public String toString() {
        return "RobotStatusDTO{" +
                "timestampMillis=" + timestampMillis +
                ", batteryPct=" + batteryPct +
                ", currentWorkId=" + currentWorkId +
                ", robotPosition=" + robotPosition.toString() +
                ", ultrasonicReading=" + ultrasonicReading.toString() +
                ", robotState=" + robotState.toString() +
                '}';
    }

}
