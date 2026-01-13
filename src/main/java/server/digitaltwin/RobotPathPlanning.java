package server.digitaltwin;

import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import server.opcua.SimpleNamespace;
import example.mas.Config;
import java.util.ArrayList;
import java.util.List;
import org.locationtech.jts.geom.Coordinate;
import org.json.JSONArray;
import org.json.JSONObject;

public class RobotPathPlanning extends TickerBehaviour {

    private String robotName;
    private String lastTarget = "";

    public RobotPathPlanning(Agent a, long period, String robotName) {
        super(a, period);
        this.robotName = robotName;
    }

    @Override
    protected void onTick() {
        // [Safety] Register position
        double curX = SimpleNamespace.getRobotPositionX(robotName);
        double curY = SimpleNamespace.getRobotPositionY(robotName);
        TrafficManager.getInstance().updateRobotPosition(robotName, curX, curY);

        String targetName = SimpleNamespace.getRobotTargetPath(robotName);

        // If we have a target and we are somewhat localized
        if (targetName != null && !targetName.isEmpty() && (Math.abs(curX) > 0.001 || Math.abs(curY) > 0.001)) {

            // Always check for safety if we are moving?
            // For now, we only re-plan if target changes.
            // Ideally we should check next step safety EVERY tick if moving.

            if (!targetName.equals(lastTarget)) {
                // Resolve Target Coordinates
                Coordinate targetCoord = resolveTarget(targetName);

                if (targetCoord != null) {
                    System.out.println("RobotPathPlanning: Planning path for " + robotName + " from (" + curX + ","
                            + curY + ") to " + targetName);

                    // Simple Manhattan Path Calculation (L-Shape)
                    List<Coordinate> path = calculateManhattanPath(curX, curY, targetCoord.x, targetCoord.y);

                    if (path != null && !path.isEmpty()) {
                        // [Safety] Check immediate next step
                        boolean safeToMove = true;
                        if (path.size() > 1) {
                            Coordinate nextStep = path.get(1);
                            if (!TrafficManager.getInstance().canMoveTo(robotName, nextStep.x, nextStep.y)) {
                                System.out.println(
                                        "RobotPathPlanning: HALTED. " + robotName + " blocked by Safety Zone.");
                                safeToMove = false;
                            }
                        }

                        if (safeToMove) {
                            JSONArray arr = new JSONArray();
                            for (Coordinate c : path) {
                                JSONObject pt = new JSONObject();
                                pt.put("X", c.x);
                                pt.put("Y", c.y);
                                arr.put(pt);
                            }
                            SimpleNamespace.setRobotCurrentTrajectory(robotName, arr.toString());
                            System.out.println("RobotPathPlanning: Trajectory sent (" + arr.length() + " points)");
                            lastTarget = targetName;
                        } else {
                            // Stop - Clear Trajectory or send empty?
                            // Visual Components script expects list. Sending empty list stops robot.
                            SimpleNamespace.setRobotCurrentTrajectory(robotName, "[]");
                        }
                    }
                } else {
                    // System.out.println("RobotPathPlanning: Unknown target " + targetName);
                }
            }
        }
    }

    private Coordinate resolveTarget(String name) {
        // Check Config for Conveyors
        if (Config.CONVEYOR_LOCATIONS.containsKey(name)) {
            double[] loc = Config.CONVEYOR_LOCATIONS.get(name);
            return new Coordinate(loc[0], loc[1]);
        }
        // Check other locations
        if (name.equals("Output Location"))
            return new Coordinate(Config.LOC_OUTPUT[0], Config.LOC_OUTPUT[1]);
        if (name.equals("Charging Station"))
            return new Coordinate(Config.LOC_CS1[0], Config.LOC_CS1[1]);

        return null;
    }

    private List<Coordinate> calculateManhattanPath(double startX, double startY, double targetX, double targetY) {
        List<Coordinate> path = new ArrayList<>();
        path.add(new Coordinate(startX, startY));

        // Simple L-Shape: Move X then Move Y
        // Corner Point
        Coordinate corner = new Coordinate(targetX, startY);
        path.add(corner);
        path.add(new Coordinate(targetX, targetY));

        return path;
    }
}
