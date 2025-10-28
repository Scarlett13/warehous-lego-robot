package robot.dto;

public class MotorStateDTO {
    private long millis;
    private boolean isHalted;
    private boolean inTurn;

    public MotorStateDTO(long millis, boolean isHalted, boolean inTurn) {
        this.millis = millis;
        this.isHalted = isHalted;
        this.inTurn = inTurn;
    }

    public long getMillis() {
        return millis;
    }

    public void setMillis(long millis) {
        this.millis = millis;
    }

    public boolean isHalted() {
        return isHalted;
    }

    public void setHalted(boolean halted) {
        isHalted = halted;
    }

    public boolean isInTurn() {
        return inTurn;
    }

    public void setInTurn(boolean inTurn) {
        this.inTurn = inTurn;
    }

    public String toString(){
        return "MotorStateDTO{" + "millis=" + millis + ", inTurn=" + inTurn + '}';
    }
}
