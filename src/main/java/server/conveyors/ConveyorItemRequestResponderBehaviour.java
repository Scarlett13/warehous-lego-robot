package server.conveyors;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import shared.dto.FruitItemDTO;
import shared.messaging.MessagingConstants;
import shared.utils.JsonUtil;

import java.util.Optional;

public class ConveyorItemRequestResponderBehaviour extends CyclicBehaviour {
    private final ConveyorAgent conveyorAgent;
    private final MessageTemplate mt;

    public ConveyorItemRequestResponderBehaviour(ConveyorAgent agent) {
        super(agent);
        this.conveyorAgent = agent;

        this.mt = MessageTemplate.and(
                MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
                MessageTemplate.and(
                        MessageTemplate.MatchOntology(MessagingConstants.ONTOLOGY_ITEM_REQUEST),
                        MessageTemplate.MatchConversationId(MessagingConstants.CONVERSATION_ITEM_REQUEST)));
    }

    @Override
    public void action() {
        ACLMessage msg = myAgent.receive(mt);
        if (msg != null) {
            handleRequest(msg);
        } else {
            block();
        }
    }

    private void handleRequest(ACLMessage msg) {
        ACLMessage reply = msg.createReply();
        String senderName = msg.getSender().getName();

        synchronized (conveyorAgent.getConveyorData().getConveyorItems()) {
            Optional<FruitItemDTO> itemOpt = conveyorAgent.getConveyorData().getConveyorItems().stream()
                    .filter(i -> i.getStatus() == FruitItemDTO.FruitItemWorkStatusEnum.READY)
                    .findFirst();

            if (itemOpt.isPresent()) {
                FruitItemDTO item = itemOpt.get();
                // GUARD: Mark as taken immediately interactions
                item.setStatus(FruitItemDTO.FruitItemWorkStatusEnum.SCHEDULED);
                item.updateFruitJobStatus(senderName, FruitItemDTO.FruitItemWorkStatusEnum.SCHEDULED);

                conveyorAgent.updateOpcuaStatus();

                reply.setPerformative(ACLMessage.AGREE);
                reply.setContent(JsonUtil.toJson(item)); // Send the full item DTO
                System.out
                        .println(myAgent.getLocalName() + ": Assigned Item " + item.getItemId() + " to " + senderName);
            } else {
                reply.setPerformative(ACLMessage.REFUSE);
                reply.setContent("No items available");
                System.out.println(myAgent.getLocalName() + ": Refused request from " + senderName + " (No items)");
            }
        }
        myAgent.send(reply);
    }
}
