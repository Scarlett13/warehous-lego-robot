package server.digitaltwin.teletubbies;

import jade.core.AID;
import jade.core.Agent;

import jade.core.messaging.TopicManagementHelper;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import server.ServerConfig;
import server.opcua.OPCUAServer;
import server.opcua.SimpleNamespace;
import shared.DistancePidResultDTO;
import shared.dto.FruitItemDTO;
import shared.dto.Point2D;
import shared.dto.RobotStatusDTO;
import shared.messaging.MessagingConstants;
import shared.utils.CommonPid;

import static robot.RobotConstants.*;

public class TeletubbiesDTAgent extends Agent {

    private String robotName;
    private static final CommonPid localisationPid = new CommonPid(LOCALISATION_P, LOCALISATION_I, LOCALISATION_D);

    private volatile RobotStatusDTO robotStatus = new RobotStatusDTO();
    private volatile DistancePidResultDTO lastLocalisationPidResult = new DistancePidResultDTO(0, 0, false, false);

    // Assigned Work from Conveyor
    private String assignedWorkId;
    private String assignedTarget;
    private FruitItemDTO assignedFruitItem;

    @Override
    protected void setup() {
        Object[] args = getArguments();
        robotName = (args != null && args.length > 0) ? String.valueOf(args[0]) : "UNKNOWN";
        instantiatePid();
        registerInYellowPages();
        SimpleNamespace namespace = OPCUAServer.getNamespace();

        try {
            TopicManagementHelper tmh = (TopicManagementHelper) getHelper(TopicManagementHelper.SERVICE_NAME);

            AID topic = tmh.createTopic(MessagingConstants.ROBOT_STATE_TOPICS);
            tmh.register(topic);
            namespace.registerRobot(robotName, robotStatus);

            System.out.println("[DT] registered topic=" + topic.getName()
                    + " robot=" + robotName);
        } catch (Exception e) {
            e.printStackTrace();
        }

        addBehaviour(new TeletubbiesDTCommandBehaviour(this, this, robotName));
        addBehaviour(new RobotItemNegotiationBehaviour(this));
        addBehaviour(new server.digitaltwin.RobotPathPlanning(this, 1000, robotName)); // Check every 1s
    }

    public String getRobotName() {
        return robotName;
    }

    public void instantiatePid() {
        localisationPid.setOutputLimits(200);
        localisationPid.setSetpoint(0);
        localisationPid.setSetpoint(SPEED_MAX);
        localisationPid.setSetpointRange(SPEED_MIN);
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

    public static CommonPid getLocalisationPid() {
        return localisationPid;
    }

    public RobotStatusDTO getRobotStatus() {
        return robotStatus;
    }

    public void setRobotStatus(RobotStatusDTO robotStatus) {
        this.robotStatus = robotStatus;
    }

    public DistancePidResultDTO getLastLocalisationPidResult() {
        return lastLocalisationPidResult;
    }

    public void setLastLocalisationPidResult(DistancePidResultDTO lastLocalisationPidResult) {
        this.lastLocalisationPidResult = lastLocalisationPidResult;
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
