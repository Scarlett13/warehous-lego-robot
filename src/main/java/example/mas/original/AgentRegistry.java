package example.mas.original;

import org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode;
import java.util.HashMap;
import java.util.Map;

/**
 * AGENT REGISTRY
 * 
 * Keeps track of all agents (robots, conveyors) in the system and their OPC-UA variables.

 */
public class AgentRegistry {
    
    // Stores OPC-UA nodes for each robot
    private static Map<String, RobotNodes> robotMap = new HashMap<>();
    
    // Stores OPC-UA nodes for each conveyor
    private static Map<String, ConveyorNodes> conveyorMap = new HashMap<>();
    
    /**
     * Container for a robot's OPC-UA variable nodes
     */
    public static class RobotNodes {
        public UaVariableNode location;
        public UaVariableNode target;
        public UaVariableNode battery;
        public UaVariableNode hasProduct;
        
        public RobotNodes(UaVariableNode location, UaVariableNode target,
                         UaVariableNode battery, UaVariableNode hasProduct) {
            this.location = location;
            this.target = target;
            this.battery = battery;
            this.hasProduct = hasProduct;
        }
    }
    
    /**
     * Container for a conveyor's OPC-UA variable nodes
     */
    public static class ConveyorNodes {
        public UaVariableNode produced;
        
        public ConveyorNodes(UaVariableNode produced) {
            this.produced = produced;
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
