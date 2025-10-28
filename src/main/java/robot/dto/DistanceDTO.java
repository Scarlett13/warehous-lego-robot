package robot.dto;

public class DistanceDTO {
    private long timestamp;
    private double distance;

    public DistanceDTO() {}

    public DistanceDTO(long timestamp, double distance) {
        this.timestamp = timestamp;
        this.distance = distance;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    @Override
    public String toString() {
        return "DistanceDTO{" + "timestamp=" + timestamp + ", distance=" + distance + '}';
    }

}
