package robot.behaviours;

import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import robot.RobotConstants;
import robot.RobotContext;
import server.control.UltrasonicPidControl;
import shared.DistancePidResultDTO;
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
        DistancePidResultDTO lastDistancePidResult = ctx.getUltrasonicPidResult();

        DistancePidResultDTO newDistancePidResult = UltrasonicPidControl.computeControl(currentTimestamp, distance, lastDistancePidResult.getForwardTarget(), lastDistancePidResult.isHalted(), ctx.getDistancePid(), dt);

        ctx.setLastUltrasonicReading(ultrasonicReadingDto);
        ctx.setUltrasonicPidResult(newDistancePidResult);
//        System.out.println("New ultrasonic reading: "+ultrasonicReadingDto.toString());
    }

}
