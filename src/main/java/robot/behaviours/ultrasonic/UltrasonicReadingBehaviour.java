package robot.behaviours.ultrasonic;

import ev3dev.sensors.ev3.EV3UltrasonicSensor;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import lejos.hardware.port.SensorPort;
import lejos.robotics.SampleProvider;
import robot.Constants;
import robot.RobotContext;
import robot.acl.Acl;
import robot.dto.DistanceDTO;
import robot.utils.*;

public class UltrasonicReadingBehaviour extends TickerBehaviour {
    private final AID topic;
    private final RobotContext ctx;
    private SampleProvider distMode;
    private UltrasonicReadingUtil ultrasonic;
    private EV3UltrasonicSensor usFront = new EV3UltrasonicSensor(SensorPort.S2);

    public UltrasonicReadingBehaviour(Agent a, long period, RobotContext ctx) {
        super(a, period);
        this.topic = TopicHelper.topic(a, Constants.US_DISTANCE_TOPIC);
        this.ctx = ctx;


        distMode = usFront.getDistanceMode();
        ultrasonic = new UltrasonicReadingUtil(distMode);
    }

    @Override
    protected void onTick() {
        if (ctx.getState() != RobotState.WORKING) return;

        long currentTimestamp = System.currentTimeMillis();
        long lastContextTimestamp = ctx.getDistance().getTimestamp();
        if(lastContextTimestamp >= currentTimestamp) return;

        double distance = Constants.IS_SIMS ?
                Hardware.get().ultrasonicDummy().readRawSim() :
                ultrasonic.readRaw();

        DistanceDTO distanceDto = new DistanceDTO(currentTimestamp, distance > 200 ? 200 : distance);

        ctx.setDistance(distanceDto);
        Acl.publish(myAgent, topic, JsonUtil.toJson(distanceDto));
        System.out.println(distanceDto);
    }

}
