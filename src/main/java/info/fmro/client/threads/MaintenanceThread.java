package info.fmro.client.threads;

import info.fmro.client.main.VarsIO;
import info.fmro.client.objects.Statics;
import info.fmro.shared.utility.Generic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("ReuseOfLocalVariable")
public class MaintenanceThread
        extends Thread {
    private static final Logger logger = LoggerFactory.getLogger(MaintenanceThread.class);

    private static long timedPrintAverages() {
        long timeForNext = Statics.timeStamps.getLastPrintAverages();
        long timeTillNext = timeForNext - System.currentTimeMillis();
        if (timeTillNext <= 0) {
            Statics.timeStamps.lastPrintAveragesStamp(Statics.DELAY_PRINT_AVERAGES);

            final int size = Statics.linkedBlockingQueue.size();
            if (size >= 10) {
                logger.error("elements in linkedBlockingQueue: {}", size);
            } else if (size > 0) {
                logger.warn("elements in linkedBlockingQueue: {}", size);
            }

            timeForNext = Statics.timeStamps.getLastPrintAverages();

            timeTillNext = timeForNext - System.currentTimeMillis();
        } else { // nothing to be done
        }
        return timeTillNext;
    }

    private static long timedPrintDebug() {
        long timeForNext = Statics.timeStamps.getLastPrintDebug();
        long timeTillNext = timeForNext - System.currentTimeMillis();
        if (timeTillNext <= 0) {
            Statics.timeStamps.lastPrintDebugStamp(Generic.MINUTE_LENGTH_MILLISECONDS << 1);
            logger.info("maxMemory: {} totalMemory: {} freeMemory: {}", Generic.addCommas(Runtime.getRuntime().maxMemory()), Generic.addCommas(Runtime.getRuntime().totalMemory()), Generic.addCommas(Runtime.getRuntime().freeMemory()));
            logger.info("threadPool active/mostEver: {}/{} scheduledPool active/mostEver: {}/{}", Statics.threadPoolExecutor.getActiveCount(), Statics.threadPoolExecutor.getLargestPoolSize(), Statics.scheduledThreadPoolExecutor.getActiveCount(),
                        Statics.scheduledThreadPoolExecutor.getLargestPoolSize());

            timeForNext = Statics.timeStamps.getLastPrintDebug();

            timeTillNext = timeForNext - System.currentTimeMillis();
        } else { // nothing to be done
        }
        return timeTillNext;
    }

    private static long timedSaveObjects() {
        long timeForNext = Statics.timeStamps.getLastObjectsSave();
        long timeTillNext = timeForNext - System.currentTimeMillis();
        if (timeTillNext <= 0) {
            VarsIO.writeObjectsToFiles();
            timeForNext = Statics.timeStamps.getLastObjectsSave();

            timeTillNext = timeForNext - System.currentTimeMillis();
        } else { // nothing to be done
        }
        return timeTillNext;
    }

    @Override
    public void run() {
        while (!Statics.mustStop.get()) {
            try {
                long timeToSleep = 5L * Generic.MINUTE_LENGTH_MILLISECONDS; // initialized with maximum sleep time

                if (Statics.mustWriteObjects.get()) {
                    VarsIO.writeObjectsToFiles();
                    Statics.mustWriteObjects.set(false);
                }

                timeToSleep = Math.min(timeToSleep, timedSaveObjects());
                timeToSleep = Math.min(timeToSleep, timedPrintDebug());
                timeToSleep = Math.min(timeToSleep, timedPrintAverages());

                Generic.threadSleepSegmented(timeToSleep, 100L, Statics.mustStop, Statics.mustWriteObjects);
            } catch (Throwable throwable) {
                logger.error("STRANGE ERROR inside Maintenance loop", throwable);
            }
        } // end while

        logger.info("maintenance thread ends");
    }
}
