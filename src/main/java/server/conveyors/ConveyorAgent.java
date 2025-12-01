package server.conveyors;

import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import server.NamedInterface;
import shared.dto.ConveyorDTO;
import shared.dto.FruitItemDTO;
import shared.dto.Point2D;

import java.util.List;

public class ConveyorAgent extends Agent implements NamedInterface {
    private ConveyorDTO conveyorData;

    @Override
    protected void setup() {
        // Read initialization parameters
        Object[] args = getArguments();
        if (args != null && args.length > 0) {
            // Example: args[0] = name, args[1] = Position, args[2] = List<ItemDTO>
            String name = (String) args[0];
            Point2D position = (Point2D) args[1];
            @SuppressWarnings("unchecked")
            List<FruitItemDTO> items = (List<FruitItemDTO>) args[2];

            this.conveyorData = new ConveyorDTO(name, position);
            if (items != null) {
                conveyorData.getConveyorItems().addAll(items);
            }
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
            sd.setType("warehouse-conveyor");
            sd.setName(conveyorData.getConveyorName());

            dfd.addServices(sd);

            ServiceDescription sditems = new ServiceDescription();
            sd.setType("warehouse-conveyor-items");
            sd.setName(conveyorData.getConveyorName()+"/Items");

            dfd.addServices(sditems);
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
