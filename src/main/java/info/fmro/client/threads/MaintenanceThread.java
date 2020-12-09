package info.fmro.client.threads;

import info.fmro.client.main.VarsIO;
import info.fmro.client.objects.Statics;
import info.fmro.shared.objects.SharedStatics;
import info.fmro.shared.utility.Generic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("ReuseOfLocalVariable")
public class MaintenanceThread
        extends Thread {
    private static final Logger logger = LoggerFactory.getLogger(MaintenanceThread.class);

    private static long timedPrintAverages() {
        long timeForNext = SharedStatics.timeStamps.getLastPrintAverages();
        long timeTillNext = timeForNext - System.currentTimeMillis();
        if (timeTillNext <= 0) {
            SharedStatics.timeStamps.lastPrintAveragesStamp(Statics.DELAY_PRINT_AVERAGES);

            final int size = SharedStatics.linkedBlockingQueue.size();
            if (size >= 10) {
                logger.error("elements in linkedBlockingQueue: {}", size);
            } else if (size > 0) {
                logger.warn("elements in linkedBlockingQueue: {}", size);
            }

            timeForNext = SharedStatics.timeStamps.getLastPrintAverages();

            timeTillNext = timeForNext - System.currentTimeMillis();
        } else { // nothing to be done
        }
        return timeTillNext;
    }

    private static long timedPrintDebug() {
        long timeForNext = SharedStatics.timeStamps.getLastPrintDebug();
        long timeTillNext = timeForNext - System.currentTimeMillis();
        if (timeTillNext <= 0) {
            SharedStatics.timeStamps.lastPrintDebugStamp(Generic.MINUTE_LENGTH_MILLISECONDS << 1);
            logger.debug("maxMemory: {} totalMemory: {} freeMemory: {}", Generic.addCommas(Runtime.getRuntime().maxMemory()), Generic.addCommas(Runtime.getRuntime().totalMemory()), Generic.addCommas(Runtime.getRuntime().freeMemory()));
            logger.debug("threadPool active/mostEver: {}/{} scheduledPool active/mostEver: {}/{}", SharedStatics.threadPoolExecutor.getActiveCount(), SharedStatics.threadPoolExecutor.getLargestPoolSize(),
                         Statics.scheduledThreadPoolExecutor.getActiveCount(), Statics.scheduledThreadPoolExecutor.getLargestPoolSize());

            timeForNext = SharedStatics.timeStamps.getLastPrintDebug();

            timeTillNext = timeForNext - System.currentTimeMillis();
        } else { // nothing to be done
        }
        return timeTillNext;
    }

    private static long timedSaveObjects() {
        long timeForNext = SharedStatics.timeStamps.getLastObjectsSave();
        long timeTillNext = timeForNext - System.currentTimeMillis();
        if (timeTillNext <= 0) {
            VarsIO.writeObjectsToFiles();
            timeForNext = SharedStatics.timeStamps.getLastObjectsSave();

            timeTillNext = timeForNext - System.currentTimeMillis();
        } else { // nothing to be done
        }
        return timeTillNext;
    }

    @Override
    public void run() {
        while (!SharedStatics.mustStop.get()) {
            try {
                long timeToSleep = 5L * Generic.MINUTE_LENGTH_MILLISECONDS; // initialized with maximum sleep time

                if (SharedStatics.mustWriteObjects.get()) {
                    VarsIO.writeObjectsToFiles();
                    SharedStatics.mustWriteObjects.set(false);
                }

                timeToSleep = Math.min(timeToSleep, timedSaveObjects());
                timeToSleep = Math.min(timeToSleep, timedPrintDebug());
                timeToSleep = Math.min(timeToSleep, timedPrintAverages());

                Generic.threadSleepSegmented(timeToSleep, 100L, SharedStatics.mustStop, SharedStatics.mustWriteObjects);
            } catch (Throwable throwable) {
                logger.error("STRANGE ERROR inside Maintenance loop", throwable);
            }
        } // end while

        logger.debug("maintenance thread ends");
    }
}
