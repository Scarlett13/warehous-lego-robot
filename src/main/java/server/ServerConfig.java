package server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServerConfig {
    public ServerConfig() {
    }

    // OPC-UA Server Configuration - bunun testi yazılacak
    public static final String SERVER_NAME = "WarehouseMAS";
    public static final int SERVER_PORT = 4840;
    public static final String NAMESPACE_URI = "urn:warehouse:mas";

    // Centralized IP Configuration
    public static final String MAIN_HOST_IP = "192.168.0.157";

    // Robot Configuration - Dynamic list
    public static final List<String> ROBOT_NAMES = new ArrayList<String>() {
        {
            add("TinkyWinky");
        }
    };

    // Conveyor Configuration - Dynamic list
    public static final List<String> CONVEYOR_NAMES = new ArrayList<String>() {
        {
            add("Input Location");
            add("Fresh Output Location");
            add("Rotten Output Location");
        }
    };

    // Location Coordinates (X, Y, Z in mm)
    public static final double[] LOC_INPUT_CONVEYOR = { 6530, 14750, 0 }; // Was LOC_A
    public static final double[] LOC_ROTTEN_OUTPUT = { 12720, 14070, 0 }; // Was LOC_B
    public static final double[] LOC_FRESH_OUTPUT = { 12600, 15100, 0 }; // Was LOC_OUTPUT
    public static final double[] LOC_CHARGING_STATION_1 = { 6380, 13720, 0 }; // Was LOC_CS1
    public static final double[] LOC_CHARGING_STATION_2 = { 5280, 14720, 0 }; // Was LOC_CS2
    public static final double[] LOC_IDLE_AREA = { 1000, 0, 0 }; // Was LOC_IDLE

    // Conveyor locations - Dynamic map
    public static final Map<String, double[]> CONVEYOR_LOCATIONS = new HashMap<String, double[]>() {
        {
            put("Input Location", LOC_INPUT_CONVEYOR);
            put("Fresh Output Location", LOC_FRESH_OUTPUT);
            put("Rotten Output Location", LOC_ROTTEN_OUTPUT);
        }
    };

    // All pickup/delivery locations - Dynamic list
    public static final List<double[]> TASK_LOCATIONS = new ArrayList<double[]>() {
        {
            add(LOC_INPUT_CONVEYOR);
            add(LOC_ROTTEN_OUTPUT);
            add(LOC_FRESH_OUTPUT);
        }
    };

    // All charging stations - Dynamic list
    public static final List<double[]> CHARGING_STATIONS = new ArrayList<double[]>() {
        {
            add(LOC_CHARGING_STATION_1);
            add(LOC_CHARGING_STATION_2);
        }
    };

    // Battery Management
    public static final int INITIAL_BATTERY = 100;
    public static final int LOW_BATTERY_THRESHOLD = 1;
    public static final int CHARGE_RATE = 10;

    // Agent Update Interval
    public static final int UPDATE_INTERVAL = 1000;

    // JADE Service Names
    public static final String ROBOT_YELLOW_PAGES_SERVICE = "warehouse-robot";
    public static final String CONVEYOR_YELLOW_PAGES_SERVICE = "warehouse-conveyor";

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
