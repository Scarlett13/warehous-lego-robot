package server.digitaltwin;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.util.GeometricShapeFactory;

/**
 * Singleton TrafficManager to prevent collisions and resolve deadlocks.
 * Acts as the "Police" for the robots.
 */
public class TrafficManager {

    private static TrafficManager instance;
    private final GeometryFactory gf = new GeometryFactory();
    private static final double SAFETY_ZONE_SIZE = 600.0;

    // Robot Safety Zones: Name -> Safe Polygon (Square)
    private Map<String, Polygon> safetyZones = new ConcurrentHashMap<>();

    // Robot Priorities: Name -> Integer (Higher is better)
    private Map<String, Integer> robotPriorities = new ConcurrentHashMap<>();

    private TrafficManager() {
        // Init default priorities if needed
        robotPriorities.put("TinkyWinky", 10); // Example
    }

    public static synchronized TrafficManager getInstance() {
        if (instance == null) {
            instance = new TrafficManager();
        }
        return instance;
    }

    /**
     * Updates the robot's current safety zone based on position.
     * The Safety Zone is an imaginary square around the robot.
     */
    public void updateRobotPosition(String robotName, double x, double y) {
        GeometricShapeFactory shapeFactory = new GeometricShapeFactory(gf);
        shapeFactory.setNumPoints(4);
        shapeFactory.setCentre(new Coordinate(x, y));
        shapeFactory.setWidth(SAFETY_ZONE_SIZE);
        shapeFactory.setHeight(SAFETY_ZONE_SIZE);
        Polygon zone = shapeFactory.createRectangle();

        safetyZones.put(robotName, zone);
    }

    /**
     * Checks if a robot can move to a target coordinate.
     * Returns TRUE if the move is safe (no overlap with other robot zones).
     */
    public boolean canMoveTo(String askingRobot, double targetX, double targetY) {
        GeometricShapeFactory shapeFactory = new GeometricShapeFactory(gf);
        shapeFactory.setNumPoints(4);
        shapeFactory.setCentre(new Coordinate(targetX, targetY));
        shapeFactory.setWidth(SAFETY_ZONE_SIZE);
        shapeFactory.setHeight(SAFETY_ZONE_SIZE);
        Polygon targetZone = shapeFactory.createRectangle();

        for (Map.Entry<String, Polygon> entry : safetyZones.entrySet()) {
            String otherRobot = entry.getKey();
            Polygon otherZone = entry.getValue();

            // Self check
            if (otherRobot.equals(askingRobot))
                continue;

            // Collision Check
            if (targetZone.intersects(otherZone)) {
                System.out.println("TrafficManager: " + askingRobot + " blocked by " + otherRobot);
                return false; // Collision imminent
            }
        }
        return true; // Path clear
    }

    /**
     * Resolve deadlock between two robots.
     * Returns instructions on who should yield.
     * (Placeholder for more complex logic)
     */
    public String resolveConflict(String robotA, String robotB) {
        int pA = robotPriorities.getOrDefault(robotA, 0);
        int pB = robotPriorities.getOrDefault(robotB, 0);

        if (pA > pB)
            return robotA; // Robot A has right of way
        return robotB;
    }
}
