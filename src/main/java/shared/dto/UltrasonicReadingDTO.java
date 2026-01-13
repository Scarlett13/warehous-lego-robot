package shared.dto;

public class UltrasonicReadingDTO {
    private long timestamp;
    private int distance;
    private long dtsec;

    public UltrasonicReadingDTO() {}

    public UltrasonicReadingDTO(long timestamp, int distance, long dtsec) {
        this.timestamp = timestamp;
        this.distance = distance;
        this.dtsec = dtsec;
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
        return "UltrasonicDTO{" +
                "timestamp=" + timestamp +
                ", distance=" + distance +
                ", dtsec=" + dtsec +
                '}';
    }


    public long getDtsec() {
        return dtsec;
    }

    public void setDtsec(long dtsec) {
        this.dtsec = dtsec;
    }
}
