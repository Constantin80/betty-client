package info.fmro.client.utility;

import info.fmro.client.objects.ClientStreamSynchronizedMap;
import info.fmro.client.objects.Statics;
import info.fmro.shared.enums.RulesManagerModificationCommand;
import info.fmro.shared.logic.ManagedEvent;
import info.fmro.shared.logic.ManagedMarket;
import info.fmro.shared.stream.objects.SerializableObjectModification;
import info.fmro.shared.utility.Formulas;
import info.fmro.shared.utility.Generic;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;

@SuppressWarnings("UtilityClass")
public final class Utils {
    private static final Logger logger = LoggerFactory.getLogger(Utils.class);

    private Utils() {
    }

    public static void createManagedEvent(final String eventId) {
        if (eventId == null) {
            logger.error("null eventId in createManagedEvent");
        } else {
            final String eventName = Formulas.getEventName(eventId, Statics.eventsMap);
            if (Statics.rulesManager.events.containsKey(eventId)) {
                logger.warn("trying to recreate existing ManagedEvent for {} {}", eventId, eventName);
            } else {
                logger.info("client->server create default ManagedEvent for {} {}", eventId, eventName);
                final ManagedEvent managedEvent = new ManagedEvent(eventId, Statics.eventsMap, Statics.rulesManager.markets, Statics.rulesManager.listOfQueues);
                Statics.sslClientThread.sendQueue.add(new SerializableObjectModification<>(RulesManagerModificationCommand.addManagedEvent, eventId, managedEvent));
            }
        }
    }

    public static void removeManagedEvent(final String eventId) {
        if (eventId == null) {
            logger.error("null eventId in removeManagedEvent");
        } else {
            if (Statics.rulesManager.events.containsKey(eventId)) {
                logger.info("client->server remove ManagedEvent {}", eventId);
                Statics.sslClientThread.sendQueue.add(new SerializableObjectModification<>(RulesManagerModificationCommand.removeManagedEvent, eventId));
            } else {
                logger.warn("trying to remove non existing ManagedEvent {}", eventId);
            }
        }
    }

    public static void createManagedMarket(final String marketId, final String eventId) {
        if (marketId == null) {
            logger.error("null marketId in createManagedMarket: {}", eventId);
        } else {
            final String eventName = Formulas.getEventName(eventId, Statics.eventsMap);
            final String marketName = Formulas.getMarketCatalogueName(marketId, Statics.marketCataloguesMap);
            if (Statics.rulesManager.markets.containsKey(marketId)) {
                logger.warn("trying to recreate existing ManagedMarket for {} {} {} {}", eventId, marketId, eventName, marketName);
            } else {
                logger.info("client->server create default ManagedMarket for {} {} {} {}", eventId, marketId, eventName, marketName);
                final ManagedMarket managedMarket = new ManagedMarket(marketId, Statics.marketCache.markets, Statics.rulesManager.listOfQueues, Statics.rulesManager.marketsToCheck, Statics.rulesManager.events, Statics.rulesManager.markets,
                                                                      Statics.rulesManager.rulesHaveChanged, Statics.marketCataloguesMap, Statics.PROGRAM_START_TIME);
                Statics.sslClientThread.sendQueue.add(new SerializableObjectModification<>(RulesManagerModificationCommand.addManagedMarket, marketId, managedMarket));
            }
        }
    }

    public static void removeManagedMarket(final String marketId) {
        if (marketId == null) {
            logger.error("null marketId in removeManagedMarket");
        } else {
            if (Statics.rulesManager.markets.containsKey(marketId)) {
                logger.info("client->server remove ManagedMarket {}", marketId);
                Statics.sslClientThread.sendQueue.add(new SerializableObjectModification<>(RulesManagerModificationCommand.removeManagedMarket, marketId));
            } else {
                logger.error("trying to remove non existing ManagedMarket {}", marketId);
            }
        }
    }

    public static void checkMapValues(final ClientStreamSynchronizedMap<String, ? extends Serializable> mapToUse, @NotNull final Iterable<String> ids) {
        if (mapToUse == Statics.marketCataloguesMap) {
            // not used for now
        } else if (mapToUse == Statics.eventsMap) {
            // not used for now
        } else {
            logger.error("unknown mapToUse in checkMapValues for: {} {}", Generic.objectToString(ids), Generic.objectToString(mapToUse));
        }
    }
}
