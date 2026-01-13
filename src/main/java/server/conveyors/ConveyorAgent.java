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
     * 1. adding a conveyor max items to handle produce logic and OPCUA
     * 2. handling new fruit produced from UI button click into ACL message
     */
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

            this.conveyorData = new ConveyorDTO(name, conveyorPositionPoint);

            // Generate 10 random items ONLY for Input Location
            if (name.equals("Input Location")) {
                List<FruitItemDTO> items = new ArrayList<>();
                for (int i = 0; i < 10; i++) {
                    String itemId = "Fruit" + i;
                    int freshness = new java.util.Random().nextInt(10) + 1;
                    items.add(new FruitItemDTO(name, itemId, System.currentTimeMillis(), freshness));
                }
                conveyorData.getConveyorItems().addAll(items);
                conveyorData.sortItems();
            }
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

        // addBehaviour(new ConveyorItemAddedBehaviour(this)); // Deprecated
        addBehaviour(new ConveyorItemRequestResponderBehaviour(this));
        addBehaviour(new ConveyorItemUpdateBehaviour(this));

        // Initial OPC UA Update
        updateOpcuaStatus();
    }

    public void updateOpcuaStatus() {
        // Simple logic: Count READY items, and find the first READY item ID
        long readyCount = conveyorData.getConveyorItems().stream()
                .filter(i -> i.getStatus() == FruitItemDTO.FruitItemWorkStatusEnum.READY)
                .count();

        java.util.Optional<FruitItemDTO> nextItem = conveyorData.getConveyorItems().stream()
                .filter(i -> i.getStatus() == FruitItemDTO.FruitItemWorkStatusEnum.READY)
                .findFirst(); // Assuming list is sorted or we just take first

        String nextId = nextItem.map(FruitItemDTO::getItemId).orElse("");

        server.opcua.SimpleNamespace.setConveyorTotalItems(conveyorData.getConveyorName(), (int) readyCount);
        server.opcua.SimpleNamespace.setConveyorNextItemId(conveyorData.getConveyorName(), nextId);

        // Update Items List JSON
        String itemsJson = shared.utils.JsonUtil.toJson(conveyorData.getConveyorItems());
        server.opcua.SimpleNamespace.setConveyorItemsJson(conveyorData.getConveyorName(), itemsJson);
    }

    private void registerInYellowPages() {
        try {
            DFAgentDescription dfd = new DFAgentDescription();
            dfd.setName(getAID());

            ServiceDescription sd = new ServiceDescription();
            sd.setType(ServerConfig.CONVEYOR_YELLOW_PAGES_SERVICE);
            sd.setName(conveyorData.getConveyorName());

            dfd.addServices(sd);

            // ServiceDescription sditems = new ServiceDescription();
            // sd.setType("warehouse-conveyor-items");
            // sd.setName(conveyorData.getConveyorName()+"/Items");
            //
            // dfd.addServices(sditems);
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
