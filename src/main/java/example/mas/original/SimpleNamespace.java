package example.mas.original;

import org.eclipse.milo.opcua.sdk.core.AccessLevel;
import org.eclipse.milo.opcua.sdk.server.OpcUaServer;
import org.eclipse.milo.opcua.sdk.server.api.DataItem;
import org.eclipse.milo.opcua.sdk.server.api.ManagedNamespace;
import org.eclipse.milo.opcua.sdk.server.api.MonitoredItem;
import org.eclipse.milo.opcua.sdk.server.model.nodes.objects.FolderTypeNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaFolderNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNodeContext;
import org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;

import java.util.List;
import java.util.Optional;


public class SimpleNamespace extends ManagedNamespace {
    
    public static final String URI = Config.NAMESPACE_URI;
    
    public SimpleNamespace(OpcUaServer server) throws Exception {
        super(server, URI);
    }
    
    /**
     * Register a new robot and create its OPC-UA variables
     */
    public void registerRobot(String robotName) throws Exception {
        if (AgentRegistry.hasRobot(robotName)) {
            System.out.println("⚠️  Robot " + robotName + " already registered");
            return;
        }
        
        System.out.println(" Creating OPC-UA variables for " + robotName);
        
        UaNodeContext context = getNodeContext();
        
        // Create folder for this robot (e.g., "Robot1", "Robot2")
        UaFolderNode robotFolder = new UaFolderNode(
            context,
            newNodeId(robotName),
            newQualifiedName(robotName),
            LocalizedText.english(robotName)
        );
        
        // Register folder
        context.getNodeManager().addNode(robotFolder);
        
        // Add to Objects folder
        Optional<UaNode> objectsFolder = context.getServer()
            .getAddressSpaceManager()
            .getManagedNode(Identifiers.ObjectsFolder);
        objectsFolder.ifPresent(node -> ((FolderTypeNode) node).addComponent(robotFolder));
        
        // Create variables for this robot
        UaVariableNode location = createVariable(robotFolder, "Location", "");
        UaVariableNode target = createVariable(robotFolder, "Target", "");
        UaVariableNode battery = createVariable(robotFolder, "BatteryLevel", Config.INITIAL_BATTERY);
        UaVariableNode hasProduct = createVariable(robotFolder, "HasProduct", false);
        
        // Register in registry
        AgentRegistry.RobotNodes nodes = new AgentRegistry.RobotNodes(location, target, battery, hasProduct);
        AgentRegistry.registerRobot(robotName, nodes);
        
        System.out.println(" " + robotName + " ready (ns=2;s=" + robotName + "/*)");
    }
    
    /**
     * Register a new conveyor and create its OPC-UA variables
     */
    public void registerConveyor(String conveyorName) throws Exception {
        if (AgentRegistry.hasConveyor(conveyorName)) {
            System.out.println("️  Conveyor " + conveyorName + " already registered");
            return;
        }
        
        System.out.println(" Creating OPC-UA variables for " + conveyorName);
        
        UaNodeContext context = getNodeContext();
        
        // Create folder for this conveyor
        UaFolderNode conveyorFolder = new UaFolderNode(
            context,
            newNodeId(conveyorName),
            newQualifiedName(conveyorName),
            LocalizedText.english(conveyorName)
        );
        
        // Register folder
        context.getNodeManager().addNode(conveyorFolder);
        
        // Add to Objects folder
        Optional<UaNode> objectsFolder = context.getServer()
            .getAddressSpaceManager()
            .getManagedNode(Identifiers.ObjectsFolder);
        objectsFolder.ifPresent(node -> ((FolderTypeNode) node).addComponent(conveyorFolder));
        
        // Create variables for this conveyor
        UaVariableNode produced = createVariable(conveyorFolder, "Produced", false);
        
        // Register in registry
        AgentRegistry.ConveyorNodes nodes = new AgentRegistry.ConveyorNodes(produced);
        AgentRegistry.registerConveyor(conveyorName, nodes);
        
        System.out.println(" " + conveyorName + " ready (ns=2;s=" + conveyorName + "/*)");
    }
    
    /**
     * Helper method to create a variable node
     */
    private UaVariableNode createVariable(UaFolderNode folder, String name, Object initialValue) {
        UaVariableNode variable = new UaVariableNode.UaVariableNodeBuilder(getNodeContext())
            .setNodeId(newNodeId(folder.getBrowseName().getName() + "/" + name))
            .setAccessLevel(AccessLevel.READ_WRITE)
            .setUserAccessLevel(AccessLevel.READ_WRITE)
            .setBrowseName(newQualifiedName(name))
            .setDisplayName(LocalizedText.english(name))
            .setDataType(getDataTypeIdentifier(initialValue))
            .setTypeDefinition(Identifiers.BaseDataVariableType)
            .build();
        
        // Set initial value
        if (initialValue instanceof String) {
            variable.setValue(new DataValue(new Variant((String) initialValue)));
        } else if (initialValue instanceof Integer) {
            variable.setValue(new DataValue(new Variant((Integer) initialValue)));
        } else if (initialValue instanceof Boolean) {
            variable.setValue(new DataValue(new Variant((Boolean) initialValue)));
        } else if (initialValue instanceof Double) {
            variable.setValue(new DataValue(new Variant((Double) initialValue)));
        }
        
        // Add to folder
        folder.addComponent(variable);
        getNodeManager().addNode(variable);
        
        return variable;
    }
    
    /**
     * Get the correct OPC-UA data type identifier based on the value type
     */
    private org.eclipse.milo.opcua.stack.core.types.builtin.NodeId getDataTypeIdentifier(Object value) {
        if (value instanceof String) {
            return Identifiers.String;
        } else if (value instanceof Integer) {
            return Identifiers.Int32;
        } else if (value instanceof Boolean) {
            return Identifiers.Boolean;
        } else if (value instanceof Double) {
            return Identifiers.Double;
        }
        return Identifiers.String;
    }
    
    // Static helper methods for agents to access robot data
    
    public static void setRobotLocation(String robotName, String location) {
        AgentRegistry.RobotNodes nodes = AgentRegistry.getRobot(robotName);
        if (nodes != null) {
            nodes.location.setValue(new DataValue(new Variant(location)));
        }
    }
    
    public static void setRobotTarget(String robotName, double[] coordinates) {
        AgentRegistry.RobotNodes nodes = AgentRegistry.getRobot(robotName);
        if (nodes != null) {
            // Send coordinates as semicolon-separated string: "x;y;z" (avoid comma conflict with decimal separator)
            String coordString = String.format(java.util.Locale.US, "%.3f;%.3f;%.3f", coordinates[0], coordinates[1], coordinates[2]);
            nodes.target.setValue(new DataValue(new Variant(coordString)));
        }
    }
    
    public static void setRobotBattery(String robotName, int battery) {
        AgentRegistry.RobotNodes nodes = AgentRegistry.getRobot(robotName);
        if (nodes != null) {
            nodes.battery.setValue(new DataValue(new Variant(battery)));
        }
    }
    
    public static void setRobotHasProduct(String robotName, boolean hasProduct) {
        AgentRegistry.RobotNodes nodes = AgentRegistry.getRobot(robotName);
        if (nodes != null) {
            nodes.hasProduct.setValue(new DataValue(new Variant(hasProduct)));
        }
    }
    
    public static String getRobotLocation(String robotName) {
        AgentRegistry.RobotNodes nodes = AgentRegistry.getRobot(robotName);
        if (nodes != null) {
            try {
                return (String) nodes.location.getValue().getValue().getValue();
            } catch (Exception e) {
                return "";
            }
        }
        return "";
    }
    
    public static int getRobotBattery(String robotName) {
        AgentRegistry.RobotNodes nodes = AgentRegistry.getRobot(robotName);
        if (nodes != null) {
            try {
                return (Integer) nodes.battery.getValue().getValue().getValue();
            } catch (Exception e) {
                return Config.INITIAL_BATTERY;
            }
        }
        return Config.INITIAL_BATTERY;
    }
    
    public static boolean getRobotHasProduct(String robotName) {
        AgentRegistry.RobotNodes nodes = AgentRegistry.getRobot(robotName);
        if (nodes != null) {
            try {
                return (Boolean) nodes.hasProduct.getValue().getValue().getValue();
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }
    
    // Conveyor access methods
    
    public static Boolean getConveyorProduced(String conveyorName) {
        AgentRegistry.ConveyorNodes nodes = AgentRegistry.getConveyor(conveyorName);
        if (nodes != null) {
            try {
                Object rawValue = nodes.produced.getValue().getValue().getValue();
                
                // Handle both Boolean and String types from Visual Components
                if (rawValue instanceof Boolean) {
                    return (Boolean) rawValue;
                } else if (rawValue instanceof String) {
                    return Boolean.parseBoolean((String) rawValue);
                }
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }
    
    public static void setConveyorProduced(String conveyorName, boolean produced) {
        AgentRegistry.ConveyorNodes nodes = AgentRegistry.getConveyor(conveyorName);
        if (nodes != null) {
            nodes.produced.setValue(new DataValue(new Variant(produced)));
        }
    }
    
    // Required abstract methods from ManagedNamespace
    @Override
    public void onDataItemsCreated(List<DataItem> dataItems) {}
    
    @Override
    public void onDataItemsModified(List<DataItem> dataItems) {}
    
    @Override
    public void onDataItemsDeleted(List<DataItem> dataItems) {}
    
    @Override
    public void onMonitoringModeChanged(List<MonitoredItem> monitoredItems) {}
}
