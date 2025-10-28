package example.java.Connexion;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.util.ExtendedProperties;
import jade.util.leap.Properties;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import robot.BootRobot;
import robot.agents.TeletubbiesAgent;

public class container2 {
    public static void main(String[] args) {

        /*String[] bootOptions = new String[7];
        bootOptions[0] = "-gui";
        bootOptions[1] = "-local-port";
        bootOptions[2] = "1099";
        bootOptions[3] = "-container-name";
        bootOptions[4] = "Launch container";
        bootOptions[5] = "-agents";
        bootOptions[6] = "Agent2:Connexion.Agent2";
        jade.Boot.main(bootOptions);

*/
        try {
            Runtime runtime = Runtime.instance();
            String target ="192.168.0.116";
            String source ="192.168.0.222";
            ProfileImpl p = new ProfileImpl();
            p.setParameter(Profile.MAIN_HOST, target);

            p.setParameter(Profile.LOCAL_HOST,source);
            p.setParameter(Profile.LOCAL_PORT,"1099");

            AgentContainer agentContainer=runtime.createAgentContainer(p);
            start();

           // Properties properties = new ExtendedProperties();
           // properties.setProperty(Profile.GUI, "true");
      //      properties.
            //Profile profile = new ProfileImpl(properties);
            AgentController agent2=agentContainer.createNewAgent("Agent1",
                    "example.java.Connexion.Agent1",new Object[]{});
            agent2.start();

//            Runtime rt = Runtime.instance();
//
//            ProfileImpl p = new ProfileImpl();
//            p.setParameter(ProfileImpl.MAIN, "true");   // explicit
//            p.setParameter(ProfileImpl.GUI,  "false");   // optional GUI
//            p.setParameter(Profile.SERVICES,
//                    "jade.core.event.NotificationService;jade.core.messaging.TopicManagementService");
//            p.setParameter(Profile.MAIN_PORT, "1099");
////            p.setParameter(Profile.MAIN_HOST, "192.168.0.116");
////            p.setParameter(Profile.LOCAL_HOST, "127.0.0.1");
//            System.out.println("[Boot] SERVICES=" + p.getParameter(Profile.SERVICES, null));
//
//            BootRobot.start();
//
//            ContainerController cc = rt.createMainContainer(p);
//            AgentController robot = cc.createNewAgent("TeletubbiesRobot",
//                    "example.java.Connexion.Agent1", new Object[]{});
//            robot.start();

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static void start() {
        Device2.init();
    }
}
