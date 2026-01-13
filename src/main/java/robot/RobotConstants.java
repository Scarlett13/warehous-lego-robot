package robot;

public class RobotConstants {

    public RobotConstants() {
    }

    public static final String ROBOT_NAME = "T_TinkyWinky";
    public static final String JADE_HOST_ADDRESS = "localhost";
    // -------- MQTT --------
    public static final String MQTT_HOST = "wss://mqtt.cloud.pozyxlabs.com:443";
    public static final String MQTT_TOPIC = "61d730870295a7f3798fdb31";
    public static final String MQTT_USERNAME = "61d730870295a7f3798fdb31";
    public static final String MQTT_PASSWORD = "1d761f94-6fe7-4549-aaa5-73a4ffecc2ee";
    public static final String MQTT_TAG = "682E";

    // Reference Shared Config
    public static final boolean IS_SIMS = shared.SharedConstants.IS_SIMS;

    // -------- Distances --------
    public static final double STOP_DISTANCE = 8.0;
    public static final double RESUME_DISTANCE = 12.0;
    public static final double AVOID_DISTANCE = 35.0;
    public static final double FAR_DISTANCE = 200.0;
    public static final double MAX_DISTANCE = 250.0;
    public static final double SPEED_UP_DISTANCE = 40.0;

    // -------- Motor & control limits --------
    public static final double SPEED_MIN = 320.0;
    public static final double SPEED_MAX = 850.0;
    public static final double TURN_MAX = 1;
    public static final double SLEW_RATE = 180.0;

    // -------- Goal thresholds (mm) --------
    public static final double DEST_GOAL_STOP = 20; // 1) reaching destination
    public static final double DEST_GOAL_TURN_ANGLE = 1.1; // 4) begin turn bias toward destination
    public static final double DEST_GOAL_SLOW = 200.0; // 3) slow down window
    public static final double DEST_GOAL_FAST = 500.0; // 2) speed up window

    // -------- CommonPid gains (base + trim pattern) --------
    public static final double ULTRASONIC_P = 0.4;
    public static final double ULTRASONIC_I = 0.05;
    public static final double ULTRASONIC_D = 0.4;

    public static final double LOCALISATION_P = 0.4;
    public static final double LOCALISATION_I = 0.05;
    public static final double LOCALISATION_D = 0.4;

    // -------- Loop timing --------
    public static final int LOOP_HZ = 1;
}
