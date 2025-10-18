package robot.pid;

import robot.Main;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;

/** Generic fixed-rate PID loop. Error comes from a sensor-owned supplier. */
public class PidLoop implements Runnable {
    private final int hz;
    private final double kp, ki, kd, iClamp;
    private final DoubleSupplier errorSupplier;
    private final DoubleConsumer outputConsumer;
    private final AtomicBoolean running;

    private double prevError = 0.0;
    private double integral  = 0.0;

    public PidLoop(int hz, double kp, double ki, double kd, double iClamp,
                   DoubleSupplier errorSupplier,
                   DoubleConsumer outputConsumer,
                   AtomicBoolean runningFlag) {
        this.hz = hz;
        this.kp = kp; this.ki = ki; this.kd = kd; this.iClamp = iClamp;
        this.errorSupplier = errorSupplier;
        this.outputConsumer = outputConsumer;
        this.running = runningFlag;
    }

    @Override
    public void run() {
        final long periodNs = 1_000_000_000L / hz;
        long tNext = System.nanoTime();

        while (running.get()) {
            long tStart = System.nanoTime();

            double err = errorSupplier.getAsDouble();
            double dt = Math.max(1.0 / hz, (System.nanoTime() - tStart) / 1e9);

            integral += err * dt;
            if (integral > iClamp)  integral = iClamp;
            if (integral < -iClamp) integral = -iClamp;

            double deriv = (err - prevError) / dt;
            prevError = err;

            double out = kp * err + ki * integral + kd * deriv;
            outputConsumer.accept(out);

            tNext = Main.getNext(tNext, periodNs);
        }
    }
}
