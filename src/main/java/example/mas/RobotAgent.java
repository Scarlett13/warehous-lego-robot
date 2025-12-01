package example.mas;

import example.java.Connexion.Device2;
import example.java.UWB.mqtt.TagIdMqtt;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.core.AID;

import java.awt.geom.Point2D;
import java.util.*;

import jade.lang.acl.ACLMessage;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import org.eclipse.paho.client.mqttv3.MqttException;

import static robot.RobotConstants.IS_SIMS;
import static robot.RobotConstants.MQTT_TAG;


public class RobotAgent extends Agent {
    private static final double ARRIVAL_TOLERANCE_MM = 200.0;

    private static final Object MESSAGE_LOCK = new Object();
    private static final Set<String> ASSIGNED_CONVEYORS = new HashSet<>();
    private static final Set<Integer> ASSIGNED_CHARGING_STATIONS = new HashSet<>();  // Track which charging stations are in use
    
    private String robotName;
    private String currentLocation;
    private String targetLocation;
    private String assignedConveyor;
    private Integer assignedChargingStationIndex;  // Track which charging station this robot is using
    private boolean justPickedUp;
    private int batteryLevel;
    private boolean hasProduct;
    private boolean isCharging;
    private Random random;
    private boolean isBusy;
    private boolean hasReceivedFirstTarget;

    int value = 0;
    static TagIdMqtt tag;

    static {
        if(!IS_SIMS) {
            try {
                tag = new TagIdMqtt(MQTT_TAG);
            } catch (MqttException e) {
                System.out.println("wubbafailed, " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    @Override
    protected void setup() {
        // Get robot name from agent arguments
        Object[] args = getArguments();
        if (args != null && args.length > 0) {
            robotName = (String) args[0];
        } else {
            robotName = getLocalName();
        }

        if(!IS_SIMS) {
            Device2.init();
        }

        // Initialize
        currentLocation = "";
        targetLocation = "";
        assignedConveyor = null;
        assignedChargingStationIndex = null;
        justPickedUp = false;
        batteryLevel = Config.INITIAL_BATTERY;
        hasProduct = false;
        isCharging = false;
        random = new Random();
        isBusy = false;
        hasReceivedFirstTarget = false;
        
        System.out.println(" Robot Agent " + robotName + " is ready!");
        
        // Register in Yellow Pages so other robots can find us
        registerInYellowPages();
        
        // Add message listener for conveyor notifications
        addBehaviour(new jade.core.behaviours.CyclicBehaviour(this) {
            @Override
            public void action() {
                ACLMessage msg = receive();
                if (msg != null) {
                    if (msg.getConversationId() != null && msg.getConversationId().equals("product-pickup")) {
                        handleMessage(msg);
                    }
                } else {
                    block();
                }
            }
        });
        
        // Start main behavior (runs every second)
        addBehaviour(new TickerBehaviour(this, Config.UPDATE_INTERVAL) {
            @Override
            protected void onTick() {
                tick();
            }
        });
    }
    
    /**
     * Handle incoming JADE messages
     */
    private void handleMessage(ACLMessage msg) {
        synchronized (MESSAGE_LOCK) {
            if (msg.getConversationId() != null && msg.getConversationId().equals("product-pickup")) {
                String content = msg.getContent();
                if (content.startsWith("PRODUCT_READY:")) {
                    String[] parts = content.split(":");
                    
                    // STRICT RULE: NEVER accept if robot has product or is already busy
                    // This prevents ANY task acceptance during pickup-to-delivery journey
                    if (hasProduct) {
                        return;  // Immediate rejection - robot has product
                    }
                    
                    if (isBusy) {
                        return;  // Immediate rejection - robot is busy
                    }
                    
                    if (isCharging) {
                        return;  // Immediate rejection - robot is charging
                    }
                    
                    if (needsCharging()) {
                        return;  // Immediate rejection - battery too low
                    }
                    
                    // Only proceed if ALL conditions are safe
                    if (parts.length == 3) {
                        String conveyorName = parts[1];
                        String coords = parts[2];
                        
                        // Check if this conveyor is already assigned to another robot
                        if (ASSIGNED_CONVEYORS.contains(conveyorName)) {
                            return;
                        }
                        
                        System.out.println(" " + robotName + " accepting " + conveyorName + " (isBusy=" + isBusy + ", isCharging=" + isCharging + ", battery=" + batteryLevel + ")");
                        
                        isBusy = true;
                        targetLocation = coords.replace(";", ",");
                        System.out.println("testtargetlocation: " + targetLocation);

                        assignedConveyor = conveyorName;
                        ASSIGNED_CONVEYORS.add(conveyorName);
                        hasReceivedFirstTarget = true;
                        
                        try {
                            String[] coordParts = coords.split(";");
                            double[] coordArray = new double[3];
                            for (int i = 0; i < 3; i++) {
                                coordArray[i] = Double.parseDouble(coordParts[i]);
                            }
                            sendTargetCoordinates(coordArray);
                        } catch (Exception e) {
                            isBusy = false;
                            assignedConveyor = null;
                            ASSIGNED_CONVEYORS.remove(conveyorName);
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Main robot logic - runs every tick
     */
    private void tick() {
        // Read current location from OPC-UA (updated by VC)
        readLocationFromOPCUA();

        if(!targetLocation.isEmpty()) {
            List<String> coordtargetloc = Arrays.asList(targetLocation.replaceAll(";",",").split(","));
            System.out.println("coordtargetloc: " + coordtargetloc.get(0));
            System.out.println("double x: "+Double.parseDouble(coordtargetloc.get(0)));
        }


        synchronized (MESSAGE_LOCK) {
            // Check if arrived at assigned conveyor and pick up product
            if (assignedConveyor != null && !justPickedUp && !isCharging && coordinatesMatch(currentLocation, targetLocation)) {
                // Verify this is actually the conveyor location
                double[] conveyorCoords = Config.CONVEYOR_LOCATIONS.get(assignedConveyor);
                String conveyorLocation = String.format(Locale.US, "%.0f;%.0f;%.0f", conveyorCoords[0], conveyorCoords[1], conveyorCoords[2]);
                if (conveyorCoords != null && coordinatesMatch(currentLocation, conveyorLocation)) {
                    SimpleNamespace.setConveyorProduced(assignedConveyor, false);
                    
                    // UPDATE STATE FIRST before any message can be processed
                    hasProduct = true;
                    isBusy = true;  // Keep busy during delivery
                    justPickedUp = true;
                    ASSIGNED_CONVEYORS.remove(assignedConveyor);
                    assignedConveyor = null;
                    
                    // Update OPC-UA IMMEDIATELY so other threads see correct state
                    SimpleNamespace.setRobotHasProduct(robotName, true);
                    
                    // Now set target to output
                    targetLocation = String.format(Locale.US, "%.0f;%.0f;%.0f", Config.LOC_OUTPUT[0], Config.LOC_OUTPUT[1], Config.LOC_OUTPUT[2]);
                    sendTargetCoordinates(Config.LOC_OUTPUT);
                    
                    System.out.println(" " + robotName + " picked up from " + assignedConveyor);
                }
            }
            // Check if arrived at output location and deliver product
            else if (hasProduct && isBusy && assignedConveyor == null && !isCharging && !targetLocation.isEmpty() && coordinatesMatch(currentLocation, targetLocation)) {
                // Verify this is actually the output location
                String outputLocation = String.format(Locale.US, "%.0f;%.0f;%.0f", Config.LOC_OUTPUT[0], Config.LOC_OUTPUT[1], Config.LOC_OUTPUT[2]);
                if (coordinatesMatch(currentLocation, outputLocation)) {
                    // UPDATE STATE FIRST
                    hasProduct = false;
                    isBusy = false;
                    targetLocation = "";
                    justPickedUp = false;
                    
                    // Update OPC-UA IMMEDIATELY
                    SimpleNamespace.setRobotHasProduct(robotName, false);
                    
                    System.out.println(" " + robotName + " delivered product, now available (isBusy=" + isBusy + ")");
                }
            }
        }
        
        // Broadcast location to other robots (JADE messaging!)
        broadcastLocation();
        
        // Drain battery only after first target received
        if (hasReceivedFirstTarget && !isCharging && batteryLevel > 0) {
            batteryLevel--;
        }
        
        // ONLY charge if actually at charging station
        if (isCharging) {
            if (isAtChargingStation()) {
                batteryLevel += Config.CHARGE_RATE;
                if (batteryLevel >= Config.INITIAL_BATTERY) {
                    synchronized (MESSAGE_LOCK) {
                        batteryLevel = Config.INITIAL_BATTERY;
                        isCharging = false;
                        
                        // If robot has product, resume delivery to output
                        if (hasProduct) {
                            isBusy = true;
                            targetLocation = String.format(Locale.US, "%.0f;%.0f;%.0f", Config.LOC_OUTPUT[0], Config.LOC_OUTPUT[1], Config.LOC_OUTPUT[2]);
                            sendTargetCoordinates(Config.LOC_OUTPUT);
                        } else {
                            isBusy = false;
                            targetLocation = "";
                        }
                        
                        // Release the assigned charging station so other robots can use it
                        if (assignedChargingStationIndex != null) {
                            ASSIGNED_CHARGING_STATIONS.remove(assignedChargingStationIndex);
                            System.out.println(" " + robotName + " released charging station " + assignedChargingStationIndex);
                            assignedChargingStationIndex = null;
                        }
                        
                        System.out.println(" " + robotName + " battery fully charged!");
                    }
                }
            }
            // Still traveling to charging station - don't charge yet
            return;  // Skip rest of tick while charging
        }
        
        // Check if battery low
        if (!isCharging && needsCharging()) {
            System.out.println("️  " + robotName + " battery low! Going to charging station...");
            goToCharging();
            return;
        }
        
        // Execute task if not charging
        if (!isCharging) {
            executeTask();
        }
        
        // Update OPC-UA (send to VC)
        updateOPCUA();
        if(!IS_SIMS) {
            addBehaviour(move);
        }
    }
    
    /**
     * JADE MESSAGING: Broadcast location to all other robots

     */
    private void broadcastLocation() {
        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        msg.setContent("LOCATION:" + currentLocation);
        msg.setConversationId("location-broadcast");
        
        // Send to all other robots
        for (String otherRobot : AgentRegistry.getAllRobotNames()) {
            if (!otherRobot.equals(robotName)) {
                msg.addReceiver(new AID(otherRobot, AID.ISLOCALNAME));
            }
        }
        
        send(msg);
    }
    
    /**
     * JADE MESSAGING: Check if target location is occupied
     */
    private boolean isLocationOccupied(double[] coords) {
        String location = coordsToString(coords);
        // Query other robots for their location
        ACLMessage query = new ACLMessage(ACLMessage.QUERY_IF);
        query.setContent("AT_LOCATION:" + location);
        query.setConversationId("collision-check");
        query.setReplyWith("check" + System.currentTimeMillis());
        
        // Send to all other robots
        for (String otherRobot : AgentRegistry.getAllRobotNames()) {
            if (!otherRobot.equals(robotName)) {
                query.addReceiver(new AID(otherRobot, AID.ISLOCALNAME));
            }
        }
        
        send(query);
        
        // Wait for responses (simple blocking for educational purposes)
        ACLMessage reply = blockingReceive(100); // 100ms timeout
        if (reply != null && reply.getPerformative() == ACLMessage.CONFIRM) {
            return true; // Someone is at that location
        }
        
        return false;
    }
    
    /**
     * Execute robot task logic
     */
    private void executeTask() {

        
        // Check if at target by comparing coordinates numerically
        boolean atTarget = coordinatesMatch(currentLocation, targetLocation);
        

        if (atTarget && !currentLocation.isEmpty() && !hasProduct) {
            if (false) {  // Disabled - conveyor system uses tick() logic
                // Pick up product from random location
                double[] pickup = Config.TASK_LOCATIONS.get(random.nextInt(Config.TASK_LOCATIONS.size()));
                
                // Check if location is occupied (collision avoidance!)
                if (!isLocationOccupied(pickup)) {
                    targetLocation = coordsToString(pickup);
                    sendTargetCoordinates(pickup);
                    hasProduct = !hasProduct; // Toggle hasProduct
                    System.out.println(" " + robotName + " going to pickup from " + coordsToString(pickup));
                } else {
                    System.out.println("️  " + robotName + " waiting - location is occupied");
                }
            } else {
                // Deliver product to random location
                double[] delivery = Config.TASK_LOCATIONS.get(random.nextInt(Config.TASK_LOCATIONS.size()));
                
                if (!isLocationOccupied(delivery)) {
                    targetLocation = coordsToString(delivery);
                    sendTargetCoordinates(delivery);
                    hasProduct = !hasProduct; // Toggle hasProduct
                    System.out.println(" " + robotName + " delivering to " + coordsToString(delivery));
                } else {
                    System.out.println("️  " + robotName + " waiting - location is occupied");
                }
            }
        }
        
        // If idle and no target, allow conveyor tasks
        if (targetLocation.isEmpty()) {
//            removeBehaviour(move);
            isBusy = false;
        }
    }
    
    /**
     * Compare two coordinate strings numerically (handles different formats)
     */
    private boolean coordinatesMatch(String coord1, String coord2) {
        try {
            String[] parts1 = coord1.replace("[", "").replace("]", "").replace(";", ",").split(",");
            String[] parts2 = coord2.replace("[", "").replace("]", "").replace(";", ",").split(",");

            if (parts1.length != 3 || parts2.length != 3) {
                return false;
            }

            double dx = Double.parseDouble(parts1[0].trim()) - Double.parseDouble(parts2[0].trim());
            double dy = Double.parseDouble(parts1[1].trim()) - Double.parseDouble(parts2[1].trim());
            double dz = Double.parseDouble(parts1[2].trim()) - Double.parseDouble(parts2[2].trim());

            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            return distance <= ARRIVAL_TOLERANCE_MM;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Check if robot is at any charging station
     */
    private boolean isAtChargingStation() {
        for (double[] cs : Config.CHARGING_STATIONS) {
            String csLocation = String.format(Locale.US, "%.0f;%.0f;%.0f", cs[0], cs[1], cs[2]);
            if (coordinatesMatch(currentLocation, csLocation)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Go to charging station (with coordination like conveyors)
     */
    private void goToCharging() {
        synchronized (MESSAGE_LOCK) {
            // Clear only conveyor assignment, keep hasProduct state
            if (assignedConveyor != null) {
                ASSIGNED_CONVEYORS.remove(assignedConveyor);
                assignedConveyor = null;
            }
            
            isCharging = true;
            isBusy = true;  // Busy going to charging station
            justPickedUp = false;
            
            // Find available charging station (check ASSIGNED_CHARGING_STATIONS)
            for (int i = 0; i < Config.CHARGING_STATIONS.size(); i++) {
                if (!ASSIGNED_CHARGING_STATIONS.contains(i)) {
                    // This charging station is available!
                    ASSIGNED_CHARGING_STATIONS.add(i);
                    assignedChargingStationIndex = i;
                    
                    double[] cs = Config.CHARGING_STATIONS.get(i);
                    targetLocation = String.format(Locale.US, "%.0f;%.0f;%.0f", cs[0], cs[1], cs[2]);
                    sendTargetCoordinates(cs);
                    System.out.println(" " + robotName + " assigned to charging station " + i + " (available stations: " + getAvailableChargingStations() + ")");
                    return;
                }
            }
            
            // All stations occupied - wait for one to become available
            System.out.println("️  " + robotName + " waiting - all charging stations occupied");
            isCharging = false;  // Not actually going to charge yet
            isBusy = false;  // Allow to try again next tick
        }
    }
    
    /**
     * Helper method to show available charging stations for debugging
     */
    private String getAvailableChargingStations() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Config.CHARGING_STATIONS.size(); i++) {
            if (!ASSIGNED_CHARGING_STATIONS.contains(i)) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(i);
            }
        }
        return sb.length() > 0 ? sb.toString() : "none";
    }
    
    private boolean needsCharging() {
        return batteryLevel < Config.LOW_BATTERY_THRESHOLD;
    }
    
    private void readLocationFromOPCUA() {
        String location = SimpleNamespace.getRobotLocation(robotName);
        if (location != null && !location.isEmpty()) {
            currentLocation = location;
        }
    }
    
    private void updateOPCUA() {
        SimpleNamespace.setRobotLocation(robotName, currentLocation);
        // Parse targetLocation string back to coordinates if needed
        // For now, just update battery and product status
        SimpleNamespace.setRobotBattery(robotName, batteryLevel);
        SimpleNamespace.setRobotHasProduct(robotName, hasProduct);
    }
    
    /**
     * Convert coordinates to string for tracking/logging
     */
    private String coordsToString(double[] coords) {
        return String.format("[%.0f,%.0f,%.0f]", coords[0], coords[1], coords[2]);
    }
    
    /**
     * Convert coordinate array to string (semicolon format for comparison)
     */
    private String coordsArrayToString(double[] coords) {
        return String.format("%.3f;%.3f;%.3f", coords[0], coords[1], coords[2]);
    }
    

    private void sendTargetCoordinates(double[] coords) {
        synchronized (MESSAGE_LOCK) {
            // Double-check state before writing to OPC-UA
            // This prevents any stale calls from overwriting valid targets
            if (hasProduct && !isOutputLocation(coords) && !isChargingStationLocation(coords)) {
                // Robot has product but target is NOT output or charging station - REJECT
                System.out.println("️  " + robotName + " BLOCKED target change - has product, must deliver first!");
                return;
            }
            
            SimpleNamespace.setRobotTarget(robotName, coords);
        }
    }
    
    /**
     * Check if coordinates match any charging station location
     */
    private boolean isChargingStationLocation(double[] coords) {
        for (double[] cs : Config.CHARGING_STATIONS) {
            if (coordinatesMatch(
                String.format(Locale.US, "%.0f;%.0f;%.0f", coords[0], coords[1], coords[2]),
                String.format(Locale.US, "%.0f;%.0f;%.0f", cs[0], cs[1], cs[2])
            )) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Check if coordinates match the output location
     */
    private boolean isOutputLocation(double[] coords) {
        return coordinatesMatch(
            String.format(Locale.US, "%.0f;%.0f;%.0f", coords[0], coords[1], coords[2]),
            String.format(Locale.US, "%.0f;%.0f;%.0f", Config.LOC_OUTPUT[0], Config.LOC_OUTPUT[1], Config.LOC_OUTPUT[2])
        );
    }
    
    /**
     * Register this robot in JADE Yellow Pages
     * Other robots can discover us!
     */
    private void registerInYellowPages() {
        try {
            DFAgentDescription dfd = new DFAgentDescription();
            dfd.setName(getAID());
            
            ServiceDescription sd = new ServiceDescription();
            sd.setType(Config.YELLOW_PAGES_SERVICE);
            sd.setName(robotName);
            
            dfd.addServices(sd);
            DFService.register(this, dfd);
            
            System.out.println(" " + robotName + " registered in Yellow Pages");
        } catch (FIPAException e) {
            e.printStackTrace();
        }
    }
    
    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
            System.out.println(" " + robotName + " deregistered from Yellow Pages");
        } catch (FIPAException e) {
            e.printStackTrace();
        }
    }

    CyclicBehaviour obstacle_check = new CyclicBehaviour() {
        @Override
        public void action() {
            try {
                value = Device2.check_Emergency();
                //   System.out.println(" LEFT "+ultra_left+" "+" RIGHT "+ultra_right);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    OneShotBehaviour go_forward = new OneShotBehaviour() {
        @Override
        public void action() {
            try {
                Device2.forward();
                System.out.println("Forward");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    OneShotBehaviour go_backward = new OneShotBehaviour() {
        @Override
        public void action() {
            try {
                Device2.backward();
                System.out.println("Backward");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    OneShotBehaviour turn_right = new OneShotBehaviour() {
        @Override
        public void action() {
            try {
                Device2.turnRight();
                System.out.println("Right");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    OneShotBehaviour turn_left = new OneShotBehaviour() {
        @Override
        public void action() {
            try {
                Device2.turnLeft();
                System.out.println("Left");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    OneShotBehaviour stop = new OneShotBehaviour() {
        @Override
        public void action() {
            try {
                Device2.stop();
//                System.out.println("Stop");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    example.java.UWB.helpers.Point2D loc;
    Behaviour move = new Behaviour() {
        @Override
        public void action() {
            try {
                System.out.println("Entering move.action()");

                loc = tag.getSmoothenedLocation(10);
                currentLocation = loc.toOpcUa();
                if (loc == null || (loc.x == 0 && loc.y == 0)) {
                    block(500);
                    return;
                }
                System.out.println("Tag returned: x=" + loc.x + ", y=" + loc.y);
                int x = loc.x;
                int y = loc.y;

                if (x != 0 && y != 0) {
                    List<String> coordtargetloc = Arrays.asList(targetLocation.replaceAll(";",",").split(","));
                    System.out.println();
                    double target_x =  Double.parseDouble(coordtargetloc.get(0));
                    double target_y = Double.parseDouble(coordtargetloc.get(1));

                    System.out.println("x: " + x + ", y: " + y + ", target x: " + target_x + ", target y: " + target_y);

                    float yaw = tag.getAngle();
                    // yaw = (float) (yaw-301.5);

                    double diff_y = target_y - y;
                    double diff_x = target_x - x;


                    double dist = Point2D.distance(x, y, target_x, target_y);

                    //double atan2 =Math.atan2(diff_y, diff_x);

                    //System.out.println("atan2="+atan2);

                    //float target_angle = (float) Math.toDegrees(atan2); //??


                    float target_angle = (float) Math.toDegrees(Math.atan2((double)(target_y - y), (double)(target_x - x)));



//                    System.out.println("Target Angle"+target_angle);

                    float diff_angle = target_angle - yaw;
                    diff_angle = (float) (((diff_angle + 540.0) % 360.0) - 180.0);


//                    System.out.println("- AFTER Diff Angle**"+diff_angle);


                    if (Math.abs(target_x - x) < 100 && Math.abs(target_y - y) < 100) {    // old params diff_angle > 10 && diff_angle <= 180, diff_angle < 350 && diff_angle > 180
//                        if (path_iterator >= path.length - 1) {}
//                        path_iterator += 2;
                        Device2.setSpeed(0);
                        System.out.println("STOP");
                        addBehaviour(stop);
                    } else if (diff_angle > 10) {
                        Device2.setSpeed(250);
                        addBehaviour(turn_right);
                        System.out.println("RIGHT");
                    } else if (diff_angle < -10) {
                        Device2.setSpeed(250);
                        addBehaviour(turn_left);
                        System.out.println("LEFT");
                    } else {
                        Device2.setSpeed(200);
                        addBehaviour(go_forward);
                        System.out.println("FORWARD");
                    }
                }

            } catch (Exception e) {
                System.out.println(e);
            }
        }

        @Override
        public boolean done() {
            return true;
        }
    };
}
