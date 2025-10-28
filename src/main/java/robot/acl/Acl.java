package robot.acl;

import jade.core.AID;
import jade.core.Agent;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public final class Acl {
    private Acl() {}

    public static void publish(Agent a, AID topic, String json) {
        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        msg.addReceiver(topic);
        msg.setOntology("teletubbies-agent");
        msg.setLanguage("json");
        msg.setContent(json);
        a.send(msg);
    }

    public static MessageTemplate topicTemplate(AID topic) {
        return MessageTemplate.MatchTopic(topic);
    }

    public static MessageTemplate and(MessageTemplate... ts) {
        MessageTemplate m = ts[0];
        for (int i=1;i<ts.length;i++) m = MessageTemplate.and(m, ts[i]);
        return m;
    }
}
