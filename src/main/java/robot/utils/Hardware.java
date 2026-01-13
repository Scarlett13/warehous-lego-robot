package robot.utils;

import ev3dev.actuators.lego.motors.EV3LargeRegulatedMotor;
import ev3dev.sensors.ev3.EV3UltrasonicSensor;
import robot.RobotConstants;
import robot.hardware.MotorMixer;
import robot.sims.UltrasonicReadingUtilDummy;

@Deprecated
public final class Hardware {
    private static final Hardware INSTANCE = new Hardware();

    private EV3LargeRegulatedMotor left;
    private EV3LargeRegulatedMotor right;
    private EV3UltrasonicSensor usFront;
//    private SampleProvider distMode;

//    private UltrasonicReadingUtil ultrasonic;
    private final UltrasonicReadingUtilDummy dummyUltrasonic = new UltrasonicReadingUtilDummy();

    private MotorMixer mixer;

    public Hardware() {
//        if (!Constants.IS_SIMS) {
//            // ONLY on the brick
//            left  = new EV3LargeRegulatedMotor(MotorPort.D);
//            right = new EV3LargeRegulatedMotor(MotorPort.A);
//            left.setAcceleration(800);
//            right.setAcceleration(800);
//
//            usFront  = new EV3UltrasonicSensor(SensorPort.S2);
//            distMode = usFront.getDistanceMode();
//            ultrasonic = new UltrasonicReadingUtil(distMode);
//            mixer = new MotorMixer(SPEED_MIN, SPEED_MAX, TURN_MAX, SLEW_RATE, left, right);
//        } else {
//            mixer = new MotorMixer(SPEED_MIN, SPEED_MAX, TURN_MAX, SLEW_RATE, null, null); // if your mixer needs motors, stub apply()
//        }
    }

    public void init(){
//        if (!Constants.IS_SIMS) {
//            // ONLY on the brick
//            left  = new EV3LargeRegulatedMotor(MotorPort.D);
//            right = new EV3LargeRegulatedMotor(MotorPort.A);
//            left.setAcceleration(800);
//            right.setAcceleration(800);
//
//            usFront  = new EV3UltrasonicSensor(SensorPort.S2);
////            distMode = usFront.getDistanceMode();
////            ultrasonic = new UltrasonicReadingUtil(distMode);
//            mixer = new MotorMixer(SPEED_MIN, SPEED_MAX, TURN_MAX, SLEW_RATE, left, right);
//        } else {
//            mixer = new MotorMixer(SPEED_MIN, SPEED_MAX, TURN_MAX, SLEW_RATE, null, null); // if your mixer needs motors, stub apply()
//        }
    }

    public static Hardware get() {
        return INSTANCE;
    }

    // Guard getters so they’re not used in sim
    public EV3LargeRegulatedMotor left() {
        if (RobotConstants.IS_SIMS) throw new IllegalStateException("left() not available in SIM mode");
        return left;
    }

    public EV3LargeRegulatedMotor right() {
        if (RobotConstants.IS_SIMS) throw new IllegalStateException("right() not available in SIM mode");
        return right;
    }

    public EV3UltrasonicSensor getUsFront() {
        return this.usFront;
    }

//    public UltrasonicReadingUtil ultrasonic() {
//        if (Constants.IS_SIMS) throw new IllegalStateException("ultrasonic() not available in SIM mode");
//        return ultrasonic;
//    }

    public UltrasonicReadingUtilDummy ultrasonicDummy() {
        return dummyUltrasonic;
    }

    public MotorMixer mixer() {
        return mixer;
    }
}

