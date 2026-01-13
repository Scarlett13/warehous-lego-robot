package shared.dto;

import java.io.Serializable;

public class FruitItemDTO implements Serializable {
    public enum FruitItemWorkStatusEnum {
        READY,
        PICKED_UP,
        DELIVERED,
        POSTPONED,
        SCHEDULED
    }

    private String conveyorName;
    private String itemId;
    private Long lastDeliveryMillis;
    private int freshness;

    private FruitItemWorkStatusEnum status;
    private String robotId;

    public FruitItemDTO(String conveyorName, String itemId, Long lastDeliveryMillis, int freshness) {
        this.conveyorName = conveyorName;
        this.itemId = itemId;
        this.lastDeliveryMillis = lastDeliveryMillis;
        this.freshness = freshness;
        this.status = FruitItemWorkStatusEnum.READY;
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public Long getLastDeliveryMillis() {
        return lastDeliveryMillis;
    }

    public void setLastDeliveryMillis(Long lastDeliveryMillis) {
        this.lastDeliveryMillis = lastDeliveryMillis;
    }

    public int getFreshness() {
        return freshness;
    }

    public void setFreshness(int freshness) {
        this.freshness = freshness;
    }

    public FruitItemWorkStatusEnum getStatus() {
        return status;
    }

    public void setStatus(FruitItemWorkStatusEnum status) {
        this.status = status;
    }

    public String getConveyorName() {
        return conveyorName;
    }

    public void setConveyorName(String conveyorName) {
        this.conveyorName = conveyorName;
    }

    private Long statusChangeTimestamp;

    public void updateFruitJobStatus(String robotId, FruitItemWorkStatusEnum status) {
        this.robotId = robotId;
        this.status = status;
        this.statusChangeTimestamp = System.currentTimeMillis();
    }

    public Long getStatusChangeTimestamp() {
        return statusChangeTimestamp;
    }

    public void setStatusChangeTimestamp(Long statusChangeTimestamp) {
        this.statusChangeTimestamp = statusChangeTimestamp;
    }

    public String getRobotId() {
        return this.robotId;
    }
}
