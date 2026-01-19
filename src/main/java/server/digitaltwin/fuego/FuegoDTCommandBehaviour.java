package server.digitaltwin.fuego;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
import server.ServerConfig;
import server.digitaltwin.TrafficManager;
import server.opcua.SimpleNamespace;
import shared.dto.FruitItemDTO;
import shared.dto.Point2D;
import shared.utils.JsonUtil;

import java.util.ArrayList;
import java.util.List;

public class FuegoDTCommandBehaviour extends TickerBehaviour {
    private final FuegoDTAgent dtAgent;
    private final String robotName;
    private String currentTarget = null;

    // Arrival Simulation
    private long travelStartTime = 0;
    private long estimatedTravelTime = 0;
    private boolean isMoving = false;

    // Current Sim Position
    private int currentX = 0;
    private int currentY = 0;

    public FuegoDTCommandBehaviour(Agent a, long period, FuegoDTAgent dtAgent, String robotName) {
        super(a, period);
        this.dtAgent = dtAgent;
        this.robotName = robotName;
        // Initial position (Input Location)
        double[] inputLoc = ServerConfig.LOC_INPUT_CONVEYOR;
        currentX = (int) inputLoc[0];
        currentY = (int) inputLoc[1];
    }

    @Override
    protected void onTick() {
        // System.out.println("fuegoticking");
        // // 1. Check for Assignment
        // String newTarget = dtAgent.getAssignedTarget();

        // 2. Simulate Movement (only if not overridden by live data)
        // Check for Live Data
        FuegoOpcUaClient client = dtAgent.getOpcUaClient();
        if (client == null) {
            System.out.println("Client is NULL");
        } else {
            System.out.println("Client is " + (client.isConnected() ? "CONNECTED" : "DISCONNECTED"));
        }

        if (client != null && client.isConnected()) {
            Point2D livePos = client.getPosition();
            // System.out.println("Live Pos Read: " + livePos.x + ", " + livePos.y);

            if (livePos.x != 0 && livePos.y != 0) {
                // We have live data! Override simulation
                // SCALE FACTOR: Fuego uses 0.1mm units? User requested /10.
                this.currentX = (int) (livePos.x / 10.0);
                this.currentY = (int) (livePos.y / 10.0);
            }

            System.out.println("fuego x and y: " + this.currentX + " " + this.currentY);

            // Update Traffic Manager for Collision Avoidance
            TrafficManager.getInstance().updateRobotPosition(robotName, currentX, currentY);
        }

        // Passively reading. No writing.
    }

    // private void handleNewTarget(String newTarget) {
    // System.out.println(" Fuego Robot received new target: " + newTarget);
    //
    // Point2D targetCoord = getTargetCoordinates(newTarget);
    // if (targetCoord == null) {
    // System.err.println(" Unknown target: " + newTarget);
    // return;
    // }
    //
    // // Generate Path (Manhattan L-Shape)
    // Point2D start = new Point2D(currentX, currentY);
    // List<Point2D> path = calculateManhattanPath(start, targetCoord);
    //
    // if (path != null && !path.isEmpty()) {
    // currentTarget = newTarget;
    //
    // // Start Sim
    // startSimulation(path);
    // } else {
    // System.err.println(" No path found to " + newTarget);
    // }
    // }
    //
    // private List<Point2D> calculateManhattanPath(Point2D start, Point2D end) {
    // List<Point2D> path = new ArrayList<>();
    // path.add(start);
    // // Corner: Move X then Y? Or Y then X?
    // // Let's do X then Y (L-Shape)
    // Point2D corner = new Point2D(end.x, start.y);
    // path.add(corner);
    // path.add(end);
    // return path;
    // }
    //
    // private void startSimulation(List<Point2D> path) {
    // isMoving = true;
    // travelStartTime = System.currentTimeMillis();
    //
    // // Estimate Time: Distance / Speed
    // double SPEED = 300.0; // mm/s (Estimated)
    // double distance = calculateTotalDistance(path);
    //
    // estimatedTravelTime = (long) ((distance / SPEED) * 1000);
    // // Minimum time 2s
    // if (estimatedTravelTime < 2000)
    // estimatedTravelTime = 2000;
    //
    // System.out.println(
    // " Estimated travel time: " + estimatedTravelTime + "ms (" +
    // String.format("%.0f", distance) + "mm)");
    // }
    //
    // private void checkArrival() {
    // if (System.currentTimeMillis() - travelStartTime > estimatedTravelTime) {
    // System.out.println(" Fuego Robot Arrived at " + currentTarget);
    // isMoving = false;
    //
    // // Update Position to Target (Simulated)
    // Point2D endPoint = getTargetCoordinates(currentTarget);
    // if (endPoint != null) {
    // currentX = (int) endPoint.x;
    // currentY = (int) endPoint.y;
    // }
    //
    // // Handle Arrival Logic (Item Status Update)
    // handleArrivalLogic();
    // }
    // }
    //
    // private void handleArrivalLogic() {
    // FruitItemDTO item = dtAgent.getAssignedFruitItem();
    // if (item != null) {
    // if (currentTarget.contains("Conveyor") ||
    // currentTarget.equals(item.getConveyorName())) {
    // // PICK UP
    // item.setStatus(FruitItemDTO.FruitItemWorkStatusEnum.PICKED_UP);
    // System.out.println(" Picked up item " + item.getItemId());
    // sendItemUpdate(currentTarget, item);
    //
    // // Next Task: Deliver
    // if (item.getFreshness() <= 4) {
    // dtAgent.setAssignedTarget("Rotten Output Location");
    // } else {
    // dtAgent.setAssignedTarget("Fresh Output Location");
    // }
    //
    // } else if (currentTarget.contains("Output")) {
    // // DELIVER
    // item.setStatus(FruitItemDTO.FruitItemWorkStatusEnum.DELIVERED);
    // System.out.println(" Delivered item " + item.getItemId());
    // sendItemUpdate(currentTarget, item);
    //
    // // Clear Assignment
    // dtAgent.setAssignedFruitItem(null);
    // dtAgent.setAssignedWorkId(null);
    // dtAgent.setAssignedTarget(null); // Wait for new job
    // currentTarget = null;
    //
    // System.out.println(" Job Complete. Waiting for next job...");
    // }
    // }
    // }
    //
    // private void sendItemUpdate(String targetAgentName, FruitItemDTO item) {
    // if (targetAgentName == null)
    // return;
    // ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
    // msg.addReceiver(new AID(targetAgentName, AID.ISLOCALNAME));
    // msg.setContent(JsonUtil.toJson(item));
    // dtAgent.send(msg);
    // }
    //
    // private double calculateTotalDistance(List<Point2D> path) {
    // if (path == null || path.size() < 2)
    // return 0;
    // double dist = 0;
    // for (int i = 0; i < path.size() - 1; i++) {
    // Point2D p1 = path.get(i);
    // Point2D p2 = path.get(i + 1);
    // dist += Math.sqrt(Math.pow(p2.x - p1.x, 2) + Math.pow(p2.y - p1.y, 2));
    // }
    // return dist;
    // }
    //
    // private Point2D getTargetCoordinates(String targetName) {
    // if (targetName == null)
    // return null;
    // if (ServerConfig.CONVEYOR_LOCATIONS.containsKey(targetName)) {
    // double[] loc = ServerConfig.CONVEYOR_LOCATIONS.get(targetName);
    // return new Point2D((int) loc[0], (int) loc[1]);
    // }
    // if (targetName.equals("Fresh Output Location"))
    // return new Point2D((int) ServerConfig.LOC_FRESH_OUTPUT[0], (int)
    // ServerConfig.LOC_FRESH_OUTPUT[1]);
    // if (targetName.equals("Rotten Output Location"))
    // return new Point2D((int) ServerConfig.LOC_ROTTEN_OUTPUT[0], (int)
    // ServerConfig.LOC_ROTTEN_OUTPUT[1]);
    //
    // return null;
    // }
}
