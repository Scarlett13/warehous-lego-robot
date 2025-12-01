package robot.sims;

import shared.dto.old.PozyxPointDTO;

public class DummyPositioningSim {
    private static final int HOLD_SEC = 60;  // 0..4 → 200
    private static final int DOWN_SEC = 28; // then 18 s decreasing by 10/s
    private static final int UP_SEC   = 25; // then 25 s increasing by 10/s
    private final long startMillis = System.currentTimeMillis();
    private int x = 0, y=0;
    private float angle = 0f;

    public DummyPositioningSim() {
    }

    public PozyxPointDTO distanceAt() {
        long now = System.currentTimeMillis();
        double tSec = Math.max(0.0, (now - startMillis) / 1000.0);


        // Phase 1: hold at 200 for first 5 seconds
        if (tSec < HOLD_SEC) {
            x+=10;
        }
        if(tSec > 56) {
            y+=10;
        }

        return new PozyxPointDTO(x, y);
    }

    public PozyxPointDTO getPositionSim() {
//        double r = ThreadLocalRandom.current().nextDouble();
//        if (r < 0.85) return 200.0;
//        else if (r < 0.95) return 15.0;
//        else return 20.0;
//        return 20.0;
//        return distanceSim;
        return distanceAt();
    }
}
