package server.ui;

import jade.core.Agent;

import javax.swing.*;

public class MonitorInterfaceAgent extends Agent {
    private MonitorUI ui;

    @Override
    protected void setup() {
        super.setup();

        this.ui = new MonitorUI(this);
        SwingUtilities.invokeLater(() -> {
            ui.setVisible(true);
        });

        addBehaviour(new MonitorInterfaceBehaviour(this, ui));
    }
}
