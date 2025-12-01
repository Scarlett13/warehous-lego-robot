package robot.behaviours;

import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import robot.RobotConstants;
import robot.RobotContext;
import robot.hardware.MotorHardware;
import robot.utils.RobotState;

public class MotorActuatorBehaviour extends TickerBehaviour {
    private final RobotContext ctx;
    private long prevTimestamp = System.currentTimeMillis();

    public MotorActuatorBehaviour(Agent a, long period, RobotContext ctx) {
        super(a, period);
        this.ctx = ctx;
    }


    @Override
    protected void onTick() {
        long now = System.currentTimeMillis();
        double deltaTime = (now - prevTimestamp)/1e9;
        prevTimestamp = now;

        if(ctx.getState() != RobotState.WORKING){
            MotorHardware.apply(0, 0, 0);
            return;
        }

        if(RobotConstants.IS_SIMS){
            System.out.println("Simulating motor speed: "+0+", Turn speed: "+0);
        }
        else{
            System.out.println("Simulating motor speed: "+0+", Turn speed: "+0);
            MotorHardware.apply(0, 0, deltaTime);
        }
    }
}
