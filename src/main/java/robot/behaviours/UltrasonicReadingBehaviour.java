package robot.behaviours;

import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import robot.RobotConstants;
import robot.RobotContext;
import shared.dto.UltrasonicReadingDTO;
import robot.hardware.UltrasonicHardware;
import robot.utils.*;

public class UltrasonicReadingBehaviour extends TickerBehaviour {
    private final RobotContext ctx;

    public UltrasonicReadingBehaviour(Agent a, long period, RobotContext ctx) {
        super(a, period);
        this.ctx = ctx;
    }

    @Override
    protected void onTick() {
        if (ctx.getState() == RobotState.CHARGING || ctx.getState() == RobotState.STANDBY) return;

        long currentTimestamp = System.currentTimeMillis();
        long lastContextTimestamp = ctx.getLastUltrasonicReading().getTimestamp();
        long dt =  currentTimestamp - lastContextTimestamp;

        if(dt <= 0) return;

        int distance = RobotConstants.IS_SIMS ?
                Hardware.get().ultrasonicDummy().readRawSim() :
                UltrasonicHardware.readRaw();

        UltrasonicReadingDTO ultrasonicReadingDto = new UltrasonicReadingDTO(currentTimestamp, Math.min(distance, 200), dt);
//        DistancePidResultDTO lastDistancePidResult = ctx.getLastDistancePidResult();

//        lastDistancePidResult = DistancePidControl.computeControl(currentTimestamp, distanceDto.getDistance(), lastDistancePidResult.getForwardTarget(), lastDistancePidResult.isHalted(), ctx.getDistancePid(), dt);

        ctx.setLastUltrasonicReading(ultrasonicReadingDto);
//        System.out.println("New ultrasonic reading: "+ultrasonicReadingDto.toString());
    }

}
