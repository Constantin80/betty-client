package info.fmro.client.threads;

import info.fmro.client.objects.Statics;
import info.fmro.shared.utility.Generic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TimeJumpDetectorThread
        implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(TimeJumpDetectorThread.class);
    public static final long SLEEP_INTERVAL = 200L;

    @Override
    public void run() {
        long timeBeforeSleep, timeAfterSleep = System.currentTimeMillis(), timeDifference;
        while (!Statics.mustStop.get()) {
            try {
                timeBeforeSleep = System.currentTimeMillis();
                timeDifference = Math.abs(timeBeforeSleep - timeAfterSleep);
                if (timeBeforeSleep > timeAfterSleep) {
                    if (timeDifference > 10_000L) {
                        logger.warn("(b)major clock jump in the future of {} ms detected", timeDifference);
                    } else if (timeDifference > 1_000L) {
                        logger.info("(b)average clock jump in the future of {} ms detected", timeDifference);
                    } else if (timeDifference > 500L) {
                        logger.debug("(b)small time difference towards the future of {} ms detected", timeDifference);
                    } else if (timeDifference > 100L) {
                        logger.trace("(b)minor time difference towards the future of {} ms detected", timeDifference);
                    }
                } else if (timeBeforeSleep < timeAfterSleep) {
//                    if (timeDifference > 100L) {
//                        logger.error("(b)possible clock jump in the past of {} ms detected", timeDifference);
//                    } else if (timeDifference > 20L) {
//                        logger.info("(b)minor time difference towards the past of {} ms detected", timeDifference);
//                    }
                    logger.error("(b)possible clock jump in the past of {} ms detected", timeDifference);
                }

                Generic.threadSleep(SLEEP_INTERVAL);
                timeAfterSleep = System.currentTimeMillis();

                final long expectedTimeAfterSleep = timeBeforeSleep + SLEEP_INTERVAL;
                timeDifference = Math.abs(expectedTimeAfterSleep - timeAfterSleep);
                if (timeAfterSleep > expectedTimeAfterSleep) {
                    if (timeDifference > 5_000L) {
                        logger.warn("(a)major clock jump in the future of {} ms detected", timeDifference);
                    } else if (timeDifference > 1_000L) {
                        logger.info("(a)average clock jump in the future of {} ms detected", timeDifference);
                    } else if (timeDifference > 500L) {
                        logger.debug("(a)small time difference towards the future of {} ms detected", timeDifference);
                    } else if (timeDifference > 100L) {
                        logger.trace("(a)minor time difference towards the future of {} ms detected", timeDifference);
                    }
                } else if (timeAfterSleep < expectedTimeAfterSleep) {
                    if (timeDifference > 100L) {
                        logger.warn("(a)possible clock jump in the past of {} ms detected", timeDifference);
                    } else if (timeDifference > 20L) {
                        logger.info("(a)minor time difference towards the past of {} ms detected", timeDifference);
                    }
                }
            } catch (Throwable throwable) {
                logger.error("STRANGE ERROR inside TimeJumpDetectorThread loop", throwable);
            }
        } // end while

        logger.info("TimeJumpDetectorThread ends");
    }
}
