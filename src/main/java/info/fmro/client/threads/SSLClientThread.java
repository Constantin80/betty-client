package info.fmro.client.threads;

import info.fmro.client.main.GUI;
import info.fmro.client.objects.ClientStreamSynchronizedMap;
import info.fmro.client.objects.Statics;
import info.fmro.shared.entities.Event;
import info.fmro.shared.entities.MarketCatalogue;
import info.fmro.shared.enums.ExistingFundsModificationCommand;
import info.fmro.shared.enums.RulesManagerModificationCommand;
import info.fmro.shared.enums.SynchronizedMapModificationCommand;
import info.fmro.shared.logic.ExistingFunds;
import info.fmro.shared.logic.ManagedEvent;
import info.fmro.shared.logic.ManagedMarket;
import info.fmro.shared.logic.ManagedRunner;
import info.fmro.shared.logic.RulesManager;
import info.fmro.shared.stream.cache.market.MarketCache;
import info.fmro.shared.stream.cache.order.OrderCache;
import info.fmro.shared.stream.definitions.MarketChangeMessage;
import info.fmro.shared.stream.definitions.OrderChangeMessage;
import info.fmro.shared.stream.objects.PoisonPill;
import info.fmro.shared.stream.objects.RunnerId;
import info.fmro.shared.stream.objects.SerializableObjectModification;
import info.fmro.shared.stream.objects.StreamObjectInterface;
import info.fmro.shared.stream.objects.StreamSynchronizedMap;
import info.fmro.shared.stream.protocol.ChangeMessageFactory;
import info.fmro.shared.utility.Formulas;
import info.fmro.shared.utility.Generic;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.net.ConnectException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;

@SuppressWarnings({"OverlyComplexClass", "OverlyCoupledClass"})
public class SSLClientThread
        extends Thread {
    private static final Logger logger = LoggerFactory.getLogger(SSLClientThread.class);
    public final LinkedBlockingQueue<StreamObjectInterface> sendQueue = new LinkedBlockingQueue<>();
    private SSLSocket socket;

    public void closeSocket() { // probably doesn't need synchronization
        logger.debug("closing SSLClientThread socket");
        Generic.closeObjects(this.socket);
    }

    @SuppressWarnings({"OverlyComplexMethod", "OverlyCoupledMethod", "OverlyLongMethod", "OverlyNestedMethod", "SwitchStatementDensity", "unchecked", "ChainOfInstanceofChecks"})
    private synchronized void runAfterReceive(@NotNull final StreamObjectInterface receivedCommand) {
        if (receivedCommand instanceof RulesManager) {
            @NotNull final RulesManager rulesManager = (RulesManager) receivedCommand;
            Statics.rulesManager.copyFromStream(rulesManager);
            GUI.publicRefreshDisplayedManagedObject();
        } else if (receivedCommand instanceof ExistingFunds) {
            @NotNull final ExistingFunds existingFunds = (ExistingFunds) receivedCommand;
            Statics.existingFunds.copyFrom(existingFunds);
            GUI.publicRefreshDisplayedManagedObject();
        } else if (receivedCommand instanceof MarketCache) {
            @NotNull final MarketCache marketCache = (MarketCache) receivedCommand;
            Statics.marketCache.copyFromStream(marketCache);
            GUI.publicRefreshDisplayedManagedObject();
        } else if (receivedCommand instanceof OrderCache) {
            @NotNull final OrderCache orderCache = (OrderCache) receivedCommand;
            Statics.orderCache.copyFromStream(orderCache);
            GUI.publicRefreshDisplayedManagedObject();
        } else if (receivedCommand instanceof StreamSynchronizedMap<?, ?>) {
            @NotNull final StreamSynchronizedMap<?, ?> streamSynchronizedMap = (StreamSynchronizedMap<?, ?>) receivedCommand;
            final Class<?> clazz = streamSynchronizedMap.getClazz();
            if (MarketCatalogue.class.equals(clazz)) {
                @SuppressWarnings("unchecked") @NotNull final StreamSynchronizedMap<String, MarketCatalogue> streamMarketCatalogueMap = (StreamSynchronizedMap<String, MarketCatalogue>) receivedCommand;
                Statics.marketCataloguesMap.copyFromStream(streamMarketCatalogueMap);
                GUI.publicRefreshDisplayedManagedObject();
            } else if (Event.class.equals(clazz)) {
                @SuppressWarnings("unchecked") @NotNull final StreamSynchronizedMap<String, Event> streamEventMap = (StreamSynchronizedMap<String, Event>) receivedCommand;
                Statics.eventsMap.copyFromStream(streamEventMap);
                GUI.publicRefreshDisplayedManagedObject();
            } else {
                logger.error("unknown streamSynchronizedMap class in runAfterReceive for: {} {}", clazz, Generic.objectToString(receivedCommand));
            }
        } else if (receivedCommand instanceof MarketChangeMessage) {
            @NotNull final MarketChangeMessage marketChangeMessage = (MarketChangeMessage) receivedCommand;
            Statics.marketCache.onMarketChange(ChangeMessageFactory.ToChangeMessage(-1, marketChangeMessage), Statics.existingFunds.currencyRate, Statics.rulesManager.listOfQueues, Statics.rulesManager.marketsToCheck, Statics.rulesManager.events,
                                               Statics.rulesManager.markets, Statics.rulesManager.rulesHaveChanged, Statics.marketCataloguesMap, Statics.PROGRAM_START_TIME);
            final HashSet<String> marketIds = marketChangeMessage.getChangedMarketIds();
            if (marketIds == null) { // normal, nothing to be done
            } else {
                for (final String marketId : marketIds) {
                    final String eventId = Formulas.getEventIdOfMarketId(marketId, Statics.marketCataloguesMap);
                    GUI.managedMarketUpdated(marketId);
                    GUI.managedEventUpdated(eventId);
                }
            }
        } else if (receivedCommand instanceof OrderChangeMessage) {
            @NotNull final OrderChangeMessage orderChangeMessage = (OrderChangeMessage) receivedCommand;
            Statics.orderCache.onOrderChange(ChangeMessageFactory.ToChangeMessage(-1, orderChangeMessage), Statics.rulesManager.orderCacheHasReset, Statics.rulesManager.newOrderMarketCreated, Statics.pendingOrdersThread,
                                             Statics.existingFunds.currencyRate, Statics.marketCache.markets, Statics.rulesManager.listOfQueues, Statics.rulesManager.marketsToCheck, Statics.rulesManager.events, Statics.rulesManager.markets,
                                             Statics.rulesManager.rulesHaveChanged, Statics.marketCataloguesMap, Statics.PROGRAM_START_TIME);
            final HashSet<String> marketIds = orderChangeMessage.getChangedMarketIds();
            if (marketIds == null) { // normal, nothing to be done
            } else {
                for (final String marketId : marketIds) {
                    final String eventId = Formulas.getEventIdOfMarketId(marketId, Statics.marketCataloguesMap);
                    GUI.managedMarketUpdated(marketId);
                    GUI.managedEventUpdated(eventId);
                }
            }
        } else if (receivedCommand instanceof SerializableObjectModification) {
            final SerializableObjectModification<?> serializableObjectModification = (SerializableObjectModification<?>) receivedCommand;
            final Enum<?> command = serializableObjectModification.getCommand();
            if (command instanceof RulesManagerModificationCommand) {
                final RulesManagerModificationCommand rulesManagerModificationCommand = (RulesManagerModificationCommand) command;
                final Object[] objectsToModify = serializableObjectModification.getArray();
                switch (rulesManagerModificationCommand) {
                    case addManagedEvent:
                        if (objectsToModify != null && objectsToModify.length == 2) {
                            if (objectsToModify[0] instanceof String && objectsToModify[1] instanceof ManagedEvent) {
                                final String eventId = (String) objectsToModify[0];
                                final ManagedEvent managedEvent = (ManagedEvent) objectsToModify[1];
                                Statics.rulesManager.addManagedEvent(eventId, managedEvent);
                                GUI.managedEventUpdated(eventId);
                            } else {
                                logger.error("wrong objectsToModify class in runAfterReceive: {} {} {}", Generic.objectToString(objectsToModify), rulesManagerModificationCommand.name(), Generic.objectToString(receivedCommand));
                            }
                        } else {
                            logger.error("wrong size objectsToModify in runAfterReceive: {} {} {}", Generic.objectToString(objectsToModify), rulesManagerModificationCommand.name(), Generic.objectToString(receivedCommand));
                        }
                        break;
                    case removeManagedEvent:
                        if (objectsToModify != null && objectsToModify.length == 1) {
                            if (objectsToModify[0] instanceof String) {
                                final String eventId = (String) objectsToModify[0];
                                Statics.rulesManager.removeManagedEvent(eventId, Statics.marketCataloguesMap);
                            } else {
                                logger.error("wrong objectsToModify class in runAfterReceive: {} {} {}", Generic.objectToString(objectsToModify), rulesManagerModificationCommand.name(), Generic.objectToString(receivedCommand));
                            }
                        } else {
                            logger.error("wrong size objectsToModify in runAfterReceive: {} {} {}", Generic.objectToString(objectsToModify), rulesManagerModificationCommand.name(), Generic.objectToString(receivedCommand));
                        }
                        break;
                    case addManagedMarket:
                        if (objectsToModify != null && objectsToModify.length == 2) {
                            if (objectsToModify[0] instanceof String && objectsToModify[1] instanceof ManagedMarket) {
                                final String marketId = (String) objectsToModify[0];
                                final ManagedMarket managedMarket = (ManagedMarket) objectsToModify[1];
                                Statics.rulesManager.addManagedMarket(marketId, managedMarket);
                                GUI.managedMarketUpdated(marketId);
                            } else {
                                logger.error("wrong objectsToModify class in runAfterReceive: {} {} {}", Generic.objectToString(objectsToModify), rulesManagerModificationCommand.name(), Generic.objectToString(receivedCommand));
                            }
                        } else {
                            logger.error("wrong size objectsToModify in runAfterReceive: {} {} {}", Generic.objectToString(objectsToModify), rulesManagerModificationCommand.name(), Generic.objectToString(receivedCommand));
                        }
                        break;
                    case removeManagedMarket:
                        if (objectsToModify != null && objectsToModify.length == 1) {
                            if (objectsToModify[0] instanceof String) {
                                final String marketId = (String) objectsToModify[0];
                                final String eventId = Formulas.getEventIdOfMarketId(marketId, Statics.marketCataloguesMap);
                                Statics.rulesManager.removeManagedMarket(marketId, Statics.marketCataloguesMap);
                                GUI.managedEventUpdated(eventId);
                            } else {
                                logger.error("wrong objectsToModify class in runAfterReceive: {} {} {}", Generic.objectToString(objectsToModify), rulesManagerModificationCommand.name(), Generic.objectToString(receivedCommand));
                            }
                        } else {
                            logger.error("wrong size objectsToModify in runAfterReceive: {} {} {}", Generic.objectToString(objectsToModify), rulesManagerModificationCommand.name(), Generic.objectToString(receivedCommand));
                        }
                        break;
                    case setEventAmountLimit:
                        if (objectsToModify != null && objectsToModify.length == 2) {
                            if (objectsToModify[0] instanceof String && objectsToModify[1] instanceof Double) {
                                final String eventId = (String) objectsToModify[0];
                                final Double newAmount = (Double) objectsToModify[1];
                                Statics.rulesManager.setEventAmountLimit(eventId, newAmount, Statics.pendingOrdersThread, Statics.orderCache.markets, Statics.existingFunds, Statics.marketCataloguesMap, Statics.marketCache.markets,
                                                                         Statics.orderCache.initializedFromStreamStamp, Statics.PROGRAM_START_TIME);
                                GUI.managedEventUpdated(eventId);
                            } else {
                                logger.error("wrong objectsToModify class in runAfterReceive: {} {} {}", Generic.objectToString(objectsToModify), rulesManagerModificationCommand.name(), Generic.objectToString(receivedCommand));
                            }
                        } else {
                            logger.error("wrong size objectsToModify in runAfterReceive: {} {} {}", Generic.objectToString(objectsToModify), rulesManagerModificationCommand.name(), Generic.objectToString(receivedCommand));
                        }
                        break;
                    case addManagedRunner:
                        if (objectsToModify != null && objectsToModify.length == 1) {
                            if (objectsToModify[0] instanceof ManagedRunner) {
                                final ManagedRunner managedRunner = (ManagedRunner) objectsToModify[0];
                                final String marketId = managedRunner.getMarketId();
                                final String eventId = Formulas.getEventIdOfMarketId(marketId, Statics.marketCataloguesMap);
                                Statics.rulesManager.addManagedRunner(managedRunner, Statics.marketCataloguesMap, Statics.marketCache.markets, Statics.eventsMap, Statics.PROGRAM_START_TIME);
                                GUI.managedMarketUpdated(marketId);
                                GUI.managedEventUpdated(eventId);
                            } else {
                                logger.error("wrong objectsToModify class in runAfterReceive: {} {} {}", Generic.objectToString(objectsToModify), rulesManagerModificationCommand.name(), Generic.objectToString(receivedCommand));
                            }
                        } else {
                            logger.error("wrong size objectsToModify in runAfterReceive: {} {} {}", Generic.objectToString(objectsToModify), rulesManagerModificationCommand.name(), Generic.objectToString(receivedCommand));
                        }
                        break;
                    case removeManagedRunner:
                        if (objectsToModify != null && objectsToModify.length == 2) {
                            if (objectsToModify[0] instanceof String && objectsToModify[1] instanceof RunnerId) {
                                final String marketId = (String) objectsToModify[0];
                                final String eventId = Formulas.getEventIdOfMarketId(marketId, Statics.marketCataloguesMap);
                                final RunnerId runnerId = (RunnerId) objectsToModify[1];
                                Statics.rulesManager.removeManagedRunner(marketId, runnerId);
                                GUI.managedMarketUpdated(marketId);
                                GUI.managedEventUpdated(eventId);
                            } else {
                                logger.error("wrong objectsToModify class in runAfterReceive: {} {} {}", Generic.objectToString(objectsToModify), rulesManagerModificationCommand.name(), Generic.objectToString(receivedCommand));
                            }
                        } else {
                            logger.error("wrong size objectsToModify in runAfterReceive: {} {} {}", Generic.objectToString(objectsToModify), rulesManagerModificationCommand.name(), Generic.objectToString(receivedCommand));
                        }
                        break;
                    case setMarketAmountLimit:
                        if (objectsToModify != null && objectsToModify.length == 2) {
                            if (objectsToModify[0] instanceof String && objectsToModify[1] instanceof Double) {
                                final String marketId = (String) objectsToModify[0];
                                final String eventId = Formulas.getEventIdOfMarketId(marketId, Statics.marketCataloguesMap);
                                final Double amountLimit = (Double) objectsToModify[1];
                                Statics.rulesManager.setMarketAmountLimit(marketId, amountLimit, Statics.existingFunds);
                                GUI.managedMarketUpdated(marketId);
                                GUI.managedEventUpdated(eventId);
                            } else {
                                logger.error("wrong objectsToModify class in runAfterReceive: {} {} {}", Generic.objectToString(objectsToModify), rulesManagerModificationCommand.name(), Generic.objectToString(receivedCommand));
                            }
                        } else {
                            logger.error("wrong size objectsToModify in runAfterReceive: {} {} {}", Generic.objectToString(objectsToModify), rulesManagerModificationCommand.name(), Generic.objectToString(receivedCommand));
                        }
                        break;
                    case setBackAmountLimit:
                        if (objectsToModify != null && objectsToModify.length == 3) {
                            if (objectsToModify[0] instanceof String && objectsToModify[1] instanceof RunnerId && objectsToModify[2] instanceof Double) {
                                final String marketId = (String) objectsToModify[0];
                                final RunnerId runnerId = (RunnerId) objectsToModify[1];
                                final Double amountLimit = (Double) objectsToModify[2];
                                Statics.rulesManager.setRunnerBackAmountLimit(marketId, runnerId, amountLimit);
                                final String eventId = Formulas.getEventIdOfMarketId(marketId, Statics.marketCataloguesMap);
                                GUI.managedMarketUpdated(marketId);
                                GUI.managedEventUpdated(eventId);
                            } else {
                                logger.error("wrong objectsToModify class in runAfterReceive: {} {} {}", Generic.objectToString(objectsToModify), rulesManagerModificationCommand.name(), Generic.objectToString(receivedCommand));
                            }
                        } else {
                            logger.error("wrong size objectsToModify in runAfterReceive: {} {} {}", Generic.objectToString(objectsToModify), rulesManagerModificationCommand.name(), Generic.objectToString(receivedCommand));
                        }
                        break;
                    case setLayAmountLimit:
                        if (objectsToModify != null && objectsToModify.length == 3) {
                            if (objectsToModify[0] instanceof String && objectsToModify[1] instanceof RunnerId && objectsToModify[2] instanceof Double) {
                                final String marketId = (String) objectsToModify[0];
                                final RunnerId runnerId = (RunnerId) objectsToModify[1];
                                final Double amountLimit = (Double) objectsToModify[2];
                                Statics.rulesManager.setRunnerLayAmountLimit(marketId, runnerId, amountLimit);
                                final String eventId = Formulas.getEventIdOfMarketId(marketId, Statics.marketCataloguesMap);
                                GUI.managedMarketUpdated(marketId);
                                GUI.managedEventUpdated(eventId);
                            } else {
                                logger.error("wrong objectsToModify class in runAfterReceive: {} {} {}", Generic.objectToString(objectsToModify), rulesManagerModificationCommand.name(), Generic.objectToString(receivedCommand));
                            }
                        } else {
                            logger.error("wrong size objectsToModify in runAfterReceive: {} {} {}", Generic.objectToString(objectsToModify), rulesManagerModificationCommand.name(), Generic.objectToString(receivedCommand));
                        }
                        break;
                    case setMinBackOdds:
                        if (objectsToModify != null && objectsToModify.length == 3) {
                            if (objectsToModify[0] instanceof String && objectsToModify[1] instanceof RunnerId && objectsToModify[2] instanceof Double) {
                                final String marketId = (String) objectsToModify[0];
                                final RunnerId runnerId = (RunnerId) objectsToModify[1];
                                final Double odds = (Double) objectsToModify[2];
                                Statics.rulesManager.setRunnerMinBackOdds(marketId, runnerId, odds);
                                final String eventId = Formulas.getEventIdOfMarketId(marketId, Statics.marketCataloguesMap);
                                GUI.managedMarketUpdated(marketId);
                                GUI.managedEventUpdated(eventId);
                            } else {
                                logger.error("wrong objectsToModify class in runAfterReceive: {} {} {}", Generic.objectToString(objectsToModify), rulesManagerModificationCommand.name(), Generic.objectToString(receivedCommand));
                            }
                        } else {
                            logger.error("wrong size objectsToModify in runAfterReceive: {} {} {}", Generic.objectToString(objectsToModify), rulesManagerModificationCommand.name(), Generic.objectToString(receivedCommand));
                        }
                        break;
                    case setMaxLayOdds:
                        if (objectsToModify != null && objectsToModify.length == 3) {
                            if (objectsToModify[0] instanceof String && objectsToModify[1] instanceof RunnerId && objectsToModify[2] instanceof Double) {
                                final String marketId = (String) objectsToModify[0];
                                final RunnerId runnerId = (RunnerId) objectsToModify[1];
                                final Double odds = (Double) objectsToModify[2];
                                Statics.rulesManager.setRunnerMaxLayOdds(marketId, runnerId, odds);
                                final String eventId = Formulas.getEventIdOfMarketId(marketId, Statics.marketCataloguesMap);
                                GUI.managedMarketUpdated(marketId);
                                GUI.managedEventUpdated(eventId);
                            } else {
                                logger.error("wrong objectsToModify class in runAfterReceive: {} {} {}", Generic.objectToString(objectsToModify), rulesManagerModificationCommand.name(), Generic.objectToString(receivedCommand));
                            }
                        } else {
                            logger.error("wrong size objectsToModify in runAfterReceive: {} {} {}", Generic.objectToString(objectsToModify), rulesManagerModificationCommand.name(), Generic.objectToString(receivedCommand));
                        }
                        break;
                    case setMarketName:
                        if (objectsToModify != null && objectsToModify.length == 2) {
                            if (objectsToModify[0] instanceof String && objectsToModify[1] instanceof String) {
                                final String marketId = (String) objectsToModify[0];
                                final String marketName = (String) objectsToModify[1];
                                Statics.rulesManager.setMarketName(marketId, marketName);
                                GUI.managedMarketUpdated(marketId);
                            } else {
                                logger.error("wrong objectsToModify class in runAfterReceive: {} {} {}", Generic.objectToString(objectsToModify), rulesManagerModificationCommand.name(), Generic.objectToString(receivedCommand));
                            }
                        } else {
                            logger.error("wrong size objectsToModify in runAfterReceive: {} {} {}", Generic.objectToString(objectsToModify), rulesManagerModificationCommand.name(), Generic.objectToString(receivedCommand));
                        }
                        break;
                    case setEventName:
                        if (objectsToModify != null && objectsToModify.length == 2) {
                            if (objectsToModify[0] instanceof String && objectsToModify[1] instanceof String) {
                                final String eventId = (String) objectsToModify[0];
                                final String eventName = (String) objectsToModify[1];
                                Statics.rulesManager.setEventName(eventId, eventName);
                                GUI.managedEventUpdated(eventId);
                            } else {
                                logger.error("wrong objectsToModify class in runAfterReceive: {} {} {}", Generic.objectToString(objectsToModify), rulesManagerModificationCommand.name(), Generic.objectToString(receivedCommand));
                            }
                        } else {
                            logger.error("wrong size objectsToModify in runAfterReceive: {} {} {}", Generic.objectToString(objectsToModify), rulesManagerModificationCommand.name(), Generic.objectToString(receivedCommand));
                        }
                        break;
                    case setMarketEnabled:
                        if (objectsToModify != null && objectsToModify.length == 2) {
                            if (objectsToModify[0] instanceof String && objectsToModify[1] instanceof Boolean) {
                                final String marketId = (String) objectsToModify[0];
                                final Boolean marketEnabled = (Boolean) objectsToModify[1];
                                Statics.rulesManager.setMarketEnabled(marketId, marketEnabled);
                                GUI.managedMarketUpdated(marketId);
                            } else {
                                logger.error("wrong objectsToModify class in runAfterReceive: {} {} {}", Generic.objectToString(objectsToModify), rulesManagerModificationCommand.name(), Generic.objectToString(receivedCommand));
                            }
                        } else {
                            logger.error("wrong size objectsToModify in runAfterReceive: {} {} {}", Generic.objectToString(objectsToModify), rulesManagerModificationCommand.name(), Generic.objectToString(receivedCommand));
                        }
                        break;
                    case setMarketCalculatedLimit:
                        if (objectsToModify != null && objectsToModify.length == 2) {
                            if (objectsToModify[0] instanceof String && objectsToModify[1] instanceof Double) {
                                final String marketId = (String) objectsToModify[0];
                                final Double calculatedLimit = (Double) objectsToModify[1];
                                Statics.rulesManager.setMarketCalculatedLimit(marketId, calculatedLimit, Statics.existingFunds);
                                GUI.managedMarketUpdated(marketId);
                            } else {
                                logger.error("wrong objectsToModify class in runAfterReceive: {} {} {}", Generic.objectToString(objectsToModify), rulesManagerModificationCommand.name(), Generic.objectToString(receivedCommand));
                            }
                        } else {
                            logger.error("wrong size objectsToModify in runAfterReceive: {} {} {}", Generic.objectToString(objectsToModify), rulesManagerModificationCommand.name(), Generic.objectToString(receivedCommand));
                        }
                        break;
                    default:
                        logger.error("unknown rulesManagerModificationCommand in runAfterReceive: {} {}", rulesManagerModificationCommand.name(), Generic.objectToString(receivedCommand));
                } // end switch
            } else if (command instanceof ExistingFundsModificationCommand) {
                final ExistingFundsModificationCommand existingFundsModificationCommand = (ExistingFundsModificationCommand) command;
                final Object[] objectsToModify = serializableObjectModification.getArray();
                switch (existingFundsModificationCommand) {
                    case setCurrencyRate:
                        if (objectsToModify != null && objectsToModify.length == 1) {
                            if (objectsToModify[0] instanceof Double) {
                                final Double rate = (Double) objectsToModify[0];
                                Statics.existingFunds.setCurrencyRate(rate);
                                GUI.publicRefreshDisplayedManagedObject();
                            } else {
                                logger.error("wrong objectsToModify class in runAfterReceive: {} {} {}", Generic.objectToString(objectsToModify), existingFundsModificationCommand.name(), Generic.objectToString(receivedCommand));
                            }
                        } else {
                            logger.error("wrong size objectsToModify in runAfterReceive: {} {} {}", Generic.objectToString(objectsToModify), existingFundsModificationCommand.name(), Generic.objectToString(receivedCommand));
                        }
                        break;
                    case setReserve:
                        if (objectsToModify != null && objectsToModify.length == 1) {
                            if (objectsToModify[0] instanceof Double) {
                                final Double reserve = (Double) objectsToModify[0];
                                Statics.existingFunds.setReserve(reserve);
                                GUI.publicRefreshDisplayedManagedObject();
                            } else {
                                logger.error("wrong objectsToModify class in runAfterReceive: {} {} {}", Generic.objectToString(objectsToModify), existingFundsModificationCommand.name(), Generic.objectToString(receivedCommand));
                            }
                        } else {
                            logger.error("wrong size objectsToModify in runAfterReceive: {} {} {}", Generic.objectToString(objectsToModify), existingFundsModificationCommand.name(), Generic.objectToString(receivedCommand));
                        }
                        break;
                    case setAvailableFunds:
                        if (objectsToModify != null && objectsToModify.length == 1) {
                            if (objectsToModify[0] instanceof Double) {
                                final Double availableFunds = (Double) objectsToModify[0];
                                Statics.existingFunds.setAvailableFunds(availableFunds);
                            } else {
                                logger.error("wrong objectsToModify class in runAfterReceive: {} {} {}", Generic.objectToString(objectsToModify), existingFundsModificationCommand.name(), Generic.objectToString(receivedCommand));
                            }
                        } else {
                            logger.error("wrong size objectsToModify in runAfterReceive: {} {} {}", Generic.objectToString(objectsToModify), existingFundsModificationCommand.name(), Generic.objectToString(receivedCommand));
                        }
                        break;
                    case setExposure:
                        if (objectsToModify != null && objectsToModify.length == 1) {
                            if (objectsToModify[0] instanceof Double) {
                                final Double exposure = (Double) objectsToModify[0];
                                Statics.existingFunds.setExposure(exposure);
                            } else {
                                logger.error("wrong objectsToModify class in runAfterReceive: {} {} {}", Generic.objectToString(objectsToModify), existingFundsModificationCommand.name(), Generic.objectToString(receivedCommand));
                            }
                        } else {
                            logger.error("wrong size objectsToModify in runAfterReceive: {} {} {}", Generic.objectToString(objectsToModify), existingFundsModificationCommand.name(), Generic.objectToString(receivedCommand));
                        }
                        break;
                    default:
                        logger.error("unknown existingFundsModificationCommand in runAfterReceive: {} {}", existingFundsModificationCommand.name(), Generic.objectToString(receivedCommand));
                } // end switch
            } else if (command instanceof SynchronizedMapModificationCommand) {
                @NotNull final SynchronizedMapModificationCommand synchronizedMapModificationCommand = (SynchronizedMapModificationCommand) command;
                final Object[] objectsToModify = serializableObjectModification.getArray();
                if (objectsToModify == null || !(objectsToModify[0] instanceof Class<?>)) {
                    logger.error("improper objectsToModify for SynchronizedMapModificationCommand: {} {} {}", Generic.objectToString(objectsToModify), synchronizedMapModificationCommand.name(), Generic.objectToString(receivedCommand));
                } else {
                    final Class<?> clazz = (Class<?>) objectsToModify[0];
                    @Nullable final ClientStreamSynchronizedMap<String, Serializable> mapToUse;

                    if (MarketCatalogue.class.equals(clazz)) {
                        //noinspection rawtypes
                        mapToUse = (ClientStreamSynchronizedMap) Statics.marketCataloguesMap;
                    } else if (Event.class.equals(clazz)) {
                        //noinspection rawtypes
                        mapToUse = (ClientStreamSynchronizedMap) Statics.eventsMap;
                    } else {
                        logger.error("unknown streamSynchronizedMap class in runAfterReceive for: {} {}", clazz, Generic.objectToString(receivedCommand));
                        mapToUse = null;
                    }

                    if (mapToUse == null) { // nothing to do, error message was already printed
                    } else {
                        final int nObjects = objectsToModify.length;
                        switch (synchronizedMapModificationCommand) {
                            case clear:
                                if (nObjects == 1) {
                                    mapToUse.clear();
                                } else {
                                    logger.error("wrong size objectsToModify in runAfterReceive: {} {} {}", Generic.objectToString(objectsToModify), synchronizedMapModificationCommand.name(), Generic.objectToString(receivedCommand));
                                }
                                break;
                            case put:
                                if (nObjects >= 3) {
                                    if (objectsToModify[1] instanceof String && Generic.objectInstanceOf(clazz, objectsToModify[2])) {
                                        final String key = (String) objectsToModify[1];
                                        final Serializable value = (Serializable) objectsToModify[2];
                                        if (nObjects == 3) {
                                            mapToUse.put(key, value);
                                            info.fmro.client.utility.Utils.checkMapValues(mapToUse, Set.of(key));
                                        } else if (nObjects == 4 && objectsToModify[3] instanceof Boolean) {
                                            final boolean b = (boolean) objectsToModify[3];
                                            mapToUse.put(key, value, b);
                                            info.fmro.client.utility.Utils.checkMapValues(mapToUse, Set.of(key));
                                        } else {
                                            logger.error("wrong inner size or class objectsToModify in runAfterReceive: {} {} {}", Generic.objectToString(objectsToModify), synchronizedMapModificationCommand.name(), Generic.objectToString(receivedCommand));
                                        }
                                    } else {
                                        logger.error("wrong objectsToModify class in runAfterReceive: {} {} {}", Generic.objectToString(objectsToModify), synchronizedMapModificationCommand.name(), Generic.objectToString(receivedCommand));
                                    }
                                } else {
                                    logger.error("wrong size objectsToModify in runAfterReceive: {} {} {}", Generic.objectToString(objectsToModify), synchronizedMapModificationCommand.name(), Generic.objectToString(receivedCommand));
                                }
                                break;
                            case putIfAbsent:
                                if (nObjects == 3) {
                                    if (objectsToModify[1] instanceof String && Generic.objectInstanceOf(clazz, objectsToModify[2])) {
                                        final String key = (String) objectsToModify[1];
                                        final Serializable value = (Serializable) objectsToModify[2];
                                        mapToUse.putIfAbsent(key, value);
                                        info.fmro.client.utility.Utils.checkMapValues(mapToUse, Set.of(key));
                                    } else {
                                        logger.error("wrong objectsToModify class in runAfterReceive: {} {} {}", Generic.objectToString(objectsToModify), synchronizedMapModificationCommand.name(), Generic.objectToString(receivedCommand));
                                    }
                                } else {
                                    logger.error("wrong size objectsToModify in runAfterReceive: {} {} {}", Generic.objectToString(objectsToModify), synchronizedMapModificationCommand.name(), Generic.objectToString(receivedCommand));
                                }
                                break;
                            case putAll:
                                if (nObjects == 2) {
                                    if (objectsToModify[1] instanceof HashMap) {
                                        @SuppressWarnings("unchecked") final Map<String, Serializable> m = (Map<String, Serializable>) objectsToModify[1];
                                        mapToUse.putAll(m);
                                        info.fmro.client.utility.Utils.checkMapValues(mapToUse, m.keySet());
                                    } else {
                                        logger.error("wrong objectsToModify class in runAfterReceive: {} {} {}", Generic.objectToString(objectsToModify), synchronizedMapModificationCommand.name(), Generic.objectToString(receivedCommand));
                                    }
                                } else {
                                    logger.error("wrong size objectsToModify in runAfterReceive: {} {} {}", Generic.objectToString(objectsToModify), synchronizedMapModificationCommand.name(), Generic.objectToString(receivedCommand));
                                }
                                break;
                            case remove:
                                if (nObjects >= 2) {
                                    if (objectsToModify[1] instanceof String) {
                                        final String key = (String) objectsToModify[1];
                                        if (nObjects == 2) {
                                            mapToUse.remove(key);
                                        } else if (nObjects == 3 && Generic.objectInstanceOf(clazz, objectsToModify[2])) {
                                            final Serializable value = (Serializable) objectsToModify[2];
                                            mapToUse.remove(key, value);
                                        } else {
                                            logger.error("wrong inner size or class objectsToModify in runAfterReceive: {} {} {}", Generic.objectToString(objectsToModify), synchronizedMapModificationCommand.name(), Generic.objectToString(receivedCommand));
                                        }
                                    } else {
                                        logger.error("wrong objectsToModify class in runAfterReceive: {} {} {}", Generic.objectToString(objectsToModify), synchronizedMapModificationCommand.name(), Generic.objectToString(receivedCommand));
                                    }
                                } else {
                                    logger.error("wrong size objectsToModify in runAfterReceive: {} {} {}", Generic.objectToString(objectsToModify), synchronizedMapModificationCommand.name(), Generic.objectToString(receivedCommand));
                                }
                                break;
                            case removeEntry:
                                if (nObjects == 2) {
                                    if (objectsToModify[1] instanceof AbstractMap.SimpleEntry) {
                                        @SuppressWarnings("unchecked") final Map.Entry<String, Serializable> entry = (Map.Entry<String, Serializable>) objectsToModify[1];
                                        mapToUse.removeEntry(entry);
                                    } else {
                                        logger.error("wrong objectsToModify class in runAfterReceive: {} {} {}", Generic.objectToString(objectsToModify), synchronizedMapModificationCommand.name(), Generic.objectToString(receivedCommand));
                                    }
                                } else {
                                    logger.error("wrong size objectsToModify in runAfterReceive: {} {} {}", Generic.objectToString(objectsToModify), synchronizedMapModificationCommand.name(), Generic.objectToString(receivedCommand));
                                }
                                break;
                            case removeAllEntries:
                                if (nObjects == 2) {
                                    if (objectsToModify[1] instanceof HashSet) {
                                        final Collection<?> set = (Collection<?>) objectsToModify[1];
                                        mapToUse.removeAllEntries(set);
                                    } else {
                                        logger.error("wrong objectsToModify class in runAfterReceive: {} {} {}", Generic.objectToString(objectsToModify), synchronizedMapModificationCommand.name(), Generic.objectToString(receivedCommand));
                                    }
                                } else {
                                    logger.error("wrong size objectsToModify in runAfterReceive: {} {} {}", Generic.objectToString(objectsToModify), synchronizedMapModificationCommand.name(), Generic.objectToString(receivedCommand));
                                }
                                break;
                            case retainAllEntries:
                                if (nObjects == 2) {
                                    if (objectsToModify[1] instanceof HashSet) {
                                        final Collection<?> set = (Collection<?>) objectsToModify[1];
                                        mapToUse.retainAllEntries(set);
                                    } else {
                                        logger.error("wrong objectsToModify class in runAfterReceive: {} {} {}", Generic.objectToString(objectsToModify), synchronizedMapModificationCommand.name(), Generic.objectToString(receivedCommand));
                                    }
                                } else {
                                    logger.error("wrong size objectsToModify in runAfterReceive: {} {} {}", Generic.objectToString(objectsToModify), synchronizedMapModificationCommand.name(), Generic.objectToString(receivedCommand));
                                }
                                break;
                            case removeAllKeys:
                                if (nObjects == 2) {
                                    if (objectsToModify[1] instanceof HashSet) {
                                        final Collection<?> set = (Collection<?>) objectsToModify[1];
                                        mapToUse.removeAllKeys(set);
                                    } else {
                                        logger.error("wrong objectsToModify class in runAfterReceive: {} {} {}", Generic.objectToString(objectsToModify), synchronizedMapModificationCommand.name(), Generic.objectToString(receivedCommand));
                                    }
                                } else {
                                    logger.error("wrong size objectsToModify in runAfterReceive: {} {} {}", Generic.objectToString(objectsToModify), synchronizedMapModificationCommand.name(), Generic.objectToString(receivedCommand));
                                }
                                break;
                            case retainAllKeys:
                                if (nObjects == 2) {
                                    if (objectsToModify[1] instanceof HashSet) {
                                        final Collection<?> set = (Collection<?>) objectsToModify[1];
                                        mapToUse.retainAllKeys(set);
                                    } else {
                                        logger.error("wrong objectsToModify class in runAfterReceive: {} {} {}", Generic.objectToString(objectsToModify), synchronizedMapModificationCommand.name(), Generic.objectToString(receivedCommand));
                                    }
                                } else {
                                    logger.error("wrong size objectsToModify in runAfterReceive: {} {} {}", Generic.objectToString(objectsToModify), synchronizedMapModificationCommand.name(), Generic.objectToString(receivedCommand));
                                }
                                break;
                            case removeValue:
                                if (nObjects == 2) {
                                    if (Generic.objectInstanceOf(clazz, objectsToModify[1])) {
                                        final Serializable value = (Serializable) objectsToModify[1];
                                        mapToUse.removeValue(value);
                                    } else {
                                        logger.error("wrong objectsToModify class in runAfterReceive: {} {} {}", Generic.objectToString(objectsToModify), synchronizedMapModificationCommand.name(), Generic.objectToString(receivedCommand));
                                    }
                                } else {
                                    logger.error("wrong size objectsToModify in runAfterReceive: {} {} {}", Generic.objectToString(objectsToModify), synchronizedMapModificationCommand.name(), Generic.objectToString(receivedCommand));
                                }
                                break;
                            case removeValueAll:
                                if (nObjects == 2) {
                                    if (Generic.objectInstanceOf(clazz, objectsToModify[1])) {
                                        final Serializable value = (Serializable) objectsToModify[1];
                                        mapToUse.removeValueAll(value);
                                    } else {
                                        logger.error("wrong objectsToModify class in runAfterReceive: {} {} {}", Generic.objectToString(objectsToModify), synchronizedMapModificationCommand.name(), Generic.objectToString(receivedCommand));
                                    }
                                } else {
                                    logger.error("wrong size objectsToModify in runAfterReceive: {} {} {}", Generic.objectToString(objectsToModify), synchronizedMapModificationCommand.name(), Generic.objectToString(receivedCommand));
                                }
                                break;
                            case removeAllValues:
                                if (nObjects == 2) {
                                    if (objectsToModify[1] instanceof HashSet) {
                                        final Collection<?> set = (Collection<?>) objectsToModify[1];
                                        mapToUse.removeAllValues(set);
                                    } else {
                                        logger.error("wrong objectsToModify class in runAfterReceive: {} {} {}", Generic.objectToString(objectsToModify), synchronizedMapModificationCommand.name(), Generic.objectToString(receivedCommand));
                                    }
                                } else {
                                    logger.error("wrong size objectsToModify in runAfterReceive: {} {} {}", Generic.objectToString(objectsToModify), synchronizedMapModificationCommand.name(), Generic.objectToString(receivedCommand));
                                }
                                break;
                            case retainAllValues:
                                if (nObjects == 2) {
                                    if (objectsToModify[1] instanceof HashSet) {
                                        final Collection<?> set = (Collection<?>) objectsToModify[1];
                                        mapToUse.retainAllValues(set);
                                    } else {
                                        logger.error("wrong objectsToModify class in runAfterReceive: {} {} {}", Generic.objectToString(objectsToModify), synchronizedMapModificationCommand.name(), Generic.objectToString(receivedCommand));
                                    }
                                } else {
                                    logger.error("wrong size objectsToModify in runAfterReceive: {} {} {}", Generic.objectToString(objectsToModify), synchronizedMapModificationCommand.name(), Generic.objectToString(receivedCommand));
                                }
                                break;
                            default:
                                logger.error("unsupported synchronizedMapModificationCommand in runAfterReceive: {} {}", synchronizedMapModificationCommand.name(), Generic.objectToString(receivedCommand));
                        } // end switch
                        GUI.publicRefreshDisplayedManagedObject();
                    }
                }
            } else {
                logger.error("unknown command object in runAfterReceive: {} {} {} {}", command == null ? null : command.getClass(), command, receivedCommand.getClass(), Generic.objectToString(receivedCommand));
            }
        } else {
            logger.error("unknown receivedCommand object in runAfterReceive: {} {}", receivedCommand.getClass(), Generic.objectToString(receivedCommand));
        }
    }

    @SuppressWarnings("OverlyNestedMethod")
    @Override
    public void run() {
        final KeyManager[] keyManagers = Generic.getKeyManagers(Statics.KEY_STORE_FILE_NAME, Statics.KEY_STORE_PASSWORD, Statics.KEY_STORE_TYPE);
        SSLContext sSLContext = null;
        try {
            sSLContext = SSLContext.getInstance("TLS");
            sSLContext.init(keyManagers, Generic.getTrustAllCertsManager(), new SecureRandom());
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            logger.error("SSLContext exception in InterfaceServer", e);
        }

        final SSLSocketFactory socketFactory = sSLContext != null ? sSLContext.getSocketFactory() : null;
        if (socketFactory != null) {
            int nErrors = 0;
            do {
                SSLWriterThread writerThread = null;
                ObjectInputStream objectInputStream = null;
                try {
                    this.socket = (SSLSocket) socketFactory.createSocket(Statics.SERVER_ADDRESS, Statics.SERVER_PORT);
                    if (this.socket != null) {
                        nErrors = 0;
                        writerThread = new SSLWriterThread(this.socket, this.sendQueue);
                        writerThread.start();
                        //noinspection resource,IOResourceOpenedButNotSafelyClosed
                        objectInputStream = new ObjectInputStream(this.socket.getInputStream());

                        Object receivedObject;
                        do {
                            receivedObject = objectInputStream.readObject();
                            if (receivedObject instanceof StreamObjectInterface) {
                                final StreamObjectInterface receivedCommand = (StreamObjectInterface) receivedObject;

                                runAfterReceive(receivedCommand);
                            } else if (receivedObject == null) { // nothing to be done, will reach end of loop and exit loop
                            } else {
                                logger.error("unknown type of object in interfaceConnection stream: {} {}", receivedObject.getClass(), Generic.objectToString(receivedObject));
                            }
                        } while (receivedObject != null && !Statics.mustStop.get() && !writerThread.finished.get());
                    } else {
                        logger.error("STRANGE socket null in SSLClientThread thread, timeStamp={}", System.currentTimeMillis());
                    }
//                } catch (EOFException e) {
//                    if (Statics.mustStop.get()) {
//                        logger.info("EOFException received in SSLClientThread while program stops");
//                    } else {
//                        logger.error("EOFException received in SSLClientThread: ", e);
//                    }
                } catch (@SuppressWarnings("OverlyBroadCatchBlock") IOException e) {
                    if (Statics.mustStop.get()) {
                        logger.info("IOException received in SSLClientThread, as program stops");
                    } else {
                        if (e.getClass().equals(ConnectException.class)) {
                            logger.warn("ConnectException in SSLClientThread, will retry: {}", e.toString());
                        } else if (e.getClass().equals(EOFException.class)) {
                            logger.warn("EOFException in SSLClientThread, will retry: {}", e.toString());
                        } else {
                            logger.error("IOException in SSLClientThread, will retry", e);
                        }
                    }
                } catch (ClassNotFoundException e) {
                    logger.error("STRANGE ClassNotFoundException in SSLClientThread, will retry", e);
                } finally {
                    //noinspection ConstantConditions
                    Generic.closeObjects(objectInputStream, this.socket);
                    if (writerThread != null) {
                        writerThread.finished.set(true);
                    } else { // writerThread is null, nothing to be done about it
                    }
                }

                if (Statics.mustStop.get()) { // no need for anti throttle, nothing to be done
                } else {
                    nErrors++;

                    if (nErrors < 3) {
                        Generic.threadSleepSegmented(1_000L, 100, Statics.mustStop);
                    } else if (nErrors < 10) {
                        Generic.threadSleepSegmented(5_000L, 100, Statics.mustStop);
                    } else if (nErrors < 50) {
                        Generic.threadSleepSegmented(20_000L, 100, Statics.mustStop);
                    } else {
                        logger.error("huge number of errors in SSLClientThread: {}", nErrors);
                        Generic.threadSleepSegmented(Generic.MINUTE_LENGTH_MILLISECONDS, 100, Statics.mustStop);
                    }
                }

                if (writerThread != null && writerThread.isAlive()) {
                    this.sendQueue.add(new PoisonPill());
                    try {
                        writerThread.join();
                    } catch (InterruptedException e) {
                        logger.error("InterruptedException in sslClientThread end", e);
                    }
                }
            } while (!Statics.mustStop.get());
        } else {
            logger.error("STRANGE socketFactory null in SSLClientThread thread, timeStamp={}", System.currentTimeMillis());
            Statics.mustStop.set(true);
        }

        logger.debug("SSLClientThread ends");
    }
}
