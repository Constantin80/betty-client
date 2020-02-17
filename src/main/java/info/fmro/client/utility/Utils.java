package info.fmro.client.utility;

import info.fmro.client.objects.Statics;
import info.fmro.shared.enums.RulesManagerModificationCommand;
import info.fmro.shared.logic.ManagedEvent;
import info.fmro.shared.logic.ManagedMarket;
import info.fmro.shared.stream.objects.SerializableObjectModification;
import info.fmro.shared.utility.Formulas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
                final ManagedEvent managedEvent = new ManagedEvent(eventId, Statics.eventsMap, Statics.rulesManager);
                Statics.sslClientThread.sendQueue.add(new SerializableObjectModification<>(RulesManagerModificationCommand.addManagedEvent, eventId, managedEvent));
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
                final ManagedMarket managedMarket = new ManagedMarket(marketId, Statics.marketCache, Statics.rulesManager);
                Statics.sslClientThread.sendQueue.add(new SerializableObjectModification<>(RulesManagerModificationCommand.addManagedMarket, marketId, managedMarket));
            }
        }
    }
}
