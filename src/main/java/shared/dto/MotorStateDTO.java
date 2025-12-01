package shared.dto;

import java.io.Serializable;

public class MotorStateDTO implements Serializable {
    private long millis;
    private double motorSpeed;

    public MotorStateDTO(long millis, double motorSpeed) {
        this.millis = millis;
        this.motorSpeed = motorSpeed;
    }

    public long getMillis() {
        return millis;
    }

    public void setMillis(long millis) {
        this.millis = millis;
    }

    public double getMotorSpeed() {
        return motorSpeed;
    }

    public void setMotorSpeed(double motorSpeed) {
        this.motorSpeed = motorSpeed;
    }

    public String toString(){
        return "MotorStateDTO{" + "millis=" + millis + ", motorSpeed=" + motorSpeed + '}';
    }
}
