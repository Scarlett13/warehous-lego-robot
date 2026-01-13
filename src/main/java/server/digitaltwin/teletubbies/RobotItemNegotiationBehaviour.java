package server.digitaltwin.teletubbies;

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
import robot.utils.RobotState;

import shared.SharedConstants;

import java.util.Random;

public class RobotItemNegotiationBehaviour extends CyclicBehaviour {
    private final TeletubbiesDTAgent myAgent;
    private long lastCheck = 0;
    private static final long CHECK_INTERVAL = 1000; // Check more frequently
    private final MessageTemplate mt;
    private final Random random = new Random();

    public RobotItemNegotiationBehaviour(TeletubbiesDTAgent agent) {
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

        // 2. Periodic Request (Only if idle and no work assigned)
        if (System.currentTimeMillis() - lastCheck > CHECK_INTERVAL) {
            RobotState state = myAgent.getRobotStatus().getRobotState();
            String currentWork = myAgent.getRobotStatus().getCurrentWorkId();
            String assignedWork = myAgent.getAssignedWorkId();
            double battery = myAgent.getRobotStatus().getBatteryPct();

            boolean isAbleToWork = (state == RobotState.STANDBY || state == RobotState.BACK_TO_STATION);
            boolean hasGoodBattery = battery > SharedConstants.BATTERY_CHARGE_THRESHOLD;

            if (isAbleToWork && hasGoodBattery &&
                    (currentWork == null || currentWork.isEmpty()) &&
                    (assignedWork == null || assignedWork.isEmpty())) {

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
                // Check if we already got an assignment in the meantime
                if (myAgent.getAssignedWorkId() == null) {
                    System.out.println(myAgent.getLocalName() + ": Assigned Item " + item.getItemId() + " from "
                            + msg.getSender().getLocalName());
                    myAgent.setAssignedWorkId(item.getItemId());
                    myAgent.setAssignedFruitItem(item);
                    // Target is the conveyor that sent it (or item.conveyorName)
                    myAgent.setAssignedTarget(item.getConveyorName());
                } else {
                    System.out.println(myAgent.getLocalName() + ": Received extra item assignment " + item.getItemId()
                            + ", ignoring/warning.");
                    // Ideally send cancel/reject back if we care about zombie items
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
                // Pick ONE random conveyor to avoid race conditions/multiple assignments
                AID conveyor = result[random.nextInt(result.length)].getName();

                ACLMessage req = new ACLMessage(ACLMessage.REQUEST);
                req.addReceiver(conveyor);
                req.setOntology(MessagingConstants.ONTOLOGY_ITEM_REQUEST);
                req.setConversationId(MessagingConstants.CONVERSATION_ITEM_REQUEST);
                req.setContent("RequestItem"); // Content irrelevant
                myAgent.send(req);
                System.out.println(myAgent.getLocalName() + ": Requesting item from " + conveyor.getLocalName());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
