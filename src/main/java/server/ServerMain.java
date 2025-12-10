package server;

import example.mas.Config;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import server.agentmonitor.DigitalTwinAgentManager;
import server.opcua.OPCUAServer;
import server.opcua.SimpleNamespace;
import server.ui.MonitorInterfaceAgent;
import server.ui.MonitorUI;

import javax.swing.*;

import static server.ServerConfig.CONVEYOR_LOCATIONS;

public class ServerMain {

    public static void main(String[] args) {
        System.out.println("=".repeat(60));
        System.out.println("  WAREHOUSE SERVER SYSTEM");
        System.out.println("=".repeat(60));

        try {
            // 1. Start OPC-UA Server
            System.out.println("\n[1] Starting OPC-UA Server...");
            OPCUAServer.start();
            SimpleNamespace namespace = OPCUAServer.getNamespace();

            // 4. Start JADE Platform
            System.out.println("\n[4] Starting JADE Platform...");
            Runtime runtime = Runtime.instance();
            ProfileImpl profile = new ProfileImpl();
            profile.setParameter(ProfileImpl.MAIN, "true");
            profile.setParameter(Profile.LOCAL_PORT, "1099");
            profile.setParameter(ProfileImpl.GUI, "true"); // Show JADE GUI
            profile.setParameter(Profile.SERVICES,
                    "jade.core.event.NotificationService;jade.core.messaging.TopicManagementService");

            ContainerController container = runtime.createMainContainer(profile);

            AgentController agentMonitor = container.createNewAgent("dt-agent-monitor",
                    DigitalTwinAgentManager.class.getName(), new Object[]{});
            agentMonitor.start();

            AgentController agentUiMonitor = container.createNewAgent("agent-ui-monitor",
                    MonitorInterfaceAgent.class.getName(), new Object[]{});
            agentUiMonitor.start();

            // registering conveyor agent and opcua nodes
            for (String conveyorName : Config.CONVEYOR_NAMES) {
                namespace.registerConveyor(conveyorName);

                Object[] agentArgs = new Object[]{conveyorName, CONVEYOR_LOCATIONS.get(conveyorName)};
                AgentController agent = container.createNewAgent(
                        conveyorName,
                        "server.conveyors.ConveyorAgent",
                        agentArgs
                );
                agent.start();
                System.out.println("    Started " + conveyorName + " agent");
            }

            // 5. Start Conveyor Agents
            System.out.println("\n[5] Starting Conveyor Agents...");

            System.out.println("\n" + "=".repeat(60));
            System.out.println("  SYSTEM READY");
            System.out.println("=".repeat(60));



        } catch (Exception e) {
            System.err.println("CRITICAL ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
