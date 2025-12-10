package example.mas.original;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ConfigDialog extends JDialog {
    
    private boolean configAccepted = false;
    private JTextArea configDisplay;
    
    public ConfigDialog(JFrame parent) {
        super(parent, "Warehouse System Configuration", true);
        setSize(600, 500);
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
        
        JLabel titleLabel = new JLabel("Current System Configuration", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        mainPanel.add(titleLabel, BorderLayout.NORTH);
        
        configDisplay = new JTextArea();
        configDisplay.setEditable(false);
        configDisplay.setFont(new Font("Monospaced", Font.PLAIN, 12));
        updateConfigDisplay();
        JScrollPane scrollPane = new JScrollPane(configDisplay);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        
        JPanel buttonPanel = new JPanel(new GridLayout(3, 1, 5, 5));
        
        JButton addRobotBtn = new JButton("Add Robot Agent");
        addRobotBtn.addActionListener(e -> addRobotAgent());
        buttonPanel.add(addRobotBtn);
        
        JButton addConveyorBtn = new JButton("Add Conveyor Agent");
        addConveyorBtn.addActionListener(e -> addConveyorAgent());
        buttonPanel.add(addConveyorBtn);
        
        JButton proceedBtn = new JButton("Proceed with Current Config");
        proceedBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        proceedBtn.addActionListener(e -> {
            configAccepted = true;
            dispose();
        });
        buttonPanel.add(proceedBtn);
        
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        add(mainPanel);
    }
    
    private void updateConfigDisplay() {
        StringBuilder sb = new StringBuilder();
        sb.append("ROBOTS (").append(Config.ROBOT_NAMES.size()).append("):\n");
        for (String robot : Config.ROBOT_NAMES) {
            sb.append("  - ").append(robot).append("\n");
        }
        
        sb.append("\nCONVEYORS (").append(Config.CONVEYOR_NAMES.size()).append("):\n");
        for (String conveyor : Config.CONVEYOR_NAMES) {
            sb.append("  - ").append(conveyor).append("\n");
        }
        
        sb.append("\nTASK LOCATIONS (").append(Config.TASK_LOCATIONS.size()).append("):\n");
        for (int i = 0; i < Config.TASK_LOCATIONS.size(); i++) {
            double[] loc = Config.TASK_LOCATIONS.get(i);
            sb.append("  ").append(i+1).append(". [").append(String.format("%.1f", loc[0])).append(", ")
              .append(String.format("%.1f", loc[1])).append(", ").append(String.format("%.1f", loc[2])).append("]\n");
        }
        
        sb.append("\nCHARGING STATIONS (").append(Config.CHARGING_STATIONS.size()).append("):\n");
        for (int i = 0; i < Config.CHARGING_STATIONS.size(); i++) {
            double[] cs = Config.CHARGING_STATIONS.get(i);
            sb.append("  ").append(i+1).append(". [").append(String.format("%.1f", cs[0])).append(", ")
              .append(String.format("%.1f", cs[1])).append(", ").append(String.format("%.1f", cs[2])).append("]\n");
        }
        
        configDisplay.setText(sb.toString());
    }
    
    private void addRobotAgent() {
        JPanel panel = new JPanel(new GridLayout(5, 2, 5, 5));
        
        JTextField nameField = new JTextField("Robot" + (Config.ROBOT_NAMES.size() + 1));
        JTextField csXField = new JTextField("0");
        JTextField csYField = new JTextField("0");
        JTextField csZField = new JTextField("0");
        
        panel.add(new JLabel("Robot Name:"));
        panel.add(nameField);
        panel.add(new JLabel("Charging Station X:"));
        panel.add(csXField);
        panel.add(new JLabel("Charging Station Y:"));
        panel.add(csYField);
        panel.add(new JLabel("Charging Station Z:"));
        panel.add(csZField);
        
        int result = JOptionPane.showConfirmDialog(this, panel, "Add Robot Agent", 
                                                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        
        if (result == JOptionPane.OK_OPTION) {
            try {
                String robotName = nameField.getText().trim();
                if (robotName.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Robot name cannot be empty");
                    return;
                }
                
                double csX = Double.parseDouble(csXField.getText().trim());
                double csY = Double.parseDouble(csYField.getText().trim());
                double csZ = Double.parseDouble(csZField.getText().trim());
                
                Config.addRobot(robotName);
                Config.addChargingStation(new double[]{csX, csY, csZ});
                
                updateConfigDisplay();
                JOptionPane.showMessageDialog(this, "Robot agent added successfully!");
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Invalid coordinate values. Please enter valid numbers.");
            }
        }
    }
    
    private void addConveyorAgent() {
        JPanel panel = new JPanel(new GridLayout(5, 2, 5, 5));
        
        JTextField nameField = new JTextField("Conveyor" + (Config.CONVEYOR_NAMES.size() + 1));
        JTextField locXField = new JTextField("0");
        JTextField locYField = new JTextField("0");
        JTextField locZField = new JTextField("0");
        
        panel.add(new JLabel("Conveyor Name:"));
        panel.add(nameField);
        panel.add(new JLabel("Task Location X:"));
        panel.add(locXField);
        panel.add(new JLabel("Task Location Y:"));
        panel.add(locYField);
        panel.add(new JLabel("Task Location Z:"));
        panel.add(locZField);
        
        int result = JOptionPane.showConfirmDialog(this, panel, "Add Conveyor Agent", 
                                                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        
        if (result == JOptionPane.OK_OPTION) {
            try {
                String conveyorName = nameField.getText().trim();
                if (conveyorName.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Conveyor name cannot be empty");
                    return;
                }
                
                double locX = Double.parseDouble(locXField.getText().trim());
                double locY = Double.parseDouble(locYField.getText().trim());
                double locZ = Double.parseDouble(locZField.getText().trim());
                
                double[] location = new double[]{locX, locY, locZ};
                Config.addConveyor(conveyorName);
                Config.addConveyorLocation(conveyorName, location);
                Config.addTaskLocation(location);
                
                updateConfigDisplay();
                JOptionPane.showMessageDialog(this, "Conveyor agent added successfully!");
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Invalid coordinate values. Please enter valid numbers.");
            }
        }
    }
    
    public boolean isConfigAccepted() {
        return configAccepted;
    }
    
    public static boolean showDialog() {
        ConfigDialog dialog = new ConfigDialog(null);
        dialog.setVisible(true);
        return dialog.isConfigAccepted();
    }
}
