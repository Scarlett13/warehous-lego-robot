package shared;

import java.io.Serializable;

public class DistancePidResultDTO implements Serializable {
    private double forwardTarget;   // [-TURN_MAX..+TURN_MAX]
    private boolean halted;
    private boolean inAvoid;
    private long millis;

    public DistancePidResultDTO(long millis, double forwardTarget, boolean halted, boolean inAvoid) {
        this.forwardTarget = forwardTarget;
        this.halted = halted;
        this.inAvoid = inAvoid;
        this.millis = millis;
    }

    public double getForwardTarget() {
        return forwardTarget;
    }

    public void setForwardTarget(double forwardTarget) {
        this.forwardTarget = forwardTarget;
    }

    public boolean isHalted() {
        return halted;
    }

    public void setHalted(boolean halted) {
        this.halted = halted;
    }

    public boolean isInAvoid() {
        return inAvoid;
    }

    public void setInAvoid(boolean inAvoid) {
        this.inAvoid = inAvoid;
    }

    public long getMillis() {
        return millis;
    }

    public void setMillis(long millis) {
        this.millis = millis;
    }
}
