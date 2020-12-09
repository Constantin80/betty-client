package info.fmro.client.main;

import info.fmro.client.objects.Statics;
import info.fmro.shared.objects.SharedStatics;
import info.fmro.shared.objects.TimeStamps;
import info.fmro.shared.utility.Generic;
import org.jetbrains.annotations.Contract;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map.Entry;

@SuppressWarnings("UtilityClass")
public final class VarsIO {
    private static final Logger logger = LoggerFactory.getLogger(VarsIO.class);

    @Contract(pure = true)
    private VarsIO() {
    }

    static void readObjectsFromFiles() {
        for (final Entry<String, String> entry : Statics.objectFileNamesMap.entrySet()) {
            final String key = entry.getKey();
            final Object objectFromFile = Generic.readObjectFromFile(entry.getValue());
            if (objectFromFile != null) {
                try {
                    //noinspection SwitchStatementWithTooFewBranches
                    switch (key) {
                        case "timeStamps" -> {
                            final TimeStamps timeStamps = (TimeStamps) objectFromFile;
                            SharedStatics.timeStamps.copyFrom(timeStamps);
                        }
                        default -> logger.error("unknown object in the fileNames map: {} {}", key, entry.getValue());
                    } // end switch
                } catch (ClassCastException classCastException) { // the object class was probably changed recently
                    logger.error("classCastException while reading objects from files for: {}", key, classCastException);
                } catch (@SuppressWarnings("ProhibitedExceptionCaught") NullPointerException nullPointerException) { // the object class was probably changed recently; this is normally thrown from copyFrom methods
                    logger.error("nullPointerException while reading objects from files for: {}", key, nullPointerException);
                }
            } else {
                logger.warn("objectFromFile null for: {} {}", key, entry.getValue());
            }
        } // end for
        logger.debug("have read objects from files");
    }

    public static void writeObjectsToFiles() {
        SharedStatics.timeStamps.lastObjectsSaveStamp(Generic.MINUTE_LENGTH_MILLISECONDS * 10L);
        SharedStatics.timeLastSaveToDisk.set(System.currentTimeMillis());
        for (final Entry<String, String> entry : Statics.objectFileNamesMap.entrySet()) {
            final String key = entry.getKey();
            //noinspection SwitchStatementWithTooFewBranches
            switch (key) {
                case "timeStamps" -> Generic.synchronizedWriteObjectToFile(SharedStatics.timeStamps, entry.getValue());
                default -> logger.error("unknown key in the fileNames map: {} {}", key, entry.getValue());
            } // end switch
        } // end for
    }
}
