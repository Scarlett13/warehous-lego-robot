package robot.agents;

import jade.core.AID;
import jade.core.Agent;
import jade.lang.acl.ACLMessage;
import robot.Constants;
import robot.RobotContext;
import robot.acl.Acl;
import robot.behaviours.command.DestinationReceiverBehaviour;
import robot.behaviours.command.PozyxPositionBehaviour;
import robot.behaviours.motor.MotorActuatorBehaviour;
import robot.behaviours.pid.SpeedPidBehaviour;
import robot.behaviours.ultrasonic.UltrasonicReadingBehaviour;
import robot.dto.DestinationDTO;
import robot.utils.JsonUtil;
import robot.utils.MqttUtil;
import robot.utils.TopicHelper;

import javax.swing.*;
import java.awt.*;

import static robot.Constants.DESTINATION_TOPIC;


public class TeletubbiesAgent extends Agent {

    private final RobotContext ctx = new RobotContext();
    private MqttUtil mqttUtil;
    private JTextField xField;
    private JTextField yField;
    private AID topicAID;

    @Override
    protected void setup() {
        super.setup();
        ctx.instantiatePid();

        try {
            mqttUtil = new MqttUtil(Constants.MQTT_TAG);
            topicAID = TopicHelper.topic(this, DESTINATION_TOPIC );

            addBehaviour(new DestinationReceiverBehaviour(this, ctx));
            addBehaviour(new PozyxPositionBehaviour(this, 1000 / Constants.LOOP_HZ, ctx, mqttUtil));
            addBehaviour(new UltrasonicReadingBehaviour(this, 1000 / Constants.LOOP_HZ, ctx));
//            addBehaviour(new DummyBehaviour(this, 1000 / Constants.LOOP_HZ, ctx, topicHelper));
//            addBehaviour(new DummyCyclicBehaviour(this, ctx));
            addBehaviour(new SpeedPidBehaviour(this, 1000/Constants.LOOP_HZ, ctx));
            addBehaviour(new MotorActuatorBehaviour(this, 1000/Constants.LOOP_HZ, ctx));

//            buildUi();
        } catch (Exception e) {
//            System.err.println(getLocalName()+" topic setup error: "+e.getMessage());
            e.printStackTrace();
        }

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

            Acl.publish(this, topicAID, json);

            System.out.println(getLocalName() + " published: " + json + topicAID.toString());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(null, "Please enter valid numbers for X and Y.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void takeDown() {
        super.takeDown();
        try {
            if (mqttUtil != null)
                mqttUtil.shutdown();
        } catch (Exception ignore) {}
    }
}
