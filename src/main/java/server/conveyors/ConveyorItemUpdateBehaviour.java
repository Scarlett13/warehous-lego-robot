package server.conveyors;

import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import shared.dto.FruitItemDTO;
import shared.utils.JsonUtil;

public class ConveyorItemUpdateBehaviour extends CyclicBehaviour {
    private final ConveyorAgent agent;
    private final MessageTemplate mt;

    public ConveyorItemUpdateBehaviour(ConveyorAgent agent) {
        this.agent = agent;
        // Listen for INFORM messages (Updates)
        this.mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
    }

    @Override
    public void action() {
        ACLMessage msg = agent.receive(mt);
        if (msg != null) {
            String content = msg.getContent();
            try {
                // Try to parse as FruitItemDTO
                FruitItemDTO item = JsonUtil.fromJson(content, FruitItemDTO.class);

                if (item != null && item.getItemId() != null) {
                    handleItemUpdate(item);
                }
            } catch (Exception e) {
                // Ignore non-FruitItem messages
            }
        } else {
            block();
        }
    }

    private void handleItemUpdate(FruitItemDTO item) {
        String conveyorName = agent.getConveyorData().getConveyorName();
        boolean changed = false;

        if (item.getStatus() == FruitItemDTO.FruitItemWorkStatusEnum.PICKED_UP) {
            // Remove from this conveyor (Input Location)
            boolean removed = agent.getConveyorData().getConveyorItems()
                    .removeIf(ExistingItem -> ExistingItem.getItemId().equals(item.getItemId()));
            if (removed) {
                System.out.println(" [" + conveyorName + "] Item Picked Up: " + item.getItemId());
                changed = true;
            }
        } else if (item.getStatus() == FruitItemDTO.FruitItemWorkStatusEnum.DELIVERED) {
            // Add to this conveyor (Output Location)
            // Check if already exists to avoid duplicates
            boolean exists = agent.getConveyorData().getConveyorItems().stream()
                    .anyMatch(i -> i.getItemId().equals(item.getItemId()));

            if (!exists) {
                agent.getConveyorData().getConveyorItems().add(item);
                System.out.println(" [" + conveyorName + "] Item Delivered: " + item.getItemId());
                changed = true;
            }
        }

        if (changed) {
            agent.updateOpcuaStatus();
        }
    }
}
