package robot.sims;

import jade.core.AID;
import jade.core.Agent;
import jade.lang.acl.ACLMessage;
import robot.dto.DestinationDTO;
import robot.utils.JsonUtil;
import robot.utils.TopicHelper;

import javax.swing.*;
import java.awt.*;

import static robot.Constants.DESTINATION_TOPIC;

public class DestinationSenderSim extends Agent {

    private JTextField xField;
    private JTextField yField;
    private AID topicAID;

    @Override
    protected void setup() {
        System.out.println(getLocalName() + " starting…");
        try {
            // For a publisher you only need the AID; no register() needed
            topicAID = TopicHelper.topic(this, DESTINATION_TOPIC );
        } catch (Exception e) {
            e.printStackTrace();
            doDelete();
            return;
        }
        SwingUtilities.invokeLater(this::buildUi);
    }

    private void buildUi() {
        JFrame f = new JFrame("Destination Publisher");
        f.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        f.addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent e) { doDelete(); }
        });

        JPanel root = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6,6,6,6);
        c.anchor = GridBagConstraints.WEST;

        // Row 0: Label + Field (Destination X)
        c.gridx=0; c.gridy=0;
        root.add(new JLabel("Destination X:"), c);
        xField = new JTextField(12);
        c.gridx=1;
        root.add(xField, c);

        // Row 1: Label + Field (Destination Y)
        c.gridx=0; c.gridy=1;
        root.add(new JLabel("Destination Y:"), c);
        yField = new JTextField(12);
        c.gridx=1;
        root.add(yField, c);

        // Row 2: Button
        JButton sendBtn = new JButton("Send Data");
        sendBtn.addActionListener(ev -> onSend());
        c.gridx=0; c.gridy=2; c.gridwidth=2; c.anchor=GridBagConstraints.CENTER;
        root.add(sendBtn, c);

        f.getContentPane().add(root);
        f.pack();
        f.setLocationByPlatform(true);
        f.setResizable(false);
        f.setVisible(true);
    }

    private void onSend() {
        try {
            double x = Double.parseDouble(xField.getText().trim());
            double y = Double.parseDouble(yField.getText().trim());
            long timemillis = System.currentTimeMillis();
            DestinationDTO dto = new DestinationDTO(timemillis, x, y);
            String json = JsonUtil.toJson(dto);

            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
            msg.addReceiver(topicAID);                 // publish to topic
            msg.setOntology("teletubbies-agent");
            msg.setContent(json);
            msg.setConversationId("DESTINATION_COMMAND");
            msg.setEncoding("UTF-8");
            send(msg);

            System.out.println(getLocalName() + " published: " + json + topicAID.toString());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(null, "Please enter valid numbers for X and Y.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
