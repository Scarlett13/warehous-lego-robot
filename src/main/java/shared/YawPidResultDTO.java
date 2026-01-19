package shared;

import java.io.Serializable;

public class YawPidResultDTO implements Serializable {
    private double turnCorrection;
    private long millis;

    public YawPidResultDTO(long millis, double turnCorrection) {
        this.millis = millis;
        this.turnCorrection = turnCorrection;
    }

    public double getTurnCorrection() {
        return turnCorrection;
    }

    public void setTurnCorrection(double turnCorrection) {
        this.turnCorrection = turnCorrection;
    }

    public long getMillis() {
        return millis;
    }

    public void setMillis(long millis) {
        this.millis = millis;
    }
}
