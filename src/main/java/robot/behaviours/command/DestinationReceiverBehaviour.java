package robot.behaviours.command;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import robot.Constants;
import robot.RobotContext;
import robot.dto.DestinationDTO;
import robot.utils.JsonUtil;
import robot.utils.RobotState;
import robot.utils.TopicHelper;

public class DestinationReceiverBehaviour extends CyclicBehaviour {
    private final AID topic;
    private final MessageTemplate mt;
    private final RobotContext ctx;

    public DestinationReceiverBehaviour(Agent a, RobotContext ctx) {
        super(a);
        this.ctx = ctx;

        this.topic = TopicHelper.topic(a, Constants.DESTINATION_TOPIC);

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
        System.out.println(msg == null ? "sininull" : msg.getContent());
//        System.out.println(topic.toString());

        if (msg != null && ctx.getState() != RobotState.WORKING) {
            DestinationDTO contentmessage = JsonUtil.fromJson(msg.getContent(), DestinationDTO.class);
            ctx.setDestination(contentmessage);
            ctx.setState(RobotState.WORKING);
        } else {
            block();
        }
    }
}
