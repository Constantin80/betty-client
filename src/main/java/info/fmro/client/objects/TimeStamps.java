package info.fmro.client.objects;

import info.fmro.shared.utility.Generic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;

public class TimeStamps
        implements Serializable {
    private static final Logger logger = LoggerFactory.getLogger(TimeStamps.class);
    private static final long serialVersionUID = 3910852662747610947L;
    private long lastObjectsSave, lastPrintDebug, lastPrintAverages;

    public synchronized long getLastObjectsSave() {
        return this.lastObjectsSave;
    }

    public synchronized void setLastObjectsSave(final long lastObjectsSave) {
        this.lastObjectsSave = lastObjectsSave;
    }

    public synchronized void lastObjectsSaveStamp() {
        this.lastObjectsSave = System.currentTimeMillis();
    }

    public synchronized void lastObjectsSaveStamp(final long timeStamp) {
        final long currentTime = System.currentTimeMillis();
        if (currentTime - this.lastObjectsSave >= timeStamp) {
            this.lastObjectsSave = currentTime + timeStamp;
        } else {
            this.lastObjectsSave += timeStamp;
        }
    }

    public synchronized long getLastPrintDebug() {
        return this.lastPrintDebug;
    }

    public synchronized void setLastPrintDebug(final long lastPrintDebug) {
        this.lastPrintDebug = lastPrintDebug;
    }

    public synchronized void lastPrintDebugStamp() {
        this.lastPrintDebug = System.currentTimeMillis();
    }

    public synchronized void lastPrintDebugStamp(final long timeStamp) {
        final long currentTime = System.currentTimeMillis();
        if (currentTime - this.lastPrintDebug >= timeStamp) {
            this.lastPrintDebug = currentTime + timeStamp;
        } else {
            this.lastPrintDebug += timeStamp;
        }
    }

    public synchronized long getLastPrintAverages() {
        return this.lastPrintAverages;
    }

    public synchronized void setLastPrintAverages(final long lastPrintAverages) {
        this.lastPrintAverages = lastPrintAverages;
    }

    public synchronized void lastPrintAveragesStamp() {
        this.lastPrintAverages = System.currentTimeMillis();
    }

    public synchronized void lastPrintAveragesStamp(final long timeStamp) {
        final long currentTime = System.currentTimeMillis();
        if (currentTime - this.lastPrintAverages >= timeStamp) {
            this.lastPrintAverages = currentTime + timeStamp;
        } else {
            this.lastPrintAverages += timeStamp;
        }
    }

    public synchronized void copyFrom(final TimeStamps timeStamps) {
        if (timeStamps == null) {
            logger.error("null timeStamps in copyFrom for: {}", Generic.objectToString(this));
        } else {
            this.lastObjectsSave = timeStamps.lastObjectsSave;
            this.lastPrintDebug = timeStamps.lastPrintDebug;
            this.lastPrintAverages = timeStamps.lastPrintAverages;
        }
    }
}
