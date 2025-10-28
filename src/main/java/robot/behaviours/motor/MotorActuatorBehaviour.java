package robot.behaviours.motor;

import ev3dev.actuators.lego.motors.EV3LargeRegulatedMotor;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.MessageTemplate;
import lejos.hardware.port.MotorPort;
import robot.Constants;
import robot.RobotContext;
import robot.acl.Acl;
import robot.dto.SpeedDTO;
import robot.dto.TurnDTO;
import robot.utils.Hardware;
import robot.utils.MotorMixer;
import robot.utils.RobotState;
import robot.utils.TopicHelper;

import static robot.Constants.*;
import static robot.Constants.SLEW_RATE;

public class MotorActuatorBehaviour extends TickerBehaviour {
    private final RobotContext ctx;
    private long prevTimestamp = System.currentTimeMillis();
    private EV3LargeRegulatedMotor left;
    private EV3LargeRegulatedMotor right;
    private MotorMixer mixer;
    private double prevLeft = 0.0, prevRight = 0.0;

    public MotorActuatorBehaviour(Agent a, long period, RobotContext ctx) {
        super(a, period);
        this.ctx = ctx;

        left  = new EV3LargeRegulatedMotor(MotorPort.D);
        right = new EV3LargeRegulatedMotor(MotorPort.A);
        left.setAcceleration(800);
        right.setAcceleration(800);
        mixer = new MotorMixer(SPEED_MIN, SPEED_MAX, TURN_MAX, SLEW_RATE, left, right);
    }


    @Override
    protected void onTick() {
        long now = System.currentTimeMillis();
        double deltaTime = (now - prevTimestamp)/1e9;
        prevTimestamp = now;

        if(ctx.getState() != RobotState.WORKING){
            return;
        }

        SpeedDTO currentSpeed = ctx.getLastMotorSpeed();
        TurnDTO currentTurn = ctx.getLastTurnSpeed();

        if(Constants.IS_SIMS){
            System.out.println("Simulating motor speed: "+currentSpeed+", Turn speed: "+currentTurn);
        }
        else{
            System.out.println("Simulating motor speed: "+currentSpeed.getSpeed()+", Turn speed: "+currentTurn.getTurnFraction());
            mixer.apply(currentSpeed.getSpeed(), currentTurn.getTurnFraction(), deltaTime);
        }
    }

//    public void apply(double forwardDegPerSec, double turnFrac, double dt) {
//        // clamp inputs
//        double fwd  = clamp(forwardDegPerSec, 0.0, SPEED_MAX);
//        double turn = clamp(turnFrac, -TURN_MAX, +TURN_MAX);
//
//        // floor only if moving
//        if (fwd > 0 && fwd < SPEED_MIN) fwd = SPEED_MIN;
//
//        // mix
//        double leftCmd  = fwd * (1.0 - turn);
//        double rightCmd = fwd * (1.0 + turn);
//
//        prevLeft = leftCmd;
//        prevRight = rightCmd;
//
//        // apply
//        applyMotor(left,  (int) leftCmd);
//        applyMotor(right, (int) rightCmd);
//    }
//
//
//    private static void applyMotor(EV3LargeRegulatedMotor m, int dps) {
////        dps = Math.max(0.0, dps);
//        System.out.println("dps:  "+dps+"intdps: "+(int) dps);
//        m.setSpeed((int) dps);
////        if (dps == 0.0) m.stop(true);
////        else m.backward();
//        m.backward();
//    }
//
//    private static double clamp(double v, double lo, double hi) {
//        return Math.max(lo, Math.min(hi, v));
//    }
}
