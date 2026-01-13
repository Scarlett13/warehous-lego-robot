package robot.sims;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;

public class UiSimulation {
    public static void main(String[] args) throws Exception {
        // 1) Connect to the already running main container (Robot project)
        Runtime rt = Runtime.instance();
        Profile p = new ProfileImpl();
// point to the running main container:
        p.setParameter(Profile.MAIN_HOST, "127.0.0.1");
        p.setParameter(Profile.MAIN_PORT, "1099");
        p.setParameter(Profile.SERVICES,
                "jade.core.event.NotificationService;jade.core.messaging.TopicManagementService");

// avoid local port collision (important on same host)
        p.setParameter(Profile.LOCAL_PORT, "1101"); // any free port ≠ 1099

        ContainerController container = rt.createAgentContainer(p);
        AgentController server = container.createNewAgent("serverui", "robot.sims.DestinationSenderSim", new Object[]{});
        server.start();
    }
}
