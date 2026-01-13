package server.digitaltwin.teletubbies;

import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import server.ServerConfig;
import robot.utils.RobotState;

import java.util.Random;

public class RobotChargingNegotiationBehaviour extends CyclicBehaviour {
    private final TeletubbiesDTAgent myAgent;
    private long lastCheck = 0;
    private static final long CHECK_INTERVAL = 2000;
    private final MessageTemplate mt;
    private final Random random = new Random();
    private boolean isWaitingForReply = false;

    public RobotChargingNegotiationBehaviour(TeletubbiesDTAgent agent) {
        super(agent);
        this.myAgent = agent;
        // Listen for replies to our charging requests
        this.mt = MessageTemplate.MatchPerformative(ACLMessage.AGREE);
        // We also need to handle REFUSE, so we should broaden logic or handle
        // individually
    }

    @Override
    public void action() {
        RobotState state = myAgent.getRobotStatus().getRobotState();
        String currentTarget = myAgent.getAssignedTarget();

        // 1. Check if we need to find a station
        // Logic: If going to charge, but don't have a valid target set yet
        boolean needsStation = (state == RobotState.GOING_TO_CHARGE) &&
                (currentTarget == null || currentTarget.isEmpty() || currentTarget.equals("Charging Station"));
        // Note: "Charging Station" general name is what we want to replace with
        // specific "ChargingStationN"

        if (needsStation && !isWaitingForReply && (System.currentTimeMillis() - lastCheck > CHECK_INTERVAL)) {
            findAndRequestStation();
            lastCheck = System.currentTimeMillis();
        }

        // 2. Handle Replies (AGREE / REFUSE)
        ACLMessage msg = myAgent.receive();
        if (msg != null) {
            if (msg.getPerformative() == ACLMessage.AGREE) {
                handleAgree(msg);
            } else if (msg.getPerformative() == ACLMessage.REFUSE) {
                handleRefuse(msg);
            }
        } else {
            if (!needsStation) {
                block();
            }
        }
    }

    private void findAndRequestStation() {
        try {
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.setType("warehouse-charging-station");
            template.addServices(sd);

            DFAgentDescription[] result = DFService.search(myAgent, template);
            if (result.length > 0) {
                // Pick random to distribute load
                AID station = result[random.nextInt(result.length)].getName();

                ACLMessage req = new ACLMessage(ACLMessage.REQUEST);
                req.addReceiver(station);
                req.setContent("BOOKING");
                myAgent.send(req);
                System.out.println(myAgent.getLocalName() + ": Requesting charging at " + station.getLocalName());
                isWaitingForReply = true;
            } else {
                System.out.println(myAgent.getLocalName() + ": No charging stations found in DF!");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleAgree(ACLMessage msg) {
        if (isWaitingForReply) {
            String stationName = msg.getSender().getLocalName();
            System.out.println(myAgent.getLocalName() + ": Booking CONFIRMED at " + stationName);
            myAgent.setAssignedTarget(stationName); // Set specific target
            isWaitingForReply = false;
        }
    }

    private void handleRefuse(ACLMessage msg) {
        if (isWaitingForReply) {
            System.out.println(myAgent.getLocalName() + ": Booking REFUSED by " + msg.getSender().getLocalName());
            isWaitingForReply = false;
            // Will retry on next interval
        }
    }
}
