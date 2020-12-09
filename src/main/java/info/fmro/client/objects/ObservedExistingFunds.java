package info.fmro.client.objects;

import info.fmro.client.main.GUI;
import info.fmro.shared.logic.ExistingFunds;
import info.fmro.shared.stream.objects.ListOfQueues;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;

public class ObservedExistingFunds
        extends ExistingFunds
        implements Serializable {
    @Serial
    private static final long serialVersionUID = -9002646711271737365L;

    @Serial
    private void readObject(@NotNull final java.io.ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        this.listOfQueues = new ListOfQueues();
    }

    @Override
    public synchronized void setAvailableFunds(final double newAvailableFunds) {
        super.setAvailableFunds(newAvailableFunds);
        GUI.updateAvailableLabel(this.getAvailableFunds());
    }

    @Override
    public synchronized boolean setReserve(final double newReserve) {
        final boolean modified = super.setReserve(newReserve);
        if (modified) {
            GUI.updateReserveLabel(this.getReserve());
        } else { // no modification made, nothing to be done
        }
        return modified;
    }

    @Override
    public synchronized void setExposure(final double newExposure) {
        super.setExposure(newExposure);
        GUI.updateExposureLabel(this.getExposure());
    }

    @Override
    public synchronized void setTotalFunds(final double newValue) {
        super.setTotalFunds(newValue);
        GUI.updateTotalFundsLabel(this.getTotalFunds());
    }
}
