package shared.dto;

import robot.utils.RobotState;

import java.io.Serializable;

public class RobotCommandDTO implements Serializable {
    /*
     * robot command are given by digital twin to physical twin
     * command can come from digital twin decision or VC analysis
     * external command was given based on ultrasonic reading, robot position,
     * target location, battery level, and work order
     */

    private String robotName;
    private long timestamp;
    private double targetMotorSpeed;
    private double turnCorrection;
    private RobotState robotState;
    private String itemId;
    private double targetX;
    private double targetY;

    public RobotCommandDTO(String robotName, long timestamp, double targetMotorSpeed, double turnCorrection,
            RobotState robotState, String itemId, Double targetX, Double targetY) {
        this.robotName = robotName;
        this.timestamp = timestamp;
        this.targetMotorSpeed = targetMotorSpeed;
        this.turnCorrection = turnCorrection;
        this.robotState = robotState;
        this.itemId = itemId;
        this.targetX = targetX;
        this.targetY = targetY;
    }

    public RobotCommandDTO(String robotName, long timestamp, double targetMotorSpeed, double turnCorrection,
            RobotState robotState, String itemId) {
        this(robotName, timestamp, targetMotorSpeed, turnCorrection, robotState, itemId, null, null);
    }

    public String getRobotName() {
        return robotName;
    }

    public void setRobotName(String robotName) {
        this.robotName = robotName;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public double getTargetMotorSpeed() {
        return targetMotorSpeed;
    }

    public void setTargetMotorSpeed(double targetMotorSpeed) {
        this.targetMotorSpeed = targetMotorSpeed;
    }

    public double getTurnCorrection() {
        return turnCorrection;
    }

    public void setTurnCorrection(double turnCorrection) {
        this.turnCorrection = turnCorrection;
    }

    public RobotState getRobotState() {
        return robotState;
    }

    public void setRobotState(RobotState robotState) {
        this.robotState = robotState;
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public double getTargetX() {
        return targetX;
    }

    public void setTargetX(double targetX) {
        this.targetX = targetX;
    }

    public double getTargetY() {
        return targetY;
    }

    public void setTargetY(double targetY) {
        this.targetY = targetY;
    }
}
