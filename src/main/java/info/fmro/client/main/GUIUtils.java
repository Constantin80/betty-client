package info.fmro.client.main;

import info.fmro.client.objects.Statics;
import info.fmro.shared.entities.Event;
import info.fmro.shared.entities.MarketCatalogue;
import info.fmro.shared.entities.MarketDescription;
import info.fmro.shared.logic.ManagedEvent;
import info.fmro.shared.logic.ManagedMarket;
import info.fmro.shared.stream.cache.market.Market;
import info.fmro.shared.stream.definitions.MarketDefinition;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;

@SuppressWarnings({"UtilityClass", "unused"})
final class GUIUtils {
    private static final Logger logger = LoggerFactory.getLogger(GUIUtils.class);

    private GUIUtils() {
    }

    @Nullable
    static String getManagedMarketName(@NotNull final ManagedMarket managedMarket) {
        final String marketId = managedMarket.getId();
        return getManagedMarketName(marketId, managedMarket);
    }

    @Nullable
    static String getManagedMarketName(final String marketId, @NotNull final ManagedMarket managedMarket) {
        @Nullable String name = null;

        final MarketCatalogue marketCatalogue = Statics.marketCataloguesMap.get(marketId);
        if (marketCatalogue != null) {
            name = marketCatalogue.getMarketName();
            if (name == null) {
                final MarketDescription marketDescription = marketCatalogue.getDescription();
                if (marketDescription != null) {
                    name = marketDescription.getMarketType();
                }
            } else { // found the name, won't pursue further
            }
        } else { // no marketCatalogue, can't pursue the name further on this branch
        }

        if (name == null) {
            final Market market = managedMarket.getMarket(Statics.marketCache, Statics.rulesManager);
            if (market != null) {
                final MarketDefinition marketDefinition = market.getMarketDefinition();
                if (marketDefinition != null) {
                    name = marketDefinition.getMarketType();
                } else { // no marketDefinition, I can't find the name here
                }
            } else { // no market attached, I can't find the name here
            }
        } else { // I found it before this if
        }

        return name;
    }

    @Nullable
    static String getManagedEventName(@NotNull final ManagedEvent managedEvent) {
        @Nullable String name = null;
        final String eventId = managedEvent.getId();

        @NotNull final HashSet<String> marketIds = managedEvent.marketIds.copy();
        for (final String marketId : marketIds) {
            final MarketCatalogue marketCatalogue = Statics.marketCataloguesMap.get(marketId);
            name = getManagedEventNameFromMarketCatalogue(marketCatalogue, eventId);
            if (name == null) { // for will continue
            } else { // found the name
                break;
            }
        }
        if (name == null) { // normal case, the event might not have ManagedMarkets attached, will check the entire map
            @NotNull final Collection<MarketCatalogue> marketCatalogues = Statics.marketCataloguesMap.valuesCopy();
            for (final MarketCatalogue marketCatalogue : marketCatalogues) {
                name = getManagedEventNameFromMarketCatalogue(marketCatalogue, eventId);
                if (name == null) { // for will continue
                } else { // found the name
                    break;
                }
            }
        } else { // found the name, nothing more to be done
        }

        return name;
    }

    @Nullable
    private static String getManagedEventNameFromMarketCatalogue(final MarketCatalogue marketCatalogue, @NotNull final String eventId) {
        @Nullable final String name;
        if (marketCatalogue == null) {
            logger.error("null marketCatalogue found during getManagedEventNameFromMarketCatalogue for: {}", eventId);
            Statics.marketCataloguesMap.removeValueAll(null);
            name = null;
        } else {
            final Event eventStump = marketCatalogue.getEventStump();
            if (eventStump == null) { // might be normal
                name = null;
            } else {
                final String eventName = eventStump.getName();
                if (eventName == null) { // might be normal
                    name = null;
                } else {
                    final String idFromStump = eventStump.getId();
                    final boolean marketContainsTheRightEvent = Objects.equals(eventId, idFromStump);
                    name = marketContainsTheRightEvent ? eventName : null;
                }
            }
        }
        return name;
    }
}
