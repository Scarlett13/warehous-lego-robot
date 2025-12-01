package server.conveyors;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import shared.dto.FruitItemDTO;
import shared.messaging.MessagingConstants;
import shared.messaging.TopicHelper;
import shared.utils.JsonUtil;

import java.util.Comparator;

public class ConveyorItemAddedBehaviour extends CyclicBehaviour {
    private ConveyorAgent conveyorAgent;
    private final AID topic;
    private final MessageTemplate mt;

    public ConveyorItemAddedBehaviour(Agent a) {
        this.conveyorAgent = (ConveyorAgent) a;

        this.topic = TopicHelper.topic(a, MessagingConstants.CONVEYOR_EVENTS_TOPIC);

        MessageTemplate t = MessageTemplate.MatchTopic(topic);
        t = MessageTemplate.and(t, MessageTemplate.MatchOntology(MessagingConstants.ONTOLOGY));
        t = MessageTemplate.and(t, MessageTemplate.MatchLanguage("json"));
        t = MessageTemplate.and(t, MessageTemplate.MatchConversationId(MessagingConstants.CONVEYOR_ITEMS_ADDED));

        this.mt = t;
    }

    @Override
    public void onStart() {
        TopicHelper.subscribe(conveyorAgent, topic);
    }

    @Override
    public void action() {
        ACLMessage msg = conveyorAgent.receive(mt);
        if (msg == null || msg.getContent().isEmpty()) {
            block();
            return;
        }

        System.out.println(msg.getContent());

        FruitItemDTO newFruit = JsonUtil.fromJson(msg.getContent(), FruitItemDTO.class);

        if(newFruit.getConveyorName().equals(conveyorAgent.getName())) {
            conveyorAgent.getConveyorData().addFruitItem(newFruit);
            setPriority();
            //TODO: only add the first index

        }

    }

    private void setPriority() {
        conveyorAgent.getConveyorData().getConveyorItems().sort(
                Comparator
                        // 4 and below means rotten, set priority to 1
                        // 5 - 10 freshness means not rotten, set priority to 0
                        .comparingInt((FruitItemDTO f) -> f.getFreshness() <= 4 ? 1 : 0)

                        // for each group of 0 and 1 from previous comparator,
                        // sort the priority in ascending order,
                        // means the freshness priority would be like:
                        // 5,6,7,8,9,10,1,2,3,4
                        .thenComparingInt(FruitItemDTO::getFreshness)

                        // this is just sorting the last delivery time
                        // only compare the time with the same freshness
                        // order it in ascending order
                        .thenComparingLong(f -> {
                            Long t = f.getLastDeliveryMillis();
                            // put nulls last if any
                            return t != null ? t : Long.MAX_VALUE;
                        })
        );
    }
}
