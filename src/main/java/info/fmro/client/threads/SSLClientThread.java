package info.fmro.client.threads;

import info.fmro.client.objects.Statics;
import info.fmro.shared.enums.ExistingFundsModificationCommand;
import info.fmro.shared.enums.RulesManagerModificationCommand;
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
import info.fmro.shared.stream.protocol.ChangeMessageFactory;
import info.fmro.shared.utility.Generic;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.concurrent.LinkedBlockingQueue;

public class SSLClientThread
        extends Thread {
    private static final Logger logger = LoggerFactory.getLogger(SSLClientThread.class);
    @SuppressWarnings("FieldAccessedSynchronizedAndUnsynchronized")
    public final LinkedBlockingQueue<StreamObjectInterface> sendQueue = new LinkedBlockingQueue<>();
    private SSLSocket socket;

    public synchronized void closeSocket() {
        logger.info("closing SSLClientThread socket");
        Generic.closeObjects(this.socket);
    }

    private synchronized void runAfterReceive(@NotNull final StreamObjectInterface receivedCommand) {
        if (receivedCommand instanceof RulesManager) {
            @NotNull final RulesManager rulesManager = (RulesManager) receivedCommand;
            Statics.rulesManager.copyFromStream(rulesManager);
        } else if (receivedCommand instanceof ExistingFunds) {
            @NotNull final ExistingFunds existingFunds = (ExistingFunds) receivedCommand;
            Statics.existingFunds.copyFromStream(existingFunds);
        } else if (receivedCommand instanceof MarketCache) {
            @NotNull final MarketCache marketCache = (MarketCache) receivedCommand;
            Statics.marketCache.copyFromStream(marketCache);
        } else if (receivedCommand instanceof OrderCache) {
            @NotNull final OrderCache orderCache = (OrderCache) receivedCommand;
            Statics.orderCache.copyFromStream(orderCache);
        } else if (receivedCommand instanceof MarketChangeMessage) {
            @NotNull final MarketChangeMessage marketChangeMessage = (MarketChangeMessage) receivedCommand;
            Statics.marketCache.onMarketChange(ChangeMessageFactory.ToChangeMessage(-1, marketChangeMessage), Statics.existingFunds.currencyRate);
        } else if (receivedCommand instanceof OrderChangeMessage) {
            @NotNull final OrderChangeMessage orderChangeMessage = (OrderChangeMessage) receivedCommand;
            Statics.orderCache.onOrderChange(ChangeMessageFactory.ToChangeMessage(-1, orderChangeMessage), Statics.rulesManager.orderCacheHasReset, Statics.rulesManager.newOrderMarketCreated, Statics.pendingOrdersThread,
                                             Statics.existingFunds.currencyRate);
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
                            } else {
                                logger.error("wrong objectsToModify class in betty runAfterReceive: {} {} {}", Generic.objectToString(objectsToModify), rulesManagerModificationCommand.name(), Generic.objectToString(receivedCommand));
                            }
                        } else {
                            logger.error("wrong size objectsToModify in betty runAfterReceive: {} {} {}", Generic.objectToString(objectsToModify), rulesManagerModificationCommand.name(), Generic.objectToString(receivedCommand));
                        }
                        break;
                    case removeManagedEvent:
                        if (objectsToModify != null && objectsToModify.length == 1) {
                            if (objectsToModify[0] instanceof String) {
                                final String eventId = (String) objectsToModify[0];
                                Statics.rulesManager.removeManagedEvent(eventId);
                            } else {
                                logger.error("wrong objectsToModify class in betty runAfterReceive: {} {} {}", Generic.objectToString(objectsToModify), rulesManagerModificationCommand.name(), Generic.objectToString(receivedCommand));
                            }
                        } else {
                            logger.error("wrong size objectsToModify in betty runAfterReceive: {} {} {}", Generic.objectToString(objectsToModify), rulesManagerModificationCommand.name(), Generic.objectToString(receivedCommand));
                        }
                        break;
                    case addManagedMarket:
                        if (objectsToModify != null && objectsToModify.length == 2) {
                            if (objectsToModify[0] instanceof String && objectsToModify[1] instanceof ManagedMarket) {
                                final String marketId = (String) objectsToModify[0];
                                final ManagedMarket managedMarket = (ManagedMarket) objectsToModify[1];
                                Statics.rulesManager.addManagedMarket(marketId, managedMarket);
                            } else {
                                logger.error("wrong objectsToModify class in betty runAfterReceive: {} {} {}", Generic.objectToString(objectsToModify), rulesManagerModificationCommand.name(), Generic.objectToString(receivedCommand));
                            }
                        } else {
                            logger.error("wrong size objectsToModify in betty runAfterReceive: {} {} {}", Generic.objectToString(objectsToModify), rulesManagerModificationCommand.name(), Generic.objectToString(receivedCommand));
                        }
                        break;
                    case removeManagedMarket:
                        if (objectsToModify != null && objectsToModify.length == 1) {
                            if (objectsToModify[0] instanceof String) {
                                final String marketId = (String) objectsToModify[0];
                                Statics.rulesManager.removeManagedMarket(marketId);
                            } else {
                                logger.error("wrong objectsToModify class in betty runAfterReceive: {} {} {}", Generic.objectToString(objectsToModify), rulesManagerModificationCommand.name(), Generic.objectToString(receivedCommand));
                            }
                        } else {
                            logger.error("wrong size objectsToModify in betty runAfterReceive: {} {} {}", Generic.objectToString(objectsToModify), rulesManagerModificationCommand.name(), Generic.objectToString(receivedCommand));
                        }
                        break;
                    case setEventAmountLimit:
                        if (objectsToModify != null && objectsToModify.length == 2) {
                            if (objectsToModify[0] instanceof String && objectsToModify[1] instanceof Double) {
                                final String eventId = (String) objectsToModify[0];
                                final Double newAmount = (Double) objectsToModify[1];
                                Statics.rulesManager.setEventAmountLimit(eventId, newAmount, Statics.pendingOrdersThread, Statics.orderCache, Statics.existingFunds, Statics.marketCataloguesMap);
                            } else {
                                logger.error("wrong objectsToModify class in betty runAfterReceive: {} {} {}", Generic.objectToString(objectsToModify), rulesManagerModificationCommand.name(), Generic.objectToString(receivedCommand));
                            }
                        } else {
                            logger.error("wrong size objectsToModify in betty runAfterReceive: {} {} {}", Generic.objectToString(objectsToModify), rulesManagerModificationCommand.name(), Generic.objectToString(receivedCommand));
                        }
                        break;
                    case addManagedRunner:
                        if (objectsToModify != null && objectsToModify.length == 1) {
                            if (objectsToModify[0] instanceof ManagedRunner) {
                                final ManagedRunner managedRunner = (ManagedRunner) objectsToModify[0];
                                Statics.rulesManager.addManagedRunner(managedRunner);
                            } else {
                                logger.error("wrong objectsToModify class in betty runAfterReceive: {} {} {}", Generic.objectToString(objectsToModify), rulesManagerModificationCommand.name(), Generic.objectToString(receivedCommand));
                            }
                        } else {
                            logger.error("wrong size objectsToModify in betty runAfterReceive: {} {} {}", Generic.objectToString(objectsToModify), rulesManagerModificationCommand.name(), Generic.objectToString(receivedCommand));
                        }
                        break;
                    case removeManagedRunner:
                        if (objectsToModify != null && objectsToModify.length == 2) {
                            if (objectsToModify[0] instanceof String && objectsToModify[1] instanceof RunnerId) {
                                final String marketId = (String) objectsToModify[0];
                                final RunnerId runnerId = (RunnerId) objectsToModify[1];
                                Statics.rulesManager.removeManagedRunner(marketId, runnerId);
                            } else {
                                logger.error("wrong objectsToModify class in betty runAfterReceive: {} {} {}", Generic.objectToString(objectsToModify), rulesManagerModificationCommand.name(), Generic.objectToString(receivedCommand));
                            }
                        } else {
                            logger.error("wrong size objectsToModify in betty runAfterReceive: {} {} {}", Generic.objectToString(objectsToModify), rulesManagerModificationCommand.name(), Generic.objectToString(receivedCommand));
                        }
                        break;
                    case setMarketAmountLimit:
                        if (objectsToModify != null && objectsToModify.length == 2) {
                            if (objectsToModify[0] instanceof String && objectsToModify[1] instanceof Double) {
                                final String marketId = (String) objectsToModify[0];
                                final Double amountLimit = (Double) objectsToModify[1];
                                Statics.rulesManager.setMarketAmountLimit(marketId, amountLimit);
                            } else {
                                logger.error("wrong objectsToModify class in betty runAfterReceive: {} {} {}", Generic.objectToString(objectsToModify), rulesManagerModificationCommand.name(), Generic.objectToString(receivedCommand));
                            }
                        } else {
                            logger.error("wrong size objectsToModify in betty runAfterReceive: {} {} {}", Generic.objectToString(objectsToModify), rulesManagerModificationCommand.name(), Generic.objectToString(receivedCommand));
                        }
                        break;
                    case setBackAmountLimit:
                        if (objectsToModify != null && objectsToModify.length == 3) {
                            if (objectsToModify[0] instanceof String && objectsToModify[1] instanceof RunnerId && objectsToModify[2] instanceof Double) {
                                final String marketId = (String) objectsToModify[0];
                                final RunnerId runnerId = (RunnerId) objectsToModify[1];
                                final Double amountLimit = (Double) objectsToModify[2];
                                Statics.rulesManager.setRunnerBackAmountLimit(marketId, runnerId, amountLimit);
                            } else {
                                logger.error("wrong objectsToModify class in betty runAfterReceive: {} {} {}", Generic.objectToString(objectsToModify), rulesManagerModificationCommand.name(), Generic.objectToString(receivedCommand));
                            }
                        } else {
                            logger.error("wrong size objectsToModify in betty runAfterReceive: {} {} {}", Generic.objectToString(objectsToModify), rulesManagerModificationCommand.name(), Generic.objectToString(receivedCommand));
                        }
                        break;
                    case setLayAmountLimit:
                        if (objectsToModify != null && objectsToModify.length == 3) {
                            if (objectsToModify[0] instanceof String && objectsToModify[1] instanceof RunnerId && objectsToModify[2] instanceof Double) {
                                final String marketId = (String) objectsToModify[0];
                                final RunnerId runnerId = (RunnerId) objectsToModify[1];
                                final Double amountLimit = (Double) objectsToModify[2];
                                Statics.rulesManager.setRunnerLayAmountLimit(marketId, runnerId, amountLimit);
                            } else {
                                logger.error("wrong objectsToModify class in betty runAfterReceive: {} {} {}", Generic.objectToString(objectsToModify), rulesManagerModificationCommand.name(), Generic.objectToString(receivedCommand));
                            }
                        } else {
                            logger.error("wrong size objectsToModify in betty runAfterReceive: {} {} {}", Generic.objectToString(objectsToModify), rulesManagerModificationCommand.name(), Generic.objectToString(receivedCommand));
                        }
                        break;
                    case setMinBackOdds:
                        if (objectsToModify != null && objectsToModify.length == 3) {
                            if (objectsToModify[0] instanceof String && objectsToModify[1] instanceof RunnerId && objectsToModify[2] instanceof Double) {
                                final String marketId = (String) objectsToModify[0];
                                final RunnerId runnerId = (RunnerId) objectsToModify[1];
                                final Double odds = (Double) objectsToModify[2];
                                Statics.rulesManager.setRunnerMinBackOdds(marketId, runnerId, odds);
                            } else {
                                logger.error("wrong objectsToModify class in betty runAfterReceive: {} {} {}", Generic.objectToString(objectsToModify), rulesManagerModificationCommand.name(), Generic.objectToString(receivedCommand));
                            }
                        } else {
                            logger.error("wrong size objectsToModify in betty runAfterReceive: {} {} {}", Generic.objectToString(objectsToModify), rulesManagerModificationCommand.name(), Generic.objectToString(receivedCommand));
                        }
                        break;
                    case setMaxLayOdds:
                        if (objectsToModify != null && objectsToModify.length == 3) {
                            if (objectsToModify[0] instanceof String && objectsToModify[1] instanceof RunnerId && objectsToModify[2] instanceof Double) {
                                final String marketId = (String) objectsToModify[0];
                                final RunnerId runnerId = (RunnerId) objectsToModify[1];
                                final Double odds = (Double) objectsToModify[2];
                                Statics.rulesManager.setRunnerMaxLayOdds(marketId, runnerId, odds);
                            } else {
                                logger.error("wrong objectsToModify class in betty runAfterReceive: {} {} {}", Generic.objectToString(objectsToModify), rulesManagerModificationCommand.name(), Generic.objectToString(receivedCommand));
                            }
                        } else {
                            logger.error("wrong size objectsToModify in betty runAfterReceive: {} {} {}", Generic.objectToString(objectsToModify), rulesManagerModificationCommand.name(), Generic.objectToString(receivedCommand));
                        }
                        break;
                    default:
                        logger.error("unknown rulesManagerModificationCommand in betty runAfterReceive: {} {}", rulesManagerModificationCommand.name(), Generic.objectToString(receivedCommand));
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
                            } else {
                                logger.error("wrong objectsToModify class in betty runAfterReceive: {} {} {}", Generic.objectToString(objectsToModify), existingFundsModificationCommand.name(), Generic.objectToString(receivedCommand));
                            }
                        } else {
                            logger.error("wrong size objectsToModify in betty runAfterReceive: {} {} {}", Generic.objectToString(objectsToModify), existingFundsModificationCommand.name(), Generic.objectToString(receivedCommand));
                        }
                        break;
                    case setReserve:
                        if (objectsToModify != null && objectsToModify.length == 1) {
                            if (objectsToModify[0] instanceof Double) {
                                final Double reserve = (Double) objectsToModify[0];
                                Statics.existingFunds.setReserve(reserve);
                            } else {
                                logger.error("wrong objectsToModify class in betty runAfterReceive: {} {} {}", Generic.objectToString(objectsToModify), existingFundsModificationCommand.name(), Generic.objectToString(receivedCommand));
                            }
                        } else {
                            logger.error("wrong size objectsToModify in betty runAfterReceive: {} {} {}", Generic.objectToString(objectsToModify), existingFundsModificationCommand.name(), Generic.objectToString(receivedCommand));
                        }
                        break;
                    case setAvailableFunds:
                        if (objectsToModify != null && objectsToModify.length == 1) {
                            if (objectsToModify[0] instanceof Double) {
                                final Double availableFunds = (Double) objectsToModify[0];
                                Statics.existingFunds.setAvailableFunds(availableFunds);
                            } else {
                                logger.error("wrong objectsToModify class in betty runAfterReceive: {} {} {}", Generic.objectToString(objectsToModify), existingFundsModificationCommand.name(), Generic.objectToString(receivedCommand));
                            }
                        } else {
                            logger.error("wrong size objectsToModify in betty runAfterReceive: {} {} {}", Generic.objectToString(objectsToModify), existingFundsModificationCommand.name(), Generic.objectToString(receivedCommand));
                        }
                        break;
                    case setExposure:
                        if (objectsToModify != null && objectsToModify.length == 1) {
                            if (objectsToModify[0] instanceof Double) {
                                final Double exposure = (Double) objectsToModify[0];
                                Statics.existingFunds.setExposure(exposure);
                            } else {
                                logger.error("wrong objectsToModify class in betty runAfterReceive: {} {} {}", Generic.objectToString(objectsToModify), existingFundsModificationCommand.name(), Generic.objectToString(receivedCommand));
                            }
                        } else {
                            logger.error("wrong size objectsToModify in betty runAfterReceive: {} {} {}", Generic.objectToString(objectsToModify), existingFundsModificationCommand.name(), Generic.objectToString(receivedCommand));
                        }
                        break;
                    default:
                        logger.error("unknown existingFundsModificationCommand in betty runAfterReceive: {} {}", existingFundsModificationCommand.name(), Generic.objectToString(receivedCommand));
                } // end switch
            } else {
                logger.error("unknown command object in betty runAfterReceive: {} {} {} {}", command == null ? null : command.getClass(), command, receivedCommand.getClass(), Generic.objectToString(receivedCommand));
            }
        } else {
            logger.error("unknown receivedCommand object in betty runAfterReceive: {} {}", receivedCommand.getClass(), Generic.objectToString(receivedCommand));
        }
    }

    @SuppressWarnings("OverlyNestedMethod")
    @Override
    public void run() {
        final KeyManager[] keyManagers = Generic.getKeyManagers(Statics.KEY_STORE_FILE_NAME, Statics.KEY_STORE_PASSWORD, Statics.KEY_STORE_TYPE);
        SSLContext sSLContext = null;
        try {
            sSLContext = SSLContext.getInstance("TLS");
            sSLContext.init(keyManagers, null, new SecureRandom());
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            logger.error("SSLContext exception in InterfaceServer", e);
        }

        final SSLSocketFactory socketFactory = sSLContext != null ? sSLContext.getSocketFactory() : null;
        if (socketFactory != null) {
            do {
                SSLWriterThread writerThread = null;
                ObjectInputStream objectInputStream = null;
                try {
                    this.socket = (SSLSocket) socketFactory.createSocket(Statics.SERVER_ADDRESS, Statics.SERVER_PORT);
                    // todo ssl auth check test

                    if (this.socket != null) {
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
                } catch (@SuppressWarnings("OverlyBroadCatchBlock") IOException e) {
                    logger.error("IOException in SSLClientThread, will retry", e);
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
                    Generic.threadSleep(1_000L);
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

        logger.info("SSLClientThread ends");
    }
}
