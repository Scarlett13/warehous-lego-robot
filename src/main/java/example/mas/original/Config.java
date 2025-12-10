package example.mas.original;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Config {
    
    // OPC-UA Server Configuration - bunun testi yazılacak
    public static final String SERVER_NAME = "WarehouseMAS";
    public static final int SERVER_PORT = 4840;
    public static final String NAMESPACE_URI = "urn:warehouse:mas";
    
    // Robot Configuration - Dynamic list
    public static final List<String> ROBOT_NAMES = new ArrayList<String>() {{
        add("Robot1");
        add("Robot2");
    }};
    
    // Conveyor Configuration - Dynamic list
    public static final List<String> CONVEYOR_NAMES = new ArrayList<String>() {{
        add("Conveyor1");
        add("Conveyor2");
    }};
    
    // Location Coordinates (X, Y, Z in mm)
    public static final double[] LOC_A = {-11252.725, -7394.995, 0};
    public static final double[] LOC_B = {-11762.35, 3057.931, 0};
    public static final double[] LOC_OUTPUT = {11137.976, -2198.091, 0};
    public static final double[] LOC_CS1 = {-983.039, -10358.081, 0};
    public static final double[] LOC_CS2 = {-1187.116, 5050.898, 0};
    public static final double[] LOC_IDLE = {1000, 0, 0};
    
    // Conveyor locations - Dynamic map
    public static final Map<String, double[]> CONVEYOR_LOCATIONS = new HashMap<String, double[]>() {{
        put("Conveyor1", LOC_A);
        put("Conveyor2", LOC_B);
    }};
    
    // All pickup/delivery locations - Dynamic list
    public static final List<double[]> TASK_LOCATIONS = new ArrayList<double[]>() {{
        add(LOC_A);
        add(LOC_B);
    }};
    
    // All charging stations - Dynamic list
    public static final List<double[]> CHARGING_STATIONS = new ArrayList<double[]>() {{
        add(LOC_CS1);
        add(LOC_CS2);
    }};
    
    // Battery Management
    public static final int INITIAL_BATTERY = 100;
    public static final int LOW_BATTERY_THRESHOLD = 1;
    public static final int CHARGE_RATE = 10;
    
    // Agent Update Interval
    public static final int UPDATE_INTERVAL = 1000;
    
    // JADE Service Names
    public static final String YELLOW_PAGES_SERVICE = "warehouse-robot";
    public static final String COORDINATOR_SERVICE = "warehouse-coordinator";
    
    // Dynamic Configuration Methods
    public static void addRobot(String robotName) {
        if (!ROBOT_NAMES.contains(robotName)) {
            ROBOT_NAMES.add(robotName);
        }
    }
    
    public static void addConveyor(String conveyorName) {
        if (!CONVEYOR_NAMES.contains(conveyorName)) {
            CONVEYOR_NAMES.add(conveyorName);
        }
    }
    
    public static void addConveyorLocation(String conveyorName, double[] location) {
        CONVEYOR_LOCATIONS.put(conveyorName, location);
    }
    
    public static void addTaskLocation(double[] location) {
        TASK_LOCATIONS.add(location);
    }
    
    public static void addChargingStation(double[] location) {
        CHARGING_STATIONS.add(location);
    }
}
