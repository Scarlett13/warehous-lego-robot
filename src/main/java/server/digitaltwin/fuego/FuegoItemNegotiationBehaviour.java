package server.digitaltwin.fuego;

import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import server.ServerConfig;
import shared.dto.FruitItemDTO;
import shared.messaging.MessagingConstants;
import shared.utils.JsonUtil;

import java.util.Random;

public class FuegoItemNegotiationBehaviour extends CyclicBehaviour {
    private final FuegoDTAgent myAgent;
    private long lastCheck = 0;
    private static final long CHECK_INTERVAL = 1000;
    private final MessageTemplate mt;
    private final Random random = new Random();

    public FuegoItemNegotiationBehaviour(FuegoDTAgent agent) {
        super(agent);
        this.myAgent = agent;
        this.mt = MessageTemplate.and(
                MessageTemplate.MatchOntology(MessagingConstants.ONTOLOGY_ITEM_REQUEST),
                MessageTemplate.MatchConversationId(MessagingConstants.CONVERSATION_ITEM_REQUEST));
    }

    @Override
    public void action() {
        // 1. Handle Replies
        ACLMessage msg = myAgent.receive(mt);
        if (msg != null) {
            handleMessage(msg);
        }

        // 2. Periodic Request
        if (System.currentTimeMillis() - lastCheck > CHECK_INTERVAL) {
            // Check State
            boolean isAbleToWork = (myAgent.getAssignedWorkId() == null || myAgent.getAssignedWorkId().isEmpty());

            if (isAbleToWork) {
                sendRequest();
            }
            lastCheck = System.currentTimeMillis();
        }

        if (msg == null)
            block();
    }

    private void handleMessage(ACLMessage msg) {
        if (msg.getPerformative() == ACLMessage.AGREE) {
            FruitItemDTO item = JsonUtil.fromJson(msg.getContent(), FruitItemDTO.class);
            if (item != null) {
                if (myAgent.getAssignedWorkId() == null) {
                    System.out.println("🔥 " + myAgent.getLocalName() + " [Fuego]: Assigned Item " + item.getItemId());
                    myAgent.setAssignedWorkId(item.getItemId());
                    myAgent.setAssignedFruitItem(item);
                    myAgent.setAssignedTarget(item.getConveyorName());
                } else {
                    System.out.println("🔥 " + myAgent.getLocalName() + " [Fuego]: Received extra item, ignoring.");
                }
            }
        }
    }

    private void sendRequest() {
        try {
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.setType(ServerConfig.CONVEYOR_YELLOW_PAGES_SERVICE);
            template.addServices(sd);

            DFAgentDescription[] result = DFService.search(myAgent, template);
            if (result.length > 0) {
                AID conveyor = result[random.nextInt(result.length)].getName();
                ACLMessage req = new ACLMessage(ACLMessage.REQUEST);
                req.addReceiver(conveyor);
                req.setOntology(MessagingConstants.ONTOLOGY_ITEM_REQUEST);
                req.setConversationId(MessagingConstants.CONVERSATION_ITEM_REQUEST);
                req.setContent("RequestItem");
                myAgent.send(req);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
