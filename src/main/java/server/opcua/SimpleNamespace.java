package server.opcua;

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
import server.ServerConfig;
import shared.dto.RobotStatusDTO;

import java.util.List;
import java.util.Optional;


public class SimpleNamespace extends ManagedNamespace {
    
    public static final String URI = ServerConfig.NAMESPACE_URI;
    
    public SimpleNamespace(OpcUaServer server) throws Exception {
        super(server, URI);
    }
    
    /**
     * Register a new robot and create its OPC-UA variables
     */
    public void registerRobot(String robotName, RobotStatusDTO robotStatus) throws Exception {
        if (OpcuaNodeRegistry.hasRobot(robotName)) {
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
        UaVariableNode currentBatteryPercentage = createVariable(robotFolder, "currentBatteryPercentage", robotStatus.getBatteryPct());
        UaVariableNode currentWorkId = createVariable(robotFolder, "currentWorkId", robotStatus.getCurrentWorkId());
        UaVariableNode currentPath = createVariable(robotFolder, "currentPath", "");
        UaVariableNode targetPath = createVariable(robotFolder, "targetPath", "");
        UaVariableNode currentSpeed = createVariable(robotFolder, "currentSpeed", 0);
        UaVariableNode currentState = createVariable(robotFolder, "currentState", "STANDBY");

        
        // Register in registry
        OpcuaNodeRegistry.RobotNodes nodes = new OpcuaNodeRegistry.RobotNodes(
                currentBatteryPercentage,
                currentWorkId,
                currentPath,
                targetPath,
                currentSpeed,
                currentState
        );
        OpcuaNodeRegistry.registerRobot(robotName, nodes);
        
        System.out.println(" " + robotName + " ready (ns=2;s=" + robotName + "/*)");
    }
    
    /**
     * Register a new conveyor and create its OPC-UA variables
     */
    public void registerConveyor(String conveyorName) throws Exception {
        if (OpcuaNodeRegistry.hasConveyor(conveyorName)) {
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
        UaVariableNode totalItems = createVariable(conveyorFolder, "totalItems", 0);
        UaVariableNode nextWorkId = createVariable(conveyorFolder, "nextWorkId", "");
        
        // Register in registry
        OpcuaNodeRegistry.ConveyorNodes nodes = new OpcuaNodeRegistry.ConveyorNodes(nextWorkId, totalItems);
        OpcuaNodeRegistry.registerConveyor(conveyorName, nodes);
        
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

    public static void setRobotCurrentBatteryPercentage(String robotName, int batteryPct) {
        OpcuaNodeRegistry.RobotNodes nodes = OpcuaNodeRegistry.getRobot(robotName);
        if (nodes != null) {
            nodes.currentBatteryPercentage.setValue(new DataValue(new Variant(batteryPct)));
        }
    }

    public static void setRobotCurrentWorkId(String robotName, String workId) {
        OpcuaNodeRegistry.RobotNodes nodes = OpcuaNodeRegistry.getRobot(robotName);
        if (nodes != null) {
            nodes.currentWorkId.setValue(new DataValue(new Variant(workId)));
        }
    }

    public static void setRobotCurrentPath(String robotName, String path) {
        OpcuaNodeRegistry.RobotNodes nodes = OpcuaNodeRegistry.getRobot(robotName);
        if (nodes != null) {
            nodes.currentPath.setValue(new DataValue(new Variant(path)));
        }
    }

    public static void setRobotTargetPath(String robotName, String path) {
        OpcuaNodeRegistry.RobotNodes nodes = OpcuaNodeRegistry.getRobot(robotName);
        if (nodes != null) {
            nodes.targetPath.setValue(new DataValue(new Variant(path)));
        }
    }

    public static void setRobotCurrentSpeed(String robotName, double speed) {
        OpcuaNodeRegistry.RobotNodes nodes = OpcuaNodeRegistry.getRobot(robotName);
        if (nodes != null) {
            nodes.currentSpeed.setValue(new DataValue(new Variant(speed)));
        }
    }

    public static void setRobotCurrentState(String robotName, String state) {
        OpcuaNodeRegistry.RobotNodes nodes = OpcuaNodeRegistry.getRobot(robotName);
        if (nodes != null) {
            nodes.currentState.setValue(new DataValue(new Variant(state)));
        }
    }


    // ==== GETTERS ==== //

    public static int getRobotCurrentBatteryPercentage(String robotName) {
        OpcuaNodeRegistry.RobotNodes nodes = OpcuaNodeRegistry.getRobot(robotName);
        if (nodes != null) {
            try {
                return (Integer) nodes.currentBatteryPercentage
                        .getValue().getValue().getValue();
            } catch (Exception e) {
                return ServerConfig.INITIAL_BATTERY;
            }
        }
        return ServerConfig.INITIAL_BATTERY;
    }

    public static String getRobotCurrentWorkId(String robotName) {
        OpcuaNodeRegistry.RobotNodes nodes = OpcuaNodeRegistry.getRobot(robotName);
        if (nodes != null) {
            try {
                Object v = nodes.currentWorkId.getValue().getValue().getValue();
                return v != null ? (String) v : "";
            } catch (Exception e) {
                return "";
            }
        }
        return "";
    }

    public static String getRobotCurrentPath(String robotName) {
        OpcuaNodeRegistry.RobotNodes nodes = OpcuaNodeRegistry.getRobot(robotName);
        if (nodes != null) {
            try {
                Object v = nodes.currentPath.getValue().getValue().getValue();
                return v != null ? (String) v : "";
            } catch (Exception e) {
                return "";
            }
        }
        return "";
    }

    public static String getRobotTargetPath(String robotName) {
        OpcuaNodeRegistry.RobotNodes nodes = OpcuaNodeRegistry.getRobot(robotName);
        if (nodes != null) {
            try {
                Object v = nodes.targetPath.getValue().getValue().getValue();
                return v != null ? (String) v : "";
            } catch (Exception e) {
                return "";
            }
        }
        return "";
    }

    public static double getRobotCurrentSpeed(String robotName) {
        OpcuaNodeRegistry.RobotNodes nodes = OpcuaNodeRegistry.getRobot(robotName);
        if (nodes != null) {
            try {
                return (Double) nodes.currentSpeed.getValue().getValue().getValue();
            } catch (Exception e) {
                return 0.0;
            }
        }
        return 0.0;
    }

    public static String getRobotCurrentState(String robotName) {
        OpcuaNodeRegistry.RobotNodes nodes = OpcuaNodeRegistry.getRobot(robotName);
        if (nodes != null) {
            try {
                Object v = nodes.currentState.getValue().getValue().getValue();
                return v != null ? (String) v : "";
            } catch (Exception e) {
                return "";
            }
        }
        return "";
    }


    // Conveyor access methods

    // nextitemid: ID/index of the next item on the conveyor
    public static String getConveyorNextItemId(String conveyorName) {
        OpcuaNodeRegistry.ConveyorNodes nodes = OpcuaNodeRegistry.getConveyor(conveyorName);
        if (nodes != null) {
            try {
                try {
                    Object v = nodes.nextitemid.getValue().getValue().getValue();
                    return v != null ? (String) v : "";
                } catch (Exception e) {
                    return "";
                }
            } catch (Exception e) {
                return ""; // default if anything goes wrong
            }
        }
        return "";
    }

    public static void setConveyorNextItemId(String conveyorName, String nextItemId) {
        OpcuaNodeRegistry.ConveyorNodes nodes = OpcuaNodeRegistry.getConveyor(conveyorName);
        if (nodes != null) {
            nodes.nextitemid.setValue(new DataValue(new Variant(nextItemId)));
        }
    }


    // totalitems: total number of items processed/seen by this conveyor
    public static int getConveyorTotalItems(String conveyorName) {
        OpcuaNodeRegistry.ConveyorNodes nodes = OpcuaNodeRegistry.getConveyor(conveyorName);
        if (nodes != null) {
            try {
                Object rawValue = nodes.totalitems.getValue().getValue().getValue();

                if (rawValue instanceof Number) {
                    return ((Number) rawValue).intValue();
                } else if (rawValue instanceof String) {
                    return Integer.parseInt((String) rawValue);
                }
            } catch (Exception e) {
                return 0; // default
            }
        }
        return 0;
    }

    public static void setConveyorTotalItems(String conveyorName, int totalItems) {
        OpcuaNodeRegistry.ConveyorNodes nodes = OpcuaNodeRegistry.getConveyor(conveyorName);
        if (nodes != null) {
            nodes.totalitems.setValue(new DataValue(new Variant(totalItems)));
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
