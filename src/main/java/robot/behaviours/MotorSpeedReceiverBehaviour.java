package robot.behaviours;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.MessageTemplate;
import robot.RobotContext;
import robot.utils.RobotState;
import shared.messaging.MessagingConstants;
import shared.messaging.TopicHelper;

public class MotorSpeedReceiverBehaviour extends CyclicBehaviour {
    private final RobotContext ctx;

    private final AID topic;
    private final MessageTemplate mt;

    public MotorSpeedReceiverBehaviour(Agent a, RobotContext ctx) {
        super(a);
        this.ctx = ctx;

        this.topic = TopicHelper.topic(a, MessagingConstants.ROBOT_DESTINATION_TOPIC);

        MessageTemplate t = MessageTemplate.MatchTopic(topic);
        t = MessageTemplate.and(t, MessageTemplate.MatchOntology("teletubbies-agent"));
        t = MessageTemplate.and(t, MessageTemplate.MatchLanguage("json"));
        this.mt = t;
    }

    @Override
    public void onStart() {
        TopicHelper.subscribe(myAgent, topic);
    }

    @Override
    public void action() {
        if (ctx.getState() == RobotState.STANDBY) {
            return;
        }


    }
}
