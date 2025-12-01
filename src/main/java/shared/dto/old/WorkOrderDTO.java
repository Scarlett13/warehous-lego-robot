package shared.dto.old;

import shared.dto.ItemDTO;

import java.io.Serializable;

public class WorkOrderDTO implements Serializable {
    public enum WorkStatusEnum {
        READY,
        PICKED_UP,
        COMPLETED,
        POSTPONED,
        SCHEDULED
    }
    private String workOrderId;
    private String workOrderName;
    private ItemDTO item;

    private int priority;
    private WorkStatusEnum status;
}
