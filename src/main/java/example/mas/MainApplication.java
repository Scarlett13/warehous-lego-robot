package example.mas;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;

import javax.swing.SwingUtilities;

import static robot.RobotConstants.IS_SIMS;

/**
 * MAIN APPLICATION - Start the Multi-Agent System
 * 
 * This application:
 * 1. Starts the OPC-UA server
 * 2. Registers robots dynamically
 * 3. Spawns JADE agents for each robot
 * 4. Launches monitoring UI
 *
 */
public class MainApplication {
    
    private static SimpleNamespace namespace;
    
    public static void main(String[] args) {
        System.out.println("=".repeat(60));
        System.out.println("  MULTI-ROBOT SYSTEM");
        System.out.println("=".repeat(60));
        
        // Step 0: Show Configuration Dialog
        System.out.println("\n Opening Configuration Dialog...");
        SwingUtilities.invokeLater(() -> {
            boolean proceed = ConfigDialog.showDialog();
            if (!proceed) {
                System.out.println("\n Configuration cancelled. Exiting...");
                System.exit(0);
            } else {
                System.out.println("\n Configuration accepted. Starting system...");
                startSystem();
            }
        });
    }
    
    private static void startSystem() {
        try {
            // Step 1: Start OPC-UA Server
            System.out.println("\n Starting OPC-UA Server...");
            OPCUAServer.start();
            
            // Step 2: Get namespace reference
            namespace = OPCUAServer.getNamespace();
            
            // Step 3: Register all robots from config
            System.out.println("\n Registering robots...");
            for (String robotName : Config.ROBOT_NAMES) {
                namespace.registerRobot(robotName);
            }
            
            // Step 3b: Register all conveyors from config
            System.out.println("\n Registering conveyors...");
            for (String conveyorName : Config.CONVEYOR_NAMES) {
                namespace.registerConveyor(conveyorName);
            }
            
            // Step 4: Start JADE platform
            System.out.println("\n Starting JADE Multi-Agent System...");
            Runtime runtime = Runtime.instance();
            ProfileImpl profile = new ProfileImpl();
//            profile.setParameter(ProfileImpl.MAIN_HOST, "localhost");
            profile.setParameter(ProfileImpl.MAIN, "true");
            profile.setParameter(Profile.LOCAL_PORT, "1099");
            profile.setParameter(ProfileImpl.GUI, "true"); // Show JADE GUI
            profile.setParameter(Profile.SERVICES,
                    "jade.core.event.NotificationService;jade.core.messaging.TopicManagementService");

            ContainerController container = runtime.createMainContainer(profile);
            
            // Step 5: Create agent for each robot
            System.out.println("\n Spawning robot agents...");
            if (IS_SIMS){
                for (String robotName : Config.ROBOT_NAMES) {
                    Object[] agentArgs = new Object[]{robotName};
                    AgentController agent = container.createNewAgent(
                            robotName,
                            "example.mas.RobotAgent",
                            agentArgs
                    );
                    agent.start();
                    System.out.println("    Started " + robotName + " agent");
                }
            }

            // Step 6: Create agent for each conveyor
            System.out.println("\n Spawning conveyor agents...");
            for (String conveyorName : Config.CONVEYOR_NAMES) {
                Object[] agentArgs = new Object[]{conveyorName};
                AgentController agent = container.createNewAgent(
                    conveyorName, 
                    "example.mas.ConveyorAgent", 
                    agentArgs
                );
                agent.start();
                System.out.println("    Started " + conveyorName + " agent");
            }
            
            System.out.println("\n" + "=".repeat(60));
            System.out.println(" SYSTEM IS RUNNING - " + Config.ROBOT_NAMES.size() + " ROBOTS + " + Config.CONVEYOR_NAMES.size() + " CONVEYORS ACTIVE");
            System.out.println("=".repeat(60));
            System.out.println("\n️  In Visual Components:");
            System.out.println("   1. Connect to: opc.tcp://localhost:4840");
            System.out.println("   2. Pair properties for each robot:");

            for (String robotName : Config.ROBOT_NAMES) {
                System.out.println("      - " + robotName + "/Location (Simulation → Server)");
                System.out.println("      - " + robotName + "/Target (Server → Simulation)");
                System.out.println("      - " + robotName + "/BatteryLevel (Server → Simulation)");
                System.out.println("      - " + robotName + "/HasProduct (Server → Simulation)");
            }

            System.out.println("\n   3. Pair properties for each conveyor:");
            for (String conveyorName : Config.CONVEYOR_NAMES) {
                System.out.println("      - " + conveyorName + "/Produced (Simulation → Server)");
            }

            System.out.println("=".repeat(60) + "\n");
            
            // Step 7: Launch Monitoring UI
            System.out.println("🖥  Launching Monitoring UI...\n");
            SwingUtilities.invokeLater(() -> {
                MonitorUI ui = new MonitorUI();
                ui.setVisible(true);
            });
            
        } catch (Exception e) {
            System.err.println(" Error starting system: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
