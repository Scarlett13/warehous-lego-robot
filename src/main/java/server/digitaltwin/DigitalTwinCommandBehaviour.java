package server.digitaltwin;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import robot.utils.RobotState;
import server.control.UltrasonicPidControl;
import shared.DistancePidResultDTO;
import shared.dto.RobotStatusDTO;
import shared.messaging.MessagingConstants;
import shared.messaging.TopicHelper;
import shared.utils.JsonUtil;

public class DigitalTwinCommandBehaviour extends CyclicBehaviour {
    private final MessageTemplate mt;
    private final DigitalTwinAgent agentClass;

    public DigitalTwinCommandBehaviour(Agent a, DigitalTwinAgent agentClass, String robotName) {
        super(a);
        this.agentClass = agentClass;
        AID topic = TopicHelper.topic(a, MessagingConstants.ROBOT_STATE_TOPICS);

        System.out.println("starting behaviour receiver for topic "+topic+" and robot "+robotName);

        MessageTemplate t = MessageTemplate.MatchTopic(topic);
        t = MessageTemplate.and(t, MessageTemplate.MatchOntology(MessagingConstants.ONTOLOGY));
        t = MessageTemplate.and(t, MessageTemplate.MatchLanguage("json"));
        t = MessageTemplate.and(t, MessageTemplate.MatchConversationId(robotName));

        this.mt = t;
    }

    @Override
    public void action() {
        ACLMessage msg = this.getAgent().receive(mt);

        if (msg == null || msg.getContent().isEmpty()) {
            block();
            return;
        }

        RobotStatusDTO newStatus = JsonUtil.fromJson(msg.getContent(), RobotStatusDTO.class);
        RobotStatusDTO oldStatus = agentClass.getRobotStatus();

        long currentTimestamp = newStatus.getTimestampMillis();
        long oldTimestamp = oldStatus.getTimestampMillis();
        long dt =  currentTimestamp - oldTimestamp;

        if(newStatus.getRobotState().equals(RobotState.STANDBY) || newStatus.getRobotState().equals(RobotState.CHARGING)){

        }

        DistancePidResultDTO lastUltrasonicPidResult = agentClass.getLastUltrasonicPidResult();

        DistancePidResultDTO newUltrasonicPidResult = UltrasonicPidControl.computeControl(
                currentTimestamp,
                newStatus.getUltrasonicReading().getDistance(),
                lastUltrasonicPidResult.getForwardTarget(),
                lastUltrasonicPidResult.isHalted(),
                agentClass.getUltrasonicPid(),
                dt);

        /*
        * TODO:
        *  1. create context class for storing ultrasonicpid, pozyxpid, yaw pid, in the agent
        *  2. look at conveyors items
        *  3. if robot current work id is not null or robot current state is idle:
        *  3.1. calculate pid speed from both ultrasonic and pozyx
        *  3.2. do the path calculation algorithms
        *  3.3. determine the speed, turn correction, workid, robot state
        *  4. send the data back to the robot
        * */

        System.out.println(" Received message from " + msg.getSender().getLocalName() + ": " + newStatus);
        agentClass.setRobotStatus(newStatus);
        agentClass.setLastUltrasonicPidResult(newUltrasonicPidResult);
    }
}
