package shared.messaging;

public class MessagingConstants {
    public static final String ONTOLOGY = "teletubbies-mas";
    public static final String UI_ONTOLOGY = "teletubbies-mas-ui";

    // Topics
    public static final String ROBOT_ULTRASONIC_PID_TOPIC = "_ultrasonic_pid";
    public static final String ROBOT_POSITION_TOPIC = "_position";
    public static final String ROBOT_SPEED_PID_TOPIC = "_speed_pid";
    public static final String ROBOT_DESTINATION_TOPIC = "_destination";
    public static final String ROBOT_EVENTS_TOPIC = "robot_events";
    public static final String CONVEYOR_EVENTS_TOPIC = "conveyor_events";
    public static final String ROBOT_TURN_TOPIC = "_turn";
    public static final String UI_TOPICS = "ui-command";

    // topics for robot
    public static final String ROBOT_STATE_TOPICS = "robot_state";
    public static final String ROBOT_COMMAND_TOPICS = "robot_commands";

    // conversation for conveyor
    public static final String CONVEYOR_ITEMS_ADDED = "conveyor_items_added";

    // Item Request Protocol
    public static final String ONTOLOGY_ITEM_REQUEST = "item-request-ontology";
    public static final String CONVERSATION_ITEM_REQUEST = "request-item";
}
