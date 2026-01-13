package example.mas.original;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;


public class MonitorUI extends JFrame {
    
    private Map<String, RobotPanel> robotPanels = new HashMap<>();
    private Map<String, ConveyorPanel> conveyorPanels = new HashMap<>();
    private JPanel robotsContainer;
    private JPanel conveyorsContainer;
    
    public MonitorUI() {
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
            new Font("Segoe UI", Font.BOLD, 16)
        ));
        return panel;
    }
    
    private void initializeAgents() {
        // Add robot panels
        for (String robotName : Config.ROBOT_NAMES) {
            RobotPanel panel = new RobotPanel(robotName);
            robotPanels.put(robotName, panel);
            robotsContainer.add(panel);
            robotsContainer.add(Box.createRigidArea(new Dimension(0, 5)));
        }
        
        // Add conveyor panels
        for (String conveyorName : Config.CONVEYOR_NAMES) {
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
                new EmptyBorder(8, 10, 8, 10)
            ));
            
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
            
            // Battery bar
            batteryBar = new JProgressBar(0, Config.INITIAL_BATTERY);
            batteryBar.setValue(Config.INITIAL_BATTERY);
            batteryBar.setStringPainted(true);
            batteryBar.setForeground(new Color(76, 175, 80));
            add(batteryBar, BorderLayout.SOUTH);
        }
        
        public void updateStatus() {
            String location = SimpleNamespace.getRobotLocation(robotName);
            int battery = SimpleNamespace.getRobotBattery(robotName);
            boolean hasProduct = SimpleNamespace.getRobotHasProduct(robotName);
            
            // Update labels
            locationLabel.setText("Location: " + (location != null && !location.isEmpty() ? formatCoords(location) : "Unknown"));
            batteryLabel.setText("Battery: " + battery + " / " + Config.INITIAL_BATTERY);
            
            // Status
            String status = "Idle";
            if (battery < Config.LOW_BATTERY_THRESHOLD) {
                status = "Charging";
            } else if (hasProduct) {
                status = "Carrying Product";
            } else if (battery < Config.INITIAL_BATTERY * 0.5) {
                status = "Active (Low Battery)";
            } else {
                status = "Active";
            }
            statusLabel.setText("Status: " + status);
            
            // Battery bar color
            batteryBar.setValue(battery);
            if (battery < Config.LOW_BATTERY_THRESHOLD) {
                batteryBar.setForeground(new Color(244, 67, 54));
            } else if (battery < Config.INITIAL_BATTERY * 0.5) {
                batteryBar.setForeground(new Color(255, 152, 0));
            } else {
                batteryBar.setForeground(new Color(76, 175, 80));
            }
        }
        
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
        private JButton produceButton;
        private JCheckBox autoCheckBox;
        private JSpinner intervalSpinner;
        private Timer autoTimer;
        
        public ConveyorPanel(String conveyorName) {
            this.conveyorName = conveyorName;
            setLayout(new BorderLayout(5, 5));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
            setBackground(Color.WHITE);
            setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
                new EmptyBorder(8, 10, 8, 10)
            ));
            
            // Header
            JLabel nameLabel = new JLabel("" + conveyorName);
            nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
            add(nameLabel, BorderLayout.NORTH);
            
            // Status
            JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            statusPanel.setBackground(Color.WHITE);
            statusLabel = new JLabel("Status: Empty");
            statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            statusPanel.add(statusLabel);
            
            // Manual produce button
            produceButton = new JButton("Produce");
            produceButton.setBackground(new Color(33, 150, 243));
            produceButton.setForeground(Color.WHITE);
            produceButton.setFocusPainted(false);
            produceButton.addActionListener(e -> manualProduce());
            statusPanel.add(produceButton);
            
            add(statusPanel, BorderLayout.CENTER);
            
            // Auto produce controls
            JPanel autoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
            autoPanel.setBackground(Color.WHITE);
            
            autoCheckBox = new JCheckBox("Auto-produce every");
            autoCheckBox.setBackground(Color.WHITE);
            autoCheckBox.addActionListener(e -> toggleAuto());
            
            intervalSpinner = new JSpinner(new SpinnerNumberModel(5, 1, 60, 1));
            intervalSpinner.setPreferredSize(new Dimension(60, 25));
            intervalSpinner.setEnabled(false);
            
            JLabel secondsLabel = new JLabel("seconds");
            
            autoPanel.add(autoCheckBox);
            autoPanel.add(intervalSpinner);
            autoPanel.add(secondsLabel);
            
            add(autoPanel, BorderLayout.SOUTH);
        }
        
        private void manualProduce() {
            SimpleNamespace.setConveyorProduced(conveyorName, true);
            produceButton.setEnabled(false);
        }
        
        private void toggleAuto() {
            if (autoCheckBox.isSelected()) {
                intervalSpinner.setEnabled(true);
                produceButton.setEnabled(false);
                startAutoProduction();
            } else {
                intervalSpinner.setEnabled(false);
                produceButton.setEnabled(true);
                stopAutoProduction();
            }
        }
        
        private void startAutoProduction() {
            stopAutoProduction(); // Clear any existing timer
            int interval = (Integer) intervalSpinner.getValue();
            autoTimer = new Timer(true);
            autoTimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    boolean produced = SimpleNamespace.getConveyorProduced(conveyorName);
                    if (!produced) {
                        SimpleNamespace.setConveyorProduced(conveyorName, true);
                    }
                }
            }, 0, interval * 1000);
        }
        
        private void stopAutoProduction() {
            if (autoTimer != null) {
                autoTimer.cancel();
                autoTimer = null;
            }
        }
        
        public void updateStatus() {
            boolean produced = SimpleNamespace.getConveyorProduced(conveyorName);
            
            if (produced) {
                statusLabel.setText("Status:Product Ready");
                statusLabel.setForeground(new Color(255, 152, 0));
                if (!autoCheckBox.isSelected()) {
                    produceButton.setEnabled(false);
                }
            } else {
                statusLabel.setText("Status: Empty");
                statusLabel.setForeground(new Color(76, 175, 80));
                if (!autoCheckBox.isSelected()) {
                    produceButton.setEnabled(true);
                }
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
