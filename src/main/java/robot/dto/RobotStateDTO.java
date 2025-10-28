// src/main/java/robot/dto/StateDTO.java
package robot.dto;


import robot.utils.RobotState;

public class RobotStateDTO {
    private long timestamp;
    private RobotState state;

    public RobotStateDTO() {}
    
    public RobotStateDTO(long timestamp, RobotState state) { 
        this.timestamp = timestamp; 
        this.state = state; 
    }
    
    public long getTimestamp() { 
        return timestamp; 
    }
    
    public void setTimestamp(long timestamp) { 
        this.timestamp = timestamp; 
    }
    
    public RobotState getState() {
        return state; 
    }
    
    public void setState(RobotState state) {
        this.state = state;
    }
}
