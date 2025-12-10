package robot.behaviours;

import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import robot.RobotContext;
import robot.utils.RobotState;

import static shared.SharedConstants.MAX_BATTERY;
import static shared.SharedConstants.MIN_BATTERY;

public class RobotBatteryBehaviour extends TickerBehaviour {
    private final RobotContext ctx;

    public RobotBatteryBehaviour(Agent a, long period, RobotContext ctx) {
        super(a, period);
        this.ctx = ctx;
    }

    @Override
    protected void onTick() {
        int batteryPercentage = ctx.getRobotBatteryPercentage();
        RobotState state = ctx.getState();

        if((state == RobotState.CHARGING || state == RobotState.STANDBY)) {
            if(batteryPercentage < MAX_BATTERY){
                batteryPercentage += 10;
            }
        }

        else if(state == RobotState.PICKINGUP || state == RobotState.DELIVERING || state == RobotState.BACK_TO_STATION) {
            if(batteryPercentage > MIN_BATTERY){
                batteryPercentage -= 1;
            }
        }

        if(batteryPercentage < MIN_BATTERY){
            batteryPercentage = MIN_BATTERY;
        }

        if(batteryPercentage > MAX_BATTERY){
            batteryPercentage = MAX_BATTERY;
        }

        ctx.setRobotBatteryPercentage(batteryPercentage);
//        System.out.println("Battery Percentage: " + batteryPercentage);
    }
}
