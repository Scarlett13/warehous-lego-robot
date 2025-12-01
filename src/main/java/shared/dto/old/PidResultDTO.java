package shared.dto.old;

public class PidResultDTO {
    private double forwardTarget; // deg/s
    private double turnFrac;      // [-TURN_MAX..+TURN_MAX]
    private boolean halted;
    private boolean inAvoid;
    private int avoidSide;        // -1 left, +1 right, 0 none
    private boolean goalReached;

    public PidResultDTO(double forwardTarget, double turnFrac, boolean halted, boolean inAvoid, int avoidSide, boolean goalReached) {
        this.forwardTarget = forwardTarget;
        this.turnFrac = turnFrac;
        this.halted = halted;
        this.inAvoid = inAvoid;
        this.avoidSide = avoidSide;
        this.goalReached = goalReached;
    }

    public double getForwardTarget() {
        return forwardTarget;
    }

    public void setForwardTarget(double forwardTarget) {
        this.forwardTarget = forwardTarget;
    }

    public double getTurnFrac() {
        return turnFrac;
    }

    public void setTurnFrac(double turnFrac) {
        this.turnFrac = turnFrac;
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

    public int getAvoidSide() {
        return avoidSide;
    }

    public void setAvoidSide(int avoidSide) {
        this.avoidSide = avoidSide;
    }

    public boolean isGoalReached() {
        return goalReached;
    }

    public void setGoalReached(boolean goalReached) {
        this.goalReached = goalReached;
    }
}
