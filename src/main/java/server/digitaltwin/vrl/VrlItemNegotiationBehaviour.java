package server.digitaltwin.vrl;

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

public class VrlItemNegotiationBehaviour extends CyclicBehaviour {
    private final VrlDTAgent myAgent;
    private long lastCheck = 0;
    private static final long CHECK_INTERVAL = 1000;
    private final MessageTemplate mt;
    private final Random random = new Random();

    public VrlItemNegotiationBehaviour(VrlDTAgent agent) {
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
            // Check State - VRL Agent uses String "lastState" or similar?
            // We need to check if it represents "STANDBY".
            // VrlDTAgent has `String lastState`.

            String state = myAgent.getLastState();
            String currentWork = myAgent.getAssignedWorkId();

            // Assume "STANDBY" string or similar
            boolean isAbleToWork = (state != null && (state.equals("STANDBY") || state.equals("BACK_TO_STATION")));
            // Battery check? VRL Agent currently doesn't track battery in same way,
            // but we can assume infinite or check if we add it.
            // For now, assume good battery if able to work.

            if (isAbleToWork && (currentWork == null || currentWork.isEmpty())) {
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
                    System.out.println(myAgent.getLocalName() + " [VRL]: Assigned Item " + item.getItemId());
                    myAgent.setAssignedWorkId(item.getItemId());
                    myAgent.setAssignedFruitItem(item);
                    myAgent.setAssignedTarget(item.getConveyorName());
                } else {
                    System.out.println(myAgent.getLocalName() + " [VRL]: Received extra item, ignoring.");
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
