package server.ui;

import jade.core.Agent;
import server.ServerConfig;
import server.opcua.OpcuaNodeRegistry;
import server.opcua.SimpleNamespace;
import shared.SharedConstants;
import shared.dto.FruitItemDTO;
import shared.utils.JsonUtil;
import shared.utils.RandomUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.Timer;

public class MonitorUI extends JFrame {

    private Map<String, RobotPanel> robotPanels = new HashMap<>();
    private Map<String, JPanel> robotRows = new HashMap<>();
    private Map<String, ConveyorPanel> conveyorPanels = new HashMap<>();
    private JPanel robotsContainer;
    private JPanel conveyorsContainer;
    private final Agent uiAgent;

    public MonitorUI() {
        // empty constructor
        this.uiAgent = null;
    }

    public MonitorUI(Agent uiAgent) {
        this.uiAgent = uiAgent;
        setTitle("Warehouse Multi-Agent System Monitor");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);

        // Main layout
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        mainPanel.setBackground(new Color(240, 240, 245));

        // Title
        JLabel titleLabel = new JLabel("Warehouse Multi-Agent System", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setBorder(new EmptyBorder(10, 0, 10, 0));
        mainPanel.add(titleLabel, BorderLayout.NORTH);

        // Center: Split panel for robots and conveyors
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(500);

        // Robots panel
        JPanel robotsSection = createSection("Robots", new Color(230, 240, 255));
        robotsContainer = new JPanel();
        robotsContainer.setLayout(new BoxLayout(robotsContainer, BoxLayout.Y_AXIS));
        robotsContainer.setBackground(new Color(230, 240, 255));
        JScrollPane robotsScroll = new JScrollPane(robotsContainer);
        robotsScroll.setBorder(null);
        robotsSection.add(robotsScroll, BorderLayout.CENTER);

        // Conveyors panel
        JPanel conveyorsSection = createSection("Conveyors", new Color(255, 245, 230));
        conveyorsContainer = new JPanel();
        conveyorsContainer.setLayout(new BoxLayout(conveyorsContainer, BoxLayout.Y_AXIS));
        conveyorsContainer.setBackground(new Color(255, 245, 230));
        JScrollPane conveyorsScroll = new JScrollPane(conveyorsContainer);
        conveyorsScroll.setBorder(null);
        conveyorsSection.add(conveyorsScroll, BorderLayout.CENTER);

        splitPane.setLeftComponent(robotsSection);
        splitPane.setRightComponent(conveyorsSection);
        mainPanel.add(splitPane, BorderLayout.CENTER);

        add(mainPanel);

        // Initialize panels for existing robots and conveyors
        initializeAgents();

        // Start update timer
        startUpdateTimer();
    }

    private JPanel createSection(String title, Color bgColor) {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBackground(bgColor);
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
                title,
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 16)));
        return panel;
    }

    public void addRobotPanel(String robotName) {
        RobotPanel panel = new RobotPanel(robotName);

        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setBorder(new EmptyBorder(0, 0, 5, 0)); // spacing below

        row.add(panel, BorderLayout.CENTER);

        // Important: prevent vertical stretching in BoxLayout
        Dimension pref = row.getPreferredSize();
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, pref.height));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        robotPanels.put(robotName, panel);
        robotRows.put(robotName, row);

        robotsContainer.add(row);
        robotsContainer.revalidate();
        robotsContainer.repaint();
    }

    public void removeRobotPanel(String robotName) {
        RobotPanel panel = robotPanels.remove(robotName);
        JPanel row = robotRows.remove(robotName);

        if (row != null) {
            robotsContainer.remove(row);
        } else if (panel != null) {
            robotsContainer.remove(panel);
        }

        robotsContainer.revalidate();
        robotsContainer.repaint();
    }

    private void initializeAgents() {
        // Add conveyor panels
        for (String conveyorName : ServerConfig.CONVEYOR_NAMES) {
            ConveyorPanel panel = new ConveyorPanel(conveyorName);
            conveyorPanels.put(conveyorName, panel);
            conveyorsContainer.add(panel);
            conveyorsContainer.add(Box.createRigidArea(new Dimension(0, 5)));
        }
    }

    private void startUpdateTimer() {
        Timer timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                SwingUtilities.invokeLater(() -> updateAllPanels());
            }
        }, 0, 500); // Update every 500ms
    }

    private void updateAllPanels() {
        for (Map.Entry<String, RobotPanel> entry : robotPanels.entrySet()) {
            entry.getValue().updateStatus();
        }
        for (Map.Entry<String, ConveyorPanel> entry : conveyorPanels.entrySet()) {
            entry.getValue().updateStatus();
        }
    }

    /**
     * Robot monitoring panel
     */
    class RobotPanel extends JPanel {
        private String robotName;
        private JLabel batteryLabel;
        private JLabel statusLabel;
        private JLabel locationLabel;
        private JProgressBar batteryBar;

        public RobotPanel(String robotName) {
            this.robotName = robotName;
            setLayout(new BorderLayout(5, 5));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
            setBackground(Color.WHITE);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
                    new EmptyBorder(8, 10, 8, 10)));

            // Header
            JLabel nameLabel = new JLabel("" + robotName);
            nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
            add(nameLabel, BorderLayout.NORTH);

            // Info panel
            JPanel infoPanel = new JPanel(new GridLayout(3, 1, 2, 2));
            infoPanel.setBackground(Color.WHITE);

            statusLabel = new JLabel("Status: Idle");
            statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));

            locationLabel = new JLabel("Location: -");
            locationLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));

            batteryLabel = new JLabel("Battery: 100%");
            batteryLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));

            infoPanel.add(statusLabel);
            infoPanel.add(locationLabel);
            infoPanel.add(batteryLabel);

            add(infoPanel, BorderLayout.CENTER);

            // Controls Panel
            JPanel controlsPanel = new JPanel(new BorderLayout(5, 5));
            controlsPanel.setBackground(Color.WHITE);

            // Battery bar
            batteryBar = new JProgressBar(0, ServerConfig.INITIAL_BATTERY);
            batteryBar.setValue(ServerConfig.INITIAL_BATTERY);
            batteryBar.setStringPainted(true);
            batteryBar.setForeground(new Color(76, 175, 80));
            controlsPanel.add(batteryBar, BorderLayout.NORTH);

            // Force Arrive Button
            JButton forceArriveBtn = new JButton("Force Arrive");
            forceArriveBtn.setFocusPainted(false);
            forceArriveBtn.setBackground(new Color(255, 193, 7));
            forceArriveBtn.addActionListener(e -> {
                SimpleNamespace.setRobotForceArrival(robotName, true);
                // System.out.println("Force Arrival Signal Sent to " + robotName); // Optional
                // log
            });
            controlsPanel.add(forceArriveBtn, BorderLayout.SOUTH);

            add(controlsPanel, BorderLayout.SOUTH);
        }

        public void updateStatus() {
            String robotState = SimpleNamespace.getRobotCurrentState(robotName);
            int batteryPercentage = SimpleNamespace.getRobotCurrentBatteryPercentage(robotName);
            String currentWorkId = SimpleNamespace.getRobotCurrentWorkId(robotName);
            double currentSpeed = SimpleNamespace.getRobotCurrentSpeed(robotName);

            // Update labels
            locationLabel.setText(
                    "Assigned Item ID: " + (currentWorkId != null && !currentWorkId.isEmpty() ? currentWorkId : "-"));
            batteryLabel.setText("Battery: " + batteryPercentage + " %");

            statusLabel.setText("Status: " + robotState);

            // Battery bar color
            batteryBar.setValue(batteryPercentage);
            if (batteryPercentage < SharedConstants.BATTERY_CHARGE_THRESHOLD) {
                batteryBar.setForeground(new Color(244, 67, 54));
            } else if (batteryPercentage < SharedConstants.MAX_BATTERY * 0.5) {
                batteryBar.setForeground(new Color(255, 152, 0));
            } else {
                batteryBar.setForeground(new Color(76, 175, 80));
            }
        }

        //
        private String formatCoords(String coords) {
            try {
                String[] parts = coords.split(";");
                return String.format("[%s, %s, %s]", parts[0], parts[1], parts[2]);
            } catch (Exception e) {
                return coords;
            }
        }
    }

    /**
     * Conveyor control panel
     */
    class ConveyorPanel extends JPanel {
        private String conveyorName;
        private JLabel statusLabel;
        private JTextArea itemsArea;

        public ConveyorPanel(String conveyorName) {
            this.conveyorName = conveyorName;
            setLayout(new BorderLayout(5, 5));
            // Increased height to show items
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));
            setBackground(Color.WHITE);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
                    new EmptyBorder(8, 10, 8, 10)));

            // Header
            JLabel nameLabel = new JLabel("" + conveyorName);
            nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
            add(nameLabel, BorderLayout.NORTH);

            // Status Panel
            JPanel centerPanel = new JPanel(new BorderLayout());
            centerPanel.setBackground(Color.WHITE);

            statusLabel = new JLabel("Status: Initializing...");
            statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            statusLabel.setBorder(new EmptyBorder(0, 0, 5, 0));
            centerPanel.add(statusLabel, BorderLayout.NORTH);

            // Items List Area
            itemsArea = new JTextArea();
            itemsArea.setEditable(false);
            itemsArea.setFont(new Font("Monospaced", Font.PLAIN, 11));

            // Fix auto-scrolling issue
            javax.swing.text.DefaultCaret caret = (javax.swing.text.DefaultCaret) itemsArea.getCaret();
            caret.setUpdatePolicy(javax.swing.text.DefaultCaret.NEVER_UPDATE);

            JScrollPane scrollPane = new JScrollPane(itemsArea);
            scrollPane.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
            centerPanel.add(scrollPane, BorderLayout.CENTER);

            add(centerPanel, BorderLayout.CENTER);
        }

        public void updateStatus() {
            int availableItems = SimpleNamespace.getConveyorTotalItems(conveyorName);
            String itemsJson = SimpleNamespace.getConveyorItemsJson(conveyorName);

            // Update items list
            StringBuilder sb = new StringBuilder();
            try {
                if (itemsJson != null && !itemsJson.isEmpty() && !itemsJson.equals("[]")) {
                    FruitItemDTO[] items = JsonUtil.fromJson(itemsJson, FruitItemDTO[].class);
                    if (items != null) {
                        for (FruitItemDTO item : items) {
                            sb.append(String.format("[%s] %s (Fresh: %d)\n",
                                    item.getStatus(), item.getItemId(), item.getFreshness()));
                        }
                    }
                } else {
                    sb.append("No items.");
                }
            } catch (Exception e) {
                sb.append("Error parsing items.");
            }
            itemsArea.setText(sb.toString());

            // Status Label Logic
            if (conveyorName.equals("Input Location")) {
                if (availableItems > 0) {
                    statusLabel.setText("Status: " + availableItems + " Items Available");
                    statusLabel.setForeground(new Color(255, 152, 0));
                } else {
                    statusLabel.setText("Status: Empty");
                    statusLabel.setForeground(new Color(76, 175, 80));
                }
            } else {
                String outputMessage = "Status: Ready to Receive Items";
                statusLabel.setText(outputMessage);
                statusLabel.setForeground(new Color(76, 175, 80));
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MonitorUI ui = new MonitorUI();
            ui.setVisible(true);
        });
    }
}
