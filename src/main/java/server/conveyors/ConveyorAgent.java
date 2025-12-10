package server.conveyors;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import server.NamedInterface;
import server.ServerConfig;
import shared.dto.ConveyorDTO;
import shared.dto.FruitItemDTO;
import shared.dto.Point2D;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ConveyorAgent extends Agent implements NamedInterface {
    private ConveyorDTO conveyorData;

    /*
    * TODO:
    *  1. adding a conveyor max items to handle produce logic and OPCUA
    *  2. handling new fruit produced from UI button click into ACL message
    * */
    @Override
    protected void setup() {
        // Read initialization parameters
        Object[] args = getArguments();
        System.out.println("args: " + Arrays.toString(args));
        if (args != null && args.length > 0) {
            // Example: args[0] = name, args[1] = Position, args[2] = List<ItemDTO>
            String name = (String) args[0];

            double[] position = (double[]) args[1];
            Point2D conveyorPositionPoint = new Point2D(position[0], position[1]);

            List<FruitItemDTO> items = new ArrayList<>();

            this.conveyorData = new ConveyorDTO(name, conveyorPositionPoint);
            conveyorData.getConveyorItems().addAll(items);
            setEnabledO2ACommunication(true, 0);
        } else {
            // Fallback: use agent local name as warehouse name
            String name = getLocalName();
            this.conveyorData = new ConveyorDTO(name, new Point2D(0.0, 0.0));
        }

        System.out.println(getLocalName() + " initialised: " +
                "name=" + conveyorData.getConveyorName() +
                ", pos=(" + conveyorData.getConveyorLocation().toString() +
                ", items=" + conveyorData.getConveyorItems().size());

        // Register in Yellow Pages
        registerInYellowPages();

        addBehaviour(new ConveyorItemAddedBehaviour(this));
    }

    private void registerInYellowPages() {
        try {
            DFAgentDescription dfd = new DFAgentDescription();
            dfd.setName(getAID());

            ServiceDescription sd = new ServiceDescription();
            sd.setType(ServerConfig.CONVEYOR_YELLOW_PAGES_SERVICE);
            sd.setName(conveyorData.getConveyorName());

            dfd.addServices(sd);

//            ServiceDescription sditems = new ServiceDescription();
//            sd.setType("warehouse-conveyor-items");
//            sd.setName(conveyorData.getConveyorName()+"/Items");
//
//            dfd.addServices(sditems);
            DFService.register(this, dfd);

            System.out.println(" " + conveyorData.getConveyorName() + " registered in Yellow Pages");
        } catch (FIPAException e) {
            e.printStackTrace();
        }
    }

    public ConveyorDTO getConveyorData() {
        return conveyorData;
    }



    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
            System.out.println(" " + conveyorData.getConveyorName() + " deregistered from Yellow Pages");
        } catch (FIPAException e) {
            e.printStackTrace();
        }
    }
}
