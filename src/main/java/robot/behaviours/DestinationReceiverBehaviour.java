package robot.behaviours;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import robot.RobotContext;
import robot.utils.RobotState;
import shared.dto.old.DestinationDTO;
import shared.messaging.MessagingConstants;
import shared.messaging.TopicHelper;
import shared.utils.JsonUtil;

public class DestinationReceiverBehaviour extends CyclicBehaviour {
    private final AID topic;
    private final MessageTemplate mt;
    private final RobotContext ctx;

    public DestinationReceiverBehaviour(Agent a, RobotContext ctx) {
        super(a);
        this.ctx = ctx;

        this.topic = TopicHelper.topic(a, MessagingConstants.ROBOT_EVENTS_TOPIC);

        MessageTemplate t = MessageTemplate.MatchTopic(topic);
        t = MessageTemplate.and(t, MessageTemplate.MatchOntology(MessagingConstants.ONTOLOGY));
        t = MessageTemplate.and(t, MessageTemplate.MatchLanguage("json"));
        t = MessageTemplate.and(t, MessageTemplate.MatchConversationId(MessagingConstants.ROBOT_TARGET_DESTINATION));
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
            ctx.setLastDestination(contentmessage);
            ctx.setState(RobotState.WORKING);
        } else {
            block();
        }
    }
}