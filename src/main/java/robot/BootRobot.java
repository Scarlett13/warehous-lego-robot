package robot;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.util.ExtendedProperties;
import jade.util.leap.Properties;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import robot.agents.TeletubbiesAgent;
import robot.sims.BootSim;
import robot.utils.Hardware;

public class BootRobot {
    public static void main(String[] args) {
        try {
            Runtime rt = Runtime.instance();

            ProfileImpl p = new ProfileImpl();
            p.setParameter(ProfileImpl.MAIN, "true");   // explicit
            p.setParameter(ProfileImpl.GUI,  "false");   // optional GUI
            p.setParameter(Profile.SERVICES,
                    "jade.core.event.NotificationService;jade.core.messaging.TopicManagementService");
            p.setParameter(Profile.MAIN_PORT, "1099");
//            p.setParameter(Profile.MAIN_HOST, "192.168.0.116");
//            p.setParameter(Profile.LOCAL_HOST, "127.0.0.1");
            System.out.println("[Boot] SERVICES=" + p.getParameter(Profile.SERVICES, null));

            BootRobot.start();

            ContainerController cc = rt.createMainContainer(p);
            AgentController robot = cc.createNewAgent("TeletubbiesRobot",
                    TeletubbiesAgent.class.getName(), new Object[]{});
            robot.start();

        } catch (Exception e) { e.printStackTrace(); }
    }

    public static void start(){
        Hardware hardware = new Hardware();
        hardware.init();
    }
}
