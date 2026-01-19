package server.digitaltwin.fuego;

import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import server.ServerConfig;
import server.opcua.OPCUAServer;
import server.opcua.SimpleNamespace;
import shared.dto.FruitItemDTO;

public class FuegoDTAgent extends Agent {
    private String robotName;

    // Assigned Work
    private String assignedWorkId;
    private String assignedTarget;
    private FruitItemDTO assignedFruitItem;

    private FuegoOpcUaClient opcUaClient;

    @Override
    protected void setup() {
        Object[] args = getArguments();
        robotName = (args != null && args.length > 0) ? String.valueOf(args[0]) : "Robot1";

        // Initialize OPC UA Client
        opcUaClient = new FuegoOpcUaClient();
        opcUaClient.connect();

        registerInYellowPages();
        SimpleNamespace namespace = OPCUAServer.getNamespace();

        try {
//            namespace.registerFuegoRobot(robotName, "Robot1");
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to register Fuego Robot in OPC UA");
        }

        addBehaviour(new FuegoDTCommandBehaviour(this, 1000, this, robotName));
        addBehaviour(new FuegoItemNegotiationBehaviour(this));
    }

    public FuegoOpcUaClient getOpcUaClient() {
        return opcUaClient;
    }

    private void registerInYellowPages() {
        try {
            DFAgentDescription dfd = new DFAgentDescription();
            dfd.setName(getAID());
            ServiceDescription sd = new ServiceDescription();
            sd.setType(ServerConfig.ROBOT_YELLOW_PAGES_SERVICE);
            sd.setName(robotName);
            dfd.addServices(sd);
            DFService.register(this, dfd);
            System.out.println("🔥 " + robotName + " (Fuego) registered in Yellow Pages");
        } catch (FIPAException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
            System.out.println("🔥 " + robotName + " (Fuego) deregistered");
        } catch (FIPAException e) {
            e.printStackTrace();
        }
    }

    // Getters/Setters
    public String getRobotName() {
        return robotName;
    }

    public String getAssignedWorkId() {
        return assignedWorkId;
    }

    public void setAssignedWorkId(String assignedWorkId) {
        this.assignedWorkId = assignedWorkId;
    }

    public String getAssignedTarget() {
        return assignedTarget;
    }

    public void setAssignedTarget(String assignedTarget) {
        this.assignedTarget = assignedTarget;
    }

    public FruitItemDTO getAssignedFruitItem() {
        return assignedFruitItem;
    }

    public void setAssignedFruitItem(FruitItemDTO assignedFruitItem) {
        this.assignedFruitItem = assignedFruitItem;
    }
}
