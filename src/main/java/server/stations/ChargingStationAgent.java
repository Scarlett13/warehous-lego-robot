package server.stations;

import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.MessageTemplate;

public class ChargingStationAgent extends Agent {
    private boolean isOccupied = false;
    private String currentOccupant = "";

    @Override
    protected void setup() {
        // Register in Yellow Pages
        try {
            DFAgentDescription dfd = new DFAgentDescription();
            dfd.setName(getAID());
            ServiceDescription sd = new ServiceDescription();
            sd.setType("warehouse-charging-station");
            sd.setName(getLocalName());
            dfd.addServices(sd);
            DFService.register(this, dfd);
        } catch (FIPAException e) {
            e.printStackTrace();
        }

        System.out.println("Charging Station " + getLocalName() + " is ready.");

        // Behaviour to handle Booking Requests
        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
                ACLMessage msg = myAgent.receive(mt);
                if (msg != null) {
                    ACLMessage reply = msg.createReply();
                    if (!isOccupied) {
                        isOccupied = true;
                        currentOccupant = msg.getSender().getLocalName();
                        reply.setPerformative(ACLMessage.AGREE);
                        reply.setContent("OK");
                        System.out.println(getLocalName() + ": Booking confirmed for " + currentOccupant);
                    } else {
                        reply.setPerformative(ACLMessage.REFUSE);
                        reply.setContent("Occupied by " + currentOccupant);
                        System.out.println(getLocalName() + ": Refused booking for " + msg.getSender().getLocalName()
                                + " (Occupied)");
                    }
                    myAgent.send(reply);
                } else {
                    block();
                }
            }
        });

        // Behaviour to handle Release (Leaving)
        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
                ACLMessage msg = myAgent.receive(mt);
                if (msg != null && msg.getContent().equalsIgnoreCase("LEAVING")) {
                    if (isOccupied && currentOccupant.equals(msg.getSender().getLocalName())) {
                        isOccupied = false;
                        currentOccupant = "";
                        System.out.println(getLocalName() + ": Released by " + msg.getSender().getLocalName());
                    }
                } else {
                    block();
                }
            }
        });
    }

    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (FIPAException e) {
            e.printStackTrace();
        }
    }
}
