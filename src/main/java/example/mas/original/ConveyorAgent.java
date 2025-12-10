package example.mas.original;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;


public class ConveyorAgent extends Agent {
    
    private String conveyorName;
    private boolean produced;
    private boolean previousProduced;
    private boolean productAssigned;
    private boolean lastNotificationSent;  // Track if we already notified for current product
    private static int globalRobotIndex = 0;
    
    @Override
    protected void setup() {
        // Get conveyor name from agent arguments
        Object[] args = getArguments();
        if (args != null && args.length > 0) {
            conveyorName = (String) args[0];
        } else {
            conveyorName = getLocalName();
        }
        
        // Initialize
        produced = false;
        previousProduced = false;
        productAssigned = false;
        lastNotificationSent = false;
        
        System.out.println(" Conveyor Agent " + conveyorName + " is ready!");
        
        // Register in Yellow Pages
        registerInYellowPages();
        
        // Start main behavior (runs every second)
        addBehaviour(new TickerBehaviour(this, Config.UPDATE_INTERVAL) {
            @Override
            protected void onTick() {
                tick();
            }
        });
    }
    
    /**
     * Main conveyor logic - runs every tick
     */
    private void tick() {
        // Read production status from OPC-UA (updated by VC)
        readStatusFromOPCUA();
        
        // Notify robots periodically if product is ready and not picked up yet
        if (produced) {
            notifyRobots();  // Keep notifying until someone picks it up
        }
    }
    
    private void readStatusFromOPCUA() {
        Boolean status = SimpleNamespace.getConveyorProduced(conveyorName);
        if (status != null) {
            produced = status;
        }
    }
    
    private void notifyRobots() {
        double[] coords = Config.CONVEYOR_LOCATIONS.get(conveyorName);
        if (coords == null) return;
        
        String coordString = String.format(java.util.Locale.US, "%.3f;%.3f;%.3f", coords[0], coords[1], coords[2]);
        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        msg.setContent("PRODUCT_READY:" + conveyorName + ":" + coordString);
        msg.setConversationId("product-pickup");
        
        // Send to all robots - first available one will take it
        for (String robotName : Config.ROBOT_NAMES) {
            msg.addReceiver(new AID(robotName, AID.ISLOCALNAME));
        }
        
        send(msg);

        System.out.println(" " + conveyorName + " notifying robots for pickup");
    }
    
    /**
     * Register this conveyor in JADE Yellow Pages
     */
    private void registerInYellowPages() {
        try {
            DFAgentDescription dfd = new DFAgentDescription();
            dfd.setName(getAID());
            
            ServiceDescription sd = new ServiceDescription();
            sd.setType("warehouse-conveyor");
            sd.setName(conveyorName);
            
            dfd.addServices(sd);
            DFService.register(this, dfd);
            
            System.out.println(" " + conveyorName + " registered in Yellow Pages");
        } catch (FIPAException e) {
            e.printStackTrace();
        }
    }
    
    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
            System.out.println(" " + conveyorName + " deregistered from Yellow Pages");
        } catch (FIPAException e) {
            e.printStackTrace();
        }
    }
}
