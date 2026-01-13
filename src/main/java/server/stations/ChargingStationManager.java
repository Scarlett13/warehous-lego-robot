package server.stations;

import server.ServerConfig;
import java.util.HashMap;
import java.util.Map;

public class ChargingStationManager {
    private static ChargingStationManager instance;
    private final Map<String, String> stationOccupancy; // stationName -> robotName (or null if free)

    private ChargingStationManager() {
        stationOccupancy = new HashMap<>();
        // Initialize stations from ServerConfig
        for (int i = 0; i < ServerConfig.CHARGING_STATIONS.size(); i++) {
            String stationName = "ChargingStation" + (i + 1);
            stationOccupancy.put(stationName, null); // Initially free
        }
    }

    public static synchronized ChargingStationManager getInstance() {
        if (instance == null) {
            instance = new ChargingStationManager();
        }
        return instance;
    }

    /**
     * Attempts to book a charging station for the given robot.
     * 
     * @param robotName The robot requesting a station.
     * @return The name of the booked station (e.g., "ChargingStation1"), or null if
     *         none available.
     */
    public synchronized String bookStation(String robotName) {
        // 1. Check if robot already has a booking (idempotency)
        for (Map.Entry<String, String> entry : stationOccupancy.entrySet()) {
            if (robotName.equals(entry.getValue())) {
                return entry.getKey(); // Return existing booking
            }
        }

        // 2. Find a free station
        for (Map.Entry<String, String> entry : stationOccupancy.entrySet()) {
            if (entry.getValue() == null) {
                String stationName = entry.getKey();
                stationOccupancy.put(stationName, robotName);
                System.out.println("[ChargingManager] Station " + stationName + " BOOKED by " + robotName);
                return stationName;
            }
        }

        return null; // No free stations
    }

    /**
     * Releases the station occupied by the given robot.
     * 
     * @param robotName The robot leaving the station.
     */
    public synchronized void releaseStation(String robotName) {
        for (Map.Entry<String, String> entry : stationOccupancy.entrySet()) {
            if (robotName.equals(entry.getValue())) {
                String stationName = entry.getKey();
                stationOccupancy.put(stationName, null);
                System.out.println("[ChargingManager] Station " + stationName + " RELEASED by " + robotName);
                return;
            }
        }
    }

    /**
     * Force reset a station (e.g. on disconnection)
     */
    public synchronized void forceRelease(String stationName) {
        if (stationOccupancy.containsKey(stationName)) {
            stationOccupancy.put(stationName, null);
            System.out.println("[ChargingManager] Station " + stationName + " FORCE RELEASED");
        }
    }
}
