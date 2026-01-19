package robot.behaviours;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import robot.RobotContext;
import robot.hardware.MotorHardware;
import shared.dto.RobotCommandDTO;
import shared.dto.old.PositionDTO;
import shared.messaging.MessagingConstants;
import shared.messaging.TopicHelper;
import shared.utils.JsonUtil;

import static robot.RobotConstants.IS_SIMS;
import static robot.RobotConstants.ROBOT_NAME;

public class RobotCommandReceiverBehaviour extends CyclicBehaviour {
    private final MessageTemplate mt;
    private final RobotContext ctx;
    private long prevTimestamp = System.currentTimeMillis();

    public RobotCommandReceiverBehaviour(Agent a, RobotContext ctx) {
        super(a);
        this.ctx = ctx;

        AID topic = TopicHelper.topic(a, MessagingConstants.ROBOT_COMMAND_TOPICS);
        TopicHelper.subscribe(a, topic);

        MessageTemplate t = MessageTemplate.MatchTopic(topic);
        t = MessageTemplate.and(t, MessageTemplate.MatchOntology(MessagingConstants.ONTOLOGY));
        t = MessageTemplate.and(t, MessageTemplate.MatchLanguage("json"));
        t = MessageTemplate.and(t, MessageTemplate.MatchConversationId(ROBOT_NAME));

        this.mt = t;
    }

    @Override
    public void action() {
        // System.out.println("action - command behaviour");
        ACLMessage msg = this.getAgent().receive(mt);
        if (msg == null || msg.getContent().isEmpty()) {
            block();
            return;
        }
        System.out.println("message received: " + msg.getContent());

        RobotCommandDTO newCommand = JsonUtil.fromJson(msg.getContent(), RobotCommandDTO.class);
        if (newCommand == null ||
                newCommand.getRobotName() == null ||
                newCommand.getRobotName().isEmpty() ||
                !newCommand.getRobotName().equals(ROBOT_NAME)) {
            block();
            return;
        }

        long currentTimestamp = System.currentTimeMillis();
        long dt = currentTimestamp - prevTimestamp;

        // TODO: apply the variables to the robot
        // * 1. apply motor command
        // * 2. set new context
        // *

        // if (!IS_SIMS)
        // MotorHardware.applyCommand(dt, newCommand.getTargetMotorSpeed(),
        // newCommand.getTurnCorrection());

        // finally, set new context
        ctx.setState(newCommand.getRobotState());
        ctx.setWorkId(newCommand.getItemId());
        ctx.setTargetSpeed(newCommand.getTargetMotorSpeed());
        ctx.setTurnCorrection(newCommand.getTurnCorrection());
        ctx.setTargetX(newCommand.getTargetX());
        ctx.setTargetY(newCommand.getTargetY());
        prevTimestamp = currentTimestamp;

    }
}
