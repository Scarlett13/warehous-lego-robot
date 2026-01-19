package server.digitaltwin.vrl;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import org.json.JSONArray;
import org.json.JSONObject;
import org.locationtech.jts.geom.Coordinate;
import robot.utils.RobotState;
import server.ServerConfig;
import server.digitaltwin.teletubbies.RobotStateChangeUtils;
import shared.dto.FruitItemDTO;
import shared.utils.JsonUtil;
import server.opcua.SimpleNamespace;

public class VrlDTCommandBehaviour extends CyclicBehaviour {
    private final VrlDTAgent agentClass;
    private final RobotStateChangeUtils stateUtils = new RobotStateChangeUtils();
    private RobotState currentState = RobotState.STANDBY;
    private String currentTarget = null;
    private AID lastRobotSender = null;

    public VrlDTCommandBehaviour(Agent a, VrlDTAgent agentClass, String robotName) {
        super(a);
        this.agentClass = agentClass;
    }

    @Override
    public void action() {
        ACLMessage msg = myAgent.receive();
        boolean arrived = false;

        if (msg != null && msg.getContent() != null) {
            lastRobotSender = msg.getSender();
            try {
                System.out.println("[VRL-DT] Received: " + msg.getContent());
                JSONObject message = new JSONObject(msg.getContent());
                String header = message.optString("Header");

                if ("LOCATION".equals(header)) {
                    JSONArray loc = message.getJSONArray("Location");
                    if (loc.length() >= 2) {
                        agentClass.setLastX(loc.getInt(0));
                        agentClass.setLastY(loc.getInt(1));
                        SimpleNamespace.setRobotPosition(agentClass.getRobotName(), loc.getInt(0), loc.getInt(1), 0);

                        // Update Traffic Manager so other robots avoid us
                        server.digitaltwin.TrafficManager.getInstance().updateRobotPosition(
                                agentClass.getRobotName(),
                                loc.getInt(0),
                                loc.getInt(1));
                    }
                } else if ("ARRIVED".equals(header)) {
                    arrived = true;
                    // System.out.println("[VRL-DT] Robot Arrived signal received.");
                }
            } catch (Exception e) {
                System.out.println("[VRL-DT] Error parsing message: " + e.getMessage());
            }
        }

        // State Machine Logic
        boolean hasWork = (agentClass.getAssignedWorkId() != null);
        boolean hasItem = (agentClass.getAssignedFruitItem() != null
                && agentClass.getAssignedFruitItem().getStatus() == FruitItemDTO.FruitItemWorkStatusEnum.PICKED_UP);

        // Force Arrived if we are at target?
        // VRL robot sends ARRIVED, so we rely on that.

        RobotState newState = stateUtils.changeRobotState(currentState, 100, hasWork, hasItem, arrived);

        // Target Logic
        String newTarget = currentTarget;
        switch (newState) {
            case PICKINGUP:
                newTarget = agentClass.getAssignedTarget(); // Conveyor Name
                break;
            case DELIVERING:
                if (agentClass.getAssignedFruitItem() != null
                        && agentClass.getAssignedFruitItem().getFreshness() <= 4) {
                    newTarget = "Rotten Output Location";
                } else {
                    newTarget = "Fresh Output Location";
                }
                break;
            case CHARGING:
            case BACK_TO_STATION: // VRL specific: Where to go after delivery?
            case GOING_TO_CHARGE:
            case STANDBY:
                newTarget = "Standby Point"; // Or Charging Station?
                break;
        }

        // Detect State Changes for Item Updates
        if (currentState == RobotState.PICKINGUP && newState == RobotState.DELIVERING) {
            FruitItemDTO item = agentClass.getAssignedFruitItem();
            if (item != null) {
                item.setStatus(FruitItemDTO.FruitItemWorkStatusEnum.PICKED_UP);
                sendItemUpdate("Input Location", item); // Assuming Input Location for now or item.getConveyor()
            }
        }

        if (currentState == RobotState.DELIVERING && newState == RobotState.BACK_TO_STATION) {
            FruitItemDTO item = agentClass.getAssignedFruitItem();
            if (item != null) {
                item.setStatus(FruitItemDTO.FruitItemWorkStatusEnum.DELIVERED);
                sendItemUpdate(currentTarget, item);

                // Clear Assignment
                agentClass.setAssignedFruitItem(null);
                agentClass.setAssignedWorkId(null);
                agentClass.setAssignedTarget(null);
            }
        }

        // Send Path if Target Changed
        if (newTarget != null && !newTarget.equals(currentTarget)) {
            System.out.println("[VRL-DT] Target changed: " + currentTarget + " -> " + newTarget);
            boolean sent = sendPath(newTarget);
            if (sent) {
                currentTarget = newTarget;
            }
        }

        currentState = newState;
        agentClass.setLastState(currentState.name());
        SimpleNamespace.setRobotCurrentState(agentClass.getRobotName(), currentState.name());
        SimpleNamespace.setRobotCurrentWorkId(agentClass.getRobotName(), agentClass.getAssignedWorkId());

        if (msg == null)
            block(100); // Small block to prevent tight loop burnout but allow polling
    }

    private boolean sendPath(String targetName) {
        Coordinate targetCoord = resolveTarget(targetName);
        if (targetCoord == null) {
            System.out.println("[VRL-DT] Unknown target: " + targetName);
            return false;
        }

        // int currentX = agentClass.getLastX();
        int currentY = agentClass.getLastY();

        // Calculate L-Shape Path
        // 1. Current -> Corner (TargetX, CurrentY) -> Target
        // This is safe for simple grid layouts.

        int targetX = (int) targetCoord.x;
        int targetY = (int) targetCoord.y;

        JSONArray points = new JSONArray();

        // Point 1: Corner
        JSONArray p1 = new JSONArray();
        p1.put(targetX);
        p1.put(currentY);
        points.put(p1);

        // Point 2: Target
        JSONArray p2 = new JSONArray();
        p2.put(targetX);
        p2.put(targetY);
        points.put(p2);

        // Construct Message
        JSONObject messageContent = new JSONObject();
        messageContent.put("Header", "PATH");
        messageContent.put("Points", points);

        ACLMessage targetMsg = new ACLMessage(ACLMessage.INFORM);

        if (lastRobotSender != null) {
            targetMsg.addReceiver(lastRobotSender);
        } else {
            System.out.println("[VRL-DT] No robot sender identified yet. Using fallback.");
            // Fallback to name-based if really needed, but sender is safer.
            // For now just fail safe
            return false;
        }

        targetMsg.setContent(messageContent.toString());
        myAgent.send(targetMsg);

        System.out.println("[VRL-DT] Sent Path to " + targetName + ": " + points);
        return true;
    }

    private Coordinate resolveTarget(String name) {
        if (name == null)
            return null;
        if (ServerConfig.CONVEYOR_LOCATIONS.containsKey(name)) {
            double[] loc = ServerConfig.CONVEYOR_LOCATIONS.get(name);
            return new Coordinate(loc[0], loc[1]);
        }
        if (name.equals("Fresh Output Location"))
            return new Coordinate(ServerConfig.LOC_FRESH_OUTPUT[0], ServerConfig.LOC_FRESH_OUTPUT[1]);
        if (name.equals("Rotten Output Location"))
            return new Coordinate(ServerConfig.LOC_ROTTEN_OUTPUT[0], ServerConfig.LOC_ROTTEN_OUTPUT[1]);

        // Default Output
        if (name.contains("Output"))
            return new Coordinate(ServerConfig.LOC_FRESH_OUTPUT[0], ServerConfig.LOC_FRESH_OUTPUT[1]);

        if (name.equals("Standby Point"))
            return new Coordinate(1000, 1000); // Example Standby

        return null; // Unknown
    }

    private void sendItemUpdate(String conveyorName, FruitItemDTO item) {
        if (conveyorName == null || conveyorName.isEmpty())
            return;

        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        msg.addReceiver(new AID(conveyorName, AID.ISLOCALNAME));
        msg.setContent(JsonUtil.toJson(item));
        this.getAgent().send(msg);
    }
}
