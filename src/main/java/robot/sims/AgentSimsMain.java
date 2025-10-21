package robot.sims;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;

public class AgentSimsMain {
    public static void main(String[] args) throws StaleProxyException {
        Runtime rt = Runtime.instance();
        Profile p = new ProfileImpl();
        AgentContainer mainContainer = rt.createMainContainer(p);

        // Start one UltrasonicAgent
        AgentController ac = mainContainer.createNewAgent(
                "ultrasonic-1",
                "robot.agents.UltrasonicAgent",
                new Object[]{} // you can pass params here later
        );

        ac.start();
    }
}
