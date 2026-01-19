package server.agentmonitor;

import jade.lang.acl.ACLMessage;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.AMSService;
import jade.domain.FIPAAgentManagement.AMSAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import server.digitaltwin.fuego.FuegoDTAgent;
import server.digitaltwin.teletubbies.TeletubbiesDTAgent;
import server.digitaltwin.vrl.VrlDTAgent;
import shared.messaging.MessagingConstants;
import shared.messaging.TopicHelper;
import shared.messaging.acl.Acl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DigitalTwinAgentManager extends Agent {

    // childLocalName -> twinController
    private final Map<String, AgentController> twins = new HashMap<>();
    private final Set<String> knownChildren = new HashSet<>();

    @Override
    protected void setup() {
        // Prime initial snapshot so you don't create twins for already-running agents
        // (optional)
        knownChildren.addAll(fetchCurrentChildren());

        addBehaviour(new TickerBehaviour(this, 1000) { // 1s tick
            @Override
            protected void onTick() {

                Set<String> current = fetchCurrentChildren();

                // NEW children
                for (String child : current) {
                    if (!knownChildren.contains(child)) {
                        onChildJoined(child);
                    }
                }

                // LEFT children
                for (String child : new HashSet<>(knownChildren)) {
                    if (!current.contains(child)) {
                        onChildLeft(child);
                    }
                }

                knownChildren.clear();
                knownChildren.addAll(current);
            }
        });
    }

    private Set<String> fetchCurrentChildren() {
        Set<String> result = new HashSet<>();
        try {
            SearchConstraints sc = new SearchConstraints();
            sc.setMaxResults(-1L);

            AMSAgentDescription template = new AMSAgentDescription();
            AMSAgentDescription[] res = AMSService.search(this, template, sc);

            for (AMSAgentDescription d : res) {
                AID aid = d.getName();
                if (aid == null)
                    continue;

                String local = aid.getLocalName();

                // Filter out JADE system agents and ALSO avoid treating twins as children
                if (!shouldTrackAsChild(local))
                    continue;

                result.add(local);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    private boolean shouldTrackAsChild(String localName) {
        if (localName == null)
            return false;

        String n = localName.trim();
        if (n.isEmpty())
            return false;

        String lower = n.toLowerCase();

        // Core JADE system/UI agents
        if (lower.startsWith("ams") || lower.startsWith("df") || lower.startsWith("rma")
                || lower.startsWith("sniffer") || lower.startsWith("introspector") || lower.startsWith("dt-")
                || lower.startsWith("conveyor") || lower.startsWith("fresh") || lower.startsWith("rotten")
                || lower.startsWith("input") || lower.startsWith("charging")) {
            return false;
        }

        // Avoid recursion: don't create twins of twins
        if (lower.endsWith("_dt"))
            return false;

        // Avoid tracking yourself
        if (n.equals(getLocalName()))
            return false;

        return true;
    }

    private String dtGroupIdentification(String childLocalName) {
        if (childLocalName.startsWith("T_")) {
            return TeletubbiesDTAgent.class.getName();
        } else if (childLocalName.startsWith("Robot")) {
            return FuegoDTAgent.class.getName();
        } else if (childLocalName.startsWith("PID") || childLocalName.startsWith("Virtual")) {
            return VrlDTAgent.class.getName();
        } else {
            return childLocalName;
        }
    }

    private void onChildJoined(String childLocalName) {
        try {
            // Create a deterministic twin name
            String twinName = childLocalName + "_DT";

            // If already exists in map, skip
            if (twins.containsKey(childLocalName))
                return;

            ContainerController cc = getContainerController();

            // Pass child name as argument to DigitalTwinAgent
            Object[] args = new Object[] { childLocalName };

            AgentController twinCtrl = cc.createNewAgent(twinName, dtGroupIdentification(childLocalName), args);

            twinCtrl.start();

            twins.put(childLocalName, twinCtrl);

            System.out.println("[TWIN MANAGER] Child joined: " + childLocalName
                    + " -> started twin: " + twinName);

            sendUiCommand("robot_added", childLocalName);

        } catch (Exception e) {
            System.err.println("[TWIN MANAGER] Failed to start twin for " + childLocalName);
            e.printStackTrace();
        }
    }

    private void onChildLeft(String childLocalName) {
        AgentController twinCtrl = twins.remove(childLocalName);

        if (twinCtrl == null) {
            System.out.println("[TWIN MANAGER] Child left: " + childLocalName
                    + " -> no twin tracked");
            return;
        }

        try {
            twinCtrl.kill();
            System.out.println("[TWIN MANAGER] Child left: " + childLocalName
                    + " -> killed twin " + childLocalName + "_DT");
            sendUiCommand("robot_removed", childLocalName);
        } catch (Exception e) {
            System.err.println("[TWIN MANAGER] Failed to kill twin for " + childLocalName);
            e.printStackTrace();
        }
    }

    private void sendUiCommand(String command, String robotName) {
        AID topic = TopicHelper.topic(this, MessagingConstants.UI_TOPICS);
        Acl.publish(this, topic, MessagingConstants.UI_ONTOLOGY, robotName, command, "string", ACLMessage.INFORM);
    }
}