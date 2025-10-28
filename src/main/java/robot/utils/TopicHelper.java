package robot.utils;

import jade.core.AID;
import jade.core.Agent;
import jade.core.messaging.TopicManagementHelper;

public final class TopicHelper {
    private TopicHelper() {}

    public static AID topic(Agent a, String name) {
        try {
            TopicManagementHelper helper = (TopicManagementHelper) a.getHelper(TopicManagementHelper.SERVICE_NAME);
            return helper.createTopic(name);
        } catch (Exception e) {
            throw new RuntimeException("Topic creation failed for: " + name + "- " + e);
        }
    }

    public static void subscribe(Agent a, AID topic) {
        try {
            TopicManagementHelper helper = (TopicManagementHelper) a.getHelper(TopicManagementHelper.SERVICE_NAME);
            helper.register(topic);
        } catch (Exception e) {
            throw new RuntimeException("Topic subscribe failed: " + topic.getLocalName(), e);
        }
    }
}