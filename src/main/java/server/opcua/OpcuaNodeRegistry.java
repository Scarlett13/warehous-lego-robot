package server.opcua;

import org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode;

import java.util.HashMap;
import java.util.Map;

/**
 * AGENT REGISTRY
 * 
 * Keeps track of all agents (robots, conveyors) in the system and their OPC-UA variables.

 */
public class OpcuaNodeRegistry {
    
    // Stores OPC-UA nodes for each robot
    private static Map<String, RobotNodes> robotMap = new HashMap<>();
    
    // Stores OPC-UA nodes for each conveyor
    private static Map<String, ConveyorNodes> conveyorMap = new HashMap<>();
    
    /**
     * Container for a robot's OPC-UA variable nodes
     */
    public static class RobotNodes {
        public UaVariableNode currentBatteryPercentage;
        public UaVariableNode currentWorkId;
        public UaVariableNode currentPath;
        public UaVariableNode targetPath;
        public UaVariableNode currentSpeed;
        public UaVariableNode currentState;

        public RobotNodes(UaVariableNode curretnBatteryPercentage,
                          UaVariableNode currentWorkId,
                          UaVariableNode currentPath,
                          UaVariableNode targetPath,
                          UaVariableNode currentSpeed,
                          UaVariableNode currentState) {
            this.currentBatteryPercentage = curretnBatteryPercentage;
            this.currentWorkId = currentWorkId;
            this.currentPath = currentPath;
            this.targetPath = targetPath;
            this.currentSpeed = currentSpeed;
            this.currentState = currentState;
        }
    }
    
    /**
     * Container for a conveyor's OPC-UA variable nodes
     */
    public static class ConveyorNodes {
        public UaVariableNode nextitemid;
        public UaVariableNode totalitems;
        
        public ConveyorNodes(UaVariableNode nextitemid, UaVariableNode totalitems) {
            this.nextitemid = nextitemid;
            this.totalitems = totalitems;
        }
    }
    
    /**
     * Register a new robot in the system
     */
    public static void registerRobot(String robotName, RobotNodes nodes) {
        robotMap.put(robotName, nodes);
        System.out.println("✅ Registered robot: " + robotName);
    }
    
    /**
     * Register a new conveyor in the system
     */
    public static void registerConveyor(String conveyorName, ConveyorNodes nodes) {
        conveyorMap.put(conveyorName, nodes);
        System.out.println("✅ Registered conveyor: " + conveyorName);
    }
    
    /**
     * Get OPC-UA nodes for a specific robot
     */
    public static RobotNodes getRobot(String robotName) {
        return robotMap.get(robotName);
    }
    
    /**
     * Get OPC-UA nodes for a specific conveyor
     */
    public static ConveyorNodes getConveyor(String conveyorName) {
        return conveyorMap.get(conveyorName);
    }
    
    /**
     * Check if a robot is registered
     */
    public static boolean hasRobot(String robotName) {
        return robotMap.containsKey(robotName);
    }
    
    /**
     * Check if a conveyor is registered
     */
    public static boolean hasConveyor(String conveyorName) {
        return conveyorMap.containsKey(conveyorName);
    }
    
    /**
     * Get all registered robot names
     */
    public static String[] getAllRobotNames() {
        return robotMap.keySet().toArray(new String[0]);
    }
    
    /**
     * Get all registered conveyor names
     */
    public static String[] getAllConveyorNames() {
        return conveyorMap.keySet().toArray(new String[0]);
    }
    
    /**
     * Clear all registered components (for reset)
     */
    public static void clear() {
        robotMap.clear();
        conveyorMap.clear();
    }
}
