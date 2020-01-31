package info.fmro.client.main;

import info.fmro.client.objects.Statics;
import info.fmro.shared.logic.ManagedEvent;
import info.fmro.shared.logic.ManagedMarket;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class GUI
        extends Application {
    private static final Logger logger = LoggerFactory.getLogger(GUI.class);
    private static final String DEFAULT_EVENT_NAME = "no event attached";
    private static final Label totalFundsLabel = new Label("€ " + Generic.addCommas(Statics.existingFunds.getTotalFunds(), 2));
    private static final Label availableLabel = new Label("€ " + Generic.addCommas(Statics.existingFunds.getAvailableFunds(), 2));
    private static final Label reserveLabel = new Label("€ " + Generic.addCommas(Statics.existingFunds.getReserve(), 2));
    private static final Label exposureLabel = new Label("€ " + Generic.addCommas(Statics.existingFunds.getExposure(), 2));
    private static final DualHashBidiMap<String, TreeItem<String>> eventsTreeItemMap = new DualHashBidiMap<>(), marketsTreeItemMap = new DualHashBidiMap<>();
    private static final TreeItem<String> eventTreeRoot = new TreeItem<>();
    private static final ObservableList<TreeItem<String>> eventRootChildrenList = eventTreeRoot.getChildren();

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

    public static void checkTreeItemsWithNullName() {
        Platform.runLater(GUI::privateCheckTreeItemsWithNullName);
    }

    public static void publicRemoveManagedEvent(final String eventId, final HashSet<String> marketIds) {
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

    private static void removeManagedEvent(final String eventId, final HashSet<String> marketIds) {
        final TreeItem<String> eventTreeItem = eventsTreeItemMap.get(eventId);
        @Nullable final ObservableList<TreeItem<String>> listOfChildren;
        if (eventTreeItem != null) {
            listOfChildren = eventTreeItem.getChildren();
        } else {
            listOfChildren = null;
        }
        if (marketIds != null) {
            for (final String marketId : marketIds) {
                removeManagedMarket(marketId, listOfChildren);
            }
        } else { // no marketIds to remove
        }
        if (listOfChildren != null) { // just in case there are children left, that were not removed
            for (final TreeItem<String> marketItem : listOfChildren) {
                marketsTreeItemMap.removeValue(marketItem);
            }
            listOfChildren.clear();
        } else { // no markets to remove from children
        }
        eventRootChildrenList.remove(eventTreeItem);
        eventsTreeItemMap.remove(eventId);
    }

    private static boolean removeManagedMarket(final String marketId, final String parentEventId) {
        final TreeItem<String> eventTreeItem = eventsTreeItemMap.get(parentEventId);
        @Nullable final ObservableList<TreeItem<String>> listOfChildren;
        if (eventTreeItem != null) {
            listOfChildren = eventTreeItem.getChildren();
        } else {
            listOfChildren = null;
        }
        return removeManagedMarket(marketId, listOfChildren);
    }

    private static boolean removeManagedMarket(final String marketId, final ObservableList<TreeItem<String>> listOfChildren) {
        boolean removed = false;
        final TreeItem<String> marketTreeItem = marketsTreeItemMap.remove(marketId);
        if (listOfChildren != null) {
            removed = listOfChildren.remove(marketTreeItem);
        } else {
            removed = false;
        }
        if (!removed) {
            final TreeItem<String> defaultEvent = eventsTreeItemMap.get(null);
            if (defaultEvent != null) {
                @NotNull final ObservableList<TreeItem<String>> defaultChildren = defaultEvent.getChildren();
                removed = defaultChildren.remove(marketTreeItem);
            } else {
                removed = false;
            }
        } else { // already removed, nothing to be done
        }

        return removed;
    }

    public static void initializeRulesManagerTreeView() {
        Platform.runLater(GUI::initializeTreeView);
    }

    private static void clearTreeView() {
        for (final TreeItem<String> treeItem : eventRootChildrenList) {
            treeItem.getChildren().clear();
        }
        eventRootChildrenList.clear();

        eventsTreeItemMap.clear();
        marketsTreeItemMap.clear();
    }

    private static int initializeTreeView() {
        int modified = 0;

        clearTreeView();
        modified += addManagedEvents();
        modified += addManagedMarkets();

        return modified;
    }

    @NotNull
    private static TreeView<String> createTreeView() {
        final TreeView<String> treeView = new TreeView<>();
        treeView.setShowRoot(false);
        treeView.setRoot(eventTreeRoot);
        eventTreeRoot.setExpanded(true);

        initializeTreeView();

        return treeView;
    }

    private static void privateCheckTreeItemsWithNullName() {
        for (@NotNull final TreeItem<String> eventItem : eventsTreeItemMap.values()) {
            final String treeName = eventItem.getValue();
            if (treeName == null) {
                final String eventId = eventsTreeItemMap.getKey(eventItem);
                if (eventId != null) {
                    @NotNull final ManagedEvent managedEvent = Statics.rulesManager.events.get(eventId, Statics.rulesManager.rulesHaveChanged);
                    final String eventName = GUIUtils.getManagedEventName(managedEvent);
                    if (eventName != null) {
                        eventItem.setValue(eventName);
                    } else { // still null, nothing to be done
                    }
                } else {
                    logger.error("null both eventId and treeName for: {} {}", Generic.objectToString(eventItem), Generic.objectToString(eventsTreeItemMap));
                }
            } else { // name not null, nothing to be done
            }
        }
        for (@NotNull final TreeItem<String> marketItem : marketsTreeItemMap.values()) {
            final String treeName = marketItem.getValue();
            if (treeName == null) {
                final String marketId = marketsTreeItemMap.getKey(marketItem);
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
                    logger.error("null both marketId and treeName for: {} {}", Generic.objectToString(marketItem), Generic.objectToString(marketsTreeItemMap));
                }
            } else { // name not null, nothing to be done
            }
        }
    }

    private static int addManagedMarkets() {
        int modified = 0;
        @NotNull final HashMap<String, ManagedMarket> mapCopy = Statics.rulesManager.markets.copy();
        for (@NotNull final Map.Entry<String, ManagedMarket> entry : mapCopy.entrySet()) {
            modified += addManagedMarket(entry);
        }

        return modified;
    }

    private static int addManagedMarket(@NotNull final Map.Entry<String, ManagedMarket> entry) {
        final String marketId = entry.getKey();
        final ManagedMarket managedMarket = entry.getValue();
        return addManagedMarket(marketId, managedMarket);
    }

    private static int addManagedMarket(final String marketId, final ManagedMarket managedMarket) {
        int modified = 0;
        if (marketsTreeItemMap.containsKey(marketId)) { // already contained, nothing to be done, won't update the object here
        } else {
            if (managedMarket == null) {
                logger.error("null ManagedMarket in addManagedMarkets for: {}", marketId);
                Statics.rulesManager.markets.removeValueAll(null);
            } else {
                final String eventId = managedMarket.getParentEventId(Statics.marketCataloguesMap, Statics.rulesManager.rulesHaveChanged);

                @Nullable TreeItem<String> parentItem = eventsTreeItemMap.get(eventId);
                if (parentItem == null) {
                    parentItem = getDefaultEventAndCreateIfNotExist();
                } else { // I have the parentItem, nothing more to be done
                }

                final String marketName = GUIUtils.getManagedMarketName(marketId, managedMarket);
                final TreeItem<String> marketTreeItem = new TreeItem<>(marketName);
                marketTreeItem.setExpanded(true);
                marketsTreeItemMap.put(marketId, marketTreeItem);
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
            defaultEvent = new TreeItem<>(DEFAULT_EVENT_NAME);
            defaultEvent.setExpanded(true);
            eventsTreeItemMap.put(null, defaultEvent);
            addTreeItem(defaultEvent, eventRootChildrenList);
        } else { // I have the defaultEvent, nothing more to be done
        }
        return defaultEvent;
    }

    private static int addManagedEvent(@NotNull final Map.Entry<String, ManagedEvent> entry) {
        final String eventId = entry.getKey();
        final ManagedEvent managedEvent = entry.getValue();
        return addManagedEvent(eventId, managedEvent);
    }

    private static int addManagedEvent(final String eventId, final ManagedEvent managedEvent) {
        int modified = 0;
        if (eventsTreeItemMap.containsKey(eventId)) { // already contained, nothing to be done, won't update the object here
        } else {
            if (managedEvent == null) {
                logger.error("null ManagedEvent in updateManagedEvents for: {}", eventId);
                Statics.rulesManager.events.removeValueAll(null);
            } else {
                final String eventName = GUIUtils.getManagedEventName(managedEvent);
                final TreeItem<String> eventTreeItem = new TreeItem<>(eventName);
                eventTreeItem.setExpanded(true);
                eventsTreeItemMap.put(eventId, eventTreeItem);
                addTreeItem(eventTreeItem, eventRootChildrenList);
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

    private static int checkMarketsOnDefaultNode(@NotNull final ManagedEvent managedEvent, @NotNull final TreeItem<String> eventTreeItem) {
        int modified = 0;
        final TreeItem<String> defaultEvent = eventsTreeItemMap.get(null);
        if (defaultEvent != null) {
            @NotNull final HashMap<String, ManagedMarket> marketsMap = managedEvent.marketsMap.copy(Statics.rulesManager);
            final int nMarkets = marketsMap.size();
            if (nMarkets > 0) {
                @NotNull final ObservableList<TreeItem<String>> defaultChildrenList = defaultEvent.getChildren();
                final List<TreeItem<String>> marketsToMove = new ArrayList<>(0);
                for (final TreeItem<String> marketTreeItem : defaultChildrenList) {
                    final String marketId = marketsTreeItemMap.getKey(marketTreeItem);
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
                    eventRootChildrenList.remove(defaultEvent);
                    eventsTreeItemMap.remove(null);
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
        final SplitPane mainSplitPane = new SplitPane();
        @NotNull final ObservableList<Node> rootNodesList = rootVBox.getChildren(), topBarNodesList = topBar.getChildren(), mainSplitPaneNodesList = mainSplitPane.getItems();
        rootNodesList.addAll(topBar, mainSplitPane);
        @NotNull final TreeView<String> treeView = createTreeView();
        final VBox leftVBox = new VBox(treeView);
        mainSplitPaneNodesList.add(leftVBox);

        // totalFunds € {} available € {} reserve € {} exposure € {}
        final Label fixedTotalFundsLabel = new Label("Total Funds "), fixedAvailableLabel = new Label(" Available "), fixedReserveLabel = new Label(" out of which safety reserve "), fixedExposureLabel = new Label(" Exposure ");

        final Pane hugeSpacer = new Pane();
        HBox.setHgrow(hugeSpacer, Priority.ALWAYS);

        final Button rightPaneButton = new Button("Sho_w cached markets");
        rightPaneButton.setMnemonicParsing(true);

        topBarNodesList.addAll(fixedTotalFundsLabel, totalFundsLabel, fixedAvailableLabel, availableLabel, fixedReserveLabel, reserveLabel, fixedExposureLabel, exposureLabel, hugeSpacer, rightPaneButton);

        primaryStage.show();

        logger.info("GUI has started");

        Statics.scheduledThreadPoolExecutor.scheduleAtFixedRate(GUI::checkTreeItemsWithNullName, 1L, 1L, TimeUnit.MINUTES);

//        final ClientRulesManager clientRulesManager = new ClientRulesManager();
//        clientRulesManager.copyFromStream(new RulesManager());

//        TreeItem<String> item1=new TreeItem<>("1"), item2=new TreeItem<>("2"),item3=new TreeItem<>("3");
//eventRootChildrenList.addAll(item1,item2 );
//item1.getChildren().add(item3);
//logger.info(item3.getParent().toString());
//        try {
//            Thread.sleep(1000);
//
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//        item2.getChildren().add(item3);
//        logger.info(item3.getParent().toString());
//        item3.setValue("xxxxx");
    }
}
