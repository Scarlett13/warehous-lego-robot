package shared.messaging;

public class MessagingConstants {
    public static final String ONTOLOGY = "teletubbies-mas";
    // Topics
    public static final String ROBOT_ULTRASONIC_PID_TOPIC = "_ultrasonic_pid";
    public static final String ROBOT_POSITION_TOPIC = "_position";
    public static final String ROBOT_SPEED_PID_TOPIC = "_speed_pid";
    public static final String ROBOT_DESTINATION_TOPIC = "_destination";
    public static final String ROBOT_EVENTS_TOPIC = "robot_events";
    public static final String CONVEYOR_EVENTS_TOPIC = "conveyor_events";
    public static final String ROBOT_TURN_TOPIC     = "_turn";

    //conversation for robot
    public static final String ROBOT_STATE = "robot_state";
    public static final String ROBOT_TARGET_DESTINATION = "robot_target_destination";


    //conversation for conveyor
    public static final String CONVEYOR_ITEMS_ADDED = "conveyor_items_added";
}
