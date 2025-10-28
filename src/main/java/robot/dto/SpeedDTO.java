package robot.dto;

public class SpeedDTO {
    private long timestamp;
    private double speed;

    public SpeedDTO() {}

    public SpeedDTO(long timestamp, double speed) { 
        this.timestamp = timestamp; 
        this.speed = speed; 
    }
    
    public long getTimestamp() { 
        return timestamp; 
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public double getSpeed() {
        return speed;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    public String toString() {
        return "SpeedDTO{" + "timestamp=" + timestamp + ", speed=" + speed + '}';
    }
}
