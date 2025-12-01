package shared.dto;

public class PositionSpeedPidDTO {
    private double forwardTarget;
    private long millis;

    public PositionSpeedPidDTO(double forwardTarget, long millis) {
        this.forwardTarget = forwardTarget;
        this.millis = millis;
    }

    public double getForwardTarget() {
        return forwardTarget;
    }

    public void setForwardTarget(double forwardTarget) {
        this.forwardTarget = forwardTarget;
    }

    public long getMillis() {
        return millis;
    }

    public void setMillis(long millis) {
        this.millis = millis;
    }
}
