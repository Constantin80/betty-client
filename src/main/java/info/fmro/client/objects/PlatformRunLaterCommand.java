package info.fmro.client.objects;

import org.apache.commons.lang3.SerializationUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serial;
import java.io.Serializable;

public class PlatformRunLaterCommand<T extends Enum<T>>
        implements Serializable {
    private static final Logger logger = LoggerFactory.getLogger(PlatformRunLaterCommand.class);
    @Serial
    private static final long serialVersionUID = 140177397632319612L;
    private final T command;
    @Nullable
    private final Serializable[] objectsToModify;

    public PlatformRunLaterCommand(final T command, final Serializable... objectsToModify) {
        this.command = command;
        if (objectsToModify == null) {
            logger.error("null objectsToModify in PlatformRunLaterCommand constructor for: {} {}", command == null ? null : command.getClass(), command);
            this.objectsToModify = null;
        } else {
            final int size = objectsToModify.length;
            this.objectsToModify = new Serializable[size];
            for (int i = 0; i < size; i++) {
                this.objectsToModify[i] = SerializationUtils.clone(objectsToModify[i]);
            }
        }
    }

    public synchronized T getCommand() {
        return this.command;
    }

    @Nullable
    public synchronized Serializable[] getArray() {
        @Nullable final Serializable[] returnValue;
        if (this.objectsToModify == null) {
            returnValue = null;
        } else {
            final int size = this.objectsToModify.length;
            returnValue = new Serializable[size];
            System.arraycopy(this.objectsToModify, 0, returnValue, 0, size);
        }

        return returnValue;
    }

    public synchronized PlatformRunLaterCommand<T> getCopy() {
        return SerializationUtils.clone(this);
    }
}
