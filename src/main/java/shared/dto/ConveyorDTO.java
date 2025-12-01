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
}
