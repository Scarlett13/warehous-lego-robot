package robot.sims;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import robot.agents.TeletubbiesAgent;


public class BootSim {
    public static void main(String[] args) {
        try {
            Runtime rt = Runtime.instance();

            ProfileImpl p = new ProfileImpl();
            p.setParameter(ProfileImpl.MAIN, "true");   // explicit
            p.setParameter(ProfileImpl.GUI,  "true");   // optional GUI
            p.setParameter(Profile.SERVICES,
                    "jade.core.event.NotificationService;jade.core.messaging.TopicManagementService");
            p.setParameter(Profile.MAIN_PORT, "1099");
            System.out.println("[Boot] SERVICES=" + p.getParameter(Profile.SERVICES, null));

            ContainerController cc = rt.createMainContainer(p);
            AgentController robot = cc.createNewAgent("TeletubbiesRobot",
                    TeletubbiesAgent.class.getName(), new Object[]{});
            robot.start();

        } catch (Exception e) { e.printStackTrace(); }
    }
}