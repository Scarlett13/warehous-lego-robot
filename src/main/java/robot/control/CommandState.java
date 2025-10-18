package robot.control;

import java.util.concurrent.atomic.AtomicBoolean;

@Deprecated
public class CommandState {
    // forward speed setpoint (deg/s)
    public volatile double forwardDegPerSec = 0.0;
    // turn setpoint (fraction of forward speed, [-1 / +1], clamped in mixer)
    public volatile double turnFrac = 0.0;
    // emergency stop flag
    public final AtomicBoolean stop = new AtomicBoolean(false);
}
