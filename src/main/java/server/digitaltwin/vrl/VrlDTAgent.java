package server.digitaltwin.vrl;

import jade.core.AID;
import jade.core.Agent;
import jade.core.messaging.TopicManagementHelper;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import server.ServerConfig;
import server.digitaltwin.teletubbies.RobotItemNegotiationBehaviour;
import server.digitaltwin.teletubbies.TeletubbiesDTCommandBehaviour;
import server.opcua.OPCUAServer;
import server.opcua.SimpleNamespace;
import shared.DistancePidResultDTO;
import shared.dto.FruitItemDTO;
import shared.dto.RobotStatusDTO;
import shared.messaging.MessagingConstants;
import shared.utils.CommonPid;

import static robot.RobotConstants.*;
import static robot.RobotConstants.SPEED_MAX;
import static robot.RobotConstants.SPEED_MIN;

public class VrlDTAgent extends Agent {
    private String robotName;

    private int lastX = -1;
    private int lastY = -1;
    private String lastState = "STANDBY";

    // Assigned Work from Conveyor
    private String assignedWorkId;
    private String assignedTarget;
    private FruitItemDTO assignedFruitItem;

    @Override
    protected void setup() {
        Object[] args = getArguments();
        robotName = (args != null && args.length > 0) ? String.valueOf(args[0]) : "UNKNOWN";
        // instantiatePid();
        registerInYellowPages();
        SimpleNamespace namespace = OPCUAServer.getNamespace();

        try {
            RobotStatusDTO status = new RobotStatusDTO();
            status.setBatteryPct(100.0);
            status.setRobotState(robot.utils.RobotState.STANDBY);
            namespace.registerRobot(robotName, status);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        addBehaviour(new VrlDTCommandBehaviour(this, this, robotName));
        addBehaviour(new VrlItemNegotiationBehaviour(this));
    }

    public String getRobotName() {
        return robotName;
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

            System.out.println(" " + robotName + " registered in Yellow Pages");
        } catch (FIPAException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
            System.out.println(" " + robotName + " deregistered from Yellow Pages");
        } catch (FIPAException e) {
            e.printStackTrace();
        }
    }

    public void setRobotName(String robotName) {
        this.robotName = robotName;
    }

    public int getLastX() {
        return lastX;
    }

    public void setLastX(int lastX) {
        this.lastX = lastX;
    }

    public int getLastY() {
        return lastY;
    }

    public void setLastY(int lastY) {
        this.lastY = lastY;
    }

    public String getLastState() {
        return lastState;
    }

    public void setLastState(String lastState) {
        this.lastState = lastState;
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
