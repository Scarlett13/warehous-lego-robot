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
            System.out.println("Robot " + robotName + " already registered");
            return;
        }

        System.out.println(" Creating OPC-UA variables for " + robotName);

        UaNodeContext context = getNodeContext();

        // Create folder for this robot (e.g., "Robot1", "Robot2")
        UaFolderNode robotFolder = new UaFolderNode(
                context,
                newNodeId(robotName),
                newQualifiedName(robotName),
                LocalizedText.english(robotName));

        // Register folder
        context.getNodeManager().addNode(robotFolder);

        // Add to Objects folder
        Optional<UaNode> objectsFolder = context.getServer()
                .getAddressSpaceManager()
                .getManagedNode(Identifiers.ObjectsFolder);
        objectsFolder.ifPresent(node -> ((FolderTypeNode) node).addComponent(robotFolder));

        // Create variables for this robot
        UaVariableNode currentTrajectory = createVariable(robotFolder, robotName + "currentTrajectory", "");
        UaVariableNode currentBatteryPercentage = createVariable(robotFolder, robotName + "currentBatteryPercentage",
                robotStatus.getBatteryPct());
        UaVariableNode currentWorkId = createVariable(robotFolder, robotName + "currentWorkId",
                robotStatus.getCurrentWorkId());
        UaVariableNode currentPath = createVariable(robotFolder, robotName + "currentPath", "");
        UaVariableNode targetPath = createVariable(robotFolder, robotName + "targetPath", "");
        UaVariableNode currentSpeed = createVariable(robotFolder, robotName + "currentSpeed", 0);
        UaVariableNode currentState = createVariable(robotFolder, robotName + "currentState",
                robotStatus.getRobotState());
        UaVariableNode ultrasonicSensorReading = createVariable(robotFolder, robotName + "ultrasonicSensorReading",
                robotStatus.getUltrasonicReading() != null ? robotStatus.getUltrasonicReading().getDistance() : 0.0);
        UaVariableNode posX = createVariable(robotFolder, robotName + "posX",
                robotStatus.getRobotPosition() != null ? robotStatus.getRobotPosition().getX() : 0.0);
        UaVariableNode posY = createVariable(robotFolder, robotName + "posY",
                robotStatus.getRobotPosition() != null ? robotStatus.getRobotPosition().getY() : 0.0);
        UaVariableNode yawAngle = createVariable(robotFolder, robotName + "yawAngle",
                robotStatus.getRobotPosition() != null ? (double) robotStatus.getRobotPosition().getAngleDeg() : 0.0);
        UaVariableNode robotNameNode = createVariable(robotFolder, "robotName", robotName);
        UaVariableNode forceArrival = createVariable(robotFolder, "forceArrival", false);

        // Register in registry
        OpcuaNodeRegistry.RobotNodes nodes = new OpcuaNodeRegistry.RobotNodes(
                currentBatteryPercentage,
                currentWorkId,
                currentPath,
                targetPath,
                currentSpeed,
                currentState,
                ultrasonicSensorReading,
                posX,
                posY,
                yawAngle,
                robotNameNode,
                currentTrajectory,
                forceArrival);
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
                LocalizedText.english(conveyorName));

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
        UaVariableNode conveyorItems = createVariable(conveyorFolder, "conveyorItems", "[]");

        // Register in registry
        OpcuaNodeRegistry.ConveyorNodes nodes = new OpcuaNodeRegistry.ConveyorNodes(nextWorkId, totalItems,
                conveyorItems);
        OpcuaNodeRegistry.registerConveyor(conveyorName, nodes);

        System.out.println(" " + conveyorName + " ready (ns=2;s=" + conveyorName + "/*)");
    }

    /**
     * Register a new Fuego robot and create its OPC-UA variables
     */
    public void registerFuegoRobot(String robotName, String opcuaFolderName) throws Exception {
        if (OpcuaNodeRegistry.hasFuegoRobot(robotName)) {
            System.out.println("Fuego Robot " + robotName + " already registered");
            return;
        }

        System.out.println(
                " Creating OPC-UA variables for Fuego Robot: " + robotName + " (Folder: " + opcuaFolderName + ")");

        UaNodeContext context = getNodeContext();

        // Create folder for this robot (Use hardcoded folder name)
        UaFolderNode robotFolder = new UaFolderNode(
                context,
                newNodeId(opcuaFolderName),
                newQualifiedName(opcuaFolderName),
                LocalizedText.english(opcuaFolderName));

        // Register folder
        context.getNodeManager().addNode(robotFolder);

        // Add to Objects folder
        Optional<UaNode> objectsFolder = context.getServer()
                .getAddressSpaceManager()
                .getManagedNode(Identifiers.ObjectsFolder);
        objectsFolder.ifPresent(node -> ((FolderTypeNode) node).addComponent(robotFolder));

        // Create variables (Nodes will be "FolderName/Points")
        UaVariableNode points = createVariable(robotFolder, "Points", "[]");
        UaVariableNode pathId = createVariable(robotFolder, "PathID", "");

        // Register in registry (Key is robotName e.g "Robot1Name")
        OpcuaNodeRegistry.FuegoNodes nodes = new OpcuaNodeRegistry.FuegoNodes(points, pathId);
        OpcuaNodeRegistry.registerFuegoRobot(robotName, nodes);

        System.out.println(" " + robotName + " ready (ns=2;s=" + opcuaFolderName + "/*)");
    }

    /**
     * Register global Graph Configuration node
     */
    public void registerGraphConfig() throws Exception {
        UaNodeContext context = getNodeContext();

        Optional<UaNode> objectsFolder = context.getServer()
                .getAddressSpaceManager()
                .getManagedNode(Identifiers.ObjectsFolder);

        if (objectsFolder.isPresent()) {
            // Fix: Cast to FolderTypeNode instead of UaFolderNode
            FolderTypeNode root = (FolderTypeNode) objectsFolder.get();

            // Create variable directly under Objects folder
            // ns=2;s=GraphConfiguration
            UaVariableNode graphConfig = new UaVariableNode.UaVariableNodeBuilder(getNodeContext())
                    .setNodeId(newNodeId("GraphConfiguration"))
                    .setAccessLevel(AccessLevel.READ_WRITE)
                    .setUserAccessLevel(AccessLevel.READ_WRITE)
                    .setBrowseName(newQualifiedName("GraphConfiguration"))
                    .setDisplayName(LocalizedText.english("GraphConfiguration"))
                    .setDataType(Identifiers.String)
                    .setTypeDefinition(Identifiers.BaseDataVariableType)
                    .build();

            graphConfig.setValue(new DataValue(new Variant("")));

            root.addComponent(graphConfig);
            getNodeManager().addNode(graphConfig);
            graphConfigNode = graphConfig;

            // OccupancyState node
            UaVariableNode occupancyState = new UaVariableNode.UaVariableNodeBuilder(getNodeContext())
                    .setNodeId(newNodeId("OccupancyState"))
                    .setAccessLevel(AccessLevel.READ_WRITE)
                    .setUserAccessLevel(AccessLevel.READ_WRITE)
                    .setBrowseName(newQualifiedName("OccupancyState"))
                    .setDisplayName(LocalizedText.english("OccupancyState"))
                    .setDataType(Identifiers.String)
                    .setTypeDefinition(Identifiers.BaseDataVariableType)
                    .build();

            occupancyState.setValue(new DataValue(new Variant("[]")));
            root.addComponent(occupancyState);
            getNodeManager().addNode(occupancyState);
            occupancyStateNode = occupancyState;

            System.out.println("Registered Global nodes (GraphConfig, OccupancyState)");
        }
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

    public static void setRobotUltrasonicSensorReading(String robotName, double distance) {
        OpcuaNodeRegistry.RobotNodes nodes = OpcuaNodeRegistry.getRobot(robotName);
        if (nodes != null) {
            nodes.ultrasonicSensorReading.setValue(new DataValue(new Variant(distance)));
        }
    }

    public static void setRobotPosition(String robotName, double x, double y, double yaw) {
        OpcuaNodeRegistry.RobotNodes nodes = OpcuaNodeRegistry.getRobot(robotName);
        if (nodes != null) {
            nodes.posX.setValue(new DataValue(new Variant(x)));
            nodes.posY.setValue(new DataValue(new Variant(y)));
            nodes.yawAngle.setValue(new DataValue(new Variant(yaw)));
        }
    }

    public static void setRobotCurrentTrajectory(String robotName, String trajectory) {
        OpcuaNodeRegistry.RobotNodes nodes = OpcuaNodeRegistry.getRobot(robotName);
        if (nodes != null) {
            nodes.currentTrajectory.setValue(new DataValue(new Variant(trajectory)));
        }
    }

    public static void setRobotForceArrival(String robotName, boolean force) {
        OpcuaNodeRegistry.RobotNodes nodes = OpcuaNodeRegistry.getRobot(robotName);
        if (nodes != null) {
            nodes.forceArrival.setValue(new DataValue(new Variant(force)));
        }
    }

    public static boolean getRobotForceArrival(String robotName) {
        OpcuaNodeRegistry.RobotNodes nodes = OpcuaNodeRegistry.getRobot(robotName);
        if (nodes != null) {
            try {
                return (Boolean) nodes.forceArrival.getValue().getValue().getValue();
            } catch (Exception e) {
                return false;
            }
        }
        return false;
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

    public static String getConveyorItemsJson(String conveyorName) {
        OpcuaNodeRegistry.ConveyorNodes nodes = OpcuaNodeRegistry.getConveyor(conveyorName);
        if (nodes != null) {
            try {
                Object v = nodes.conveyorItems.getValue().getValue().getValue();
                return v != null ? (String) v : "[]";
            } catch (Exception e) {
                return "[]";
            }
        }
        return "[]";
    }

    public static void setConveyorItemsJson(String conveyorName, String json) {
        OpcuaNodeRegistry.ConveyorNodes nodes = OpcuaNodeRegistry.getConveyor(conveyorName);
        if (nodes != null) {
            nodes.conveyorItems.setValue(new DataValue(new Variant(json)));
        }
    }

    private static UaVariableNode graphConfigNode;
    private static UaVariableNode occupancyStateNode;

    public static String getGraphConfiguration() {
        if (graphConfigNode != null) {
            try {
                return (String) graphConfigNode.getValue().getValue().getValue();
            } catch (Exception e) {
                return "";
            }
        }
        return "";
    }

    public static String getOccupancyState() {
        if (occupancyStateNode != null) {
            try {
                return (String) occupancyStateNode.getValue().getValue().getValue();
            } catch (Exception e) {
                return "[]";
            }
        }
        return "[]";
    }

    public static double getRobotPositionX(String robotName) {
        OpcuaNodeRegistry.RobotNodes nodes = OpcuaNodeRegistry.getRobot(robotName);
        if (nodes != null) {
            try {
                return (Double) nodes.posX.getValue().getValue().getValue();
            } catch (Exception e) {
                return 0.0;
            }
        }
        return 0.0;
    }

    public static double getRobotPositionY(String robotName) {
        OpcuaNodeRegistry.RobotNodes nodes = OpcuaNodeRegistry.getRobot(robotName);
        if (nodes != null) {
            try {
                return (Double) nodes.posY.getValue().getValue().getValue();
            } catch (Exception e) {
                return 0.0;
            }
        }
        return 0.0;
    }

    public static double getRobotYawAngle(String robotName) {
        OpcuaNodeRegistry.RobotNodes nodes = OpcuaNodeRegistry.getRobot(robotName);
        if (nodes != null) {
            try {
                return (Double) nodes.yawAngle.getValue().getValue().getValue();
            } catch (Exception e) {
                return 0.0;
            }
        }
        return 0.0;
    }

    // Fuego access methods

    public static void setFuegoPoints(String robotName, String pointsJson) {
        OpcuaNodeRegistry.FuegoNodes nodes = OpcuaNodeRegistry.getFuegoRobot(robotName);
        if (nodes != null) {
            nodes.points.setValue(new DataValue(new Variant(pointsJson)));
        }
    }

    public static void setFuegoPathId(String robotName, String pathId) {
        OpcuaNodeRegistry.FuegoNodes nodes = OpcuaNodeRegistry.getFuegoRobot(robotName);
        if (nodes != null) {
            nodes.pathId.setValue(new DataValue(new Variant(pathId)));
        }
    }

    public static String getFuegoPoints(String robotName) {
        OpcuaNodeRegistry.FuegoNodes nodes = OpcuaNodeRegistry.getFuegoRobot(robotName);
        if (nodes != null) {
            try {
                Object v = nodes.points.getValue().getValue().getValue();
                return v != null ? (String) v : "[]";
            } catch (Exception e) {
                return "[]";
            }
        }
        return "[]";
    }

    public static String getFuegoPathId(String robotName) {
        OpcuaNodeRegistry.FuegoNodes nodes = OpcuaNodeRegistry.getFuegoRobot(robotName);
        if (nodes != null) {
            try {
                Object v = nodes.pathId.getValue().getValue().getValue();
                return v != null ? (String) v : "";
            } catch (Exception e) {
                return "";
            }
        }
        return "";
    }

    @Override
    public void onDataItemsCreated(List<DataItem> dataItems) {
    }

    @Override
    public void onDataItemsModified(List<DataItem> dataItems) {
    }

    @Override
    public void onDataItemsDeleted(List<DataItem> dataItems) {
    }

    @Override
    public void onMonitoringModeChanged(List<MonitoredItem> monitoredItems) {
    }
}
