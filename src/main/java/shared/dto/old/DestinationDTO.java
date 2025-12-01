package shared.dto.old;

public class DestinationDTO {
    private long timestamp;
    private double x;
    private double y;

    public DestinationDTO() {}
    
    public DestinationDTO(long timestamp, double x, double y) {
        this.timestamp = timestamp; 
        this.x = x; 
        this.y = y;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    public double getX() {
        return x;
    }
    
    public void setX(double x) {
        this.x = x;
    }
    
    public double getY() {
        return y;
    }
    
    public void setY(double y) {
        this.y = y;
    }

    @Override
    public String toString() {
        return "Destination : {x:"+x+", y:"+y+", z:0}";
    }
}
