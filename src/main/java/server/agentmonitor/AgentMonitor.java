package server.agentmonitor;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.AMSService;
import jade.domain.FIPAAgentManagement.AMSAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import server.digitaltwin.teletubbies.TeletubbiesDTAgent;

import java.util.HashSet;
import java.util.Set;

@Deprecated
public class AgentMonitor extends Agent {

    private final Set<String> known = new HashSet<>();

    @Override
    protected void setup() {
        // Prime the cache once
        refreshKnown();

        addBehaviour(new TickerBehaviour(this, 1000) { // every 1s
            @Override
            protected void onTick() {
                try {
                    SearchConstraints sc = new SearchConstraints();
                    sc.setMaxResults(-1L);

                    AMSAgentDescription template = new AMSAgentDescription();
                    AMSAgentDescription[] res = AMSService.search(myAgent, template, sc);

                    Set<String> current = new HashSet<>();
                    for (AMSAgentDescription d : res) {
                        AID aid = d.getName();
                        if (aid == null) continue;

                        String local = aid.getLocalName();

                        boolean shouldlog = JadeAgentFilter.shouldLogAgent(local);
                        if(!shouldlog) continue;

                        current.add(local);

                        if (!known.contains(local)) {
                            System.out.println("[NEW AGENT] " + local);
                        }
                    }

                    // Detect removals too (optional)
                    for (String old : known) {
                        boolean shouldlog = JadeAgentFilter.shouldLogAgent(old);
                        if(!shouldlog) continue;

                        if (!current.contains(old)) {
                            System.out.println("[AGENT LEFT] " + old);
                        }
                    }

                    known.clear();
                    known.addAll(current);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void addNewAgent(ContainerController cc, String name) throws java.lang.Exception {
        AgentController robot = cc.createNewAgent(name,
                TeletubbiesDTAgent.class.getName(), new Object[]{});
        robot.start();
    }

    private void refreshKnown() {
        try {
            SearchConstraints sc = new SearchConstraints();
            sc.setMaxResults(-1L);

            AMSAgentDescription template = new AMSAgentDescription();
            AMSAgentDescription[] res = AMSService.search(this, template, sc);

            for (AMSAgentDescription d : res) {
                if (d.getName() != null) {
                    known.add(d.getName().getLocalName());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
