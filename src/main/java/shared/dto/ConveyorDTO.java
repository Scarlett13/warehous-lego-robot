package shared.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ConveyorDTO implements Serializable {
    private String conveyorName;
    private Point2D conveyorLocation;
    private final List<FruitItemDTO> conveyorItems = new ArrayList<>();

    public ConveyorDTO(String conveyorName, Point2D conveyorLocation) {
        this.conveyorName = conveyorName;
        this.conveyorLocation = conveyorLocation;
    }

    public String getConveyorName() {
        return conveyorName;
    }

    public void setConveyorName(String conveyorName) {
        this.conveyorName = conveyorName;
    }

    public Point2D getConveyorLocation() {
        return conveyorLocation;
    }

    public void setConveyorLocation(Point2D conveyorLocation) {
        this.conveyorLocation = conveyorLocation;
    }

    public List<FruitItemDTO> getConveyorItems() {
        return conveyorItems;
    }

    public void addFruitItem(FruitItemDTO item) {
        this.conveyorItems.add(item);
    }

    public boolean removeFruitById(String itemId) {
        return conveyorItems.removeIf(i -> i.getItemId().equals(itemId));
    }

    public FruitItemDTO findItemById(String itemId) {
        return conveyorItems.stream()
                .filter(i -> i.getItemId().equals(itemId))
                .findFirst()
                .orElse(null);
    }

    public void sortItems() {
        conveyorItems.sort(
                java.util.Comparator
                        // 4 and below means rotten, set priority to 1
                        // 5 - 10 freshness means not rotten, set priority to 0
                        .comparingInt((FruitItemDTO f) -> f.getFreshness() <= 4 ? 1 : 0)

                        // for each group of 0 and 1 from previous comparator,
                        // sort the priority in ascending order,
                        // means the freshness priority would be like:
                        // 5,6,7,8,9,10,1,2,3,4
                        .thenComparingInt(FruitItemDTO::getFreshness)

                        // sort by last delivery time (ascending)
                        .thenComparingLong(f -> {
                            Long t = f.getLastDeliveryMillis();
                            return t != null ? t : Long.MAX_VALUE;
                        }));
    }
}
