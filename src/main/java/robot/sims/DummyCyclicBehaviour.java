package robot.sims;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import robot.RobotContext;
import shared.dto.DistanceDTO;
import shared.messaging.MessagingConstants;
import shared.utils.JsonUtil;
import shared.messaging.TopicHelper;

public class DummyCyclicBehaviour extends CyclicBehaviour {
    private final AID topic;
    private final MessageTemplate mt;
    private final RobotContext ctx;

    public DummyCyclicBehaviour(Agent a, RobotContext ctx) {
        super(a);
        this.ctx = ctx;
        this.topic = TopicHelper.topic(a, MessagingConstants.ROBOT_ULTRASONIC_PID_TOPIC);

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
        ACLMessage msg = myAgent.receive(mt);
        if (msg != null) {
            DistanceDTO contentmessage = JsonUtil.fromJson(msg.getContent(), DistanceDTO.class);
            DistanceDTO latestvalue = ctx.getDistance();
            System.out.println("from message: " + contentmessage);
            System.out.println("latest value: " + latestvalue);
        } else {
            block();
        }
    }
}
