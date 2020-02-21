package info.fmro.client.main;

import info.fmro.client.objects.Statics;
import info.fmro.client.utility.Utils;
import info.fmro.shared.entities.Event;
import info.fmro.shared.entities.MarketCatalogue;
import info.fmro.shared.entities.MarketDescription;
import info.fmro.shared.enums.SynchronizedMapModificationCommand;
import info.fmro.shared.javafx.FilterableTreeItem;
import info.fmro.shared.javafx.TreeItemPredicate;
import info.fmro.shared.logic.ManagedEvent;
import info.fmro.shared.logic.ManagedMarket;
import info.fmro.shared.logic.ManagedRunner;
import info.fmro.shared.stream.cache.market.Market;
import info.fmro.shared.stream.cache.market.MarketRunner;
import info.fmro.shared.stream.cache.order.OrderMarketRunner;
import info.fmro.shared.stream.definitions.MarketDefinition;
import info.fmro.shared.stream.objects.RunnerId;
import info.fmro.shared.stream.objects.SerializableObjectModification;
import info.fmro.shared.utility.Formulas;
import info.fmro.shared.utility.Generic;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.ObservableList;
import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

@SuppressWarnings({"ClassWithTooManyMethods", "OverlyComplexClass"})
public class GUI
        extends Application {
    private static final Logger logger = LoggerFactory.getLogger(GUI.class);
    private static final String DEFAULT_EVENT_NAME = "no event attached", NULL_NAME = "null value";
    private static final Label totalFundsLabel = new Label("€ " + Generic.addCommas(Statics.existingFunds.getTotalFunds(), 2));
    private static final Label availableLabel = new Label("€ " + Generic.addCommas(Statics.existingFunds.getAvailableFunds(), 2));
    private static final Label reserveLabel = new Label("€ " + Generic.addCommas(Statics.existingFunds.getReserve(), 2));
    private static final Label exposureLabel = new Label("€ " + Generic.addCommas(Statics.existingFunds.getExposure(), 2));
    private static final SplitPane mainSplitPane = new SplitPane();
    private static final ObservableList<Node> mainSplitPaneNodesList = mainSplitPane.getItems();
    private static final DualHashBidiMap<String, TreeItem<String>> managedEventsTreeItemMap = new DualHashBidiMap<>(), managedMarketsTreeItemMap = new DualHashBidiMap<>();
    private static final DualHashBidiMap<String, FilterableTreeItem<String>> eventsTreeItemMap = new DualHashBidiMap<>(), marketsTreeItemMap = new DualHashBidiMap<>();
    private static final TreeItem<String> leftEventTreeRoot = new TreeItem<>();
    private static final FilterableTreeItem<String> rightEventTreeRoot = new FilterableTreeItem<>(null);
    private static final ObservableList<TreeItem<String>> leftEventRootChildrenList = leftEventTreeRoot.getChildren(), rightEventRootChildrenList = rightEventTreeRoot.getInternalChildren();
    private static final TreeView<String> rightTreeView = createTreeView(rightEventTreeRoot);
    private static final VBox rightVBox = new VBox(rightTreeView);
    private static final TextField filterTextField = new TextField();
    private static final GridPane mainGridPane = new GridPane();
    private static boolean rightPanelVisible;
    @Nullable
    @SuppressWarnings("RedundantFieldInitialization")
    private static TreeItem<String> currentlyShownManagedObject = null;
//    public static final CountDownLatch hasStarted = new CountDownLatch(1);

    static {
        totalFundsLabel.setId("ImportantText");
        totalFundsLabel.setMinWidth(200);
        availableLabel.setId("ImportantText");
        availableLabel.setMinWidth(200);
        reserveLabel.setId("ImportantText");
        reserveLabel.setMinWidth(200);
        exposureLabel.setId("ImportantText");
        exposureLabel.setMinWidth(200);
    }

    @Override
    public void stop() {
        Statics.mustStop.set(true);
    }

    @Override
    public void init() {
    }

    public static void publicRefreshDisplayedManagedObject() {
        Platform.runLater(GUI::refreshDisplayedManagedObject);
    }

    public static void managedMarketUpdated(final String marketId) {
        Platform.runLater(() -> updateCurrentManagedMarket(marketId));
    }

    public static void managedEventUpdated(final String eventId) {
        Platform.runLater(() -> updateCurrentManagedEvent(eventId));
    }

    public static void updateTotalFundsLabel(final double funds) {
        Platform.runLater(() -> totalFundsLabel.setText("€ " + Generic.addCommas(funds, 2)));
    }

    public static void updateAvailableLabel(final double funds) {
        Platform.runLater(() -> availableLabel.setText("€ " + Generic.addCommas(funds, 2)));
    }

    public static void updateReserveLabel(final double funds) {
        Platform.runLater(() -> reserveLabel.setText("€ " + Generic.addCommas(funds, 2)));
    }

    public static void updateExposureLabel(final double funds) {
        Platform.runLater(() -> exposureLabel.setText("€ " + Generic.addCommas(funds, 2)));
    }

    @SuppressWarnings("WeakerAccess")
    public static void checkTreeItemsWithNullName() {
        Platform.runLater(GUI::privateCheckTreeItemsWithNullName);
    }

    public static void publicRemoveManagedEvent(final String eventId, final Iterable<String> marketIds) {
        Platform.runLater(() -> removeManagedEvent(eventId, marketIds));
    }

    public static void publicAddManagedEvent(final String eventId, final ManagedEvent managedEvent) {
        Platform.runLater(() -> addManagedEvent(eventId, managedEvent));
    }

    public static void publicAddManagedMarket(final String marketId, final ManagedMarket managedMarket) {
        Platform.runLater(() -> addManagedMarket(marketId, managedMarket));
    }

    public static void publicRemoveManagedMarket(final String marketId) {
        Platform.runLater(() -> removeManagedMarket(marketId));
    }

    public static void initializeRulesManagerTreeView() {
        Platform.runLater(GUI::initializeLeftTreeView);
    }

    public static void publicMarkManagedEventAsExpired(final String eventId) {
        Platform.runLater(() -> markManagedEventAsExpired(eventId));
    }

    public static void publicMarkManagedEventsAsExpired(@NotNull final Iterable<String> eventIds) {
        Platform.runLater(() -> markManagedEventsAsExpired(eventIds));
    }

    public static void publicMarkAllManagedEventsAsExpired() {
        Platform.runLater(GUI::markAllManagedEventsAsExpired);
    }

    public static void publicMarkManagedEventAsNotExpired(final String eventId) {
        Platform.runLater(() -> markManagedEventAsNotExpired(eventId));
    }

    public static void publicMarkManagedEventsAsNotExpired(@NotNull final Iterable<String> eventIds) {
        Platform.runLater(() -> markManagedEventsAsNotExpired(eventIds));
    }

    public static void initializeEventsTreeView() {
        if (rightPanelVisible) {
            Platform.runLater(GUI::initializeRightTreeView);
        } else { // I will only update the right panel if it is visible
        }
    }

    public static void initializeMarketsTreeView() {
        if (rightPanelVisible) {
            Platform.runLater(GUI::initializeMarketsRightTreeView);
        } else { // I will only update the right panel if it is visible
        }
    }

    public static void clearEventsTreeView() {
        if (rightPanelVisible) {
            Platform.runLater(GUI::clearRightTreeView);
        } else { // I will only update the right panel if it is visible
        }
    }

    public static void clearMarketsTreeView() {
        if (rightPanelVisible) {
            Platform.runLater(GUI::clearMarketsRightTreeView);
        } else { // I will only update the right panel if it is visible
        }
    }

    public static void publicPutAllEvent(final Map<String, Event> m) {
        if (rightPanelVisible) {
            Platform.runLater(() -> putAllEvent(m));
        } else { // I will only update the right panel if it is visible
        }
    }

    public static void publicPutAllMarket(final Map<String, MarketCatalogue> m) {
        if (rightPanelVisible) {
            Platform.runLater(() -> putAllMarket(m));
        } else { // I will only update the right panel if it is visible
        }
    }

    public static void publicRemoveEntriesEvent(final Iterable<? extends Map.Entry<String, Event>> c) {
        if (rightPanelVisible) {
            Platform.runLater(() -> removeEntriesEvent(c));
        } else { // I will only update the right panel if it is visible
        }
    }

    public static void publicRemoveEntriesMarket(final Iterable<? extends Map.Entry<String, MarketCatalogue>> c) {
        if (rightPanelVisible) {
            Platform.runLater(() -> removeEntriesMarket(c));
        } else { // I will only update the right panel if it is visible
        }
    }

    public static void publicRetainEntriesEvent(final Collection<? extends Map.Entry<String, Event>> c) {
        if (rightPanelVisible) {
            Platform.runLater(() -> retainEntriesEvent(c));
        } else { // I will only update the right panel if it is visible
        }
    }

    public static void publicRetainEntriesMarket(final Collection<? extends Map.Entry<String, MarketCatalogue>> c) {
        if (rightPanelVisible) {
            Platform.runLater(() -> retainEntriesMarket(c));
        } else { // I will only update the right panel if it is visible
        }
    }

    public static void publicRemoveKeysEvent(final Iterable<String> c) {
        if (rightPanelVisible) {
            Platform.runLater(() -> removeKeysEvent(c));
        } else { // I will only update the right panel if it is visible
        }
    }

    public static void publicRemoveKeysMarket(final Iterable<String> c) {
        if (rightPanelVisible) {
            Platform.runLater(() -> removeKeysMarket(c));
        } else { // I will only update the right panel if it is visible
        }
    }

    public static void publicRetainKeysEvent(final Collection<String> c) {
        if (rightPanelVisible) {
            Platform.runLater(() -> retainKeysEvent(c));
        } else { // I will only update the right panel if it is visible
        }
    }

    public static void publicRetainKeysMarket(final Collection<String> c) {
        if (rightPanelVisible) {
            Platform.runLater(() -> retainKeysMarket(c));
        } else { // I will only update the right panel if it is visible
        }
    }

    public static void publicRemoveValueAllEvent(final Event value) {
        if (rightPanelVisible) {
            Platform.runLater(() -> removeEvent(value));
        } else { // I will only update the right panel if it is visible
        }
    }

    public static void publicRemoveValueAllMarket(final MarketCatalogue value) {
        if (rightPanelVisible) {
            Platform.runLater(() -> removeMarket(value));
        } else { // I will only update the right panel if it is visible
        }
    }

    public static void publicRemoveValuesEvent(final Iterable<? extends Event> c) {
        if (rightPanelVisible) {
            Platform.runLater(() -> removeValuesEvent(c));
        } else { // I will only update the right panel if it is visible
        }
    }

    public static void publicRemoveValuesMarket(final Iterable<? extends MarketCatalogue> c) {
        if (rightPanelVisible) {
            Platform.runLater(() -> removeValuesMarket(c));
        } else { // I will only update the right panel if it is visible
        }
    }

    public static void publicRetainValuesEvent(final Collection<? extends Event> c) {
        if (rightPanelVisible) {
            Platform.runLater(() -> retainValuesEvent(c));
        } else { // I will only update the right panel if it is visible
        }
    }

    public static void publicRetainValuesMarket(final Collection<? extends MarketCatalogue> c) {
        if (rightPanelVisible) {
            Platform.runLater(() -> retainValuesMarket(c));
        } else { // I will only update the right panel if it is visible
        }
    }

    public static void publicRemoveEvent(final String eventId) {
        if (rightPanelVisible) {
            Platform.runLater(() -> removeEvent(eventId));
        } else { // I will only update the right panel if it is visible
        }
    }

    public static void publicRemoveEvent(final Event event) {
        if (rightPanelVisible) {
            Platform.runLater(() -> removeEvent(event));
        } else { // I will only update the right panel if it is visible
        }
    }

    public static void publicAddEvent(final String eventId, final Event event) {
        if (rightPanelVisible) {
            Platform.runLater(() -> addEvent(eventId, event));
        } else { // I will only update the right panel if it is visible
        }
    }

    public static void publicAddMarket(final String marketId, final MarketCatalogue market) {
        if (rightPanelVisible) {
            Platform.runLater(() -> addMarket(marketId, market));
        } else { // I will only update the right panel if it is visible
        }
    }

    public static void publicRemoveMarket(final MarketCatalogue market) {
        if (rightPanelVisible) {
            Platform.runLater(() -> removeMarket(market));
        } else { // I will only update the right panel if it is visible
        }
    }

    public static void publicRemoveMarket(final String marketId) {
        if (rightPanelVisible) {
            Platform.runLater(() -> removeMarket(marketId));
        } else { // I will only update the right panel if it is visible
        }
    }

    private static void putAllEvent(final Map<String, Event> m) {
        if (m == null) { // nothing to be done
        } else {
            for (@NotNull final Map.Entry<String, Event> entry : m.entrySet()) {
                addEvent(entry);
            }
        }
    }

    private static void putAllMarket(final Map<String, MarketCatalogue> m) {
        if (m == null) { // nothing to be done
        } else {
            for (@NotNull final Map.Entry<String, MarketCatalogue> entry : m.entrySet()) {
                addMarket(entry);
            }
        }
    }

    private static void removeEntriesEvent(final Iterable<? extends Map.Entry<String, Event>> c) {
        if (c == null) { // nothing to be done
        } else {
            for (final Map.Entry<String, Event> entry : c) {
                if (entry == null) {
                    logger.error("null entry in removeEntriesEvent for: {}", Generic.objectToString(c));
                } else {
                    removeEvent(entry.getKey());
                }
            }
        }
    }

    private static void removeEntriesMarket(final Iterable<? extends Map.Entry<String, MarketCatalogue>> c) {
        if (c == null) { // nothing to be done
        } else {
            for (final Map.Entry<String, MarketCatalogue> entry : c) {
                if (entry == null) {
                    logger.error("null entry in removeEntriesMarket for: {}", Generic.objectToString(c));
                } else {
                    removeMarket(entry.getValue());
                }
            }
        }
    }

    private static void retainEntriesEvent(final Collection<? extends Map.Entry<String, Event>> c) {
        if (c == null) { // nothing to be done
        } else {
            final Collection<String> retainEventIds = new HashSet<>(Generic.getCollectionCapacity(c));
            for (final Map.Entry<String, Event> entry : c) {
                if (entry == null) {
                    logger.error("null entry in retainEntriesEvent for: {}", Generic.objectToString(c));
                } else {
                    retainEventIds.add(entry.getKey());
                }
            }
            for (final String eventId : eventsTreeItemMap.keySet()) {
                if (retainEventIds.contains(eventId)) { // I'll retain it, nothing to be done
                } else {
                    removeEvent(eventId);
                }
            }
        }
    }

    private static void retainEntriesMarket(final Collection<? extends Map.Entry<String, MarketCatalogue>> c) {
        if (c == null) { // nothing to be done
        } else {
            final Collection<String> retainMarketIds = new HashSet<>(Generic.getCollectionCapacity(c));
            for (final Map.Entry<String, MarketCatalogue> entry : c) {
                if (entry == null) {
                    logger.error("null entry in retainEntriesMarket for: {}", Generic.objectToString(c));
                } else {
                    retainMarketIds.add(entry.getKey());
                }
            }
            for (final String marketId : marketsTreeItemMap.keySet()) {
                if (retainMarketIds.contains(marketId)) { // I'll retain it, nothing to be done
                } else {
                    removeMarket(marketId);
                }
            }
        }
    }

    private static void removeKeysEvent(final Iterable<String> c) {
        if (c == null) { // nothing to be done
        } else {
            for (final String eventId : c) {
                removeEvent(eventId);
            }
        }
    }

    private static void removeKeysMarket(final Iterable<String> c) {
        if (c == null) { // nothing to be done
        } else {
            for (final String marketId : c) {
                removeMarket(marketId);
            }
        }
    }

    private static void retainKeysEvent(final Collection<String> c) {
        if (c == null) { // nothing to be done
        } else {
            for (final String eventId : eventsTreeItemMap.keySet()) {
                if (c.contains(eventId)) { // I'll retain it, nothing to be done
                } else {
                    removeEvent(eventId);
                }
            }
        }
    }

    private static void retainKeysMarket(final Collection<String> c) {
        if (c == null) { // nothing to be done
        } else {
            for (final String marketId : marketsTreeItemMap.keySet()) {
                if (c.contains(marketId)) { // I'll retain it, nothing to be done
                } else {
                    removeMarket(marketId);
                }
            }
        }
    }

    private static void removeValuesEvent(final Iterable<? extends Event> c) {
        if (c == null) { // nothing to be done
        } else {
            for (final Event event : c) {
                removeEvent(event);
            }
        }
    }

    private static void removeValuesMarket(final Iterable<? extends MarketCatalogue> c) {
        if (c == null) { // nothing to be done
        } else {
            for (final MarketCatalogue market : c) {
                removeMarket(market);
            }
        }
    }

    private static void retainValuesEvent(final Collection<? extends Event> c) {
        if (c == null) { // nothing to be done
        } else {
            final Collection<String> retainEventIds = new HashSet<>(Generic.getCollectionCapacity(c));
            for (final Event event : c) {
                if (event == null) {
                    logger.error("null event in retainValuesEvent for: {}", Generic.objectToString(c));
                } else {
                    retainEventIds.add(event.getId());
                }
            }
            for (final String eventId : eventsTreeItemMap.keySet()) {
                if (retainEventIds.contains(eventId)) { // I'll retain it, nothing to be done
                } else {
                    removeEvent(eventId);
                }
            }
        }
    }

    private static void retainValuesMarket(final Collection<? extends MarketCatalogue> c) {
        if (c == null) { // nothing to be done
        } else {
            final Collection<String> retainMarketIds = new HashSet<>(Generic.getCollectionCapacity(c));
            for (final MarketCatalogue market : c) {
                if (market == null) {
                    logger.error("null market in retainValuesMarket for: {}", Generic.objectToString(c));
                } else {
                    retainMarketIds.add(market.getMarketId());
                }
            }
            for (final String marketId : marketsTreeItemMap.keySet()) {
                if (retainMarketIds.contains(marketId)) { // I'll retain it, nothing to be done
                } else {
                    removeMarket(marketId);
                }
            }
        }
    }

    private static void removeEvent(final Event event) {
        final String eventId = event == null ? null : event.getId();
        removeEvent(eventId);
    }

    private static void removeEvent(final String eventId) {
        final TreeItem<String> eventTreeItem = eventsTreeItemMap.get(eventId);
        @Nullable final ObservableList<TreeItem<String>> listOfChildren = eventTreeItem != null ? eventTreeItem.getChildren() : null;
        @Nullable final Set<String> toBeReAddedMarketIds;
        if (listOfChildren != null && !listOfChildren.isEmpty()) {
            toBeReAddedMarketIds = new HashSet<>(4);
            for (final TreeItem<String> marketItem : listOfChildren) {
                final String marketId = marketsTreeItemMap.getKey(marketItem);
                marketsTreeItemMap.removeValue(marketItem);
                if (marketId != null) {
                    toBeReAddedMarketIds.add(marketId);
                } else {
                    logger.error("null marketId while building reAdd set in removeEvent for: {} {}", eventId, Generic.objectToString(marketItem));
                }
            }
            listOfChildren.clear();
        } else { // no markets to remove from children
            toBeReAddedMarketIds = null;
        }
        rightEventRootChildrenList.remove(eventTreeItem);
        eventsTreeItemMap.remove(eventId);
        if (toBeReAddedMarketIds != null) { // might be normal
            for (final String marketId : toBeReAddedMarketIds) {
                addMarket(marketId);
            }
        } else { // this is the normal case, nothing to be reAdded
        }
    }

    private static void removeManagedEvent(final String eventId, final Iterable<String> marketIds) {
        final TreeItem<String> eventTreeItem = managedEventsTreeItemMap.get(eventId);
        if (marketIds != null) {
            for (final String marketId : marketIds) {
                removeManagedMarket(marketId, eventTreeItem);
            }
        } else { // no marketIds to remove
        }
        @Nullable final Set<String> toBeReAddedMarketIds;
        @Nullable final ObservableList<TreeItem<String>> listOfChildren = eventTreeItem != null ? eventTreeItem.getChildren() : null;
        if (listOfChildren != null && !listOfChildren.isEmpty()) { // just in case there are children left, that were not removed
            toBeReAddedMarketIds = new HashSet<>(2);
            for (final TreeItem<String> marketItem : listOfChildren) {
                final String marketId = managedMarketsTreeItemMap.getKey(marketItem);
                managedMarketsTreeItemMap.removeValue(marketItem);
                if (marketId != null) {
                    toBeReAddedMarketIds.add(marketId);
                } else {
                    logger.error("null marketId while building reAdd set in removeEvent for: {} {} {}", eventId, Generic.objectToString(marketIds), Generic.objectToString(marketItem));
                }
            }
            listOfChildren.clear();
        } else { // no markets to remove from children
            toBeReAddedMarketIds = null;
        }
        leftEventRootChildrenList.remove(eventTreeItem);
        managedEventsTreeItemMap.remove(eventId);
        if (toBeReAddedMarketIds != null) {
            logger.info("{} managedMarkets reAdded after managedEvent removal for: {} {} {}", toBeReAddedMarketIds.size(), eventId, Generic.objectToString(toBeReAddedMarketIds), Generic.objectToString(marketIds));
            for (final String marketId : toBeReAddedMarketIds) {
                addManagedMarket(marketId);
            }
        } else { // this is the normal case, nothing to be reAdded
        }
        clearMainGridPaneIfDisplaysTreeItem(eventTreeItem);
    }

    @SuppressWarnings("UnusedReturnValue")
    private static boolean removeMarket(final MarketCatalogue market) {
        final String marketId = market == null ? null : market.getMarketId();
        return removeMarket(marketId);
    }

    private static boolean removeMarket(final String marketId) {
        final TreeItem<String> marketTreeItem = marketsTreeItemMap.remove(marketId);
        final FilterableTreeItem<String> eventTreeItem = marketTreeItem == null ? null : (FilterableTreeItem<String>) marketTreeItem.getParent();
        @Nullable final ObservableList<TreeItem<String>> listOfChildren = eventTreeItem == null ? null : eventTreeItem.getInternalChildren();
        final boolean removed = listOfChildren != null && listOfChildren.remove(marketTreeItem);
        if (removed && eventsTreeItemMap.getKey(eventTreeItem) == null && listOfChildren.isEmpty()) { // parentEventItem is defaultEvent and it no longer has children
            eventsTreeItemMap.removeValue(eventTreeItem);
            rightEventRootChildrenList.remove(eventTreeItem);
        } else { // parentEventItem is not defaultEvent, nothing to be done
        }

        return removed;
    }

    @SuppressWarnings("UnusedReturnValue")
    private static boolean removeManagedMarket(final String marketId) {
        final TreeItem<String> marketTreeItem = managedMarketsTreeItemMap.get(marketId);
        final TreeItem<String> eventTreeItem = marketTreeItem == null ? null : marketTreeItem.getParent();
//        @Nullable final ObservableList<TreeItem<String>> parentListOfChildren = eventTreeItem == null ? null : eventTreeItem.getChildren();
        return removeManagedMarket(marketTreeItem, eventTreeItem);
    }

    @SuppressWarnings("UnusedReturnValue")
    private static boolean removeManagedMarket(final String marketId, final TreeItem<String> eventTreeItem) {
        final TreeItem<String> marketTreeItem = managedMarketsTreeItemMap.get(marketId);
//        @Nullable final ObservableList<TreeItem<String>> parentListOfChildren = eventTreeItem == null ? null : eventTreeItem.getChildren();
        return removeManagedMarket(marketTreeItem, eventTreeItem);
    }
//    @SuppressWarnings("UnusedReturnValue")
//    private static boolean removeManagedMarket(final String marketId, final Collection<TreeItem<String>> parentListOfChildren) {
//        final TreeItem<String> marketTreeItem = managedMarketsTreeItemMap.get(marketId);
//        return removeManagedMarket(marketTreeItem, parentListOfChildren);
//    }

    private static boolean removeManagedMarket(final TreeItem<String> marketTreeItem, final TreeItem<String> eventTreeItem) {
        managedMarketsTreeItemMap.removeValue(marketTreeItem);
        @Nullable final ObservableList<TreeItem<String>> parentListOfChildren = eventTreeItem == null ? null : eventTreeItem.getChildren();
        final boolean removed = parentListOfChildren != null && parentListOfChildren.remove(marketTreeItem);

        if (removed && managedEventsTreeItemMap.getKey(eventTreeItem) == null && parentListOfChildren.isEmpty()) { // parentEventItem is defaultEvent and it no longer has children
            managedEventsTreeItemMap.removeValue(eventTreeItem);
            leftEventRootChildrenList.remove(eventTreeItem);
        } else { // parentEventItem is not defaultEvent, nothing to be done
        }

//        if (removed) { // already removed, nothing to be done
//        } else {
//            final TreeItem<String> defaultEvent = managedEventsTreeItemMap.get(null);
//            if (defaultEvent != null) {
//                @NotNull final ObservableList<TreeItem<String>> defaultChildren = defaultEvent.getChildren();
//                removed = defaultChildren.remove(marketTreeItem);
//                if (defaultChildren.isEmpty()) {
//                    leftEventRootChildrenList.remove(defaultEvent);
//                } else { // still items on defaultEvent, won't remove it
//                }
//            } else {
//                removed = false;
//            }
//        }
        if (removed) {
            clearMainGridPaneIfDisplaysTreeItem(marketTreeItem);
        } else { // nothing was removed, no need to clear the gridPane
        }
        return removed;
    }

    private static void clearLeftTreeView() {
        for (final TreeItem<String> treeItem : leftEventRootChildrenList) {
            treeItem.getChildren().clear();
        }
        leftEventRootChildrenList.clear();

        managedEventsTreeItemMap.clear();
        managedMarketsTreeItemMap.clear();
        clearMainGridPane();
    }

    private static void clearMarketsRightTreeView() {
        for (final TreeItem<String> treeItem : rightEventRootChildrenList) {
            treeItem.getChildren().clear();
        }

        marketsTreeItemMap.clear();
    }

    private static void clearRightTreeView() {
        for (final TreeItem<String> treeItem : rightEventRootChildrenList) {
            treeItem.getChildren().clear();
        }
        rightEventRootChildrenList.clear();

        eventsTreeItemMap.clear();
        marketsTreeItemMap.clear();
    }

    @SuppressWarnings("UnusedReturnValue")
    private static int initializeLeftTreeView() {
        int modified = 0;

        clearLeftTreeView();
        modified += addManagedEvents();
        modified += addManagedMarkets();

        return modified;
    }

    @SuppressWarnings("UnusedReturnValue")
    private static int initializeRightTreeView() {
        int modified = 0;

        clearRightTreeView();
        modified += addEvents();
        modified += addMarkets();

        return modified;
    }

    @SuppressWarnings("UnusedReturnValue")
    private static int initializeMarketsRightTreeView() {
        int modified = 0;

        clearMarketsRightTreeView();
//        modified += addEvents();
        modified += addMarkets();

        return modified;
    }

    @NotNull
    private static TreeView<String> createTreeView(@NotNull final TreeItem<String> eventTreeRoot) {
        final TreeView<String> treeView = new TreeView<>();
        treeView.setShowRoot(false);
        treeView.setRoot(eventTreeRoot);
        eventTreeRoot.setExpanded(true);

        if (eventTreeRoot == leftEventTreeRoot) {
            initializeLeftTreeView();
        } else if (eventTreeRoot == rightEventTreeRoot) {
            initializeRightTreeView();
        } else {
            logger.error("unknown treeRoot in createTreeView: {}", Generic.objectToString(eventTreeRoot));
        }

        return treeView;
    }

    @SuppressWarnings("OverlyNestedMethod")
    private static void privateCheckTreeItemsWithNullName() { // only checks the managed items
        for (@NotNull final TreeItem<String> eventItem : managedEventsTreeItemMap.values()) {
            final String treeName = eventItem.getValue();
            if (treeName == null || NULL_NAME.equals(treeName)) {
                final String eventId = managedEventsTreeItemMap.getKey(eventItem);
                if (eventId != null) {
                    @NotNull final ManagedEvent managedEvent = Statics.rulesManager.events.get(eventId);
                    if (managedEvent == null) {
                        logger.error("null managedEvent in privateCheckTreeItemsWithNullName for: {}", eventId);
                    } else {
                        final String eventName = GUIUtils.getManagedEventName(managedEvent);
                        if (eventName != null) {
                            eventItem.setValue(eventName);
                            reorderManagedEventItem(eventItem);
                        } else {
                            if (treeName == null) {
                                eventItem.setValue(NULL_NAME);
                                reorderManagedEventItem(eventItem);
                            } else { // I already set the NULL_NAME, nothing more to be done
                            }
                        }
                    }
                } else {
                    logger.error("null both eventId and treeName for: {} {}", Generic.objectToString(eventItem), Generic.objectToString(managedEventsTreeItemMap));
                }
            } else { // name not null, nothing to be done
            }
        }
        for (@NotNull final TreeItem<String> marketItem : managedMarketsTreeItemMap.values()) {
            final String treeName = marketItem.getValue();
            if (treeName == null || NULL_NAME.equals(treeName)) {
                final String marketId = managedMarketsTreeItemMap.getKey(marketItem);
                if (marketId != null) {
                    final ManagedMarket managedMarket = Statics.rulesManager.markets.get(marketId);
                    if (managedMarket != null) {
                        final String marketName = GUIUtils.getManagedMarketName(marketId, managedMarket);
                        if (marketName != null) {
                            marketItem.setValue(marketName);
                            reorderManagedMarketItem(marketItem);
                        } else {
                            if (treeName == null) {
                                marketItem.setValue(NULL_NAME);
                                reorderManagedEventItem(marketItem);
                            } else { // I already set the NULL_NAME, nothing more to be done
                            }
                        }
                    } else {
                        logger.error("null managedMarket in privateCheckTreeItemsWithNullName for: {} {}", marketId, Generic.objectToString(Statics.rulesManager.markets));
                        Statics.rulesManager.removeManagedMarket(marketId);
                    }
                } else {
                    logger.error("null both marketId and treeName for: {} {}", Generic.objectToString(marketItem), Generic.objectToString(managedMarketsTreeItemMap));
                }
            } else { // name not null, nothing to be done
            }
        }
    }

    private static int addMarkets() {
        int modified = 0;
        @NotNull final HashMap<String, MarketCatalogue> mapCopy = Statics.marketCataloguesMap.copy();
        for (@NotNull final Map.Entry<String, MarketCatalogue> entry : mapCopy.entrySet()) {
            modified += addMarket(entry);
        }
        return modified;
    }

    private static int addManagedMarkets() {
        int modified = 0;
        @NotNull final HashMap<String, ManagedMarket> mapCopy = Statics.rulesManager.markets.copy();
        for (@NotNull final Map.Entry<String, ManagedMarket> entry : mapCopy.entrySet()) {
            modified += addManagedMarket(entry);
        }
        return modified;
    }

    @SuppressWarnings("UnusedReturnValue")
    private static int addMarket(final String marketId) {
        final MarketCatalogue market = Statics.marketCataloguesMap.get(marketId);
        return addMarket(marketId, market);
    }

    private static int addMarket(@NotNull final Map.Entry<String, ? extends MarketCatalogue> entry) {
        final String marketId = entry.getKey();
        final MarketCatalogue market = entry.getValue();
        return addMarket(marketId, market);
    }

    private static int addMarket(final String marketId, final MarketCatalogue market) {
        int modified = 0;
        if (marketsTreeItemMap.containsKey(marketId)) { // already contained, nothing to be done, won't update the object here
        } else {
            if (market == null) {
                logger.error("null MarketCatalogue in addMarkets for: {}", marketId);
                Statics.marketCataloguesMap.removeValueAll(null);
            } else {
                final String eventId = Formulas.getEventIdOfMarketCatalogue(market);

                @Nullable FilterableTreeItem<String> parentItem = eventsTreeItemMap.get(eventId);
                if (parentItem == null) { // might be ok, won't do anything
                    parentItem = getDefaultEventAndCreateIfNotExist();
                } else { // I have the parentItem, nothing more to be done
                }
                String marketName = market.getMarketName();
                if (marketName == null) {
                    final MarketDescription description = market.getDescription();
                    if (description == null) {
                        logger.error("null description for market in addMarket: {} {}", marketId, Generic.objectToString(market));
                    } else {
                        marketName = description.getMarketType();
                    }
                } else { // I have the name, nothing more to be done
                }
                final FilterableTreeItem<String> marketTreeItem = new FilterableTreeItem<>(marketName);
//                marketTreeItem.setExpanded(true);
                marketsTreeItemMap.put(marketId, marketTreeItem);
                addTreeItem(marketTreeItem, parentItem);
                modified++;
            }
        }
        return modified;
    }

    @SuppressWarnings("UnusedReturnValue")
    private static int addManagedMarket(final String marketId) {
        final ManagedMarket managedMarket = Statics.rulesManager.markets.get(marketId);
        return addManagedMarket(marketId, managedMarket);
    }

    private static int addManagedMarket(@NotNull final Map.Entry<String, ? extends ManagedMarket> entry) {
        final String marketId = entry.getKey();
        final ManagedMarket managedMarket = entry.getValue();
        return addManagedMarket(marketId, managedMarket);
    }

    private static int addManagedMarket(final String marketId, final ManagedMarket managedMarket) {
        int modified = 0;
        if (managedMarketsTreeItemMap.containsKey(marketId)) { // already contained, nothing to be done, won't update the object here
        } else {
            if (managedMarket == null) {
                logger.error("null ManagedMarket in addManagedMarkets for: {}", marketId);
                Statics.rulesManager.markets.removeValueAll(null);
            } else {
                final String eventId = managedMarket.getParentEventId(Statics.marketCataloguesMap, Statics.rulesManager.rulesHaveChanged);

                @Nullable TreeItem<String> parentItem = managedEventsTreeItemMap.get(eventId);
                if (parentItem == null) {
                    parentItem = getDefaultManagedEventAndCreateIfNotExist();
                } else { // I have the parentItem, nothing more to be done
                }

                final String marketName = GUIUtils.getManagedMarketName(marketId, managedMarket);
                final TreeItem<String> marketTreeItem = new TreeItem<>(marketName);
                marketTreeItem.setExpanded(true);
                managedMarketsTreeItemMap.put(marketId, marketTreeItem);
                addTreeItem(marketTreeItem, parentItem);
                modified++;
            }
        }

        return modified;
    }

    @NotNull
    private static FilterableTreeItem<String> getDefaultEventAndCreateIfNotExist() {
        FilterableTreeItem<String> defaultEvent = eventsTreeItemMap.get(null);
        if (defaultEvent == null) {
            defaultEvent = new FilterableTreeItem<>(DEFAULT_EVENT_NAME);
            defaultEvent.setExpanded(true);
            eventsTreeItemMap.put(null, defaultEvent);
            addTreeItem(defaultEvent, rightEventRootChildrenList);
        } else { // I have the defaultEvent, nothing more to be done
        }
        return defaultEvent;
    }

    @NotNull
    private static TreeItem<String> getDefaultManagedEventAndCreateIfNotExist() {
        TreeItem<String> defaultEvent = managedEventsTreeItemMap.get(null);
        if (defaultEvent == null) {
            defaultEvent = new TreeItem<>(DEFAULT_EVENT_NAME);
            defaultEvent.setExpanded(true);
            managedEventsTreeItemMap.put(null, defaultEvent);
            addTreeItem(defaultEvent, leftEventRootChildrenList);
        } else { // I have the defaultEvent, nothing more to be done
        }
        return defaultEvent;
    }

    private static int addEvents() {
        int modified = 0;
        @NotNull final HashMap<String, Event> mapCopy = Statics.eventsMap.copy();
        for (@NotNull final Map.Entry<String, Event> entry : mapCopy.entrySet()) {
            modified += addEvent(entry);
        }
        return modified;
    }

    private static int addEvent(@NotNull final Map.Entry<String, ? extends Event> entry) {
        final String eventId = entry.getKey();
        final Event event = entry.getValue();
        return addEvent(eventId, event);
    }

    private static int addEvent(final String eventId, final Event event) {
        int modified = 0;
        if (eventsTreeItemMap.containsKey(eventId)) { // already contained, nothing to be done, won't update the object here
        } else {
            if (event == null) {
                logger.error("null Event in addEvent for: {}", eventId);
                Statics.eventsMap.removeValueAll(null);
            } else {
                final String eventName = event.getName();
                final FilterableTreeItem<String> eventTreeItem = new FilterableTreeItem<>(eventName);
//                eventTreeItem.setExpanded(true);
                eventsTreeItemMap.put(eventId, eventTreeItem);
                if (Statics.unsupportedEventNames.contains(eventName)) { // won't add eventTreeItem to the treeView
                } else {
                    addTreeItem(eventTreeItem, rightEventRootChildrenList);
                }

                modified += checkMarketsOnDefaultNode(event, eventTreeItem);

                modified++;
            }
        }
        return modified;
    }

    private static void markManagedEventAsExpired(final String eventId) {
        final TreeItem<String> managedEventTreeItem = managedEventsTreeItemMap.get(eventId);
        if (managedEventTreeItem == null) { // can be normal, nothing to be done
        } else {
            managedEventTreeItem.setValue(GUIUtils.markNameAsExpired(managedEventTreeItem.getValue()));
            reorderManagedEventItem(managedEventTreeItem);
        }
    }

    private static void markManagedEventsAsExpired(@NotNull final Iterable<String> eventIds) {
        for (final String eventId : eventIds) {
            markManagedEventAsExpired(eventId);
        }
    }

    private static void markAllManagedEventsAsExpired() {
        @NotNull final Set<String> keySetCopy = Statics.rulesManager.events.keySetCopy();
        for (final String eventId : keySetCopy) {
            markManagedEventAsExpired(eventId);
        }
    }

    private static void markManagedEventAsNotExpired(final String eventId) {
        final TreeItem<String> managedEventTreeItem = managedEventsTreeItemMap.get(eventId);
        if (managedEventTreeItem == null) { // can be normal, nothing to be done
        } else {
            managedEventTreeItem.setValue(GUIUtils.markNameAsNotExpired(managedEventTreeItem.getValue()));
            reorderManagedEventItem(managedEventTreeItem);
        }
    }

    private static void markManagedEventsAsNotExpired(@NotNull final Iterable<String> eventIds) {
        for (final String eventId : eventIds) {
            markManagedEventAsNotExpired(eventId);
        }
    }

    private static int addManagedEvent(@NotNull final Map.Entry<String, ? extends ManagedEvent> entry) {
        final String eventId = entry.getKey();
        final ManagedEvent managedEvent = entry.getValue();
        return addManagedEvent(eventId, managedEvent);
    }

    private static int addManagedEvent(final String eventId, final ManagedEvent managedEvent) {
        int modified = 0;
        if (managedEventsTreeItemMap.containsKey(eventId)) { // already contained, nothing to be done, won't update the object here
        } else {
            if (managedEvent == null) {
                logger.error("null ManagedEvent in addManagedEvent for: {}", eventId);
                Statics.rulesManager.events.removeValueAll(null);
            } else {
                final String eventName = GUIUtils.eventExistsInMap(eventId) ? GUIUtils.getManagedEventName(managedEvent) : GUIUtils.markNameAsExpired(GUIUtils.getManagedEventName(managedEvent));
                final TreeItem<String> eventTreeItem = new TreeItem<>(eventName);
                eventTreeItem.setExpanded(true);
                managedEventsTreeItemMap.put(eventId, eventTreeItem);

                addTreeItem(eventTreeItem, leftEventRootChildrenList);
                modified += checkManagedMarketsOnDefaultNode(managedEvent, eventTreeItem);

                modified++;
            }
        }
        return modified;
    }

    private static int addManagedEvents() {
        int modified = 0;
        @NotNull final HashMap<String, ManagedEvent> mapCopy = Statics.rulesManager.events.copy();
        for (@NotNull final Map.Entry<String, ManagedEvent> entry : mapCopy.entrySet()) {
            modified += addManagedEvent(entry);
        }
        return modified;
    }

    private static int checkMarketsOnDefaultNode(@NotNull final Event event, @NotNull final FilterableTreeItem<String> eventTreeItem) {
        int modified = 0;
        final FilterableTreeItem<String> defaultEvent = eventsTreeItemMap.get(null);
        if (defaultEvent != null) {
            final String eventId = event.getId();
            @NotNull final ObservableList<TreeItem<String>> defaultChildrenList = defaultEvent.getInternalChildren();
            final Collection<TreeItem<String>> marketsToMove = new ArrayList<>(0);
            for (final TreeItem<String> marketTreeItem : defaultChildrenList) {
                final String marketId = marketsTreeItemMap.getKey(marketTreeItem);
                final String parentEventId = Formulas.getEventIdOfMarketId(marketId, Statics.marketCataloguesMap);
                if (Objects.equals(eventId, parentEventId)) {
                    marketsToMove.add(marketTreeItem);
                } else { // map does not belong to the current event, nothing to be done
                }
            }
            for (final TreeItem<String> marketTreeItem : marketsToMove) {
                defaultChildrenList.remove(marketTreeItem);
                addTreeItem(marketTreeItem, eventTreeItem);
                modified++;
            }

            final int defaultChildrenRemaining = defaultChildrenList.size();
            if (defaultChildrenRemaining == 0) {
                rightEventRootChildrenList.remove(defaultEvent);
                eventsTreeItemMap.remove(null);
            }
        } else { // no defaultEvent, nothing to check
        }
        return modified;
    }

    private static int checkManagedMarketsOnDefaultNode(@NotNull final ManagedEvent managedEvent, @NotNull final TreeItem<String> eventTreeItem) {
        int modified = 0;
        final TreeItem<String> defaultEvent = managedEventsTreeItemMap.get(null);
        if (defaultEvent != null) {
            @NotNull final HashMap<String, ManagedMarket> marketsMap = managedEvent.marketsMap.copy(Statics.rulesManager);
            final int nMarkets = marketsMap.size();
            if (nMarkets > 0) {
                @NotNull final ObservableList<TreeItem<String>> defaultChildrenList = defaultEvent.getChildren();
                final Collection<TreeItem<String>> marketsToMove = new ArrayList<>(0);
                for (final TreeItem<String> marketTreeItem : defaultChildrenList) {
                    final String marketId = managedMarketsTreeItemMap.getKey(marketTreeItem);
                    if (marketsMap.containsKey(marketId)) {
                        marketsToMove.add(marketTreeItem);
                    } else { // map does not belong to the current managedEvent, nothing to be done
                    }
                }
                for (final TreeItem<String> marketTreeItem : marketsToMove) {
                    defaultChildrenList.remove(marketTreeItem);
                    addTreeItem(marketTreeItem, eventTreeItem);
                    modified++;
                }

                final int defaultChildrenRemaining = defaultChildrenList.size();
                if (defaultChildrenRemaining == 0) {
                    leftEventRootChildrenList.remove(defaultEvent);
                    managedEventsTreeItemMap.remove(null);
                }
            } else { // no marketIds in marketsMap, nothing to check
            }
        } else { // no defaultEvent, nothing to check
        }
        return modified;
    }

    private static void reorderManagedEventItem(@NotNull final TreeItem<String> eventItem) {
        leftEventRootChildrenList.remove(eventItem);
        addTreeItem(eventItem, leftEventRootChildrenList);
    }

    private static void reorderManagedMarketItem(@NotNull final TreeItem<String> marketItem) {
        final TreeItem<String> parent = marketItem.getParent();
        if (parent == null) {
            logger.error("null parent during reorderManagedMarketItem for: {} {}", managedMarketsTreeItemMap.getKey(marketItem), marketItem);
        } else {
            addTreeItem(marketItem, parent.getChildren());
        }
    }

    @SuppressWarnings("OverloadedMethodsWithSameNumberOfParameters")
    private static void addTreeItem(@NotNull final TreeItem<String> treeItem, @NotNull final TreeItem<String> root) {
        @NotNull final ObservableList<TreeItem<String>> rootChildrenList = root.getChildren();
        addTreeItem(treeItem, rootChildrenList);
    }

    @SuppressWarnings("OverloadedMethodsWithSameNumberOfParameters")
    private static void addTreeItem(@NotNull final TreeItem<String> treeItem, @NotNull final FilterableTreeItem<String> root) {
        @NotNull final ObservableList<TreeItem<String>> rootChildrenList = root.getInternalChildren();
        addTreeItem(treeItem, rootChildrenList);
    }

    private static void addTreeItem(@NotNull final TreeItem<String> treeItem, @NotNull final ObservableList<TreeItem<String>> rootChildrenList) {
        final int size = rootChildrenList.size();
        boolean added = false;
        for (int i = 0; i < size; i++) {
            if (treeItemCompare(rootChildrenList.get(i), treeItem) >= 0) {
                rootChildrenList.add(i, treeItem);
                added = true;
                break;
            } else { // will continue, nothing to be done yet
            }
        }
        if (added) { // added, nothing to be done
        } else {
            rootChildrenList.add(treeItem); // placed at the end of the list
        }
    }

    private static int treeItemCompare(@NotNull final TreeItem<String> first, @NotNull final TreeItem<String> second) {
        final int result;
        final String firstString = first.getValue(), secondString = second.getValue();
        if (Objects.equals(firstString, secondString)) {
            result = 0;
        } else if (firstString == null) {
            result = 1;
        } else if (secondString == null) {
            result = -1;
        } else if (firstString.equals(NULL_NAME)) {
            result = 1;
        } else if (secondString.equals(NULL_NAME)) {
            result = -1;
        } else if (firstString.equals(DEFAULT_EVENT_NAME)) {
            result = 1;
        } else if (secondString.equals(DEFAULT_EVENT_NAME)) {
            result = -1;
        } else {
            result = firstString.compareToIgnoreCase(secondString);
        }
        return result;
    }

    //    @Nullable
//    private static String getManagedEventNameFromMarketCatalogue(final MarketCatalogue marketCatalogue, @NotNull final ManagedEvent managedEvent) {
//        @Nullable final String eventId = managedEvent.getId(), result;
//        if (eventId == null) {
//            logger.error("null eventId in getManagedEventNameFromMarketCatalogue for: {}", Generic.objectToString(managedEvent));
//            result = null;
//        } else {
//            result = getManagedEventNameFromMarketCatalogue(marketCatalogue, eventId);
//        }
//
//        return result;
//    }
    private static void refreshDisplayedManagedObject() {
        if (currentlyShownManagedObject == null) { // no event displayed, nothing to be done
        } else {
            final String marketId = managedMarketsTreeItemMap.getKey(currentlyShownManagedObject);
            if (marketId == null) {
                final String eventId = managedEventsTreeItemMap.getKey(currentlyShownManagedObject);
                showManagedEvent(eventId, currentlyShownManagedObject);
            } else {
                showManagedMarket(marketId, currentlyShownManagedObject);
            }
        }
    }

    private static void clearMainGridPaneIfDisplaysTreeItem(final TreeItem<String> treeItem) {
        if (currentlyShownManagedObject != null && Objects.equals(treeItem, currentlyShownManagedObject)) {
            clearMainGridPane();
        } else { // not the treeItem I'm looking for, nothing to be done
        }
    }

    private static void clearMainGridPane() {
        mainGridPane.getChildren().clear();
        currentlyShownManagedObject = null;
    }

    private static void updateCurrentManagedEvent(final String eventId) {
        final TreeItem<String> updatedEvent = managedEventsTreeItemMap.get(eventId);
        if (currentlyShownManagedObject != null && Objects.equals(updatedEvent, currentlyShownManagedObject)) {
            showManagedEvent(eventId, currentlyShownManagedObject);
        } else { // the updated event is not the same with the one currently displayed, or nothing is being displayed, nothing to be done
        }
    }

//    private static void showManagedEvent(final TreeItem<String> currentEvent) {
//        final String eventId = managedEventsTreeItemMap.getKey(currentEvent);
//        showManagedEvent(eventId, currentEvent);
//    }

    private static void showManagedEvent(final String eventId, final TreeItem<String> currentEvent) {
        currentlyShownManagedObject = currentEvent;
        mainGridPane.getChildren().clear();
        // eventName(ManagedEvent or newValue), amountLimit/simpleAmountLimit(ManagedEvent), nManagedMarkets(ManagedEvent or nChildren of TreeItem), nTotalMarkets(Event), "open for" timer, starting at "open date"(Event)
        final ManagedEvent managedEvent = Statics.rulesManager.events.get(eventId);
        if (managedEvent == null) {
            logger.error("null managedEvent in left listener for: {} {}", currentEvent, eventId);
        } else {
            final Event event = Statics.eventsMap.get(eventId);
            final String eventName = currentEvent.getValue();
            final double simpleAmountLimit = managedEvent.getSimpleAmountLimit();
            final double calculatedAmountLimit = managedEvent.getAmountLimit(Statics.existingFunds);
            final int nManagedMarkets = currentEvent.getChildren().size();
            final int nTotalMarkets = event == null ? -1 : event.getMarketCount();
            final Date eventOpenTime = event == null ? null : event.getOpenDate();

            final Label eventNameLabel = new Label(eventName);
            final TextField simpleAmountLimitNode = new TextField(String.valueOf(simpleAmountLimit));
            final Label calculatedAmountLimitLabel = new Label(String.valueOf(calculatedAmountLimit));
            final Label nManagedMarketsLabel = new Label(String.valueOf(nManagedMarkets));
            final Label nTotalMarketsLabel = new Label(String.valueOf(nTotalMarkets));
            final Label eventOpenTimeLabel = new Label(eventOpenTime == null ? null : eventOpenTime.toString());

            mainGridPane.getChildren().addAll(eventNameLabel, simpleAmountLimitNode, calculatedAmountLimitLabel, nManagedMarketsLabel, nTotalMarketsLabel, eventOpenTimeLabel);
        }
    }

    private static void updateCurrentManagedMarket(final String marketId) {
        final TreeItem<String> updatedMarket = managedMarketsTreeItemMap.get(marketId);
        if (currentlyShownManagedObject != null && Objects.equals(updatedMarket, currentlyShownManagedObject)) {
            showManagedMarket(marketId, currentlyShownManagedObject);
        } else { // the updated market is not the same with the one currently displayed, or nothing is being displayed, nothing to be done
        }
    }

//    private static void showManagedMarket(final TreeItem<String> currentMarket) {
//        final String marketId = managedMarketsTreeItemMap.getKey(currentMarket);
//        showManagedMarket(marketId, currentMarket);
//    }

    @SuppressWarnings("OverlyComplexMethod")
    private static void showManagedMarket(final String marketId, final TreeItem<String> currentMarket) {
        currentlyShownManagedObject = currentMarket;
        // marketName(ManagedMarket or newValue), eventName(parentEvent.value), getSimpleAmountLimit/getMaxMarketLimit(ManagedMarket), total value traded(Market), countDown until marketTime(MarketDefinition)
        // "open for" timer, starting at "open date"(MarketDefinition)
        // nTotalRunners(Market), nTotalActiveRunners(Market/MarketRunner/RunnerDefinition/RunnerStatus), nManagedRunners(ManagedMarket)
        // runners(ManagedMarket+Market for non managed): runnerName(MarketCatalogue/RunnerCatalogue), BackAmountLimit(ManagedRunner), LayAmountLimit(ManagedRunner), MinBackOdds(ManagedRunner), MaxLayOdds(ManagedRunner)
        // calculated from OrderMarketRunner for all runners, but should be 0 for non managed ones: matchedBackExposure, matchedLayExposure, totalBackExposure, totalLayExposure
        // total value traded(MarketRunner), last trade price(MarketRunner)
        // for all runners, taken from MarketRunner for general unmatched amounts and from OrderMarketRunner/Order for my unmatched amounts: list of 10 values on each side with price/size
        // (MarketRunner): list of 10 values on each side with price/size for traded amounts --> maybe not, occupies too much space and is not a priority, can be added later
        final ManagedMarket managedMarket = Statics.rulesManager.markets.get(marketId);
        if (managedMarket == null) {
            logger.error("null managedMarket in left listener for: {} {}", currentMarket, marketId);
        } else {
            final MarketCatalogue marketCatalogue = Statics.marketCataloguesMap.get(marketId);
            final Market cachedMarket = Statics.marketCache.getMarket(marketId);
            final MarketDefinition marketDefinition = cachedMarket == null ? null : cachedMarket.getMarketDefinition();
            final String marketName = currentMarket.getValue();
            final TreeItem<String> parentItem = currentMarket.getParent();
            final String eventName = parentItem == null ? null : parentItem.getValue();
            final double simpleAmountLimit = managedMarket.getSimpleAmountLimit();
            final double calculatedAmountLimit = managedMarket.getMaxMarketLimit(Statics.existingFunds, Statics.marketCataloguesMap);
            final double marketTotalValueTraded = cachedMarket == null ? -1d : cachedMarket.getTvEUR(Statics.existingFunds.currencyRate);
            final Date marketLiveTime = marketDefinition == null ? null : marketDefinition.getMarketTime();
            final Date marketOpenTime = marketDefinition == null ? null : marketDefinition.getOpenDate();
            final int nTotalRunners = cachedMarket == null ? -1 : cachedMarket.getNRunners();
            final int nActiveRunners = cachedMarket == null ? -1 : cachedMarket.getNActiveRunners();
            final int nManagedRunners = managedMarket.getNRunners();
            @NotNull final HashMap<RunnerId, ManagedRunner> managedRunners = managedMarket.getRunners();
            final HashMap<RunnerId, MarketRunner> allRunners = cachedMarket == null ? null : cachedMarket.getMarketRunners();
            for (@NotNull final Map.Entry<RunnerId, ManagedRunner> entry : managedRunners.entrySet()) {
                final RunnerId runnerId = entry.getKey();
                final ManagedRunner managedRunner = entry.getValue();
                final MarketRunner marketRunner = allRunners == null ? null : allRunners.get(runnerId);
                final String runnerName = marketCatalogue == null ? null : marketCatalogue.getRunnerName(runnerId);
                final double backAmountLimit = managedRunner == null ? -1d : managedRunner.getBackAmountLimit();
                final double layAmountLimit = managedRunner == null ? -1d : managedRunner.getLayAmountLimit();
                final double minBackOdds = managedRunner == null ? -1d : managedRunner.getMinBackOdds();
                final double maxLayOdds = managedRunner == null ? -1d : managedRunner.getMaxLayOdds();
                final OrderMarketRunner orderMarketRunner = Statics.orderCache.getOrderMarketRunner(marketId, runnerId);
                if (orderMarketRunner == null) { // no orderMarketRunner present, which means no orders placed on this runner, normal behavior
                } else {
                    orderMarketRunner.getExposure(managedRunner, Statics.pendingOrdersThread);
                }
                final double matchedBackExposure = orderMarketRunner == null ? 0d : orderMarketRunner.getMatchedBackExposure();
                final double matchedLayExposure = orderMarketRunner == null ? 0d : orderMarketRunner.getMatchedLayExposure();
                final double totalBackExposure = orderMarketRunner == null ? 0d : orderMarketRunner.getTotalBackExposure();
                final double totalLayExposure = orderMarketRunner == null ? 0d : orderMarketRunner.getTotalLayExposure();
                final double runnerTotalValueTraded = marketRunner == null ? 0d : marketRunner.getTvEUR(Statics.existingFunds.currencyRate);
                final double lastTradedPrice = marketRunner == null ? 0d : marketRunner.getLtp();
                final TreeMap<Double, Double> myUnmatchedLay = orderMarketRunner == null ? null : orderMarketRunner.getUnmatchedLayAmounts();
                final TreeMap<Double, Double> myUnmatchedBack = orderMarketRunner == null ? null : orderMarketRunner.getUnmatchedBackAmounts();

            }
            if (allRunners == null) { // no marketRunners list present, nothing to be done
            } else {
                for (@NotNull final Map.Entry<RunnerId, MarketRunner> entry : allRunners.entrySet()) {
                    final RunnerId runnerId = entry.getKey();
                    if (managedRunners.containsKey(runnerId)) { // I already parsed this in the previous for loop, nothing to be done here
                    } else { // non managed runner
                        final MarketRunner marketRunner = entry.getValue();
                        final String runnerName = marketCatalogue == null ? null : marketCatalogue.getRunnerName(runnerId);
                        final double backAmountLimit = -1d;
                        final double layAmountLimit = -1d;
                        final double minBackOdds = -1d;
                        final double maxLayOdds = -1d;
                        final OrderMarketRunner orderMarketRunner = Statics.orderCache.getOrderMarketRunner(marketId, runnerId);
                        if (orderMarketRunner == null) { // no orderMarketRunner present, which means no orders placed on this runner, normal behavior
                        } else {
                            orderMarketRunner.getExposure(null, Statics.pendingOrdersThread); // calculate the exposure
                        }
                        final double matchedBackExposure = orderMarketRunner == null ? 0d : orderMarketRunner.getMatchedBackExposure();
                        final double matchedLayExposure = orderMarketRunner == null ? 0d : orderMarketRunner.getMatchedLayExposure();
                        final double totalBackExposure = orderMarketRunner == null ? 0d : orderMarketRunner.getTotalBackExposure();
                        final double totalLayExposure = orderMarketRunner == null ? 0d : orderMarketRunner.getTotalLayExposure();
                        final double runnerTotalValueTraded = marketRunner == null ? 0d : marketRunner.getTvEUR(Statics.existingFunds.currencyRate);
                        final double lastTradedPrice = marketRunner == null ? 0d : marketRunner.getLtp();
                        final TreeMap<Double, Double> availableToLay = marketRunner == null ? null : marketRunner.getAvailableToLay(Statics.existingFunds.currencyRate);
                        final TreeMap<Double, Double> availableToBack = marketRunner == null ? null : marketRunner.getAvailableToBack(Statics.existingFunds.currencyRate);
                        final TreeMap<Double, Double> myUnmatchedLay = orderMarketRunner == null ? null : orderMarketRunner.getUnmatchedLayAmounts();
                        final TreeMap<Double, Double> myUnmatchedBack = orderMarketRunner == null ? null : orderMarketRunner.getUnmatchedBackAmounts();

                    }
                } // end for
            }
        }
    }

    @SuppressWarnings("OverlyLongMethod")
    @Override
    public void start(@NotNull final Stage primaryStage) {
        primaryStage.setTitle("Betty Interface");
        primaryStage.setHeight(1080);
        primaryStage.setWidth(1920);

        @NotNull final ObservableList<Screen> screens = Screen.getScreens();
        final int nScreens = screens.size();
        @NotNull final Screen chosenScreen;
        if (nScreens == 2) {
            chosenScreen = screens.get(1);
        } else {
            logger.error("wrong nScreens in GUI initial window creation: {}", nScreens);
            chosenScreen = Screen.getPrimary();
        }
        @NotNull final Rectangle2D screenBounds = chosenScreen.getBounds();
        primaryStage.setX(screenBounds.getMinX() + 500);
        primaryStage.setY(screenBounds.getMinY() + 1000);

        final VBox rootVBox = new VBox();
        final Scene mainScene = new Scene(rootVBox, Color.BLACK);
        mainScene.setFill(Color.BLACK);
        mainScene.setCursor(Cursor.CROSSHAIR);
        mainScene.getStylesheets().add("GUI.css");

        primaryStage.setScene(mainScene);

        final HBox topBar = new HBox();
        @NotNull final ObservableList<Node> rootNodesList = rootVBox.getChildren(), topBarNodesList = topBar.getChildren();
        rootNodesList.addAll(topBar, mainSplitPane);
        @NotNull final TreeView<String> leftTreeView = createTreeView(leftEventTreeRoot);
        final VBox leftVBox = new VBox(leftTreeView);
        mainSplitPaneNodesList.addAll(leftVBox, mainGridPane);

        // totalFunds € {} available € {} reserve € {} exposure € {}
        final Label fixedTotalFundsLabel = new Label("Total Funds "), fixedAvailableLabel = new Label(" Available "), fixedReserveLabel = new Label(" out of which safety reserve "), fixedExposureLabel = new Label(" Exposure ");

        final Pane hugeSpacer = new Pane();
        HBox.setHgrow(hugeSpacer, Priority.ALWAYS);

        final Button refreshEventsButton = new Button("_Refresh events");
        refreshEventsButton.setMnemonicParsing(true);
        refreshEventsButton.setOnAction(event -> {
            refreshEventsButton.setDisable(true);
            Statics.sslClientThread.sendQueue.add(new SerializableObjectModification<>(SynchronizedMapModificationCommand.refresh, Event.class)); // send command to refresh event list

            @SuppressWarnings("AnonymousInnerClassMayBeStatic") final TimerTask task = new TimerTask() {
                public void run() {
                    Platform.runLater(() -> refreshEventsButton.setDisable(false));
                }
            };
            Statics.timer.schedule(task, 5_000L);
        });

        filterTextField.setPromptText("Enter filter text ...");
        rightVBox.getChildren().add(filterTextField);

        rightEventTreeRoot.predicateProperty().bind(Bindings.createObjectBinding(() -> {
            if (filterTextField.getText() == null || filterTextField.getText().isEmpty()) {
                //noinspection ConstantConditions
                return null;
            }
            return TreeItemPredicate.create(actor -> StringUtils.containsIgnoreCase(actor.toString(), filterTextField.getText()));
        }, filterTextField.textProperty()));

        final Button rightPaneButton = new Button("Show _events");
        rightPaneButton.setMnemonicParsing(true);
        rightPaneButton.setOnAction(event -> {
            if (rightPanelVisible) {
                //noinspection AssignmentToStaticFieldFromInstanceMethod
                GUI.rightPanelVisible = false;

                mainSplitPaneNodesList.remove(rightVBox);
                topBarNodesList.remove(refreshEventsButton);
                rightPaneButton.setText("Show _events");
            } else {
                //noinspection AssignmentToStaticFieldFromInstanceMethod
                GUI.rightPanelVisible = true; // needs to be in the beginning, else the following methods that rely on condition "rightPanelVisible is true" will not work

                refreshEventsButton.fire();

                mainSplitPaneNodesList.add(rightVBox);
                topBarNodesList.add(topBarNodesList.size() - 1, refreshEventsButton);
                rightPaneButton.setText("Hide _events");
                filterTextField.requestFocus();
                filterTextField.end();

                initializeRightTreeView();
            }
        });

        topBarNodesList.addAll(fixedTotalFundsLabel, totalFundsLabel, fixedAvailableLabel, availableLabel, fixedReserveLabel, reserveLabel, fixedExposureLabel, exposureLabel, hugeSpacer, rightPaneButton);

        rightTreeView.setOnMouseClicked(ae -> {
            @NotNull final TreeItem<String> selectedItem = rightTreeView.getSelectionModel().getSelectedItem();
            if (selectedItem == null) {
                logger.info("likely not clicked on a tree member, null selectedItem in rightTreeView setOnMouseClicked");
            } else {
                selectedItem.setExpanded(true);
                final String eventId = eventsTreeItemMap.getKey(selectedItem);
                if (eventId == null) {
                    final String marketId = marketsTreeItemMap.getKey(selectedItem);
                    if (marketId == null) {
                        if (Objects.equals(selectedItem.getValue(), DEFAULT_EVENT_NAME)) { // I have clicked the default event, normal behavior, nothing to be done
                        } else {
                            logger.error("null eventId and marketId in selected right treeItem: {}", selectedItem);
                        }
                    } else {
                        Statics.sslClientThread.sendQueue.add(new SerializableObjectModification<>(SynchronizedMapModificationCommand.getMarkets, MarketCatalogue.class, marketId)); // send command to refresh market
                    }
                } else {
                    final Event event = Statics.eventsMap.get(eventId);
                    if (event == null) {
                        logger.error("null event for eventId {} in selected right treeItem: {}", eventId, selectedItem);
                    } else {
                        Statics.sslClientThread.sendQueue.add(new SerializableObjectModification<>(SynchronizedMapModificationCommand.getMarkets, Event.class, event)); // send command to get markets for the selected event
                    }
                }
            }


            ae.consume();
        });


//        rightTreeView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
//            if (newValue == null) {
//                logger.info("likely not clicked on a tree member, null newValue in rightTreeView addListener, oldValue: {}", oldValue);
//            } else {
//                newValue.setExpanded(true);
//                final String eventId = eventsTreeItemMap.getKey(newValue);
//                if (eventId == null) {
//                    final String marketId = marketsTreeItemMap.getKey(newValue);
//                    if (marketId == null) {
//                        if (Objects.equals(newValue.getValue(), DEFAULT_EVENT_NAME)) { // I have clicked the default event, normal behavior, nothing to be done
//                        } else {
//                            logger.error("null eventId and marketId in selected right treeItem: {}", newValue);
//                        }
//                    } else {
//                        Statics.sslClientThread.sendQueue.add(new SerializableObjectModification<>(SynchronizedMapModificationCommand.getMarkets, MarketCatalogue.class, marketId)); // send command to refresh market
//                    }
//                } else {
//                    final Event event = Statics.eventsMap.get(eventId);
//                    if (event == null) {
//                        logger.error("null event for eventId {} in selected right treeItem: {}", eventId, newValue);
//                    } else {
//                        Statics.sslClientThread.sendQueue.add(new SerializableObjectModification<>(SynchronizedMapModificationCommand.getMarkets, Event.class, event)); // send command to get markets for the selected event
//                    }
//                }
//            }
//        });

        final MenuItem entryRight1 = new MenuItem("Add Managed Object");
        entryRight1.setOnAction(ae -> {
            @NotNull final TreeItem<String> treeItem = rightTreeView.getSelectionModel().getSelectedItem();
            final String marketId = marketsTreeItemMap.getKey(treeItem);
            final TreeItem<String> parentNode = treeItem.getParent();
            if (marketId == null) {
                final String eventId = eventsTreeItemMap.getKey(treeItem);
                if (eventId == null) {
                    if (Objects.equals(treeItem.getValue(), DEFAULT_EVENT_NAME)) { // I have clicked the default event, normal behavior, nothing to be done
                    } else {
                        logger.error("null marketId and eventId in entryRight for: {} {} {}", Objects.equals(parentNode, rightEventTreeRoot), treeItem, parentNode);
                    }
                } else {
                    Statics.threadPoolExecutor.submit(() -> Utils.createManagedEvent(eventId));
                }
            } else {
                @Nullable final String eventId;
                if (parentNode == null) {
                    logger.error("null parentNode in entryRight for marketId {} treeItem: {}", marketId, treeItem);
                    eventId = null;
                } else {
                    eventId = eventsTreeItemMap.getKey(parentNode);
                }
                Statics.threadPoolExecutor.submit(() -> Utils.createManagedMarket(marketId, eventId));
            }
            ae.consume();
        });
        final ContextMenu rightContextMenu = new ContextMenu(entryRight1);
        rightContextMenu.setOnShowing(ae -> {
            @NotNull final TreeItem<String> treeItem = rightTreeView.getSelectionModel().getSelectedItem();
            if (marketsTreeItemMap.containsValue(treeItem)) {
                entryRight1.setText("Add Managed Market");
            } else if (eventsTreeItemMap.containsValue(treeItem)) {
                entryRight1.setText("Add Managed Event");
            } else {
                logger.error("treeItem not contained in markets nor events maps in rightContextMenu: {} {}", treeItem, treeItem == null ? null : treeItem.getParent());
                entryRight1.setText("Add Managed Object");
            }
            ae.consume();
        });
        rightTreeView.setContextMenu(rightContextMenu);

        final MenuItem entryLeft1 = new MenuItem("Remove Managed Object");
        entryLeft1.setOnAction(ae -> {
            @NotNull final TreeItem<String> treeItem = leftTreeView.getSelectionModel().getSelectedItem();
            final String marketId = managedMarketsTreeItemMap.getKey(treeItem);
            final TreeItem<String> parentNode = treeItem.getParent();
            if (marketId == null) {
                final String eventId = managedEventsTreeItemMap.getKey(treeItem);
                if (eventId == null) {
                    if (Objects.equals(treeItem.getValue(), DEFAULT_EVENT_NAME)) { // I have clicked the default event, normal behavior, nothing to be done
                    } else {
                        logger.error("null marketId and eventId in entryLeft for: {} {} {}", Objects.equals(parentNode, leftEventTreeRoot), treeItem, parentNode);
                    }
                } else {
                    Statics.threadPoolExecutor.submit(() -> Utils.removeManagedEvent(eventId));
                }
            } else {
                Statics.threadPoolExecutor.submit(() -> Utils.removeManagedMarket(marketId));
            }
            ae.consume();
        });
        final ContextMenu leftContextMenu = new ContextMenu(entryLeft1);
        leftContextMenu.setOnShowing(ae -> {
            @NotNull final TreeItem<String> treeItem = leftTreeView.getSelectionModel().getSelectedItem();
            if (managedMarketsTreeItemMap.containsValue(treeItem)) {
                entryLeft1.setText("Remove Managed Market");
            } else if (managedEventsTreeItemMap.containsValue(treeItem)) {
                entryLeft1.setText("Remove Managed Event");
            } else {
                logger.error("treeItem not contained in markets nor events maps in leftContextMenu: {} {}", treeItem, treeItem == null ? null : treeItem.getParent());
                entryLeft1.setText("Remove Managed Object");
            }
            ae.consume();
        });
        leftTreeView.setContextMenu(leftContextMenu);

        leftTreeView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null) {
                logger.info("likely not clicked on a tree member, null newValue in leftTreeView addListener, oldValue: {}", oldValue);
            } else {
                newValue.setExpanded(true);
                final String eventId = managedEventsTreeItemMap.getKey(newValue);
                if (eventId == null) {
                    final String marketId = managedMarketsTreeItemMap.getKey(newValue);
                    if (marketId == null) {
                        if (Objects.equals(newValue.getValue(), DEFAULT_EVENT_NAME)) { // I have clicked the default event, normal behavior, nothing to be done
                        } else {
                            logger.error("null eventId and marketId in selected left treeItem: {}", newValue);
                        }
                    } else { // managedMarket
                        showManagedMarket(marketId, newValue);
                    }
                } else { // managedEvent
                    showManagedEvent(eventId, newValue);
                }
            }
        });

        primaryStage.show();

        Statics.scheduledThreadPoolExecutor.scheduleAtFixedRate(GUI::checkTreeItemsWithNullName, 1L, 1L, TimeUnit.MINUTES);

        logger.info("GUI has started");
//        hasStarted.countDown();
    }
}
