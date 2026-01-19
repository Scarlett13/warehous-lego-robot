package server.digitaltwin.fuego;

import jade.core.Agent;

public class FuegoPhysicalDummyAgent extends Agent {
    @Override
    protected void setup() {
        System.out.println("Fuego Physical Dummy Robot started: " + getLocalName());
    }
}
