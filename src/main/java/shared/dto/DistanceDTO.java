package shared.dto;

public class DistanceDTO {
    private long timestamp;
    private int distance;

    public DistanceDTO() {}

    public DistanceDTO(long timestamp, int distance) {
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

    public void setDistance(int distance) {
        this.distance = distance;
    }

    @Override
    public String toString() {
        return "DistanceDTO{" + "timestamp=" + timestamp + ", distance=" + distance + '}';
    }

}
