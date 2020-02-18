package info.fmro.client.main;

import info.fmro.client.objects.Statics;
import info.fmro.shared.entities.Event;
import info.fmro.shared.entities.MarketCatalogue;
import info.fmro.shared.entities.MarketDescription;
import info.fmro.shared.enums.RulesManagerModificationCommand;
import info.fmro.shared.logic.ManagedEvent;
import info.fmro.shared.logic.ManagedMarket;
import info.fmro.shared.stream.cache.market.Market;
import info.fmro.shared.stream.definitions.MarketDefinition;
import info.fmro.shared.stream.objects.SerializableObjectModification;
import org.jetbrains.annotations.Contract;
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
    public static final String MANAGED_EVENT_EXPIRED_MARKER = "zzz ";

    private GUIUtils() {
    }

    @Nullable
    static String getManagedMarketName(@NotNull final ManagedMarket managedMarket) {
        final String marketId = managedMarket.getId();
        return getManagedMarketName(marketId, managedMarket);
    }

    @Nullable
    static String getManagedMarketName(final String marketId, @NotNull final ManagedMarket managedMarket) {
        @Nullable String name = managedMarket.simpleGetMarketName();
        if (name == null) {
            name = managedMarket.getMarketName(Statics.marketCache, Statics.rulesManager);
            if (name == null) {
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
            } else { // found the name, will continue
            }
            if (name != null) { // name became not null
                Statics.sslClientThread.sendQueue.add(new SerializableObjectModification<>(RulesManagerModificationCommand.setMarketName, marketId, name));
            }
        } else { // found the name, nothing more to be done
        }
        return name;
    }

    @Nullable
    static String getManagedEventName(@NotNull final ManagedEvent managedEvent) {
        @Nullable String name = managedEvent.simpleGetEventName();
        if (name == null) {
            name = managedEvent.getEventName(Statics.eventsMap, Statics.rulesManager);
            final String eventId = managedEvent.getId();
            if (name == null) {
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
            } else { // found the name, will continue
            }
            if (name != null) { // name became not null
                Statics.sslClientThread.sendQueue.add(new SerializableObjectModification<>(RulesManagerModificationCommand.setEventName, eventId, name));
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

    static boolean eventExistsInMap(final String eventId) {
        return Statics.eventsMap.containsKey(eventId);
    }

    @Nullable
    static String markNameAsExpired(final String name) {
        @Nullable final String result;
        if (name == null) {
            logger.error("null name in markNameAsExpired");
            result = null;
        } else {
            // logger.error("name already marked as expired: {}", name); // normal behavior, no need to print anything
            result = nameIsMarkedAsExpired(name) ? name : addExpiredMarker(name);
        }
        return result;
    }

    @Nullable
    static String markNameAsNotExpired(final String name) {
        @Nullable final String result;
        if (name == null) {
            logger.error("null name in markNameAsNotExpired");
            result = null;
        } else {
            if (nameIsMarkedAsExpired(name)) {
                result = removeExpiredMarker(name);
            } else {
                logger.error("name not marked as expired in markNameAsNotExpired: {}", name);
                result = name;
            }
        }
        return result;
    }

    private static boolean nameIsMarkedAsExpired(@NotNull final String name) {
        return name.startsWith(MANAGED_EVENT_EXPIRED_MARKER);
    }

    @NotNull
    @Contract(value = "_ -> new", pure = true)
    private static String addExpiredMarker(@NotNull final String name) {
        return new String(MANAGED_EVENT_EXPIRED_MARKER + name);
    }

    @NotNull
    @Contract("_ -> new")
    private static String removeExpiredMarker(@NotNull final String name) {
        return new String(name.substring(MANAGED_EVENT_EXPIRED_MARKER.length()));
    }
}
