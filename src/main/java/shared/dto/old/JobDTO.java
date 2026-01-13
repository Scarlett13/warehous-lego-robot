package shared.dto.old;

public class JobDTO {
    long lastDeliveryTime;
    long startPickingTime;
    int is_fresh; //0 = rotten, 1 = almost rot, 2 = fresh

    int status; //0=not picked, 1=in delivery, 2=delivered
    int id; //id of the item
}