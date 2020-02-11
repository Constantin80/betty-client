package info.fmro.client.main;

import info.fmro.client.objects.Statics;
import info.fmro.shared.entities.MarketDescription;
import info.fmro.shared.enums.SynchronizedMapModificationCommand;
import info.fmro.shared.javafx.FilterableTreeItem;
import info.fmro.shared.logic.ManagedEvent;
import info.fmro.shared.logic.ManagedMarket;
import info.fmro.shared.stream.objects.EventInterface;
import info.fmro.shared.stream.objects.MarketCatalogueInterface;
import info.fmro.shared.stream.objects.SerializableObjectModification;
import info.fmro.shared.utility.Formulas;
import info.fmro.shared.utility.Generic;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

@SuppressWarnings({"ClassWithTooManyMethods", "OverlyComplexClass"})
public class GUI
        extends Application {
    private static final Logger logger = LoggerFactory.getLogger(GUI.class);
    private static final String DEFAULT_EVENT_NAME = "no event attached";
    private static final Label totalFundsLabel = new Label("€ " + Generic.addCommas(Statics.existingFunds.getTotalFunds(), 2));
    private static final Label availableLabel = new Label("€ " + Generic.addCommas(Statics.existingFunds.getAvailableFunds(), 2));
    private static final Label reserveLabel = new Label("€ " + Generic.addCommas(Statics.existingFunds.getReserve(), 2));
    private static final Label exposureLabel = new Label("€ " + Generic.addCommas(Statics.existingFunds.getExposure(), 2));
    private static final SplitPane mainSplitPane = new SplitPane();
    private static final ObservableList<Node> mainSplitPaneNodesList = mainSplitPane.getItems();
    private static final DualHashBidiMap<String, TreeItem<String>> managedEventsTreeItemMap = new DualHashBidiMap<>(), managedMarketsTreeItemMap = new DualHashBidiMap<>();
    private static final DualHashBidiMap<String, TreeItem<String>> eventsTreeItemMap = new DualHashBidiMap<>(), marketsTreeItemMap = new DualHashBidiMap<>();
    private static final TreeItem<String> leftEventTreeRoot = new TreeItem<>();
    private static final FilterableTreeItem<String> rightEventTreeRoot = new FilterableTreeItem<>(null);
    private static final ObservableList<TreeItem<String>> leftEventRootChildrenList = leftEventTreeRoot.getChildren(), rightEventRootChildrenList = rightEventTreeRoot.getChildren();
    private static final TreeView<String> rightTreeView = createTreeView(rightEventTreeRoot);
    private static final VBox rightVBox = new VBox(rightTreeView);
    private static boolean rightPanelVisible;

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

    public static void publicRemoveManagedMarket(final String marketId, final String parentId) {
        Platform.runLater(() -> removeManagedMarket(marketId, parentId));
    }

    public static void initializeRulesManagerTreeView() {
        Platform.runLater(GUI::initializeLeftTreeView);
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

    public static void publicPutAllEvent(final Map<String, EventInterface> m) {
        if (rightPanelVisible) {
            Platform.runLater(() -> putAllEvent(m));
        } else { // I will only update the right panel if it is visible
        }
    }

    public static void publicPutAllMarket(final Map<String, MarketCatalogueInterface> m) {
        if (rightPanelVisible) {
            Platform.runLater(() -> putAllMarket(m));
        } else { // I will only update the right panel if it is visible
        }
    }

    public static void publicRemoveEntriesEvent(final Iterable<? extends Map.Entry<String, EventInterface>> c) {
        if (rightPanelVisible) {
            Platform.runLater(() -> removeEntriesEvent(c));
        } else { // I will only update the right panel if it is visible
        }
    }

    public static void publicRemoveEntriesMarket(final Iterable<? extends Map.Entry<String, MarketCatalogueInterface>> c) {
        if (rightPanelVisible) {
            Platform.runLater(() -> removeEntriesMarket(c));
        } else { // I will only update the right panel if it is visible
        }
    }

    public static void publicRetainEntriesEvent(final Collection<? extends Map.Entry<String, EventInterface>> c) {
        if (rightPanelVisible) {
            Platform.runLater(() -> retainEntriesEvent(c));
        } else { // I will only update the right panel if it is visible
        }
    }

    public static void publicRetainEntriesMarket(final Collection<? extends Map.Entry<String, MarketCatalogueInterface>> c) {
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

    public static void publicRemoveValueAllEvent(final EventInterface value) {
        if (rightPanelVisible) {
            Platform.runLater(() -> removeEvent(value));
        } else { // I will only update the right panel if it is visible
        }
    }

    public static void publicRemoveValueAllMarket(final MarketCatalogueInterface value) {
        if (rightPanelVisible) {
            Platform.runLater(() -> removeMarket(value));
        } else { // I will only update the right panel if it is visible
        }
    }

    public static void publicRemoveValuesEvent(final Iterable<? extends EventInterface> c) {
        if (rightPanelVisible) {
            Platform.runLater(() -> removeValuesEvent(c));
        } else { // I will only update the right panel if it is visible
        }
    }

    public static void publicRemoveValuesMarket(final Iterable<? extends MarketCatalogueInterface> c) {
        if (rightPanelVisible) {
            Platform.runLater(() -> removeValuesMarket(c));
        } else { // I will only update the right panel if it is visible
        }
    }

    public static void publicRetainValuesEvent(final Collection<? extends EventInterface> c) {
        if (rightPanelVisible) {
            Platform.runLater(() -> retainValuesEvent(c));
        } else { // I will only update the right panel if it is visible
        }
    }

    public static void publicRetainValuesMarket(final Collection<? extends MarketCatalogueInterface> c) {
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

    public static void publicRemoveEvent(final EventInterface event) {
        if (rightPanelVisible) {
            Platform.runLater(() -> removeEvent(event));
        } else { // I will only update the right panel if it is visible
        }
    }

    public static void publicAddEvent(final String eventId, final EventInterface event) {
        if (rightPanelVisible) {
            Platform.runLater(() -> addEvent(eventId, event));
        } else { // I will only update the right panel if it is visible
        }
    }

    public static void publicAddMarket(final String marketId, final MarketCatalogueInterface market) {
        if (rightPanelVisible) {
            Platform.runLater(() -> addMarket(marketId, market));
        } else { // I will only update the right panel if it is visible
        }
    }

    public static void publicRemoveMarket(final MarketCatalogueInterface market) {
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

    private static void putAllEvent(final Map<String, EventInterface> m) {
        if (m == null) { // nothing to be done
        } else {
            for (@NotNull final Map.Entry<String, EventInterface> entry : m.entrySet()) {
                addEvent(entry);
            }
        }
    }

    private static void putAllMarket(final Map<String, MarketCatalogueInterface> m) {
        if (m == null) { // nothing to be done
        } else {
            for (@NotNull final Map.Entry<String, MarketCatalogueInterface> entry : m.entrySet()) {
                addMarket(entry);
            }
        }
    }

    private static void removeEntriesEvent(final Iterable<? extends Map.Entry<String, EventInterface>> c) {
        if (c == null) { // nothing to be done
        } else {
            for (final Map.Entry<String, EventInterface> entry : c) {
                if (entry == null) {
                    logger.error("null entry in removeEntriesEvent for: {}", Generic.objectToString(c));
                } else {
                    removeEvent(entry.getKey());
                }
            }
        }
    }

    private static void removeEntriesMarket(final Iterable<? extends Map.Entry<String, MarketCatalogueInterface>> c) {
        if (c == null) { // nothing to be done
        } else {
            for (final Map.Entry<String, MarketCatalogueInterface> entry : c) {
                if (entry == null) {
                    logger.error("null entry in removeEntriesMarket for: {}", Generic.objectToString(c));
                } else {
                    removeMarket(entry.getValue());
                }
            }
        }
    }

    private static void retainEntriesEvent(final Collection<? extends Map.Entry<String, EventInterface>> c) {
        if (c == null) { // nothing to be done
        } else {
            final Collection<String> retainEventIds = new HashSet<>(Generic.getCollectionCapacity(c));
            for (final Map.Entry<String, EventInterface> entry : c) {
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

    private static void retainEntriesMarket(final Collection<? extends Map.Entry<String, MarketCatalogueInterface>> c) {
        if (c == null) { // nothing to be done
        } else {
            final Collection<String> retainMarketIds = new HashSet<>(Generic.getCollectionCapacity(c));
            for (final Map.Entry<String, MarketCatalogueInterface> entry : c) {
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

    private static void removeValuesEvent(final Iterable<? extends EventInterface> c) {
        if (c == null) { // nothing to be done
        } else {
            for (final EventInterface event : c) {
                removeEvent(event);
            }
        }
    }

    private static void removeValuesMarket(final Iterable<? extends MarketCatalogueInterface> c) {
        if (c == null) { // nothing to be done
        } else {
            for (final MarketCatalogueInterface market : c) {
                removeMarket(market);
            }
        }
    }

    private static void retainValuesEvent(final Collection<? extends EventInterface> c) {
        if (c == null) { // nothing to be done
        } else {
            final Collection<String> retainEventIds = new HashSet<>(Generic.getCollectionCapacity(c));
            for (final EventInterface event : c) {
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

    private static void retainValuesMarket(final Collection<? extends MarketCatalogueInterface> c) {
        if (c == null) { // nothing to be done
        } else {
            final Collection<String> retainMarketIds = new HashSet<>(Generic.getCollectionCapacity(c));
            for (final MarketCatalogueInterface market : c) {
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

    private static void removeEvent(final EventInterface event) {
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
        @Nullable final ObservableList<TreeItem<String>> listOfChildren = eventTreeItem != null ? eventTreeItem.getChildren() : null;
        if (marketIds != null) {
            for (final String marketId : marketIds) {
                removeManagedMarket(marketId, listOfChildren);
            }
        } else { // no marketIds to remove
        }
        @Nullable final Set<String> toBeReAddedMarketIds;
        if (listOfChildren != null && !listOfChildren.isEmpty()) { // just in case there are children left, that were not removed
            toBeReAddedMarketIds = new HashSet<>(2);
            for (final TreeItem<String> marketItem : listOfChildren) {
                final String marketId = managedMarketsTreeItemMap.getKey(marketItem);
                managedMarketsTreeItemMap.removeValue(marketItem);
                if (marketId != null) {
                    toBeReAddedMarketIds.add(marketId);
                } else {
                    logger.error("null marketId while building reAdd set for: {} {} {}", eventId, Generic.objectToString(marketIds), Generic.objectToString(marketItem));
                }
            }
            listOfChildren.clear();
        } else { // no markets to remove from children
            toBeReAddedMarketIds = null;
        }
        leftEventRootChildrenList.remove(eventTreeItem);
        managedEventsTreeItemMap.remove(eventId);
        if (toBeReAddedMarketIds != null) {
            logger.warn("{} managedMarkets reAdded after managedEvent removal for: {} {} {}", toBeReAddedMarketIds.size(), eventId, Generic.objectToString(toBeReAddedMarketIds), Generic.objectToString(marketIds));
            for (final String marketId : toBeReAddedMarketIds) {
                addManagedMarket(marketId);
            }
        } else { // this is the normal case, nothing to be reAdded
        }
    }

    @SuppressWarnings("UnusedReturnValue")
    private static boolean removeMarket(final MarketCatalogueInterface market) {
        final String marketId = market == null ? null : market.getMarketId();
        final String eventId = Formulas.getEventIdOfMarketId(marketId, Statics.marketCataloguesMap);
        return removeMarket(marketId, eventId);
    }

    @SuppressWarnings("UnusedReturnValue")
    private static boolean removeMarket(final String marketId) {
        final String eventId = Formulas.getEventIdOfMarketId(marketId, Statics.marketCataloguesMap);
        return removeMarket(marketId, eventId);
    }

    private static boolean removeMarket(final String marketId, final String parentEventId) {
        final TreeItem<String> eventTreeItem = eventsTreeItemMap.get(parentEventId);
        @Nullable final ObservableList<TreeItem<String>> listOfChildren = eventTreeItem != null ? eventTreeItem.getChildren() : null;
        final TreeItem<String> marketTreeItem = marketsTreeItemMap.remove(marketId);
        boolean removed = listOfChildren != null && listOfChildren.remove(marketTreeItem);
        if (removed) { // already removed, nothing to be done
        } else {
            final TreeItem<String> defaultEvent = eventsTreeItemMap.get(null);
            if (defaultEvent != null) {
                @NotNull final ObservableList<TreeItem<String>> defaultChildren = defaultEvent.getChildren();
                removed = defaultChildren.remove(marketTreeItem);
            } else {
                removed = false;
            }
        }

        return removed;
    }

    @SuppressWarnings("UnusedReturnValue")
    private static boolean removeManagedMarket(final String marketId, final String parentEventId) {
        final TreeItem<String> eventTreeItem = managedEventsTreeItemMap.get(parentEventId);
        @Nullable final ObservableList<TreeItem<String>> listOfChildren = eventTreeItem != null ? eventTreeItem.getChildren() : null;
        return removeManagedMarket(marketId, listOfChildren);
    }

    private static boolean removeManagedMarket(final String marketId, final Collection<TreeItem<String>> listOfChildren) {
        final TreeItem<String> marketTreeItem = managedMarketsTreeItemMap.remove(marketId);
        boolean removed = listOfChildren != null && listOfChildren.remove(marketTreeItem);
        if (removed) { // already removed, nothing to be done
        } else {
            final TreeItem<String> defaultEvent = managedEventsTreeItemMap.get(null);
            if (defaultEvent != null) {
                @NotNull final ObservableList<TreeItem<String>> defaultChildren = defaultEvent.getChildren();
                removed = defaultChildren.remove(marketTreeItem);
            } else {
                removed = false;
            }
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

    private static void privateCheckTreeItemsWithNullName() { // only checks the managed items
        for (@NotNull final TreeItem<String> eventItem : managedEventsTreeItemMap.values()) {
            final String treeName = eventItem.getValue();
            if (treeName == null) {
                final String eventId = managedEventsTreeItemMap.getKey(eventItem);
                if (eventId != null) {
                    @NotNull final ManagedEvent managedEvent = Statics.rulesManager.events.get(eventId, Statics.rulesManager.rulesHaveChanged);
                    final String eventName = GUIUtils.getManagedEventName(managedEvent);
                    if (eventName != null) {
                        eventItem.setValue(eventName);
                    } else { // still null, nothing to be done
                    }
                } else {
                    logger.error("null both eventId and treeName for: {} {}", Generic.objectToString(eventItem), Generic.objectToString(managedEventsTreeItemMap));
                }
            } else { // name not null, nothing to be done
            }
        }
        for (@NotNull final TreeItem<String> marketItem : managedMarketsTreeItemMap.values()) {
            final String treeName = marketItem.getValue();
            if (treeName == null) {
                final String marketId = managedMarketsTreeItemMap.getKey(marketItem);
                if (marketId != null) {
                    final ManagedMarket managedMarket = Statics.rulesManager.markets.get(marketId);
                    if (managedMarket != null) {
                        final String marketName = GUIUtils.getManagedMarketName(marketId, managedMarket);
                        if (marketName != null) {
                            marketItem.setValue(marketName);
                        } else { // still null, nothing to be done
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
        @NotNull final HashMap<String, MarketCatalogueInterface> mapCopy = Statics.marketCataloguesMap.copy();
        for (@NotNull final Map.Entry<String, MarketCatalogueInterface> entry : mapCopy.entrySet()) {
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
        final MarketCatalogueInterface market = Statics.marketCataloguesMap.get(marketId);
        return addMarket(marketId, market);
    }

    private static int addMarket(@NotNull final Map.Entry<String, ? extends MarketCatalogueInterface> entry) {
        final String marketId = entry.getKey();
        final MarketCatalogueInterface market = entry.getValue();
        return addMarket(marketId, market);
    }

    private static int addMarket(final String marketId, final MarketCatalogueInterface market) {
        int modified = 0;
        if (marketsTreeItemMap.containsKey(marketId)) { // already contained, nothing to be done, won't update the object here
        } else {
            if (market == null) {
                logger.error("null MarketCatalogue in addMarkets for: {}", marketId);
                Statics.marketCataloguesMap.removeValueAll(null);
            } else {
                final String eventId = Formulas.getEventIdOfMarketCatalogue(market);

                @Nullable TreeItem<String> parentItem = eventsTreeItemMap.get(eventId);
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
                marketTreeItem.setExpanded(true);
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
    private static TreeItem<String> getDefaultEventAndCreateIfNotExist() {
        TreeItem<String> defaultEvent = eventsTreeItemMap.get(null);
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
        @NotNull final HashMap<String, EventInterface> mapCopy = Statics.eventsMap.copy();
        for (@NotNull final Map.Entry<String, EventInterface> entry : mapCopy.entrySet()) {
            modified += addEvent(entry);
        }
        return modified;
    }

    private static int addEvent(@NotNull final Map.Entry<String, ? extends EventInterface> entry) {
        final String eventId = entry.getKey();
        final EventInterface event = entry.getValue();
        return addEvent(eventId, event);
    }

    private static int addEvent(final String eventId, final EventInterface event) {
        int modified = 0;
        if (eventsTreeItemMap.containsKey(eventId)) { // already contained, nothing to be done, won't update the object here
        } else {
            if (event == null) {
                logger.error("null Event in addEvent for: {}", eventId);
                Statics.eventsMap.removeValueAll(null);
            } else {
                final String eventName = event.getName();
                final FilterableTreeItem<String> eventTreeItem = new FilterableTreeItem<>(eventName);
                eventTreeItem.setExpanded(true);
                eventsTreeItemMap.put(eventId, eventTreeItem);
                addTreeItem(eventTreeItem, rightEventRootChildrenList);
                modified += checkMarketsOnDefaultNode(event, eventTreeItem);

                modified++;
            }
        }
        return modified;
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
                final String eventName = GUIUtils.getManagedEventName(managedEvent);
                final TreeItem<String> eventTreeItem = new TreeItem<>(eventName);
                eventTreeItem.setExpanded(true);
                managedEventsTreeItemMap.put(eventId, eventTreeItem);
                addTreeItem(eventTreeItem, leftEventRootChildrenList);
                modified += checkMarketsOnDefaultNode(managedEvent, eventTreeItem);

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
// todo bind to text search

    private static int checkMarketsOnDefaultNode(@NotNull final EventInterface event, @NotNull final TreeItem<String> eventTreeItem) {
        int modified = 0;
        final TreeItem<String> defaultEvent = eventsTreeItemMap.get(null);
        if (defaultEvent != null) {
            final String eventId = event.getId();
            @NotNull final ObservableList<TreeItem<String>> defaultChildrenList = defaultEvent.getChildren();
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

    private static int checkMarketsOnDefaultNode(@NotNull final ManagedEvent managedEvent, @NotNull final TreeItem<String> eventTreeItem) {
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

    private static void addTreeItem(@NotNull final TreeItem<String> treeItem, @NotNull final TreeItem<String> root) {
        @NotNull final ObservableList<TreeItem<String>> rootChildrenList = root.getChildren();
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
//    private static String getManagedEventNameFromMarketCatalogue(final MarketCatalogueInterface marketCatalogue, @NotNull final ManagedEvent managedEvent) {
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
        final GridPane gridPane = new GridPane();
        mainSplitPaneNodesList.addAll(leftVBox, gridPane);

        // totalFunds € {} available € {} reserve € {} exposure € {}
        final Label fixedTotalFundsLabel = new Label("Total Funds "), fixedAvailableLabel = new Label(" Available "), fixedReserveLabel = new Label(" out of which safety reserve "), fixedExposureLabel = new Label(" Exposure ");

        final Pane hugeSpacer = new Pane();
        HBox.setHgrow(hugeSpacer, Priority.ALWAYS);

        final Button refreshEventsButton = new Button("_Refresh events");
        refreshEventsButton.setMnemonicParsing(true);
        refreshEventsButton.setOnAction(event -> {
            refreshEventsButton.setDisable(true);
            Statics.sslClientThread.sendQueue.add(new SerializableObjectModification<>(SynchronizedMapModificationCommand.refresh, EventInterface.class)); // send command to refresh event list

            @SuppressWarnings("AnonymousInnerClassMayBeStatic") final TimerTask task = new TimerTask() {
                public void run() {
                    Platform.runLater(() -> refreshEventsButton.setDisable(false));
                }
            };
            Statics.timer.schedule(task, 5_000L);
        });

        final Button rightPaneButton = new Button("Show _events");
        rightPaneButton.setMnemonicParsing(true);
        rightPaneButton.setOnAction(event -> {
            if (rightPanelVisible) {
                mainSplitPaneNodesList.remove(rightVBox);
                topBarNodesList.remove(refreshEventsButton);
                rightPaneButton.setText("Show _events");

                //noinspection AssignmentToStaticFieldFromInstanceMethod
                GUI.rightPanelVisible = false;
            } else {
                refreshEventsButton.fire();

                mainSplitPaneNodesList.add(rightVBox);
                topBarNodesList.add(topBarNodesList.size() - 1, refreshEventsButton);
                rightPaneButton.setText("Hide _events");

                //noinspection AssignmentToStaticFieldFromInstanceMethod
                GUI.rightPanelVisible = true;
            }
        });

        topBarNodesList.addAll(fixedTotalFundsLabel, totalFundsLabel, fixedAvailableLabel, availableLabel, fixedReserveLabel, reserveLabel, fixedExposureLabel, exposureLabel, hugeSpacer, rightPaneButton);

        primaryStage.show();

        logger.info("GUI has started");

        Statics.scheduledThreadPoolExecutor.scheduleAtFixedRate(GUI::checkTreeItemsWithNullName, 1L, 1L, TimeUnit.MINUTES);

    }
}
