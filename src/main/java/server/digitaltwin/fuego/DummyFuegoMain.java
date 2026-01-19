package server.digitaltwin.fuego;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import server.digitaltwin.vrl.DummyVrlRobotAgent;

public class DummyFuegoMain {
    public static void main(String[] args) {
        try {
            jade.core.Runtime rt = Runtime.instance();

            ProfileImpl p = new ProfileImpl();
            p.setParameter(ProfileImpl.MAIN, "false"); // explicit
            p.setParameter(ProfileImpl.GUI, "true"); // optional GUI
            p.setParameter(Profile.LOCAL_PORT, "1100");
            p.setParameter(Profile.LOCAL_HOST, server.ServerConfig.MAIN_HOST_IP);
            p.setParameter(Profile.SERVICES,
                    "jade.core.event.NotificationService;jade.core.messaging.TopicManagementService");
            p.setParameter(Profile.MAIN_PORT, "1099");
            p.setParameter(Profile.MAIN_HOST, server.ServerConfig.MAIN_HOST_IP);

            System.out.println("[Boot] SERVICES=" + p.getParameter(Profile.SERVICES, null));

            ContainerController cc = rt.createAgentContainer(p);
            AgentController robot = cc.createNewAgent("Robot1",
                    FuegoDTAgent.class.getName(), new Object[] {});
            robot.start();
        } catch (Exception e) {
        }
    }
}
