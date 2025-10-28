package robot.sims;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import jade.core.messaging.TopicManagementHelper;
import robot.RobotContext;
import robot.acl.Acl;

public class DummyBehaviour extends TickerBehaviour {
    private final RobotContext ctx;
    private final TopicManagementHelper topicHelper;

    public DummyBehaviour(Agent a, long period, RobotContext ctx, TopicManagementHelper topicHelper) {
        super(a, period);
        this.ctx = ctx;
        this.topicHelper = topicHelper;
    }

    @Override
    protected void onTick() {
        try {
            final AID sensorTopic = topicHelper.createTopic("JADE");
            topicHelper.register(sensorTopic);

            AID topic = topicHelper.createTopic("robot.position");
            Acl.publish(myAgent, topic, "HELLO");
            System.out.println("woiwoiwoi");
        }
        catch (Exception e) {
            e.printStackTrace();
        }

    }

}
