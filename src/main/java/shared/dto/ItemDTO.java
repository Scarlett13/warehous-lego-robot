package shared.dto;

import java.io.Serializable;

public class ItemDTO implements Serializable {
    private String itemId;
    private int itemFreshness;

    public ItemDTO(String itemId, int itemFreshness) {
        this.itemId = itemId;
        this.itemFreshness = itemFreshness;
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public int getItemFreshness() {
        return itemFreshness;
    }

    public void setItemFreshness(int itemFreshness) {
        this.itemFreshness = itemFreshness;
    }
}
