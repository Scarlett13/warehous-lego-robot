package server.digitaltwin.teletubbies;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import robot.utils.RobotState;
import shared.SharedConstants;
import shared.dto.RobotStatusDTO;
import shared.dto.RobotCommandDTO;
import shared.dto.FruitItemDTO;
import shared.messaging.MessagingConstants;
import shared.messaging.TopicHelper;
import shared.messaging.acl.Acl;
import shared.utils.JsonUtil;
import server.opcua.SimpleNamespace;
import org.locationtech.jts.geom.Coordinate;
import server.ServerConfig;

import static robot.RobotConstants.ROBOT_NAME;

public class TeletubbiesDTCommandBehaviour extends CyclicBehaviour {
    private final MessageTemplate mt;
    private final TeletubbiesDTAgent agentClass;
    private final RobotStateChangeUtils stateUtils = new RobotStateChangeUtils();

    // Thresholds and Targets
    private static final double ARRIVAL_THRESHOLD_MM = 200.0;

    public TeletubbiesDTCommandBehaviour(Agent a, TeletubbiesDTAgent agentClass, String robotName) {
        super(a);
        this.agentClass = agentClass;
        AID topic = TopicHelper.topic(a, MessagingConstants.ROBOT_STATE_TOPICS);

        System.out.println("starting behaviour receiver for topic " + topic + " and robot " + robotName);

        MessageTemplate t = MessageTemplate.MatchTopic(topic);
        t = MessageTemplate.and(t, MessageTemplate.MatchOntology(MessagingConstants.ONTOLOGY));
        t = MessageTemplate.and(t, MessageTemplate.MatchLanguage("json"));
        t = MessageTemplate.and(t, MessageTemplate.MatchConversationId(robotName));

        this.mt = t;
    }

    @Override
    public void action() {
        ACLMessage msg = this.getAgent().receive(mt);

        if (msg == null || msg.getContent().isEmpty()) {
            block();
            return;
        }

        RobotStatusDTO newStatus = JsonUtil.fromJson(msg.getContent(), RobotStatusDTO.class);
        String currentRobotName = agentClass.getRobotName();

        // 1. Update OPC-UA Status (Monitoring)
        SimpleNamespace.setRobotCurrentBatteryPercentage(currentRobotName, (int) newStatus.getBatteryPct());

        // 2. State & Target Logic
        String assignedWorkId = agentClass.getAssignedWorkId();
        boolean hasWorkId = (newStatus.getCurrentWorkId() != null && !newStatus.getCurrentWorkId().isEmpty()) ||
                (assignedWorkId != null && !assignedWorkId.isEmpty());

        // UI Fix: Show assigned ID if robot hasn't picked it up yet
        String displayWorkId = (newStatus.getCurrentWorkId() != null && !newStatus.getCurrentWorkId().isEmpty())
                ? newStatus.getCurrentWorkId()
                : (assignedWorkId != null ? assignedWorkId : "");

        SimpleNamespace.setRobotCurrentWorkId(currentRobotName, displayWorkId);

        SimpleNamespace.setRobotUltrasonicSensorReading(currentRobotName,
                newStatus.getUltrasonicReading().getDistance());
        SimpleNamespace.setRobotPosition(
                currentRobotName,
                newStatus.getRobotPosition().getX(),
                newStatus.getRobotPosition().getY(),
                newStatus.getRobotPosition().getAngleDeg());

        boolean arrivedAtPrevDestination = false;
        String currentTargetName = SimpleNamespace.getRobotTargetPath(currentRobotName);
        Coordinate targetCoord = null;

        // Calculate Distance to Target
        double distToTarget = Double.MAX_VALUE;
        double targetAngle = 0.0;

        if (currentTargetName != null && !currentTargetName.isEmpty()) {
            targetCoord = resolveTarget(currentTargetName);
            if (targetCoord != null) {
                double dx = targetCoord.x - newStatus.getRobotPosition().getX();
                double dy = targetCoord.y - newStatus.getRobotPosition().getY();
                distToTarget = Math.sqrt(dx * dx + dy * dy);

                // Calculate angle to target
                targetAngle = Math.toDegrees(Math.atan2(dy, dx));

                if (distToTarget < ARRIVAL_THRESHOLD_MM) {
                    arrivedAtPrevDestination = true;
                }

                // Check Force Arrive Signal from UI
                if (SimpleNamespace.getRobotForceArrival(currentRobotName)) {
                    System.out.println("⚠️ [DT] Force Arrival Triggered via UI!");
                    arrivedAtPrevDestination = true;
                    SimpleNamespace.setRobotForceArrival(currentRobotName, false); // Reset flag
                }
            }
        }

        RobotState newState = stateUtils.changeRobotState(
                newStatus.getRobotState(),
                (int) newStatus.getBatteryPct(),
                hasWorkId,
                arrivedAtPrevDestination);

        // --- CHARGING STATION MANAGEMENT (Centralized) ---
        if (newState == RobotState.GOING_TO_CHARGE) {
            String currentTarget = agentClass.getAssignedTarget();
            // If we don't have a specific station assigned yet
            if (currentTarget == null || currentTarget.equals("Charging Station")
                    || !currentTarget.startsWith("ChargingStation")) {
                String station = server.stations.ChargingStationManager.getInstance().bookStation(currentRobotName);
                if (station != null) {
                    agentClass.setAssignedTarget(station);
                    newStatus.setAssignedChargingStation(station);
                } else {
                    // Waiting for station...
                    // Ensure we don't move to generic "Charging Station"
                    agentClass.setAssignedTarget(null);
                }
            }
        } else if (newStatus.getRobotState() == RobotState.CHARGING && newState != RobotState.CHARGING) {
            // Leaving Charging State -> Release
            server.stations.ChargingStationManager.getInstance().releaseStation(currentRobotName);
            newStatus.setAssignedChargingStation(null);
        }

        // 3. Update Targets based on State
        String newTarget = currentTargetName;
        switch (newState) {
            case PICKINGUP:
                String storedTarget = agentClass.getAssignedTarget();
                if (storedTarget != null && !storedTarget.equals("Charging Station")
                        && !storedTarget.startsWith("ChargingStation")) {
                    newTarget = storedTarget;
                } else {
                    newTarget = "Input Location";
                }
                break;
            case DELIVERING:
                // Routing Logic: Fresh (>4) vs Rotten
                if (agentClass.getAssignedFruitItem() != null) {
                    //
                    newTarget = (agentClass.getAssignedFruitItem().getFreshness() <= 4)
                            ? "Rotten Output Location"
                            : "Fresh Output Location";
                } else {
                    newTarget = "Fresh Output Location"; // Default
                }
                break;
            case GOING_TO_CHARGE:
            case BACK_TO_STATION:
                // For GOING_TO_CHARGE, newTarget is already set by the Booking logic above
                // We fetch it from agentClass to be sure
                newTarget = agentClass.getAssignedTarget();
                if (newTarget == null) {
                    newTarget = "Charging Station"; // Fallback (but will resolve to null if blocked)
                }
                break;
            case STANDBY:
            case CHARGING:
                break;
        }

        // Update OPC-UA if changes
        if (newState != newStatus.getRobotState() || (newTarget != null && !newTarget.equals(currentTargetName))) {

            // --- ITEM HANDOFF LOGIC ---
            // Detect Pickup Completion
            if (newStatus.getRobotState() == RobotState.PICKINGUP && newState == RobotState.DELIVERING) {
                FruitItemDTO item = agentClass.getAssignedFruitItem();
                if (item != null) {
                    item.setStatus(FruitItemDTO.FruitItemWorkStatusEnum.PICKED_UP);
                    sendItemUpdate("Input Location", item);
                }
            }

            // Detect Delivery Completion
            if (newStatus.getRobotState() == RobotState.DELIVERING &&
                    (newState == RobotState.BACK_TO_STATION || newState == RobotState.GOING_TO_CHARGE
                            || newState == RobotState.STANDBY)) {
                FruitItemDTO item = agentClass.getAssignedFruitItem();
                if (item != null) {
                    item.setStatus(FruitItemDTO.FruitItemWorkStatusEnum.DELIVERED);
                    sendItemUpdate(currentTargetName, item);

                    agentClass.setAssignedFruitItem(null);
                    agentClass.setAssignedWorkId(null);
                    agentClass.setAssignedTarget(null);
                }
            }

            if (newTarget != null && !newTarget.equals(currentTargetName)) {
                agentClass.setAssignedTarget(newTarget);
            }

            // UI Update: Append target
            String stateDisplay = newState.name();
            if (newTarget != null && !newTarget.isEmpty()) {
                stateDisplay += " - to " + newTarget;
            }

            SimpleNamespace.setRobotCurrentState(currentRobotName, stateDisplay);

            if (newTarget != null)
                SimpleNamespace.setRobotTargetPath(currentRobotName, newTarget);
        }

        // 4. Calculate Driving Commands (PID) & Send Command
        // We send command if state changed OR if we are in a moving state
        boolean isMovingState = (newState == RobotState.PICKINGUP || newState == RobotState.DELIVERING ||
                newState == RobotState.GOING_TO_CHARGE || newState == RobotState.BACK_TO_STATION);

        if (newState != newStatus.getRobotState() || isMovingState) {

            // 0. Update Traffic Manager with CURRENT position
            server.digitaltwin.TrafficManager.getInstance().updateRobotPosition(
                    currentRobotName,
                    newStatus.getRobotPosition().getX(),
                    newStatus.getRobotPosition().getY());

            double speed = 0.0;
            double turn = 0.0;
            boolean collisionRisk = false;

            // 1. Collision Look-Ahead Check
            if (isMovingState) {
                double currentAngleRad = Math.toRadians(newStatus.getRobotPosition().getAngleDeg());
                double lookAheadDist = 200.0; // Check 20cm ahead
                double nextX = newStatus.getRobotPosition().getX() + Math.cos(currentAngleRad) * lookAheadDist;
                double nextY = newStatus.getRobotPosition().getY() + Math.sin(currentAngleRad) * lookAheadDist;

                if (!server.digitaltwin.TrafficManager.getInstance().canMoveTo(currentRobotName, nextX, nextY)) {
                    // System.out.println("🛑 [COLLISION AVOIDANCE] " + currentRobotName + "
                    // halted.");
                    collisionRisk = true;
                }
            }

            if (isMovingState && targetCoord != null && !collisionRisk) {
                // Determine speed based on distance
                // Simple P-Control for Speed or just fixed speeds
                if (distToTarget > 200) {
                    speed = 400.0; // Standard speed
                } else {
                    speed = 100.0; // Approaches
                }

                // Determine Turn Correction
                // Error = TargetAngle - CurrentAngle
                double currentAngle = newStatus.getRobotPosition().getAngleDeg();
                double error = targetAngle - currentAngle;

                // Normalize error to [-180, 180]
                while (error > 180)
                    error -= 360;
                while (error < -180)
                    error += 360;

                // Simple P-Control for Turn
                // If error is large, turn hard.
                // Using a small P gain
                turn = error * 0.02;
                // Clamp turn
                if (turn > 1.0)
                    turn = 1.0;
                if (turn < -1.0)
                    turn = -1.0;
            }

            // Determine Work ID to send
            String idToSend = "";
            if (newState == RobotState.PICKINGUP || newState == RobotState.DELIVERING) {
                idToSend = (newStatus.getCurrentWorkId() != null && !newStatus.getCurrentWorkId().isEmpty())
                        ? newStatus.getCurrentWorkId()
                        : (assignedWorkId != null ? assignedWorkId : "");
            }

            RobotCommandDTO cmd = new RobotCommandDTO(
                    currentRobotName,
                    System.currentTimeMillis(),
                    speed,
                    turn,
                    newState,
                    idToSend);

            AID topic = TopicHelper.topic(agentClass, MessagingConstants.ROBOT_COMMAND_TOPICS);
            Acl.publish(this.getAgent(), topic, ROBOT_NAME, JsonUtil.toJson(cmd), "json", ACLMessage.INFORM);

        }

        agentClass.setRobotStatus(newStatus);
    }

    private Coordinate resolveTarget(String name) {
        if (name == null || name.isEmpty())
            return null;
        if (ServerConfig.CONVEYOR_LOCATIONS.containsKey(name)) {
            double[] loc = ServerConfig.CONVEYOR_LOCATIONS.get(name);
            return new Coordinate(loc[0], loc[1]);
        }
        if (name.equals("Charging Station"))
            return new Coordinate(ServerConfig.LOC_CHARGING_STATION_1[0], ServerConfig.LOC_CHARGING_STATION_1[1]);

        if (name.startsWith("ChargingStation")) {
            try {
                int index = Integer.parseInt(name.replace("ChargingStation", "")) - 1;
                if (index >= 0 && index < ServerConfig.CHARGING_STATIONS.size()) {
                    double[] loc = ServerConfig.CHARGING_STATIONS.get(index);
                    return new Coordinate(loc[0], loc[1]);
                }
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private void sendItemUpdate(String conveyorName, FruitItemDTO item) {
        if (conveyorName == null || conveyorName.isEmpty())
            return;

        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        msg.addReceiver(new AID(conveyorName, AID.ISLOCALNAME));
        msg.setContent(JsonUtil.toJson(item));
        this.getAgent().send(msg);
        System.out.println(" [DT] Item " + item.getStatus() + " -> " + conveyorName);
    }
}
