package robot;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import robot.agents.TeletubbiesAgent;
import robot.hardware.MotorHardware;
import robot.hardware.UltrasonicHardware;

import static robot.RobotConstants.JADE_HOST_ADDRESS;
import static robot.RobotConstants.ROBOT_NAME;

public class BootRobot {
    public static void main(String[] args) {
        try{
            Runtime rt = Runtime.instance();

            ProfileImpl p = new ProfileImpl();
            p.setParameter(ProfileImpl.MAIN, "false");   // explicit
            p.setParameter(ProfileImpl.GUI,  "true");   // optional GUI
            p.setParameter(Profile.LOCAL_PORT, "1100");
            p.setParameter(Profile.SERVICES,
                    "jade.core.event.NotificationService;jade.core.messaging.TopicManagementService");
            p.setParameter(Profile.MAIN_PORT, "1099");
            p.setParameter(Profile.MAIN_HOST, JADE_HOST_ADDRESS);

//            p.setParameter(Profile.MAIN_HOST, "192.168.0.116");
            System.out.println("[Boot] SERVICES=" + p.getParameter(Profile.SERVICES, null));

//            Runtime rt = Runtime.instance();

//            ProfileImpl p = new ProfileImpl();
//            p.setParameter(ProfileImpl.MAIN, "true");   // explicit
//            p.setParameter(ProfileImpl.GUI,  "false");   // optional GUI
//            p.setParameter(Profile.SERVICES,
//                    "jade.core.event.NotificationService;jade.core.messaging.TopicManagementService");
//            p.setParameter(Profile.MAIN_PORT, "1099");
////            p.setParameter(Profile.MAIN_HOST, "192.168.0.116");
////            p.setParameter(Profile.LOCAL_HOST, "127.0.0.1");
//            System.out.println("[Boot] SERVICES=" + p.getParameter(Profile.SERVICES, null));

            if (!RobotConstants.IS_SIMS) {
                BootRobot.start();
            }

            ContainerController cc = rt.createAgentContainer(p);
            AgentController robot = cc.createNewAgent(ROBOT_NAME,
                    TeletubbiesAgent.class.getName(), new Object[]{});
            robot.start();
        }catch(Exception e){}
    }

//    public static void boot(){
//        try{
//            Runtime rt = Runtime.instance();
//
//            ProfileImpl p = new ProfileImpl();
//            p.setParameter(ProfileImpl.MAIN, "false");   // explicit
//            p.setParameter(ProfileImpl.GUI,  "false");   // optional GUI
//            p.setParameter(Profile.SERVICES,
//                    "jade.core.event.NotificationService;jade.core.messaging.TopicManagementService");
//            p.setParameter(Profile.MAIN_PORT, "1099");
//            p.setParameter(Profile.MAIN_HOST, "192.168.0.157");
//
////            p.setParameter(Profile.MAIN_HOST, "192.168.0.116");
//            p.setParameter(Profile.LOCAL_PORT, "1100");
//            System.out.println("[Boot] SERVICES=" + p.getParameter(Profile.SERVICES, null));
//
//            BootRobot.start();
//
//            ContainerController cc = rt.createAgentContainer(p);
//            AgentController robot = cc.createNewAgent("TeletubbiesRobot",
//                    TeletubbiesAgent.class.getName(), new Object[]{});
//            robot.start();
//        }catch(Exception e){}
//
//    }
//
//    public static void bootSim(){
//        try {
//            Runtime rt = Runtime.instance();
//
//            ProfileImpl p = new ProfileImpl();
//            p.setParameter(ProfileImpl.MAIN, "false");   // explicit
//            p.setParameter(ProfileImpl.GUI,  "false");   // optional GUI
//            p.setParameter(Profile.SERVICES,
//                    "jade.core.event.NotificationService;jade.core.messaging.TopicManagementService");
//            p.setParameter(Profile.MAIN_PORT, "1099");
//            p.setParameter(Profile.MAIN_HOST, "192.168.0.157");
//            System.out.println("[Boot] SERVICES=" + p.getParameter(Profile.SERVICES, null));
//            p.setParameter(Profile.LOCAL_PORT, "1100");
//
//            ContainerController cc = rt.createAgentContainer(p);
//            AgentController robot = cc.createNewAgent("TeletubbiesRobot",
//                    TeletubbiesAgent.class.getName(), new Object[]{});
//            robot.start();
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }

    public static void start(){
        UltrasonicHardware.init();
        MotorHardware.init();
    }
}
