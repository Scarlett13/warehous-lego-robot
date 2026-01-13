package server.ui;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import shared.messaging.MessagingConstants;
import shared.messaging.TopicHelper;

public class MonitorInterfaceBehaviour extends CyclicBehaviour {
    private final AID topic;
    private final MessageTemplate mt;
    private final MonitorUI ui;

    public MonitorInterfaceBehaviour(Agent a, MonitorUI ui) {
        super(a);
        this.ui = ui;
        this.topic = TopicHelper.topic(a, MessagingConstants.UI_TOPICS);

        MessageTemplate t = MessageTemplate.MatchTopic(topic);
        t = MessageTemplate.and(t, MessageTemplate.MatchOntology(MessagingConstants.UI_ONTOLOGY));
//        t = MessageTemplate.and(t, MessageTemplate.MatchLanguage("string"));
//        t = MessageTemplate.and(t, MessageTemplate.MatchConversationId(conveyorAgent.getConveyorData().getConveyorName()));

        this.mt = t;
    }

    @Override
    public void onStart() {
        TopicHelper.subscribe(this.getAgent(), topic);
    }

    @Override
    public void action() {
        ACLMessage msg = this.getAgent().receive(mt);
        if (msg == null || msg.getContent().isEmpty()) {
            block();
            return;
        }

        String command = msg.getContent();
        String agentName = msg.getConversationId();

        //this is command strictly that coming from the UI only
        switch (command) {
            case "robot_added":
                ui.addRobotPanel(agentName);
                break;
            case "robot_removed":
                ui.removeRobotPanel(agentName);
                break;
            case "fruit_added":
                break;

        }
    }


}
