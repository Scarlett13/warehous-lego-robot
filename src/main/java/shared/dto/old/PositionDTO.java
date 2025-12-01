package shared.dto.old;

public class PositionDTO {
    private long timestamp;
    private int x;
    private int y;
    private float angleDeg;

    public PositionDTO() {}

    public PositionDTO(long timestamp, int x, int y, float angleDeg) {
        this.timestamp = timestamp;
        this.x = x;
        this.y = y;
        this.angleDeg = angleDeg;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public float getAngleDeg() {
        return angleDeg;
    }

    public void setAngleDeg(float angleDeg) {
        this.angleDeg = angleDeg;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "PositionDTO{" + "timestamp=" + timestamp + ", x=" + x + ", y=" + y + ", angle(yaw):"+ angleDeg + '}';
    }
}
