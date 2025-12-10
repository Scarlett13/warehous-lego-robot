package server.digitaltwin;

import robot.utils.RobotState;

import static shared.SharedConstants.BATTERY_CHARGE_THRESHOLD;
import static shared.SharedConstants.MAX_BATTERY;

public class RobotStateChangeUtils {
    public RobotState changeRobotState (RobotState robotState, int batteryPercentage, boolean hasWorkId, boolean arrivedAtPrevDestination){
        RobotState newRobotState = robotState;
        switch(robotState){
            case STANDBY:
                if(hasWorkId && batteryPercentage > BATTERY_CHARGE_THRESHOLD){
                    newRobotState = RobotState.PICKINGUP;
                }
                break;
            case CHARGING:
                if(batteryPercentage < MAX_BATTERY){
                    break;
                }

                if (hasWorkId){
                    newRobotState = RobotState.DELIVERING;
                }
                else {
                    newRobotState = RobotState.STANDBY;
                }
                break;
            case DELIVERING:
                if(batteryPercentage < BATTERY_CHARGE_THRESHOLD){
                    newRobotState = RobotState.GOING_TO_CHARGE;
                    break;
                }

                if(arrivedAtPrevDestination){
                    newRobotState = RobotState.BACK_TO_STATION;
                }
                break;
            case PICKINGUP:
                if(batteryPercentage < BATTERY_CHARGE_THRESHOLD){
                    newRobotState = RobotState.GOING_TO_CHARGE;
                    break;
                }

                if(arrivedAtPrevDestination){
                    newRobotState = RobotState.DELIVERING;
                }
                break;
            case GOING_TO_CHARGE:
                if(arrivedAtPrevDestination){
                    newRobotState = RobotState.CHARGING;
                }
                break;
            case BACK_TO_STATION:
                if(batteryPercentage < BATTERY_CHARGE_THRESHOLD){
                    newRobotState = RobotState.GOING_TO_CHARGE;
                    break;
                }

                if(hasWorkId){
                    newRobotState = RobotState.PICKINGUP;
                    break;
                }

                if(arrivedAtPrevDestination){
                    newRobotState = RobotState.STANDBY;
                }
                break;
        }

        return newRobotState;
    }
}
