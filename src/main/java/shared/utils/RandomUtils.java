package shared.utils;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

public class RandomUtils {
    public static long randomFutureMillis(int minMinutes, int maxMinutes) {
        if (minMinutes < 0 || maxMinutes < minMinutes) {
            throw new IllegalArgumentException("Invalid minute range");
        }

        long minMs = Duration.ofMinutes(minMinutes).toMillis();
        long maxMs = Duration.ofMinutes(maxMinutes).toMillis();

        // nextLong(origin, bound) uses an exclusive upper bound
        long offset = ThreadLocalRandom.current().nextLong(minMs, maxMs + 1);

        return System.currentTimeMillis() + offset;
    }

    public static int randomFreshness(){
        return ThreadLocalRandom.current().nextInt(1, 11);
    }
}
