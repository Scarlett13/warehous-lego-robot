package server.digitaltwin.vrl;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Dummy Agent to simulate the VRL Physical Robot.
 * Features:
 * - Periodically sends LOCATION updates (1Hz).
 * - Simulates movement towards target (Interpolation).
 * - Sends ARRIVED when target reached.
 */
public class DummyVrlRobotAgent extends Agent {

    private int currentX = 7130;
    private int currentY = 13182;
    private int targetX = 7130;
    private int targetY = 13182;
    private boolean moving = false;
    private double speed = 200.0; // pixels/tick (fake speed)

    // Helper to send JSON messages
    private void sendJson(String header, JSONObject content) {
        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        // Assuming DT is named "VrlRobot_DT"
        msg.addReceiver(new AID(getLocalName() + "_DT", AID.ISLOCALNAME));

        if (content == null)
            content = new JSONObject();
        content.put("Header", header);

        msg.setContent(content.toString());
        send(msg);
    }

    @Override
    protected void setup() {
        System.out.println("Dummy VRL Robot " + getLocalName() + " started (periodic updates).");

        // 1. Periodic Location Sender (Heartbeat)
        addBehaviour(new TickerBehaviour(this, 1000) {
            @Override
            public void onTick() {
                // Update Physics if moving
                if (moving) {
                    double dx = targetX - currentX;
                    double dy = targetY - currentY;
                    double dist = Math.sqrt(dx * dx + dy * dy);

                    if (dist < speed) {
                        // Arrived
                        currentX = targetX;
                        currentY = targetY;
                        moving = false;

                        // Send Location Final
                        sendLocation();

                        // Send ARRIVED
                        System.out.println("[DummyBot] Arrived at (" + currentX + "," + currentY + ")");
                        sendJson("ARRIVED", null);

                    } else {
                        // Move
                        double ratio = speed / dist;
                        currentX += dx * ratio;
                        currentY += dy * ratio;
                        // Send Location Intermediate
                        sendLocation();
                        System.out.println("[DummyBot] Moving... (" + currentX + "," + currentY + ")");
                    }
                } else {
                    // Send Static Location (Heartbeat)
                    sendLocation();
                }
            }
        });

        // 2. Command Receiver
        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                ACLMessage msg = receive();
                if (msg != null) {
                    try {
                        JSONObject json = new JSONObject(msg.getContent());
                        String header = json.optString("Header");

                        if ("PATH".equals(header)) {
                            JSONArray points = json.getJSONArray("Points");
                            if (points.length() > 0) {
                                JSONArray lastPoint = points.getJSONArray(points.length() - 1);
                                targetX = lastPoint.getInt(0);
                                targetY = lastPoint.getInt(1);
                                moving = true;
                                System.out.println("[DummyBot] New Target: (" + targetX + "," + targetY + ")");
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    block();
                }
            }
        });
    }

    private void sendLocation() {
        JSONObject locJson = new JSONObject();
        JSONArray loc = new JSONArray();
        loc.put(currentX);
        loc.put(currentY);
        locJson.put("Location", loc);
        sendJson("LOCATION", locJson);
    }
}
