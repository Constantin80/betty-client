package info.fmro.client.objects;

import info.fmro.client.main.GUI;
import info.fmro.shared.entities.MarketCatalogue;
import info.fmro.shared.logic.ExistingFunds;
import info.fmro.shared.logic.ManagedEvent;
import info.fmro.shared.logic.ManagedMarket;
import info.fmro.shared.logic.RulesManager;
import info.fmro.shared.stream.cache.order.OrderCache;
import info.fmro.shared.stream.objects.OrdersThreadInterface;
import info.fmro.shared.utility.SynchronizedMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Objects;

public class ClientRulesManager
        extends RulesManager
        implements Serializable {
    private static final Logger logger = LoggerFactory.getLogger(ClientRulesManager.class);
    private static final long serialVersionUID = -3456154120718631120L;

    @Override
    public synchronized boolean copyFrom(final RulesManager other, final boolean isReadingFromStream) {
        final boolean readSuccessful = super.copyFrom(other, isReadingFromStream);
        if (readSuccessful) {
            GUI.initializeRulesManagerTreeView();
        } else { // I probably shouldn't initialize if !readSuccessful
        }
        return readSuccessful;
    }

    @Override
    public synchronized ManagedEvent removeManagedEvent(final String eventId) {
        @Nullable final ManagedEvent existingManagedEvent = this.events.get(eventId);
        if (existingManagedEvent != null) {
            @Nullable final HashSet<String> marketIds = existingManagedEvent.marketIds.copy();
            GUI.publicRemoveManagedEvent(eventId, marketIds); // GUI updated before actual removal, to still find the necessary info in the maps
        } else { // nothing will be removed
        }

        return super.removeManagedEvent(eventId);
    }

    @Override
    public synchronized void executeCommand(@NotNull final String commandString, @NotNull final OrdersThreadInterface pendingOrdersThread, @NotNull final OrderCache orderCache, @NotNull final ExistingFunds safetyLimits,
                                            @NotNull final SynchronizedMap<String, ? extends MarketCatalogue> marketCataloguesMap) {
        logger.error("executeCommand method is not and should not be implemented in Client");
    }

    @Override
    @NotNull
    protected synchronized ManagedEvent addManagedEvent(final String eventId) {
        final ManagedEvent existingEvent = this.events.get(eventId);
        final ManagedEvent managedEvent = super.addManagedEvent(eventId);
        if (Objects.equals(existingEvent, managedEvent)) { // no modification was made
        } else {
            GUI.publicAddManagedEvent(eventId, managedEvent);
        }
        return managedEvent;
    }

    @Override
    public synchronized boolean addManagedEvent(final String eventId, final ManagedEvent managedEvent) {
        final boolean success = super.addManagedEvent(eventId, managedEvent);
        if (success) {
            GUI.publicAddManagedEvent(eventId, managedEvent);
        }
        return success;
    }

    @Override
    protected synchronized ManagedMarket addManagedMarket(final String marketId) {
        final ManagedMarket existingMarket = this.markets.get(marketId);
        final ManagedMarket managedMarket = super.addManagedMarket(marketId);
        if (Objects.equals(existingMarket, managedMarket)) { // no modification was made
        } else {
            GUI.publicAddManagedMarket(marketId, managedMarket);
        }
        return managedMarket;
    }

    @Override
    public synchronized boolean addManagedMarket(final String marketId, final ManagedMarket managedMarket) {
        final boolean success = super.addManagedMarket(marketId, managedMarket);
        if (success) {
            GUI.publicAddManagedMarket(marketId, managedMarket);
        }
        return success;
    }

    @Override
    public synchronized ManagedMarket removeManagedMarket(final String marketId) {
        final ManagedMarket existingMarket = this.markets.get(marketId);
        if (existingMarket != null) {
            final String parentEventId = existingMarket.getParentEventId(Statics.marketCataloguesMap, Statics.rulesManager.rulesHaveChanged);
            GUI.publicRemoveManagedMarket(marketId, parentEventId); // GUI updated before actual removal, to still find the necessary info in the maps
        } else { // nothing will be removed
        }
        return super.removeManagedMarket(marketId);
    }
}
