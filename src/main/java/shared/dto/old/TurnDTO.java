package shared.dto.old;

public class TurnDTO {
    private long timestamp;
    private double turnFraction; // -1..1

    public TurnDTO() {}

    public TurnDTO(long timestamp, double turnFraction) {
        this.timestamp = timestamp;
        this.turnFraction = turnFraction;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public double getTurnFraction() {
        return turnFraction;
    }

    public void setTurnFraction(double turnFraction) {
        this.turnFraction = turnFraction;
    }

    public String toString() {
        return "TurnDTO{" + "timestamp=" + timestamp + ", turnFraction=" + turnFraction + '}';
    }
}
