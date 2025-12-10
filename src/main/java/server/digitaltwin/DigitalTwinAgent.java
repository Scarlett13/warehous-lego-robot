package server.digitaltwin;

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
import shared.dto.RobotStatusDTO;
import shared.messaging.MessagingConstants;
import shared.utils.CommonPid;

import static robot.RobotConstants.*;

public class DigitalTwinAgent extends Agent {

    private String robotName;
    private static final CommonPid ultrasonicPid                   = new CommonPid(ULTRASONIC_P, ULTRASONIC_I, ULTRASONIC_D);
    private static final CommonPid localisationPid                   = new CommonPid(LOCALISATION_P, LOCALISATION_I, LOCALISATION_D);

    private volatile RobotStatusDTO  robotStatus = new  RobotStatusDTO();
    private volatile DistancePidResultDTO lastUltrasonicPidResult = new DistancePidResultDTO(0, 0, false, false);
    private volatile DistancePidResultDTO lastLocalisationPidResult = new DistancePidResultDTO(0, 0, false, false);

    @Override
    protected void setup() {
        Object[] args = getArguments();
        robotName = (args != null && args.length > 0) ? String.valueOf(args[0]) : "UNKNOWN";
        instantiatePid();
        registerInYellowPages();
        SimpleNamespace namespace = OPCUAServer.getNamespace();

        try {
            TopicManagementHelper tmh =
                    (TopicManagementHelper) getHelper(TopicManagementHelper.SERVICE_NAME);

            AID topic = tmh.createTopic(MessagingConstants.ROBOT_STATE_TOPICS);
            tmh.register(topic);
            namespace.registerRobot(robotName, robotStatus);

            System.out.println("[DT] registered topic=" + topic.getName()
                    + " robot=" + robotName);
        } catch (Exception e) {
            e.printStackTrace();
        }

        addBehaviour(new DigitalTwinCommandBehaviour(this, this, robotName));
    }

    public String getRobotName() {
        return robotName;
    }

    public void instantiatePid(){
        ultrasonicPid.setOutputLimits(200);
        ultrasonicPid.setSetpoint(0);
        ultrasonicPid.setSetpoint(SPEED_MAX);
        ultrasonicPid.setSetpointRange(SPEED_MIN);

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

    public CommonPid getUltrasonicPid() {
        return ultrasonicPid;
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

    public DistancePidResultDTO getLastUltrasonicPidResult() {
        return lastUltrasonicPidResult;
    }

    public void setLastUltrasonicPidResult(DistancePidResultDTO lastUltrasonicPidResult) {
        this.lastUltrasonicPidResult = lastUltrasonicPidResult;
    }

    public DistancePidResultDTO getLastLocalisationPidResult() {
        return lastLocalisationPidResult;
    }

    public void setLastLocalisationPidResult(DistancePidResultDTO lastLocalisationPidResult) {
        this.lastLocalisationPidResult = lastLocalisationPidResult;
    }
}

