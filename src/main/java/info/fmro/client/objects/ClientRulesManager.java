package info.fmro.client.objects;

import info.fmro.client.main.GUI;
import info.fmro.shared.entities.Event;
import info.fmro.shared.entities.MarketCatalogue;
import info.fmro.shared.logic.ExistingFunds;
import info.fmro.shared.logic.ManagedEvent;
import info.fmro.shared.logic.ManagedMarket;
import info.fmro.shared.logic.RulesManager;
import info.fmro.shared.stream.cache.market.Market;
import info.fmro.shared.stream.cache.order.OrderMarket;
import info.fmro.shared.stream.objects.OrdersThreadInterface;
import info.fmro.shared.stream.objects.StreamSynchronizedMap;
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
    public synchronized ManagedEvent removeManagedEvent(final String eventId, @NotNull final StreamSynchronizedMap<? super String, ? extends MarketCatalogue> marketCataloguesMap) {
        if (this.events.containsKey(eventId)) {
            final ManagedEvent existingManagedEvent = this.events.get(eventId);
            @Nullable final HashSet<String> marketIds = existingManagedEvent == null ? null : existingManagedEvent.marketIds.copy();
            GUI.publicRemoveManagedEvent(eventId, marketIds);
        } else { // nothing will be removed
        }

        return super.removeManagedEvent(eventId, marketCataloguesMap);
    }

    @Override
    public synchronized void executeCommand(@NotNull final String commandString, @NotNull final OrdersThreadInterface pendingOrdersThread, @NotNull final SynchronizedMap<? super String, ? extends OrderMarket> orderCache,
                                            @NotNull final ExistingFunds safetyLimits, @NotNull final StreamSynchronizedMap<? super String, ? extends MarketCatalogue> marketCataloguesMap,
                                            @NotNull final StreamSynchronizedMap<? super String, ? extends Event> eventsMap, @NotNull final SynchronizedMap<? super String, ? extends Market> marketCache, final long programStartTime) {
        logger.error("executeCommand method is not and should not be implemented in Client");
    }

    @Override
    @NotNull
    protected synchronized ManagedEvent addManagedEvent(@NotNull final String eventId, @NotNull final StreamSynchronizedMap<? super String, ? extends Event> eventsMap,
                                                        @NotNull final StreamSynchronizedMap<? super String, ? extends MarketCatalogue> marketCataloguesMap) {
        final ManagedEvent existingEvent = this.events.get(eventId);
        final ManagedEvent managedEvent = super.addManagedEvent(eventId, eventsMap, marketCataloguesMap);
        if (Objects.equals(existingEvent, managedEvent)) { // no modification was made
        } else {
            GUI.publicAddManagedEvent(eventId, managedEvent);
        }
        return managedEvent;
    }

    @Override
    public synchronized boolean addManagedEvent(@NotNull final String eventId, final ManagedEvent managedEvent) {
        final boolean success = super.addManagedEvent(eventId, managedEvent);
        if (success) {
            GUI.publicAddManagedEvent(eventId, managedEvent);
        }
        return success;
    }

    @Override
    protected synchronized ManagedMarket addManagedMarket(@NotNull final String marketId, @NotNull final StreamSynchronizedMap<? super String, ? extends MarketCatalogue> marketCataloguesMap,
                                                          @NotNull final SynchronizedMap<? super String, ? extends Market> marketCache, @NotNull final StreamSynchronizedMap<? super String, ? extends Event> eventsMap, final long programStartTime) {
//        logger.error("no need to use ClientRulesManager overridden addManagedMarket(2 args), use the version without marketCataloguesMap!");
        return addManagedMarket(marketId);
    }

    @Override
    public synchronized boolean addManagedMarket(@NotNull final String marketId, final ManagedMarket managedMarket, @NotNull final StreamSynchronizedMap<? super String, ? extends MarketCatalogue> marketCataloguesMap,
                                                 @NotNull final StreamSynchronizedMap<? super String, ? extends Event> eventsMap) {
//        logger.error("no need to use ClientRulesManager overridden addManagedMarket(3 args), use the version without marketCataloguesMap!");
        return addManagedMarket(marketId, managedMarket);
    }

    @SuppressWarnings("WeakerAccess")
    protected synchronized ManagedMarket addManagedMarket(@NotNull final String marketId) {
        final ManagedMarket existingMarket = this.markets.get(marketId);
        final ManagedMarket managedMarket = super.addManagedMarket(marketId, Statics.marketCataloguesMap, Statics.marketCache.markets, Statics.eventsMap, Statics.PROGRAM_START_TIME);
        if (Objects.equals(existingMarket, managedMarket)) { // no modification was made
        } else {
            GUI.publicAddManagedMarket(marketId, managedMarket);
        }
        return managedMarket;
    }

    public synchronized boolean addManagedMarket(@NotNull final String marketId, final ManagedMarket managedMarket) {
        final boolean success = super.addManagedMarket(marketId, managedMarket, Statics.marketCataloguesMap, Statics.eventsMap);
        if (success) {
            GUI.publicAddManagedMarket(marketId, managedMarket);
        }
        return success;
    }

    @Override
    public synchronized ManagedMarket removeManagedMarket(final String marketId, @NotNull final StreamSynchronizedMap<? super String, ? extends MarketCatalogue> marketCataloguesMap) {
        if (this.markets.containsKey(marketId)) {
            GUI.publicRemoveManagedMarket(marketId);
        } else { // nothing will be removed
        }
        return super.removeManagedMarket(marketId, marketCataloguesMap);
    }
}
