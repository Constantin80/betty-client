package info.fmro.client.main;

import info.fmro.client.objects.FocusPosition;
import info.fmro.client.objects.PlatformRunLaterCommand;
import info.fmro.client.objects.PlatformRunLaterEnum;
import info.fmro.client.objects.Statics;
import info.fmro.client.utility.Utils;
import info.fmro.shared.entities.Event;
import info.fmro.shared.entities.MarketCatalogue;
import info.fmro.shared.entities.MarketDescription;
import info.fmro.shared.enums.PrefSide;
import info.fmro.shared.enums.RulesManagerModificationCommand;
import info.fmro.shared.enums.SynchronizedMapModificationCommand;
import info.fmro.shared.enums.TemporaryOrderType;
import info.fmro.shared.javafx.ScrollBarState;
import info.fmro.shared.logic.ManagedEvent;
import info.fmro.shared.logic.ManagedMarket;
import info.fmro.shared.logic.ManagedRunner;
import info.fmro.shared.objects.SharedStatics;
import info.fmro.shared.objects.TemporaryOrder;
import info.fmro.shared.stream.cache.market.Market;
import info.fmro.shared.stream.cache.market.MarketRunner;
import info.fmro.shared.stream.definitions.MarketDefinition;
import info.fmro.shared.stream.enums.Side;
import info.fmro.shared.stream.objects.RunnerId;
import info.fmro.shared.stream.objects.SerializableObjectModification;
import info.fmro.shared.utility.Formulas;
import info.fmro.shared.utility.Generic;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SelectionModel;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.Mnemonic;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

@SuppressWarnings({"ClassWithTooManyMethods", "OverlyComplexClass", "AccessToNonThreadSafeStaticField"})
public class GUI
        extends Application {
    private static final Logger logger = LoggerFactory.getLogger(GUI.class);
    static final DecimalFormat decimalFormatTextField = new DecimalFormat("#.###");
    static final DecimalFormat decimalFormatLabelLow = new DecimalFormat("#,###.##");
    static final DecimalFormat decimalFormatLabelMedium = new DecimalFormat("#,###.#");
    static final DecimalFormat decimalFormatLabelHigh = new DecimalFormat("#,###");
    public static final int N_PRICE_CELLS = 3;
    @SuppressWarnings("StaticVariableMayNotBeInitialized")
    private static Application instance;
    @SuppressWarnings({"StaticVariableMayNotBeInitialized", "PackageVisibleField"})
    static Stage mainStage;
    private static final FocusPosition focusPosition = new FocusPosition();
    private static final AtomicBoolean primaryStageIsShown = new AtomicBoolean(), leftTreeViewInitialized = new AtomicBoolean(), rightTreeViewInitialized = new AtomicBoolean();
    private static final AtomicBoolean refreshPaused = new AtomicBoolean(), refreshQueued = new AtomicBoolean();
    private static final String DEFAULT_EVENT_NAME = "no event attached", NULL_NAME = "null value";
    private static final Label totalFundsLabel = new Label("€ " + decimalFormatLabelLow.format(Statics.existingFunds.getTotalFunds()));
    private static final Label availableLabel = new Label("€ " + decimalFormatLabelLow.format(Statics.existingFunds.getAvailableFunds()));
    private static final Label reserveLabel = new Label("€ " + decimalFormatLabelLow.format(Statics.existingFunds.getReserve()));
    private static final Label exposureLabel = new Label("€ " + decimalFormatLabelLow.format(Statics.existingFunds.getExposure()));
    private static final SplitPane mainSplitPane = new SplitPane();
    static final Collection<String> toBeRemovedEventIds = new HashSet<>(2), toBeRemovedMarketIds = new HashSet<>(2);
    private static final ObservableList<Node> mainSplitPaneNodesList = mainSplitPane.getItems();
    private static final DualHashBidiMap<String, TreeItem<String>> managedEventsTreeItemMap = new DualHashBidiMap<>(), managedMarketsTreeItemMap = new DualHashBidiMap<>(), eventsTreeItemMap = new DualHashBidiMap<>(),
            marketsTreeItemMap = new DualHashBidiMap<>();
    private static final TreeItem<String> leftEventTreeRoot = new TreeItem<>(), rightEventTreeRoot = new TreeItem<>();
    private static final ObservableList<TreeItem<String>> leftEventRootChildrenList = leftEventTreeRoot.getChildren(), rightEventRootChildrenList = rightEventTreeRoot.getChildren();
    static final GridPane mainGridPane = new GridPane();
    private static final ScrollPane mainScrollPane = new ScrollPane();
    private static final TreeView<String> leftTreeView = createTreeView(leftEventTreeRoot), rightTreeView = createTreeView(rightEventTreeRoot);
    private static final VBox rightVBox = new VBox(rightTreeView);
    private static final TextField filterTextField = new TextField();
    private static final Button refreshEventsButton = new Button("_Refresh events");
    //    private static final Pattern NUMERIC_PATTERN = Pattern.compile("\\d*");
//    private static final Pattern NUMERIC_PATTERN = Pattern.compile("[+-]?\\d*\\.?\\d+"); // [+-]?\\d*\\.?\\d+
//    private static final Pattern NON_NUMERIC_PATTERN = Pattern.compile("[^\\d]");
    @SuppressWarnings("PackageVisibleField")
    static volatile boolean rightPanelVisible;
    @Nullable
    @SuppressWarnings("RedundantFieldInitialization")
    private static TreeItem<String> currentlyShownManagedObject = null;
    //    public static final CountDownLatch hasStarted = new CountDownLatch(1);
    private static final Label eventNamePreLabel = new Label("ManagedEvent:");
    private static final Label eventIdPreLabel = new Label("Event Id:");
    private static final Label simpleAmountLimitLabel = new Label("Set Amount Limit:");
    private static final Label maxMarketLimitPreLabel = new Label("Max Market Limit:");
    private static final Label calculatedAmountLimitPreLabel = new Label("Calculated Limit:");
    private static final Label nManagedMarketsPreLabel = new Label("nManagedMarkets:");
    private static final Label nTotalMarketsPreLabel = new Label("nTotalMarkets:");
    private static final Label eventOpenTimePreLabel = new Label("Event Open Date:");
    private static final Label marketNamePreLabel = new Label("ManagedMarket:");
    private static final Label marketIdPreLabel = new Label("Market Id:");
    private static final Label marketTotalValueTradedPreLabel = new Label("Value Traded:");
    private static final Label marketLiveCounterPreLabel = new Label("Time Till Live:");
    private static final Label marketLiveTimePreLabel = new Label("Market Live Date:");
    private static final Label marketOpenTimePreLabel = new Label("Market Open Date:");
    private static final Label nTotalRunnersPreLabel = new Label("nTotalRunners:");
    private static final Label nActiveRunnersPreLabel = new Label("nActiveRunners:");
    private static final Label nManagedRunnersPreLabel = new Label("nManagedRunners:");
    //    private static final Label unmatchedBackPreLabel = new Label("Unmatched Back:");
//    private static final Label unmatchedLayPreLabel = new Label("Unmatched Lay:");
    @SuppressWarnings("SpellCheckingInspection")
    private static final Label marketIsEnabledLabel = new Label("Mark enab:");
    @SuppressWarnings("SpellCheckingInspection")
    private static final Label marketMandatoryPlaceLabel = new Label("Mand plac:");
//    private static final Label marketKeepAtInPlayLabel = new Label("Kp inPlay:");
    private static final AtomicLong liveTime = new AtomicLong();
    private static final Label marketLiveCounterLabel = new Label();
    private static final EventHandler<ActionEvent> liveCounterEventHandler = event -> {
        final long timeTillLive = liveTime.get() - System.currentTimeMillis();
        if (timeTillLive <= 0L) {
            marketLiveCounterLabel.setText("already live");
        } else {
            final long days = TimeUnit.MILLISECONDS.toDays(timeTillLive);
            final long hours = TimeUnit.MILLISECONDS.toHours(timeTillLive) % 24L;
            final long minutes = TimeUnit.MILLISECONDS.toMinutes(timeTillLive) % 60L;
            final long seconds = TimeUnit.MILLISECONDS.toSeconds(timeTillLive) % 60L;
            final String timeString;
            if (days > 0L) {
                timeString = String.format("%dd %02d:%02d:%02d", days, hours, minutes, seconds);
            } else if (hours > 0L) {
                timeString = String.format("%d:%02d:%02d", hours, minutes, seconds);
            } else if (minutes > 0L) {
                timeString = String.format("%d:%02d", minutes, seconds);
            } else {
                timeString = String.format("%d", seconds);
            }

            marketLiveCounterLabel.setText(timeString);
//            marketLiveCounterLabel.setText(Generic.addCommas(timeTillLive / 1_000L));
        }
    };
    private static final Timeline timeline = new Timeline(new KeyFrame(Duration.millis(1_000L), liveCounterEventHandler));
    static final AtomicReference<String> currentFilterValue = new AtomicReference<>();
    private static final LinkedBlockingQueue<PlatformRunLaterCommand<PlatformRunLaterEnum>> platformRunLaterCommands = new LinkedBlockingQueue<>();
    private static final Object runLaterSync = new Object();
    private static volatile boolean runLaterIsRunning, platformStopped;
    private static final AtomicBoolean threadInWaitingExists = new AtomicBoolean();

    static {
        marketIsEnabledLabel.setContentDisplay(ContentDisplay.RIGHT);
        marketMandatoryPlaceLabel.setContentDisplay(ContentDisplay.RIGHT);
//        marketKeepAtInPlayLabel.setContentDisplay(ContentDisplay.RIGHT);

        totalFundsLabel.setId("ImportantText");
        totalFundsLabel.setMinWidth(200);
        availableLabel.setId("ImportantText");
        availableLabel.setMinWidth(200);
        reserveLabel.setId("ImportantText");
        reserveLabel.setMinWidth(200);
        exposureLabel.setId("ImportantText");
        exposureLabel.setMinWidth(200);
//        mainSplitPane.setDividerPositions(.09, .91);
//        mainSplitPane.setDividerPosition(0, .09d);
//        mainSplitPane.setDividerPosition(1, .91d);
        timeline.setCycleCount(Animation.INDEFINITE);

        mainScrollPane.setContent(mainGridPane);
    }

    @Override
    public void stop() {
        SharedStatics.mustStop.set(true);
        //noinspection AssignmentToStaticFieldFromInstanceMethod
        GUI.platformStopped = true;
    }

    @Override
    public void init() {
        //noinspection AssignmentToStaticFieldFromInstanceMethod
        GUI.instance = this;
    }

    private static Application getInstance() {
        //noinspection StaticVariableUsedBeforeInitialization
        return GUI.instance;
    }

    private static boolean isRightTreeView(@NotNull final TreeView<String> treeView) {
        return treeView == rightTreeView;
    }

    private static void addCommand(final PlatformRunLaterEnum commandEnum, final Serializable... objectsToModify) {
        platformRunLaterCommands.add(new PlatformRunLaterCommand<>(commandEnum, objectsToModify));
        runLater();
    }

    private static void runLater() {
        if (threadInWaitingExists.getAndSet(true)) { // already running, will exit
        } else {
            new Thread(() -> {
                synchronized (runLaterSync) {
                    threadInWaitingExists.set(false);
                    if (platformRunLaterCommands.isEmpty()) { // no commands in queue, won't run
                    } else {
                        runLaterIsRunning = true;
                        Platform.runLater(GUI::runLaterCore);

                        while (runLaterIsRunning && !platformStopped) {
                            Thread.onSpinWait();
                        }
                    }
                }
            }).start();
        }
    }

    @SuppressWarnings({"unchecked", "OverlyLongMethod", "OverlyComplexMethod"})
    private static void runLaterCore() {
        PlatformRunLaterCommand<PlatformRunLaterEnum> platformRunLaterCommand;
        //noinspection NestedAssignment
        while ((platformRunLaterCommand = platformRunLaterCommands.poll()) != null) {
            final PlatformRunLaterEnum platformRunLaterEnum = platformRunLaterCommand.getCommand();
            final Serializable[] variables = platformRunLaterCommand.getArray();
            switch (platformRunLaterEnum) {
                case refreshDisplayedManagedObject -> refreshDisplayedManagedObject((boolean) variables[0]);
                case managedMarketUpdated -> updateCurrentManagedMarket((String) variables[0]);
                case managedEventUpdated -> updateCurrentManagedEvent((String) variables[0]);
                case updateTotalFundsLabel -> totalFundsLabel.setText((String) variables[0]);
                case updateAvailableLabel -> availableLabel.setText((String) variables[0]);
                case updateReserveLabel -> reserveLabel.setText((String) variables[0]);
                case updateExposureLabel -> exposureLabel.setText((String) variables[0]);
                case checkTreeItemsWithNullName -> privateCheckTreeItemsWithNullName(false);
                case removeManagedEvent -> removeManagedEvent((String) variables[0], (Iterable<String>) variables[1]);
                case addManagedEvent -> addManagedEvent((String) variables[0], (ManagedEvent) variables[1]);
                case addManagedMarket -> addManagedMarket((String) variables[0], (ManagedMarket) variables[1]);
                case removeManagedMarket -> removeManagedMarket((String) variables[0]);
                case initializeRulesManagerTreeView -> initializeLeftTreeView();
                case markManagedItemAsExpired -> markManagedItemAsExpired((String) variables[0], (Boolean) variables[1]);
                case markManagedItemsAsExpired -> markManagedItemsAsExpired((Iterable<String>) variables[0], (Boolean) variables[1]);
                case markAllManagedItemsAsExpired -> markAllManagedItemsAsExpired((Boolean) variables[0]);
                case markManagedItemAsNotExpired -> markManagedItemAsNotExpired((String) variables[0], (Boolean) variables[1]);
                case markManagedItemsAsNotExpired -> markManagedItemsAsNotExpired((Iterable<String>) variables[0], (Boolean) variables[1]);
                case initializeEventsTreeView -> {
                    if (rightPanelVisible) {
                        initializeRightTreeView();
                    } else { // I will only update the right panel if it is visible
                    }
                }
                case initializeMarketsTreeView -> {
                    if (rightPanelVisible) {
                        initializeMarketsRightTreeView();
                    } else { // I will only update the right panel if it is visible
                    }
                }
                case clearEventsTreeView -> {
                    if (rightPanelVisible) {
                        clearRightTreeView();
                    } else { // I will only update the right panel if it is visible
                    }
                }
                case clearMarketsTreeView -> {
                    if (rightPanelVisible) {
                        clearMarketsRightTreeView();
                    } else { // I will only update the right panel if it is visible
                    }
                }
                case putAllEvent -> {
                    final Map<String, Event> m = (Map<String, Event>) variables[0];
                    if (rightPanelVisible) {
                        putAllEvent(m);
                    } else { // I will only update the right panel if it is visible
                    }
                    if (m == null) { // nothing to be done
                    } else {
                        updateManagedEvents(m.keySet());
                    }
                }
                case putAllMarket -> {
                    final Map<String, MarketCatalogue> m = (Map<String, MarketCatalogue>) variables[0];
                    if (rightPanelVisible) {
                        putAllMarket(m);
                    } else { // I will only update the right panel if it is visible
                    }
                    if (m == null) { // nothing to be done
                    } else {
                        updateManagedMarkets(m.keySet());
                    }
                }
                case removeEntriesEvent -> removeEntriesEvent((Iterable<? extends Map.Entry<String, Event>>) variables[0]);
                case removeEntriesMarket -> removeEntriesMarket((Iterable<? extends Map.Entry<String, MarketCatalogue>>) variables[0]);
                case retainEntriesEvent -> retainEntriesEvent((Collection<? extends Map.Entry<String, Event>>) variables[0]);
                case retainEntriesMarket -> retainEntriesMarket((Collection<? extends Map.Entry<String, MarketCatalogue>>) variables[0]);
                case removeKeysEvent -> removeKeysEvent((Iterable<String>) variables[0]);
                case removeKeysMarket -> removeKeysMarket((Iterable<String>) variables[0]);
                case retainKeysEvent -> retainKeysEvent((Collection<String>) variables[0]);
                case retainKeysMarket -> retainKeysMarket((Collection<String>) variables[0]);
                case removeValueAllEvent, removeEvent -> removeEvent((Event) variables[0]);
                case removeValueAllMarket, removeMarketCatalogue -> removeMarket((MarketCatalogue) variables[0]);
                case removeValuesEvent -> removeValuesEvent((Iterable<? extends Event>) variables[0]);
                case removeValuesMarket -> removeValuesMarket((Iterable<? extends MarketCatalogue>) variables[0]);
                case retainValuesEvent -> retainValuesEvent((Collection<? extends Event>) variables[0]);
                case retainValuesMarket -> retainValuesMarket((Collection<? extends MarketCatalogue>) variables[0]);
                case removeEventId -> removeEvent((String) variables[0]);
                case addEvent -> {
                    if (rightPanelVisible) {
                        addEvent((String) variables[0], (Event) variables[1]);
                    } else { // I will only update the right panel if it is visible
                    }
                    updateManagedEvents(Set.of((String) variables[0]));
                }
                case addMarket -> {
                    if (rightPanelVisible) {
                        addMarket((String) variables[0], (MarketCatalogue) variables[1]);
                    } else { // I will only update the right panel if it is visible
                    }
                    updateManagedMarkets(Set.of((String) variables[0]));
                }
                case removeMarketId -> removeMarket((String) variables[0]);
                case refreshEventsButtonSetDisable -> refreshEventsButton.setDisable(false);
                default -> logger.error("unknown platformRunLaterEnum in runLaterCore: {} {}", platformRunLaterEnum, Generic.objectToString(variables));
            }
        }
        runLaterIsRunning = false;
    }

    public static void publicRefreshDisplayedManagedObject() {
        publicRefreshDisplayedManagedObject(false);
    }

    public static void publicRefreshDisplayedManagedObject(final boolean nonExistentManagedObjectIsAcceptable) {
        addCommand(PlatformRunLaterEnum.refreshDisplayedManagedObject, nonExistentManagedObjectIsAcceptable);
    }

    public static void managedMarketUpdated(final String marketId) {
        addCommand(PlatformRunLaterEnum.managedMarketUpdated, marketId);
    }

    public static void managedEventUpdated(final String eventId) {
        addCommand(PlatformRunLaterEnum.managedEventUpdated, eventId);
    }

    public static void updateTotalFundsLabel(final double funds) {
        addCommand(PlatformRunLaterEnum.updateTotalFundsLabel, "€ " + decimalFormatLabelLow.format(funds));
    }

    public static void updateAvailableLabel(final double funds) {
        addCommand(PlatformRunLaterEnum.updateAvailableLabel, "€ " + decimalFormatLabelLow.format(funds));
    }

    public static void updateReserveLabel(final double funds) {
        addCommand(PlatformRunLaterEnum.updateReserveLabel, "€ " + decimalFormatLabelLow.format(funds));
    }

    public static void updateExposureLabel(final double funds) {
        addCommand(PlatformRunLaterEnum.updateExposureLabel, "€ " + decimalFormatLabelLow.format(funds));
    }

    @SuppressWarnings("WeakerAccess")
    public static void checkTreeItemsWithNullName() {
        addCommand(PlatformRunLaterEnum.checkTreeItemsWithNullName);
    }

    public static void publicRemoveManagedEvent(final String eventId, final HashSet<String> marketIds) {
        addCommand(PlatformRunLaterEnum.removeManagedEvent, eventId, marketIds);
    }

    public static void publicAddManagedEvent(final String eventId, final ManagedEvent managedEvent) {
        addCommand(PlatformRunLaterEnum.addManagedEvent, eventId, managedEvent);
    }

    public static void publicAddManagedMarket(final String marketId, final ManagedMarket managedMarket) {
        addCommand(PlatformRunLaterEnum.addManagedMarket, marketId, managedMarket);
    }

    public static void publicRemoveManagedMarket(final String marketId) {
        addCommand(PlatformRunLaterEnum.removeManagedMarket, marketId);
    }

    public static void initializeRulesManagerTreeView() {
        addCommand(PlatformRunLaterEnum.initializeRulesManagerTreeView);
    }

    public static void publicMarkManagedItemAsExpired(final String id, final boolean isEvent) {
        addCommand(PlatformRunLaterEnum.markManagedItemAsExpired, id, isEvent);
    }

    public static void publicMarkManagedItemsAsExpired(@NotNull final HashSet<String> ids, final boolean isEvent) {
        addCommand(PlatformRunLaterEnum.markManagedItemsAsExpired, ids, isEvent);
    }

    public static void publicMarkAllManagedItemsAsExpired(final boolean isEvent) {
        addCommand(PlatformRunLaterEnum.markAllManagedItemsAsExpired, isEvent);
    }

    public static void publicMarkManagedItemAsNotExpired(final String id, final boolean isEvent) {
        addCommand(PlatformRunLaterEnum.markManagedItemAsNotExpired, id, isEvent);
    }

    public static void publicMarkManagedItemsAsNotExpired(@NotNull final HashSet<String> ids, final boolean isEvent) {
        addCommand(PlatformRunLaterEnum.markManagedItemsAsNotExpired, ids, isEvent);
    }

    public static void initializeEventsTreeView() {
        addCommand(PlatformRunLaterEnum.initializeEventsTreeView);
    }

    public static void initializeMarketsTreeView() {
        addCommand(PlatformRunLaterEnum.initializeMarketsTreeView);
    }

    public static void clearEventsTreeView() {
        addCommand(PlatformRunLaterEnum.clearEventsTreeView);
    }

    public static void clearMarketsTreeView() {
        addCommand(PlatformRunLaterEnum.clearMarketsTreeView);
    }

    public static void publicPutAllEvent(final HashMap<String, Event> m) {
        addCommand(PlatformRunLaterEnum.putAllEvent, m);
    }

    public static void publicPutAllMarket(final HashMap<String, MarketCatalogue> m) {
        addCommand(PlatformRunLaterEnum.putAllMarket, m);
    }

    public static void publicRemoveEntriesEvent(final HashSet<? extends Map.Entry<String, Event>> c) {
        addCommand(PlatformRunLaterEnum.removeEntriesEvent, c);
    }

    public static void publicRemoveEntriesMarket(final HashSet<? extends Map.Entry<String, MarketCatalogue>> c) {
        addCommand(PlatformRunLaterEnum.removeEntriesMarket, c);
    }

    public static void publicRetainEntriesEvent(final HashSet<? extends Map.Entry<String, Event>> c) {
        addCommand(PlatformRunLaterEnum.retainEntriesEvent, c);
    }

    public static void publicRetainEntriesMarket(final HashSet<? extends Map.Entry<String, MarketCatalogue>> c) {
        addCommand(PlatformRunLaterEnum.retainEntriesMarket, c);
    }

    public static void publicRemoveKeysEvent(final HashSet<String> c) {
        addCommand(PlatformRunLaterEnum.removeKeysEvent, c);
    }

    public static void publicRemoveKeysMarket(final HashSet<String> c) {
        addCommand(PlatformRunLaterEnum.removeKeysMarket, c);
    }

    public static void publicRetainKeysEvent(final HashSet<String> c) {
        addCommand(PlatformRunLaterEnum.retainKeysEvent, c);
    }

    public static void publicRetainKeysMarket(final HashSet<String> c) {
        addCommand(PlatformRunLaterEnum.retainKeysMarket, c);
    }

    public static void publicRemoveValueAllEvent(final Event value) {
        addCommand(PlatformRunLaterEnum.removeValueAllEvent, value);
    }

    public static void publicRemoveValueAllMarket(final MarketCatalogue value) {
        addCommand(PlatformRunLaterEnum.removeValueAllMarket, value);
    }

    public static void publicRemoveValuesEvent(final HashSet<? extends Event> c) {
        addCommand(PlatformRunLaterEnum.removeValuesEvent, c);
    }

    public static void publicRemoveValuesMarket(final HashSet<? extends MarketCatalogue> c) {
        addCommand(PlatformRunLaterEnum.removeValuesMarket, c);
    }

    public static void publicRetainValuesEvent(final HashSet<? extends Event> c) {
        addCommand(PlatformRunLaterEnum.retainValuesEvent, c);
    }

    public static void publicRetainValuesMarket(final HashSet<? extends MarketCatalogue> c) {
        addCommand(PlatformRunLaterEnum.retainValuesMarket, c);
    }

    public static void publicRemoveEvent(final String eventId) {
        addCommand(PlatformRunLaterEnum.removeEventId, eventId);
    }

    public static void publicRemoveEvent(final Event event) {
        addCommand(PlatformRunLaterEnum.removeEvent, event);
    }

    public static void publicAddEvent(final String eventId, final Event event) {
        addCommand(PlatformRunLaterEnum.addEvent, eventId, event);
    }

    public static void publicAddMarket(final String marketId, final MarketCatalogue market) {
        addCommand(PlatformRunLaterEnum.addMarket, marketId, market);
    }

    public static void publicRemoveMarket(final MarketCatalogue market) {
        addCommand(PlatformRunLaterEnum.removeMarketCatalogue, market);
    }

    public static void publicRemoveMarket(final String marketId) {
        addCommand(PlatformRunLaterEnum.removeMarketId, marketId);
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

    private static boolean markMarketForRemoval(final String marketId) {
        return toBeRemovedMarketIds.add(marketId);
    }

    private static void unMarkMarketForRemoval(final String marketId) {
        toBeRemovedMarketIds.remove(marketId);
    }

    private static void processMarketsForRemoval() {
        for (final String marketId : toBeRemovedMarketIds) {
            removeMarket(marketId, true);
        }
        toBeRemovedEventIds.clear();
    }

    private static void markEventForRemoval(final String eventId) {
        toBeRemovedEventIds.add(eventId);
    }

    private static void unMarkEventForRemoval(final String eventId) {
        toBeRemovedEventIds.remove(eventId);
    }

    private static void processEventsForRemoval() {
        for (final String eventId : toBeRemovedEventIds) {
            removeEvent(eventId, true);
        }
        toBeRemovedEventIds.clear();
    }

    private static void removeEvent(final Event event) {
        final String eventId = event == null ? null : event.getId();
        removeEvent(eventId);
    }

    private static void removeEvent(final String eventId) {
        removeEvent(eventId, false);
    }

    private static void removeEvent(final String eventId, final boolean removeAnyway) {
        if (rightPanelVisible && !removeAnyway) {
            markEventForRemoval(eventId);
        } else {
            final TreeItem<String> eventTreeItem = eventsTreeItemMap.get(eventId);
            final ScrollBarState scrollBarState = new ScrollBarState(rightTreeView, Orientation.VERTICAL, "removeEvent " + eventId + " " + (eventTreeItem == null ? "null" : eventTreeItem.getValue()));
            scrollBarState.save(rightTreeViewInitialized.get());
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
//            rightEventRootChildrenList.remove(eventTreeItem);
            synchronizedRemoveRightChild(eventTreeItem, rightEventRootChildrenList);
            eventsTreeItemMap.remove(eventId);
            if (toBeReAddedMarketIds != null) { // might be normal
                for (final String marketId : toBeReAddedMarketIds) {
                    addMarket(marketId);
                }
            } else { // this is the normal case, nothing to be reAdded
            }
            scrollBarState.restore(rightTreeViewInitialized.get());

            Statics.eventsMap.superRemove(eventId);
        }
    }

    private static void synchronizedRemoveRightChild(final TreeItem<String> treeItem, @SuppressWarnings("TypeMayBeWeakened") @NotNull final ObservableList<TreeItem<String>> rootChildrenList) {
        rootChildrenList.remove(treeItem);
    }

    private static void removeManagedEvent(final String eventId, final Iterable<String> marketIds) {
        final TreeItem<String> eventTreeItem = managedEventsTreeItemMap.get(eventId);
        final ScrollBarState scrollBarState = new ScrollBarState(leftTreeView, Orientation.VERTICAL, "removeManagedEvent " + eventId + " " + (eventTreeItem == null ? "null" : eventTreeItem.getValue()));
        scrollBarState.save(leftTreeViewInitialized.get());
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
        scrollBarState.restore(leftTreeViewInitialized.get());
    }

    @SuppressWarnings("UnusedReturnValue")
    private static boolean removeMarket(final MarketCatalogue market) {
        final String marketId = market == null ? null : market.getMarketId();
        return removeMarket(marketId);
    }

    private static boolean removeMarket(final String marketId) {
        return removeMarket(marketId, false);
    }

    private static boolean removeMarket(final String marketId, final boolean removeAnyway) {
        final boolean removed;
        if (rightPanelVisible && !removeAnyway) {
            removed = markMarketForRemoval(marketId);
        } else {
            final TreeItem<String> marketTreeItem = marketsTreeItemMap.get(marketId);
            final ScrollBarState scrollBarState = new ScrollBarState(rightTreeView, Orientation.VERTICAL, "removeMarket " + marketId + " " + (marketTreeItem == null ? "null" : marketTreeItem.getValue()));
            scrollBarState.save(rightTreeViewInitialized.get());
            marketsTreeItemMap.remove(marketId);
            final TreeItem<String> eventTreeItem = marketTreeItem == null ? null : marketTreeItem.getParent();
            @Nullable final ObservableList<TreeItem<String>> listOfChildren = eventTreeItem == null ? null : eventTreeItem.getChildren();
            removed = listOfChildren != null && listOfChildren.remove(marketTreeItem);
            if (removed && eventsTreeItemMap.getKey(eventTreeItem) == null && listOfChildren.isEmpty()) { // parentEventItem is defaultEvent and it no longer has children
                eventsTreeItemMap.removeValue(eventTreeItem);
//            rightEventRootChildrenList.remove(eventTreeItem);
                synchronizedRemoveRightChild(eventTreeItem, rightEventRootChildrenList);
            } else { // parentEventItem is not defaultEvent, nothing to be done
            }
            scrollBarState.restore(rightTreeViewInitialized.get());

            Statics.marketCataloguesMap.superRemove(marketId);
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
        final ScrollBarState scrollBarState = new ScrollBarState(leftTreeView, Orientation.VERTICAL,
                                                                 "removeManagedMarket event:" + (eventTreeItem == null ? "null" : eventTreeItem.getValue()) + " market:" + (marketTreeItem == null ? "null" : marketTreeItem.getValue()));
        scrollBarState.save(leftTreeViewInitialized.get());
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
            scrollBarState.restore(leftTreeViewInitialized.get());
        } else { // nothing was removed, no need to clear the gridPane
        }
        return removed;
    }

    private static void clearLeftTreeView() {
        leftTreeViewInitialized.set(false);
        final Iterable<TreeItem<String>> listCopy = new ArrayList<>(leftEventRootChildrenList);
        for (final TreeItem<String> treeItem : listCopy) {
            treeItem.getChildren().clear();
        }
        managedMarketsTreeItemMap.clear();

        leftEventRootChildrenList.clear();
        managedEventsTreeItemMap.clear();
        clearMainGridPane();
    }

    private static void clearMarketsRightTreeView() {
        final Iterable<TreeItem<String>> listCopy = new ArrayList<>(rightEventRootChildrenList);
        for (final TreeItem<String> treeItem : listCopy) {
            if (treeItem == null) {
                logger.error("null treeItem during clearMarketsRightTreeView");
            } else {
                treeItem.getChildren().clear();
            }
        }
        marketsTreeItemMap.clear();
    }

    static void clearRightTreeView() {
        rightTreeViewInitialized.set(false);

        processMarketsForRemoval();
        processEventsForRemoval();

        clearMarketsRightTreeView();

        rightEventRootChildrenList.clear();
//        rightEventTreeRoot.unbind();
        eventsTreeItemMap.clear();
    }

    @SuppressWarnings("UnusedReturnValue")
    private static int initializeLeftTreeView() {
        int modified = 0;

        clearLeftTreeView();
        modified += addManagedEvents(true);
        modified += addManagedMarkets(true);
        leftTreeViewInitialized.set(true);

        return modified;
    }

    @SuppressWarnings("UnusedReturnValue")
    private static int initializeRightTreeView() {
        return initializeRightTreeView(false);
    }

    @SuppressWarnings("UnusedReturnValue")
    static int initializeRightTreeView(final boolean alreadyCleared) {
        int modified = 0;

        if (alreadyCleared) { // no need to clear again
        } else {
            clearRightTreeView();
        }
        modified += addEvents(true);
        modified += addMarkets(true);
        rightTreeViewInitialized.set(true);

        return modified;
    }

    @SuppressWarnings("UnusedReturnValue")
    private static int initializeMarketsRightTreeView() {
        int modified = 0;

        clearMarketsRightTreeView();
//        modified += addEvents();
        modified += addMarkets(true);

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
        } else if (eventTreeRoot == rightEventTreeRoot) { // no need to initialize, as this is treeView created when the program starts, and the rightPanel is not visible
            if (rightPanelVisible) {
                initializeRightTreeView();
            } else { // no initialize is not visible
            }
        } else {
            logger.error("unknown treeRoot in createTreeView: {}", Generic.objectToString(eventTreeRoot));
        }

        return treeView;
    }

    private static void privateCheckTreeItemsWithNullName(final boolean isDuringInitialization) { // only checks the managed items
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
                            reorderManagedEventItem(eventItem, isDuringInitialization);
//                            GUIUtils.triggerTreeItemRefresh(eventItem);
                            leftTreeView.refresh();
                        } else {
                            if (treeName == null) {
                                eventItem.setValue(NULL_NAME);
                                reorderManagedEventItem(eventItem, isDuringInitialization);
//                                GUIUtils.triggerTreeItemRefresh(eventItem);
                                leftTreeView.refresh();
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
                            reorderManagedMarketItem(marketItem, isDuringInitialization);
//                            GUIUtils.triggerTreeItemRefresh(marketItem);
                            leftTreeView.refresh();
                        } else {
                            if (treeName == null) {
                                marketItem.setValue(NULL_NAME);
                                reorderManagedEventItem(marketItem, isDuringInitialization);
//                                GUIUtils.triggerTreeItemRefresh(marketItem);
                                leftTreeView.refresh();
                            } else { // I already set the NULL_NAME, nothing more to be done
                            }
                        }
                    } else {
                        logger.error("null managedMarket in privateCheckTreeItemsWithNullName for: {} {}", marketId, Generic.objectToString(Statics.rulesManager.markets));
                        Statics.rulesManager.removeManagedMarket(marketId, Statics.marketCataloguesMap);
                    }
                } else {
                    logger.error("null both marketId and treeName for: {} {}", Generic.objectToString(marketItem), Generic.objectToString(managedMarketsTreeItemMap));
                }
            } else { // name not null, nothing to be done
            }
        }
    }

    private static int addMarkets(final boolean isDuringInitialization) {
        int modified = 0;
        @NotNull final HashMap<String, MarketCatalogue> mapCopy = Statics.marketCataloguesMap.copy();
        for (@NotNull final Map.Entry<String, MarketCatalogue> entry : mapCopy.entrySet()) {
            modified += addMarket(entry, isDuringInitialization);
        }
        return modified;
    }

    private static int addManagedMarkets(final boolean isDuringInitialization) {
        int modified = 0;
        @NotNull final HashMap<String, ManagedMarket> mapCopy = Statics.rulesManager.markets.copy();
        for (@NotNull final Map.Entry<String, ManagedMarket> entry : mapCopy.entrySet()) {
            modified += addManagedMarket(entry, isDuringInitialization);
        }
        return modified;
    }

    @SuppressWarnings("UnusedReturnValue")
    private static int addMarket(final String marketId) {
        final MarketCatalogue market = Statics.marketCataloguesMap.get(marketId);
        return addMarket(marketId, market);
    }

    @SuppressWarnings("UnusedReturnValue")
    private static int addMarket(@NotNull final Map.Entry<String, ? extends MarketCatalogue> entry) {
        return addMarket(entry, false);
    }

    private static int addMarket(@NotNull final Map.Entry<String, ? extends MarketCatalogue> entry, final boolean isDuringInitialization) {
        final String marketId = entry.getKey();
        final MarketCatalogue market = entry.getValue();
        return addMarket(marketId, market, isDuringInitialization);
    }

    private static int addMarket(final String marketId, final MarketCatalogue market) {
        return addMarket(marketId, market, false);
    }

    private static int addMarket(final String marketId, final MarketCatalogue market, final boolean isDuringInitialization) {
        int modified = 0;
        unMarkMarketForRemoval(marketId);
        if (marketsTreeItemMap.containsKey(marketId)) { // already contained, nothing to be done, won't update the object here
        } else {
            if (market == null) {
                logger.error("null MarketCatalogue in addMarket for: {}", marketId);
                Statics.marketCataloguesMap.superRemoveValueAll(null);
            } else {
                final String eventId = Formulas.getEventIdOfMarketCatalogue(market);

                @Nullable TreeItem<String> parentItem = eventsTreeItemMap.get(eventId);
                if (parentItem == null) { // might be ok, won't do anything
                    parentItem = getDefaultEventAndCreateIfNotExist(isDuringInitialization);
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
                final TreeItem<String> marketTreeItem = new TreeItem<>(marketName);
//                marketTreeItem.setExpanded(true);
                marketsTreeItemMap.put(marketId, marketTreeItem);
                addTreeItem(marketTreeItem, parentItem, rightTreeView, isDuringInitialization);
                modified++;
            }
        }
        return modified;
    }

    private static void updateManagedMarkets(@NotNull final Iterable<String> ids) {
        for (final String id : ids) {
            updateManagedMarket(id);
        }
    }

    private static void updateManagedMarket(final String id) {
//        logger.info("updateManagedMarket: {}", id);
        final TreeItem<String> treeItem = managedMarketsTreeItemMap.get(id);
        final ManagedMarket managedMarket = Statics.rulesManager.markets.get(id);
        if (treeItem == null) { // no managed market, nothing to be done
        } else {
            final String marketName = GUIUtils.marketExistsInMapAndNotMarkedForRemoval(id) ? GUIUtils.markNameAsNotExpired(GUIUtils.getManagedMarketName(id, managedMarket)) : GUIUtils.markNameAsExpired(GUIUtils.getManagedMarketName(id, managedMarket));
            final String treeEventName = treeItem.getValue();
            if (Objects.equals(marketName, treeEventName)) { // no need to update
            } else {
                treeItem.setValue(marketName);
            }
//            logger.info("updateManagedMarket: {} {}", id, marketName);
            final String eventId = managedMarket.getParentEventId(Statics.marketCataloguesMap, Statics.rulesManager.rulesHaveChanged);
            final TreeItem<String> parentEventTreeItem = managedEventsTreeItemMap.get(eventId);
//            logger.info("updateManagedMarket: {} {} {}", id, eventId, parentEventTreeItem);
            final ManagedEvent managedEvent = managedMarket.getParentEvent(Statics.marketCataloguesMap, Statics.rulesManager);
//            logger.info("updateManagedMarket: {} {}", id, managedEvent == null ? null : managedEvent.getId());
            if (parentEventTreeItem == null || managedEvent == null) { // no parentEvent present or found, nothing to be done
            } else {
//                logger.info("updateManagedMarket: {} {}", id, eventId);
                checkManagedMarketsOnDefaultNode(managedEvent, parentEventTreeItem, false);
            }
        }
    }

    @SuppressWarnings("UnusedReturnValue")
    private static int addManagedMarket(final String marketId) {
        final ManagedMarket managedMarket = Statics.rulesManager.markets.get(marketId);
        return addManagedMarket(marketId, managedMarket);
    }

    @SuppressWarnings("unused")
    private static int addManagedMarket(@NotNull final Map.Entry<String, ? extends ManagedMarket> entry) {
        return addManagedMarket(entry, false);
    }

    private static int addManagedMarket(@NotNull final Map.Entry<String, ? extends ManagedMarket> entry, final boolean isDuringInitialization) {
        final String marketId = entry.getKey();
        final ManagedMarket managedMarket = entry.getValue();
        return addManagedMarket(marketId, managedMarket, isDuringInitialization);
    }

    private static int addManagedMarket(final String marketId, final ManagedMarket managedMarket) {
        return addManagedMarket(marketId, managedMarket, false);

    }

    private static int addManagedMarket(final String marketId, final ManagedMarket managedMarket, final boolean isDuringInitialization) {
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
                    parentItem = getDefaultManagedEventAndCreateIfNotExist(isDuringInitialization);
                } else { // I have the parentItem, nothing more to be done
                }

                final String marketName = GUIUtils.marketExistsInMapAndNotMarkedForRemoval(marketId) ?
                                          GUIUtils.markNameAsNotExpired(GUIUtils.getManagedMarketName(marketId, managedMarket)) : GUIUtils.markNameAsExpired(GUIUtils.getManagedMarketName(marketId, managedMarket));
                final TreeItem<String> marketTreeItem = new TreeItem<>(marketName);
                marketTreeItem.setExpanded(true);
                managedMarketsTreeItemMap.put(marketId, marketTreeItem);
                addTreeItem(marketTreeItem, parentItem, leftTreeView, isDuringInitialization);
                modified++;
            }
        }

        return modified;
    }

    @NotNull
    private static TreeItem<String> getDefaultEventAndCreateIfNotExist(final boolean isDuringInitialization) {
        TreeItem<String> defaultEvent = eventsTreeItemMap.get(null);
        if (defaultEvent == null) {
            defaultEvent = new TreeItem<>(DEFAULT_EVENT_NAME);
            defaultEvent.setExpanded(true);
            eventsTreeItemMap.put(null, defaultEvent);

            final String filterValue = currentFilterValue.get();
            if (filterValue == null || filterValue.isEmpty()) {
                addTreeItem(defaultEvent, rightEventTreeRoot, rightTreeView, isDuringInitialization);
            } else { // won't add default event when a filter is active
            }
        } else { // I have the defaultEvent, nothing more to be done
        }
        return defaultEvent;
    }

    @NotNull
    private static TreeItem<String> getDefaultManagedEventAndCreateIfNotExist(final boolean isDuringInitialization) {
        TreeItem<String> defaultEvent = managedEventsTreeItemMap.get(null);
        if (defaultEvent == null) {
            defaultEvent = new TreeItem<>(DEFAULT_EVENT_NAME);
            defaultEvent.setExpanded(true);
            managedEventsTreeItemMap.put(null, defaultEvent);
            addTreeItem(defaultEvent, leftEventTreeRoot, leftTreeView, isDuringInitialization);
        } else { // I have the defaultEvent, nothing more to be done
        }
        return defaultEvent;
    }

    private static int addEvents(final boolean isDuringInitialization) {
        int modified = 0;
        @NotNull final HashMap<String, Event> mapCopy = Statics.eventsMap.copy();
        for (@NotNull final Map.Entry<String, Event> entry : mapCopy.entrySet()) {
            modified += addEvent(entry, isDuringInitialization);
        }
        return modified;
    }

    @SuppressWarnings("UnusedReturnValue")
    private static int addEvent(@NotNull final Map.Entry<String, ? extends Event> entry) {
        return addEvent(entry, false);
    }

    private static int addEvent(@NotNull final Map.Entry<String, ? extends Event> entry, final boolean isDuringInitialization) {
        final String eventId = entry.getKey();
        final Event event = entry.getValue();
        return addEvent(eventId, event, isDuringInitialization);
    }

    @SuppressWarnings("UnusedReturnValue")
    private static int addEvent(final String eventId, final Event event) {
        return addEvent(eventId, event, false);
    }

    private static int addEvent(final String eventId, final Event event, final boolean isDuringInitialization) {
        int modified = 0;
        unMarkEventForRemoval(eventId);
        if (eventsTreeItemMap.containsKey(eventId)) { // already contained, nothing to be done, won't update the object here
        } else {
            if (event == null) {
                logger.error("null Event in addEvent for: {}", eventId);
                Statics.eventsMap.superRemoveValueAll(null);
            } else {
                final String eventName = event.getName();
                final String filterValue = currentFilterValue.get();
                final TreeItem<String> eventTreeItem = new TreeItem<>(eventName);
//                eventTreeItem.setExpanded(true);
                eventsTreeItemMap.put(eventId, eventTreeItem);
                if (Statics.unsupportedEventNames.contains(eventName)) { // won't add eventTreeItem to the treeView
                } else if (filterValue == null || filterValue.isEmpty() || StringUtils.containsIgnoreCase(eventName, filterValue)) {
                    addTreeItem(eventTreeItem, rightEventTreeRoot, rightTreeView, isDuringInitialization);
                } else { // not added because of filter
                }

                modified += checkMarketsOnDefaultNode(event, eventTreeItem, isDuringInitialization);

                modified++;
            }
        }
        return modified;
    }

    private static void markManagedItemAsExpired(final String id, final boolean isEvent) {
        final TreeItem<String> managedTreeItem = isEvent ? managedEventsTreeItemMap.get(id) : managedMarketsTreeItemMap.get(id);
        if (managedTreeItem == null) { // can be normal, nothing to be done
        } else {
            managedTreeItem.setValue(GUIUtils.markNameAsExpired(managedTreeItem.getValue()));
            if (isEvent) {
                reorderManagedEventItem(managedTreeItem, false);
            } else {
                reorderManagedMarketItem(managedTreeItem, false);
            }
//            GUIUtils.triggerTreeItemRefresh(managedTreeItem);
            leftTreeView.refresh();
        }
    }

    private static void markManagedItemsAsExpired(@NotNull final Iterable<String> ids, final boolean isEvent) {
        for (final String id : ids) {
            markManagedItemAsExpired(id, isEvent);
        }
    }

    private static void markAllManagedItemsAsExpired(final boolean isEvent) {
        @NotNull final Set<String> keySetCopy = isEvent ? Statics.rulesManager.events.keySetCopy() : Statics.rulesManager.markets.keySetCopy();
        for (final String id : keySetCopy) {
            markManagedItemAsExpired(id, isEvent);
        }
    }

    private static void markManagedItemAsNotExpired(final String id, final boolean isEvent) {
        final TreeItem<String> managedTreeItem = isEvent ? managedEventsTreeItemMap.get(id) : managedMarketsTreeItemMap.get(id);
        if (managedTreeItem == null) { // can be normal, nothing to be done
        } else {
            managedTreeItem.setValue(GUIUtils.markNameAsNotExpired(managedTreeItem.getValue()));
            if (isEvent) {
                reorderManagedEventItem(managedTreeItem, false);
            } else {
                reorderManagedMarketItem(managedTreeItem, false);
            }
//            GUIUtils.triggerTreeItemRefresh(managedTreeItem);
            leftTreeView.refresh();
        }
    }

    private static void markManagedItemsAsNotExpired(@NotNull final Iterable<String> ids, final boolean isEvent) {
        for (final String id : ids) {
            markManagedItemAsNotExpired(id, isEvent);
        }
    }

    private static void updateManagedEvents(@NotNull final Iterable<String> ids) {
        for (final String id : ids) {
            updateManagedEvent(id);
        }
    }

    private static void updateManagedEvent(final String id) {
        final TreeItem<String> treeItem = managedEventsTreeItemMap.get(id);
        if (treeItem == null) { // no managed event, nothing to be done
        } else {
            final ManagedEvent managedEvent = Statics.rulesManager.events.get(id);
            final String eventName = GUIUtils.eventExistsInMapAndNotMarkedForRemoval(id) ? GUIUtils.markNameAsNotExpired(GUIUtils.getManagedEventName(managedEvent)) : GUIUtils.markNameAsExpired(GUIUtils.getManagedEventName(managedEvent));
            final String treeEventName = treeItem.getValue();
            if (Objects.equals(eventName, treeEventName)) { // no need to update
            } else {
                treeItem.setValue(eventName);
            }

            checkManagedMarketsOnDefaultNode(managedEvent, treeItem, false);
        }
    }

    @SuppressWarnings("unused")
    private static int addManagedEvent(@NotNull final Map.Entry<String, ? extends ManagedEvent> entry) {
        return addManagedEvent(entry, false);
    }

    private static int addManagedEvent(@NotNull final Map.Entry<String, ? extends ManagedEvent> entry, final boolean isDuringInitialization) {
        final String eventId = entry.getKey();
        final ManagedEvent managedEvent = entry.getValue();
        return addManagedEvent(eventId, managedEvent, isDuringInitialization);
    }

    @SuppressWarnings("UnusedReturnValue")
    private static int addManagedEvent(final String eventId, final ManagedEvent managedEvent) {
        return addManagedEvent(eventId, managedEvent, false);
    }

    private static int addManagedEvent(final String eventId, final ManagedEvent managedEvent, final boolean isDuringInitialization) {
        int modified = 0;
        if (managedEventsTreeItemMap.containsKey(eventId)) { // already contained, nothing to be done, won't update the object here
        } else {
            if (managedEvent == null) {
                logger.error("null ManagedEvent in addManagedEvent for: {}", eventId);
                Statics.rulesManager.events.removeValueAll(null);
            } else {
                final String eventName = GUIUtils.eventExistsInMapAndNotMarkedForRemoval(eventId) ? GUIUtils.markNameAsNotExpired(GUIUtils.getManagedEventName(managedEvent)) : GUIUtils.markNameAsExpired(GUIUtils.getManagedEventName(managedEvent));
                final TreeItem<String> eventTreeItem = new TreeItem<>(eventName);
                eventTreeItem.setExpanded(true);
                managedEventsTreeItemMap.put(eventId, eventTreeItem);

                addTreeItem(eventTreeItem, leftEventTreeRoot, leftTreeView, isDuringInitialization);
                modified += checkManagedMarketsOnDefaultNode(managedEvent, eventTreeItem, isDuringInitialization);
                modified++;
            }
        }
        return modified;
    }

    private static int addManagedEvents(final boolean isDuringInitialization) {
        int modified = 0;
        @NotNull final HashMap<String, ManagedEvent> mapCopy = Statics.rulesManager.events.copy();
        for (@NotNull final Map.Entry<String, ManagedEvent> entry : mapCopy.entrySet()) {
            modified += addManagedEvent(entry, isDuringInitialization);
        }
        return modified;
    }

    private static int checkMarketsOnDefaultNode(@NotNull final Event event, @NotNull final TreeItem<String> eventTreeItem, final boolean isDuringInitialization) {
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
//                defaultChildrenList.remove(marketTreeItem);
                synchronizedRemoveRightChild(marketTreeItem, defaultChildrenList);
                addTreeItem(marketTreeItem, eventTreeItem, rightTreeView, isDuringInitialization);
                modified++;
            }

            final int defaultChildrenRemaining = defaultChildrenList.size();
            if (defaultChildrenRemaining == 0) {
//                rightEventRootChildrenList.remove(defaultEvent);
                synchronizedRemoveRightChild(defaultEvent, rightEventRootChildrenList);
                eventsTreeItemMap.remove(null);
            }
        } else { // no defaultEvent, nothing to check
        }
        return modified;
    }

    private static int checkManagedMarketsOnDefaultNode(@NotNull final ManagedEvent managedEvent, @NotNull final TreeItem<String> eventTreeItem, final boolean isDuringInitialization) {
        int modified = 0;
        final TreeItem<String> defaultEvent = managedEventsTreeItemMap.get(null);
        if (defaultEvent != null) {
            @NotNull final HashMap<String, ManagedMarket> marketsMap = managedEvent.marketsMap.copy(Statics.rulesManager.markets);
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
                    addTreeItem(marketTreeItem, eventTreeItem, leftTreeView, isDuringInitialization);
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

    private static void reorderManagedEventItem(@NotNull final TreeItem<String> eventItem, final boolean isDuringInitialization) {
        leftEventRootChildrenList.remove(eventItem);
        addTreeItem(eventItem, leftEventTreeRoot, leftTreeView, isDuringInitialization);
    }

    private static void reorderManagedMarketItem(@NotNull final TreeItem<String> marketItem, final boolean isDuringInitialization) {
        final TreeItem<String> parent = marketItem.getParent();
        if (parent == null) {
            logger.error("null parent during reorderManagedMarketItem for: {} {}", managedMarketsTreeItemMap.getKey(marketItem), marketItem);
        } else {
            @NotNull final ObservableList<TreeItem<String>> parentChildren = parent.getChildren();
            parentChildren.remove(marketItem);
            addTreeItem(marketItem, parent, leftTreeView, isDuringInitialization);
        }
    }

    private static int findIndexToBeInserted(@NotNull final ObservableList<TreeItem<String>> rootChildrenList, @NotNull final TreeItem<String> treeItem, final int start, final int end) {
        final int result;
        if (start > end) { // length 0
            result = -1; // append at end
        } else if (start == end) {
            final int compareResult = treeItemCompare(rootChildrenList.get(start), treeItem);
            if (compareResult >= 0) {
                result = start;
            } else {
                result = -1; // append at end
            }
        } else {
            final int mid = (start + end) / 2;
            final int compareResult1 = treeItemCompare(rootChildrenList.get(mid), treeItem), compareResult2 = treeItemCompare(rootChildrenList.get(mid + 1), treeItem);

            if (compareResult1 <= 0 && compareResult2 >= 0) {
                result = mid + 1;
            } else if (compareResult1 < 0) {
                result = findIndexToBeInserted(rootChildrenList, treeItem, mid + 1, end);
            } else {
                result = findIndexToBeInserted(rootChildrenList, treeItem, start, mid);
            }
        }

        return result;
    }

    private static void addTreeItem(@NotNull final TreeItem<String> treeItem, @NotNull final TreeItem<String> root, @NotNull final TreeView<String> treeView, final boolean isDuringInitialization) {
        @NotNull final ObservableList<TreeItem<String>> rootChildrenList = root.getChildren();
        final int size = rootChildrenList.size();
        boolean added = false;
        @Nullable final ScrollBarState scrollBarState;
        final boolean isRightTree = isRightTreeView(treeView);
        if (isDuringInitialization) {
            scrollBarState = null;
        } else if (isRightTree) {
            scrollBarState = new ScrollBarState(rightTreeView, Orientation.VERTICAL, "addTreeItem " + treeItem.getValue() + " isRightTree:" + isRightTree);
            scrollBarState.save(rightTreeViewInitialized.get());
        } else {
            scrollBarState = new ScrollBarState(leftTreeView, Orientation.VERTICAL, "addTreeItem " + treeItem.getValue() + " isRightTree:" + isRightTree);
            scrollBarState.save(leftTreeViewInitialized.get());
        }
//        for (int i = 0; i < size; i++) {
//            if (treeItemCompare(rootChildrenList.get(i), treeItem) >= 0) {
//                synchronizedAddRootChild(treeItem, rootChildrenList, i, treeView);
////                rootChildrenList.add(i, treeItem);
//                added = true;
//                break;
//            } else { // will continue, nothing to be done yet
//            }
//        }
//        if (added) { // added, nothing to be done
//        } else { // placed at the end of the list
//            synchronizedAddRootChild(treeItem, rootChildrenList, treeView);
////            rootChildrenList.add(treeItem);
//        }
        final int positionToInsertAt = findIndexToBeInserted(rootChildrenList, treeItem, 0, size - 1);
        synchronizedAddRootChild(treeItem, rootChildrenList, positionToInsertAt, treeView);

        final SelectionModel<TreeItem<String>> selectionModel = treeView.getSelectionModel();
        final TreeItem<String> selectedItem = selectionModel.getSelectedItem();
        final TreeItem<String> parentItem = treeItem.getParent();
        if (isDuringInitialization) { // no need to scroll nor to restore scrollBarState
        } else if (parentItem == selectedItem) {
            treeView.scrollTo(selectionModel.getSelectedIndex());
        } else { // no need to scroll
            if (isRightTree) {
                scrollBarState.restore(rightTreeViewInitialized.get());
            } else {
                scrollBarState.restore(leftTreeViewInitialized.get());
            }
        }
    }

    private static void synchronizedAddRootChild(@NotNull final TreeItem<String> treeItem, @NotNull final ObservableList<TreeItem<String>> rootChildrenList, @NotNull final TreeView<String> treeView) {
        synchronizedAddRootChild(treeItem, rootChildrenList, -1, treeView);
    }

    private static void synchronizedAddRootChild(@NotNull final TreeItem<String> treeItem, @NotNull final ObservableList<TreeItem<String>> rootChildrenList, final int position, @NotNull final TreeView<String> treeView) {
        //noinspection IfStatementWithIdenticalBranches
        if (treeView == rightTreeView) {
            simpleAddRootChild(treeItem, rootChildrenList, position);
        } else {
            simpleAddRootChild(treeItem, rootChildrenList, position);
        }
    }

    private static void simpleAddRootChild(@NotNull final TreeItem<String> treeItem, @SuppressWarnings("TypeMayBeWeakened") @NotNull final ObservableList<TreeItem<String>> rootChildrenList, final int position) {
        if (position >= 0) {
            rootChildrenList.add(position, treeItem);
        } else {
            rootChildrenList.add(treeItem);
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
        refreshDisplayedManagedObject(false);
    }

    private static void refreshDisplayedManagedObject(final boolean nonExistentManagedObjectIsAcceptable) {
        if (currentlyShownManagedObject == null) { // no event displayed, nothing to be done
        } else {
            final String marketId = managedMarketsTreeItemMap.getKey(currentlyShownManagedObject);
            if (marketId == null) {
                final String eventId = managedEventsTreeItemMap.getKey(currentlyShownManagedObject);
                showManagedEvent(eventId, currentlyShownManagedObject, nonExistentManagedObjectIsAcceptable);
            } else {
                showManagedMarket(marketId, currentlyShownManagedObject, nonExistentManagedObjectIsAcceptable);
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
        showManagedEvent(eventId, currentEvent, false);
    }

    @SuppressWarnings("ValueOfIncrementOrDecrementUsed")
    private static void showManagedEvent(final String eventId, final TreeItem<String> currentEvent, final boolean nonExistentManagedObjectIsAcceptable) {
        if (refreshPaused.get()) { // refresh is paused, refresh will happen when pause ends
            refreshQueued.set(true);
        } else {
            currentlyShownManagedObject = currentEvent;
            mainGridPane.getChildren().clear();
            // eventName(ManagedEvent or newValue), amountLimit/simpleAmountLimit(ManagedEvent), nManagedMarkets(ManagedEvent or nChildren of TreeItem), nTotalMarkets(Event), "open for" timer, starting at "open date"(Event)
            final ManagedEvent managedEvent = Statics.rulesManager.events.get(eventId);
            if (managedEvent == null) {
                if (nonExistentManagedObjectIsAcceptable) {
                    logger.info("null managedEvent in left listener for: {} {}", currentEvent, eventId);
                } else {
                    logger.error("null managedEvent in left listener for: {} {}", currentEvent, eventId);
                }
            } else {
                final Event event = Statics.eventsMap.get(eventId);
                updateEventName(currentEvent, eventId, managedEvent);
                final String eventName = currentEvent.getValue();
                final double simpleAmountLimit = managedEvent.getSimpleAmountLimit();
                final double calculatedAmountLimit = managedEvent.getAmountLimit(Statics.existingFunds);
                final int nManagedMarkets = currentEvent.getChildren().size();
                final int nTotalMarkets = event == null ? -1 : event.getMarketCount();
                final Date eventOpenTime = event == null ? null : event.getOpenDate();

                final Label eventNameLabel = new Label(eventName);
                final Label eventIdLabel = new Label(eventId);
                final TextField simpleAmountLimitNode = new TextField(decimalFormatTextField.format(Generic.keepDoubleWithinRange(simpleAmountLimit)));
                final Label calculatedAmountLimitLabel = new Label("€ " + decimalFormatLabelLow.format(calculatedAmountLimit));
                final Label nManagedMarketsLabel = new Label(String.valueOf(nManagedMarkets));
                final Label nTotalMarketsLabel = new Label(String.valueOf(nTotalMarkets));
                final Label eventOpenTimeLabel = new Label(eventOpenTime == null ? null : eventOpenTime.toString());

                GUIUtils.setOnKeyPressedTextField(eventId, simpleAmountLimitNode, simpleAmountLimit, RulesManagerModificationCommand.setEventAmountLimit, eventId);
                GUIUtils.handleDoubleTextField(simpleAmountLimitNode, simpleAmountLimit);

                int rowIndex = 0;
                mainGridPane.add(eventNamePreLabel, 0, rowIndex);
                mainGridPane.add(eventNameLabel, 1, rowIndex++);
                mainGridPane.add(eventIdPreLabel, 0, rowIndex);
                mainGridPane.add(eventIdLabel, 1, rowIndex++);
                mainGridPane.add(simpleAmountLimitLabel, 0, rowIndex);
                mainGridPane.add(simpleAmountLimitNode, 1, rowIndex++);
                focusGridNode(simpleAmountLimitNode, eventId, 1, rowIndex - 1);

                mainGridPane.add(calculatedAmountLimitPreLabel, 0, rowIndex);
                mainGridPane.add(calculatedAmountLimitLabel, 1, rowIndex++);
                mainGridPane.add(nManagedMarketsPreLabel, 0, rowIndex);
                mainGridPane.add(nManagedMarketsLabel, 1, rowIndex++);
                mainGridPane.add(nTotalMarketsPreLabel, 0, rowIndex);
                mainGridPane.add(nTotalMarketsLabel, 1, rowIndex++);
                mainGridPane.add(eventOpenTimePreLabel, 0, rowIndex);
                mainGridPane.add(eventOpenTimeLabel, 1, rowIndex);
            }
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

    @SuppressWarnings({"ValueOfIncrementOrDecrementUsed", "OverlyLongMethod", "OverlyComplexMethod"})
    private static int showRunner(final String marketId, final RunnerId runnerId, final ManagedRunner managedRunner, final MarketRunner marketRunner, final MarketCatalogue marketCatalogue, final Market market, final int previousAppendRowIndex,
                                  final int runnerRowIndex) {
        final boolean isActive = managedRunner != null && managedRunner.isActive(market);
        final String runnerName = marketCatalogue == null ? null : marketCatalogue.getRunnerName(runnerId);
        final double backAmountLimit = managedRunner == null ? -1d : managedRunner.simpleGetBackAmountLimit();
        final double layAmountLimit = managedRunner == null ? -1d : managedRunner.simpleGetLayAmountLimit();
        final double minBackOdds = managedRunner == null ? -1d : managedRunner.getMinBackOdds();
        final double maxLayOdds = managedRunner == null ? -1d : managedRunner.getMaxLayOdds();
//        final OrderMarketRunner orderMarketRunner = SharedStatics.orderCache.getOrderMarketRunner(marketId, runnerId);
//        if (orderMarketRunner == null) { // no orderMarketRunner present, which means no orders placed on this runner, normal behavior
//        } else {
        if (managedRunner == null) { // normal, nothing to be done
        } else {
//            managedRunner.updateExposure();
            SharedStatics.orderCache.updateExposure(managedRunner);
        }
//        }
        final double matchedBackExposure = managedRunner == null ? 0d : managedRunner.getBackMatchedExposure();
        final double matchedLayExposure = managedRunner == null ? 0d : managedRunner.getLayMatchedExposure();
        final double totalBackExposure = managedRunner == null ? 0d : managedRunner.getBackTotalExposure();
        final double totalLayExposure = managedRunner == null ? 0d : managedRunner.getLayTotalExposure();
        final double runnerTotalValueTraded = marketRunner == null ? 0d : marketRunner.getTvEUR(Statics.existingFunds.currencyRate);
        final double lastTradedPrice = marketRunner == null ? 0d : marketRunner.getLtp();
        final TreeMap<Double, Double> availableToLay = marketRunner == null ? null : marketRunner.getAvailableToLay(Statics.existingFunds.currencyRate).getOrders();
        final TreeMap<Double, Double> availableToBack = marketRunner == null ? null : marketRunner.getAvailableToBack(Statics.existingFunds.currencyRate).getOrders();
//        final TreeMap<Double, Double> myUnmatchedLay = orderMarketRunner == null ? null : orderMarketRunner.getUnmatchedLayAmounts();
//        final TreeMap<Double, Double> myUnmatchedBack = orderMarketRunner == null ? null : orderMarketRunner.getUnmatchedBackAmounts();
        final TreeMap<Double, Double> myUnmatchedLay = SharedStatics.orderCache.getUnmatchedLayAmounts(marketId, runnerId).getOrders();
        final TreeMap<Double, Double> myUnmatchedBack = SharedStatics.orderCache.getUnmatchedBackAmounts(marketId, runnerId).getOrders();
        final Collection<TemporaryOrder> myTempOrders = SharedStatics.orderCache.getTempOrders(marketId, runnerId);
        final boolean mandatoryPlace = managedRunner != null && managedRunner.isMandatoryPlace(); // this appears both when the market and when the runner have this setting
        final PrefSide prefSide = managedRunner == null ? null : managedRunner.getPrefSide();

        final Label runnerNameLabel = new Label(runnerName == null ? "null" : runnerName);
        final TextField backAmountLimitNode = new TextField(decimalFormatTextField.format(Generic.keepDoubleWithinRange(backAmountLimit)));
        backAmountLimitNode.setMaxWidth(60);
        final TextField layAmountLimitNode = new TextField(decimalFormatTextField.format(Generic.keepDoubleWithinRange(layAmountLimit)));
        layAmountLimitNode.setMaxWidth(60);
        final TextField minBackOddsNode = new TextField(decimalFormatTextField.format(Generic.keepDoubleWithinRange(minBackOdds)));
        minBackOddsNode.setMaxWidth(40);
        final TextField maxLayOddsNode = new TextField(decimalFormatTextField.format(Generic.keepDoubleWithinRange(maxLayOdds)));
        maxLayOddsNode.setMaxWidth(40);
//        final Label matchedBackExposureLabel = new Label(String.valueOf(matchedBackExposure));
//        final Label matchedLayExposureLabel = new Label(String.valueOf(matchedLayExposure));
//        final Label totalBackExposureLabel = new Label(String.valueOf(totalBackExposure));
//        final Label totalLayExposureLabel = new Label(String.valueOf(totalLayExposure));
        final Label compositeExposureLabel = new Label("bExp:" + GUIUtils.decimalFormat(totalBackExposure) + "(" + GUIUtils.decimalFormat(matchedBackExposure) + ") lExp:" + GUIUtils.decimalFormat(totalLayExposure) + "(" +
                                                       GUIUtils.decimalFormat(matchedLayExposure) + ")");
        @SuppressWarnings("SpellCheckingInspection") final Label mandatoryPlaceLabel = new Label("Mand plac:");
        mandatoryPlaceLabel.setContentDisplay(ContentDisplay.RIGHT);
        @SuppressWarnings("NestedConditionalExpression") final Button prefSideButton = new Button(prefSide == PrefSide.BACK ? "Pref BACK" : prefSide == PrefSide.LAY ? "Pref LAY" : "Pref NONE");

        final Label runnerTotalValueTradedLabel = new Label("tv: €" + GUIUtils.decimalFormat(runnerTotalValueTraded));
        runnerTotalValueTradedLabel.setPadding(new Insets(0, 10, 0, 0));
        final Label lastTradedPriceLabel = new Label("ltp: " + decimalFormatLabelLow.format(lastTradedPrice));
        lastTradedPriceLabel.setPadding(new Insets(0, 10, 0, 0));
        final Label[] availableToBackLabel = new Label[N_PRICE_CELLS], availableToLayLabel = new Label[N_PRICE_CELLS];
        int counter = 0;
        if (availableToBack == null) { // nothing to be done
        } else {
            for (final Map.Entry<Double, Double> entry : availableToBack.entrySet()) {
                final Double price = entry.getKey();
                final Double size = entry.getValue();
                availableToBackLabel[counter] = new Label(decimalFormatLabelLow.format(price) + "\n€" + GUIUtils.decimalFormat(size) + "\n");
                availableToBackLabel[counter].setMinWidth(60);
                availableToBackLabel[counter].setMinHeight(36);
                GUIUtils.setCenterLabel(availableToBackLabel[counter]);
                counter++;
                if (counter >= N_PRICE_CELLS) {
                    break;
                }
            }
        }
        //noinspection ForLoopWithMissingComponent
        for (; counter < N_PRICE_CELLS; counter++) {
            availableToBackLabel[counter] = new Label("");
            availableToBackLabel[counter].setMinWidth(60);
            availableToBackLabel[counter].setMinHeight(36);
            GUIUtils.setCenterLabel(availableToBackLabel[counter]);
        }
        //noinspection ReuseOfLocalVariable
        counter = 0;
        if (availableToLay == null) { // nothing to be done
        } else {
            for (final Map.Entry<Double, Double> entry : availableToLay.entrySet()) {
                final Double price = entry.getKey();
                final Double size = entry.getValue();
                availableToLayLabel[counter] = new Label(decimalFormatLabelLow.format(price) + "\n€" + GUIUtils.decimalFormat(size));
                availableToLayLabel[counter].setMinWidth(60);
                availableToLayLabel[counter].setMinHeight(36);
                GUIUtils.setCenterLabel(availableToLayLabel[counter]);
                counter++;
                if (counter >= N_PRICE_CELLS) {
                    break;
                }
            }
        }
        //noinspection ForLoopWithMissingComponent
        for (; counter < N_PRICE_CELLS; counter++) {
            availableToLayLabel[counter] = new Label("");
            availableToLayLabel[counter].setMinWidth(60);
            availableToLayLabel[counter].setMinHeight(36);
            GUIUtils.setCenterLabel(availableToLayLabel[counter]);
        }

        GUIUtils.setOnKeyPressedTextField(marketId, backAmountLimitNode, backAmountLimit, RulesManagerModificationCommand.setBackAmountLimit, marketId, runnerId);
        GUIUtils.handleDoubleTextField(backAmountLimitNode, backAmountLimit);
        GUIUtils.setOnKeyPressedTextField(marketId, layAmountLimitNode, layAmountLimit, RulesManagerModificationCommand.setLayAmountLimit, marketId, runnerId);
        GUIUtils.handleDoubleTextField(layAmountLimitNode, layAmountLimit);
        GUIUtils.setOnKeyPressedTextFieldOdds(marketId, Side.B, minBackOddsNode, focusPosition, minBackOdds, RulesManagerModificationCommand.setMinBackOdds, marketId, runnerId);
        GUIUtils.handleDoubleTextField(minBackOddsNode, minBackOdds);
        GUIUtils.setOnKeyPressedTextFieldOdds(marketId, Side.L, maxLayOddsNode, focusPosition, maxLayOdds, RulesManagerModificationCommand.setMaxLayOdds, marketId, runnerId);
        GUIUtils.handleDoubleTextField(maxLayOddsNode, maxLayOdds);

        int columnIndex = 2;
        mainGridPane.add(runnerNameLabel, columnIndex++, runnerRowIndex);
        mainGridPane.add(isActive ? backAmountLimitNode : new Label("inactive"), columnIndex++, runnerRowIndex);
        focusGridNode(backAmountLimitNode, marketId, columnIndex - 1, runnerRowIndex);
        mainGridPane.add(isActive ? minBackOddsNode : new Label("inact"), columnIndex++, runnerRowIndex);
        focusGridNode(minBackOddsNode, marketId, columnIndex - 1, runnerRowIndex);

        for (int i = N_PRICE_CELLS - 1; i >= 0; i--) {
            mainGridPane.add(availableToBackLabel[i], columnIndex++, runnerRowIndex);
            if (i == 0) {
                availableToBackLabel[i].setId("BackColoredCell");
            } else {
                availableToBackLabel[i].setId("NotColoredPriceSizeCell");
            }
        }
        for (int i = 0; i < N_PRICE_CELLS; i++) {
            mainGridPane.add(availableToLayLabel[i], columnIndex++, runnerRowIndex);
            if (i == 0) {
                availableToLayLabel[i].setId("LayColoredCell");
            } else {
                availableToLayLabel[i].setId("NotColoredPriceSizeCell");
            }
        }
        mainGridPane.add(isActive ? maxLayOddsNode : new Label("inact"), columnIndex++, runnerRowIndex);
        focusGridNode(maxLayOddsNode, marketId, columnIndex - 1, runnerRowIndex);
        mainGridPane.add(isActive ? layAmountLimitNode : new Label("inactive"), columnIndex++, runnerRowIndex);
        focusGridNode(layAmountLimitNode, marketId, columnIndex - 1, runnerRowIndex);

        mainGridPane.add(lastTradedPriceLabel, columnIndex++, runnerRowIndex);
        mainGridPane.add(runnerTotalValueTradedLabel, columnIndex++, runnerRowIndex);
        mainGridPane.add(compositeExposureLabel, columnIndex++, runnerRowIndex);
        mainGridPane.add(isActive ? mandatoryPlaceLabel : new Label("inactive"), columnIndex++, runnerRowIndex);

        //noinspection StaticVariableUsedBeforeInitialization
        GUIUtils.standardCheckBoxFactory(mainStage, mandatoryPlaceLabel, marketId, runnerId, runnerName, mandatoryPlace, RulesManagerModificationCommand.setRunnerMandatoryPlace, "mandatoryPlace");

        mainGridPane.add(isActive ? prefSideButton : new Label("inactive"), columnIndex, runnerRowIndex);
        final ButtonType back = new ButtonType("BACK", ButtonBar.ButtonData.APPLY);
        final ButtonType lay = new ButtonType("LAY", ButtonBar.ButtonData.RIGHT);
        final ButtonType none = new ButtonType("NONE", ButtonBar.ButtonData.NO);
        prefSideButton.setOnAction(actionEvent -> {
            final Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Are you ok with this?", back, lay, none);
            GUIUtils.centerButtons(alert.getDialogPane());
            alert.setTitle("Confirmation Dialog");
            alert.setHeaderText("You are setting preferredSide on runner: " + runnerName + " (marketId:" + marketId + " " + runnerId + ")");
            alert.initModality(Modality.APPLICATION_MODAL);
            alert.initOwner(mainStage);
            mainGridPane.requestFocus();
            final Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent()) {
                if (result.get() == back) {
                    Statics.sslClientThread.sendQueue.add(new SerializableObjectModification<>(RulesManagerModificationCommand.setRunnerPrefSide, marketId, runnerId, PrefSide.BACK));
                } else if (result.get() == lay) {
                    Statics.sslClientThread.sendQueue.add(new SerializableObjectModification<>(RulesManagerModificationCommand.setRunnerPrefSide, marketId, runnerId, PrefSide.LAY));
                } else if (result.get() == none) {
                    Statics.sslClientThread.sendQueue.add(new SerializableObjectModification<>(RulesManagerModificationCommand.setRunnerPrefSide, marketId, runnerId, PrefSide.NONE));
                } else { // user closed the dialog
                }
            } else {
                logger.error("result not present during runner prefSide alert for: {} {} {} {}", prefSide, marketId, runnerName, runnerId);
            }
            alert.close();
        });
        prefSideButton.setOnMousePressed(actionEvent -> {
            final Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Are you ok with this?", back, lay, none);
            GUIUtils.centerButtons(alert.getDialogPane());
            alert.setTitle("Confirmation Dialog");
            alert.setHeaderText("You are setting preferredSide on runner: " + runnerName + " (marketId:" + marketId + " " + runnerId + ")");
            alert.initModality(Modality.APPLICATION_MODAL);
            alert.initOwner(mainStage);
            mainGridPane.requestFocus();
            final Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent()) {
                if (result.get() == back) {
                    Statics.sslClientThread.sendQueue.add(new SerializableObjectModification<>(RulesManagerModificationCommand.setRunnerPrefSide, marketId, runnerId, PrefSide.BACK));
                } else if (result.get() == lay) {
                    Statics.sslClientThread.sendQueue.add(new SerializableObjectModification<>(RulesManagerModificationCommand.setRunnerPrefSide, marketId, runnerId, PrefSide.LAY));
                } else if (result.get() == none) {
                    Statics.sslClientThread.sendQueue.add(new SerializableObjectModification<>(RulesManagerModificationCommand.setRunnerPrefSide, marketId, runnerId, PrefSide.NONE));
                } else { // user closed the dialog
                }
            } else {
                logger.error("result not present during runner prefSide alert for: {} {} {} {}", prefSide, marketId, runnerName, runnerId);
            }
            alert.close();
        });

        int currentAppendRowIndex = previousAppendRowIndex;

        for (final Map.Entry<Double, Double> entry : myUnmatchedBack.entrySet()) {
            mainGridPane.add(new Label(runnerName), 0, currentAppendRowIndex);
            mainGridPane.add(new Label("back: " + decimalFormatLabelLow.format(entry.getKey())), 1, currentAppendRowIndex);
            mainGridPane.add(new Label("€" + GUIUtils.decimalFormat(entry.getValue())), 2, currentAppendRowIndex++);
        }
        for (final Map.Entry<Double, Double> entry : myUnmatchedLay.entrySet()) {
            mainGridPane.add(new Label(runnerName), 0, currentAppendRowIndex);
            mainGridPane.add(new Label("lay:  " + decimalFormatLabelLow.format(entry.getKey())), 1, currentAppendRowIndex);
            mainGridPane.add(new Label("€" + GUIUtils.decimalFormat(entry.getValue())), 2, currentAppendRowIndex++);
        }
        for (@NotNull final TemporaryOrder temporaryOrder : myTempOrders) {
            final TemporaryOrderType temporaryOrderType = temporaryOrder.getType();
            if (temporaryOrderType == TemporaryOrderType.PLACE) {
                mainGridPane.add(new Label(runnerName), 0, currentAppendRowIndex);
                mainGridPane.add(new Label("temp" + temporaryOrder.getSide() + ": " + decimalFormatLabelLow.format(temporaryOrder.getPrice())), 1, currentAppendRowIndex);
                mainGridPane.add(new Label("€" + GUIUtils.decimalFormat(temporaryOrder.getSize())), 2, currentAppendRowIndex++);
            } else {
                mainGridPane.add(new Label(runnerName), 0, currentAppendRowIndex);
                mainGridPane.add(new Label("cancel" + temporaryOrder.getSide() + ": " + decimalFormatLabelLow.format(temporaryOrder.getPrice())), 1, currentAppendRowIndex);
                final Double sizeReduction = temporaryOrder.getSizeReduction();
                mainGridPane.add(new Label("€" + (sizeReduction == null ? "max" : GUIUtils.decimalFormat(sizeReduction))), 2, currentAppendRowIndex++);
            }
        }

        return currentAppendRowIndex;
    }

    static void pauseRefresh() {
        refreshPaused.set(true);
        mainGridPane.setStyle("-fx-background-color: brown;");
    }

    static void resumeRefresh() {
        mainGridPane.setStyle("-fx-background-color: black;");
        refreshPaused.set(false);
        if (refreshQueued.getAndSet(false)) {
            refreshDisplayedManagedObject();
        } else { // no need to refresh
        }
    }

    private static void focusGridNode(final TextField textField, final String id, final int columnIndex, final int rowIndex) { // field needs to be visibile for focus to work
        if (textField == null) {
            logger.error("null node in focusGridNode: {} {}", columnIndex, rowIndex);
        } else if (focusPosition.positionEquals(id, columnIndex, rowIndex)) {
            textField.requestFocus();
            textField.positionCaret(focusPosition.getCaretPosition());
//            focusPosition.reset();
        }
    }

    private static void updateEventName(final TreeItem<String> currentEvent, final String eventId, @NotNull final ManagedEvent managedEvent) {
        if (currentEvent == null) {
            logger.error("null event in updateEventName");
        } else {
            final String existingName = currentEvent.getValue();
            final String rawNewName = GUIUtils.getManagedEventName(managedEvent);
            final String newName = GUIUtils.eventExistsInMapAndNotMarkedForRemoval(eventId) ? GUIUtils.markNameAsNotExpired(rawNewName) : GUIUtils.markNameAsExpired(rawNewName);

            if (Objects.equals(existingName, newName)) { // no need for update
            } else {
                if (newName == null) {
                    logger.error("null newName in updateEventName for: {} {}", existingName, currentEvent);
                } else {
                    currentEvent.setValue(newName);
                    leftTreeView.refresh();
                }
            }
        }
    }

    private static void updateMarketName(final TreeItem<String> currentMarket, final String marketId, @NotNull final ManagedMarket managedMarket) {
        if (currentMarket == null) {
            logger.error("null market in updateMarketName");
        } else {
            final String existingName = currentMarket.getValue();
            final String rawNewName = GUIUtils.getManagedMarketName(marketId, managedMarket);
            final String newName = GUIUtils.marketExistsInMapAndNotMarkedForRemoval(marketId) ? GUIUtils.markNameAsNotExpired(rawNewName) : GUIUtils.markNameAsExpired(rawNewName);

            if (Objects.equals(existingName, newName)) { // no need for update
            } else {
                if (newName == null) {
                    logger.error("null newName in updateMarketName for: {} {}", existingName, currentMarket);
                } else {
                    currentMarket.setValue(newName);
//                    GUIUtils.triggerTreeItemRefresh(currentMarket);
                    leftTreeView.refresh();
                }
            }
        }
    }

    private static void showManagedMarket(final String marketId, final TreeItem<String> currentMarket) {
        showManagedMarket(marketId, currentMarket, false);
    }

    @SuppressWarnings({"OverlyLongMethod", "ValueOfIncrementOrDecrementUsed"})
    private static void showManagedMarket(final String marketId, final TreeItem<String> currentMarket, final boolean nonExistentManagedObjectIsAcceptable) {
        if (refreshPaused.get()) { // refresh is paused, refresh will happen when pause ends
            refreshQueued.set(true);
        } else {
            currentlyShownManagedObject = currentMarket;
            mainGridPane.getChildren().clear();
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
                if (nonExistentManagedObjectIsAcceptable) {
                    logger.info("null managedMarket in left listener for: {} {}", currentMarket, marketId);
                } else {
                    logger.error("null managedMarket in left listener for: {} {}", currentMarket, marketId);
                }
            } else {
                final MarketCatalogue marketCatalogue = Statics.marketCataloguesMap.get(marketId);
                final Market cachedMarket = SharedStatics.marketCache.getMarket(marketId);
                final MarketDefinition marketDefinition = cachedMarket == null ? null : cachedMarket.getMarketDefinition();
                updateMarketName(currentMarket, marketId, managedMarket);
                final String marketName = currentMarket.getValue();
                final TreeItem<String> parentItem = currentMarket.getParent();
                final String eventName = parentItem == null ? null : parentItem.getValue();
                final double simpleAmountLimit = managedMarket.getSimpleAmountLimit();
                final double maxMarketLimit = managedMarket.getMaxMarketLimit(Statics.existingFunds);
                final double calculatedAmountLimit = managedMarket.simpleGetCalculatedLimit();
                final double marketTotalValueTraded = cachedMarket == null ? -1d : cachedMarket.getTvEUR(Statics.existingFunds.currencyRate);
                final Date marketLiveTime = marketDefinition == null ? null : marketDefinition.getMarketTime();
                final Date marketOpenTime = marketDefinition == null ? null : marketDefinition.getOpenDate();
                final int nTotalRunners = cachedMarket == null ? -1 : cachedMarket.getNRunners();
                final int nActiveRunners = cachedMarket == null ? -1 : cachedMarket.getNActiveRunners();
                final int nManagedRunners = managedMarket.getNRunners();
                final boolean isEnabled = managedMarket.isEnabledMarket();
                final boolean mandatoryPlace = managedMarket.isMandatoryPlace();
                final boolean keepAtInPlay = managedMarket.isKeepAtInPlay();

                final Label eventNameLabel = new Label(eventName);
                final Label marketNameLabel = new Label(marketName);
                final Hyperlink marketIdLink = new Hyperlink(marketId);
                final String webLink = "https://www.betfair.ro/exchange/plus/market/" + marketId;
                final EventHandler<ActionEvent> linkEvent = event -> getInstance().getHostServices().showDocument(webLink);
                marketIdLink.setOnAction(linkEvent);
                final TextField simpleAmountLimitNode = new TextField(decimalFormatTextField.format(Generic.keepDoubleWithinRange(simpleAmountLimit)));
                final Label maxMarketLimitLabel = new Label("€ " + decimalFormatLabelLow.format(maxMarketLimit));
                final Label calculatedAmountLimitLabel = new Label("€ " + decimalFormatLabelLow.format(calculatedAmountLimit));
                final Label marketTotalValueTradedLabel = new Label("€ " + decimalFormatLabelLow.format(marketTotalValueTraded));

                if (marketLiveTime == null) {
                    marketLiveCounterLabel.setText("null");
                    timeline.stop();
                } else {
                    liveTime.set(marketLiveTime.getTime());
                    liveCounterEventHandler.handle(null);
                    timeline.play();
                }

                final Label marketLiveTimeLabel = new Label(marketLiveTime == null ? null : marketLiveTime.toString());
                final Label marketOpenTimeLabel = new Label(marketOpenTime == null ? null : marketOpenTime.toString());
                final Label nTotalRunnersLabel = new Label(String.valueOf(nTotalRunners));
                final Label nActiveRunnersLabel = new Label(String.valueOf(nActiveRunners));
                final Label nManagedRunnersLabel = new Label(String.valueOf(nManagedRunners));

                @NotNull final CheckBox isEnabledCheckBox = GUIUtils.standardCheckBoxFactory(mainStage, marketIsEnabledLabel, eventName, marketId, marketName, isEnabled, RulesManagerModificationCommand.setMarketEnabled, "");
                isEnabledCheckBox.setStyle("box-color: red;"); // color for unselected checkBox
                GUIUtils.standardCheckBoxFactory(mainStage, marketMandatoryPlaceLabel, eventName, marketId, marketName, mandatoryPlace, RulesManagerModificationCommand.setMarketMandatoryPlace, "mandatoryPlace");
//                GUIUtils.standardCheckBoxFactory(mainStage, marketKeepAtInPlayLabel, eventName, marketId, marketName, keepAtInPlay, RulesManagerModificationCommand.setMarketKeepAtInPlay, "keepAtInPlay");

                GUIUtils.setOnKeyPressedTextField(marketId, simpleAmountLimitNode, simpleAmountLimit, RulesManagerModificationCommand.setMarketAmountLimit, marketId);
                GUIUtils.handleDoubleTextField(simpleAmountLimitNode, simpleAmountLimit);

                int rowIndex = 0;
                mainGridPane.add(eventNamePreLabel, 0, rowIndex);
                mainGridPane.add(eventNameLabel, 1, rowIndex++);
                mainGridPane.add(marketNamePreLabel, 0, rowIndex);
                mainGridPane.add(marketNameLabel, 1, rowIndex++);
                mainGridPane.add(marketIdPreLabel, 0, rowIndex);
                mainGridPane.add(marketIdLink, 1, rowIndex++);
                mainGridPane.add(simpleAmountLimitLabel, 0, rowIndex);
                mainGridPane.add(simpleAmountLimitNode, 1, rowIndex++);
                focusGridNode(simpleAmountLimitNode, marketId, 1, rowIndex - 1);

                mainGridPane.add(calculatedAmountLimitPreLabel, 0, rowIndex);
                mainGridPane.add(calculatedAmountLimitLabel, 1, rowIndex++);
                mainGridPane.add(maxMarketLimitPreLabel, 0, rowIndex);
                mainGridPane.add(maxMarketLimitLabel, 1, rowIndex++);
                mainGridPane.add(marketTotalValueTradedPreLabel, 0, rowIndex);
                mainGridPane.add(marketTotalValueTradedLabel, 1, rowIndex++);
                mainGridPane.add(marketLiveCounterPreLabel, 0, rowIndex);
                mainGridPane.add(marketLiveCounterLabel, 1, rowIndex++);
                mainGridPane.add(marketLiveTimePreLabel, 0, rowIndex);
                mainGridPane.add(marketLiveTimeLabel, 1, rowIndex++);
                mainGridPane.add(marketOpenTimePreLabel, 0, rowIndex);
                mainGridPane.add(marketOpenTimeLabel, 1, rowIndex++);
                mainGridPane.add(nTotalRunnersPreLabel, 0, rowIndex);
                mainGridPane.add(nTotalRunnersLabel, 1, rowIndex++);
                mainGridPane.add(nActiveRunnersPreLabel, 0, rowIndex);
                mainGridPane.add(nActiveRunnersLabel, 1, rowIndex++);
                mainGridPane.add(nManagedRunnersPreLabel, 0, rowIndex);
                mainGridPane.add(nManagedRunnersLabel, 1, rowIndex++);
                mainGridPane.add(marketMandatoryPlaceLabel, 0, rowIndex++);
//                mainGridPane.add(marketKeepAtInPlayLabel, 0, rowIndex++);
                mainGridPane.add(marketIsEnabledLabel, 0, rowIndex++);

                int runnerIndex = 0;
                @NotNull final LinkedHashMap<RunnerId, ManagedRunner> managedRunners = managedMarket.getRunners(Statics.rulesManager, Statics.marketCataloguesMap);
                final HashMap<RunnerId, MarketRunner> allRunners = cachedMarket == null ? null : cachedMarket.getMarketRunners();
                for (@NotNull final Map.Entry<RunnerId, ManagedRunner> entry : managedRunners.entrySet()) {
                    final RunnerId runnerId = entry.getKey();
                    final ManagedRunner managedRunner = entry.getValue();
                    final MarketRunner marketRunner = allRunners == null ? null : allRunners.get(runnerId);
                    rowIndex = showRunner(marketId, runnerId, managedRunner, marketRunner, marketCatalogue, cachedMarket, rowIndex, runnerIndex++);
                }
                if (allRunners == null) { // no marketRunners list present, nothing to be done
                } else {
                    for (@NotNull final Map.Entry<RunnerId, MarketRunner> entry : allRunners.entrySet()) {
                        final RunnerId runnerId = entry.getKey();
                        if (managedRunners.containsKey(runnerId)) { // I already parsed this in the previous for loop, nothing to be done here
                        } else { // non managed runner
                            final MarketRunner marketRunner = entry.getValue();
                            rowIndex = showRunner(marketId, runnerId, null, marketRunner, marketCatalogue, cachedMarket, rowIndex, runnerIndex++);
                        }
                    } // end for
                }

//            if (focusedColumnIndex.get() >= 0 && focusedRowIndex.get() >= 0) {
//                final Node focusedNode = GUIUtils.getNodeByRowColumnIndex(mainGridPane, focusedColumnIndex.getAndSet(-1), focusedRowIndex.getAndSet(-1));
//                if (focusedNode == null) { // nothing was focused, nothing to be done
//                } else {
//                    focusedNode.requestFocus();
//                }
//            }
            }
        }
    }

    @SuppressWarnings({"OverlyLongMethod", "OverlyComplexMethod"})
    @Override
    public void start(@NotNull final Stage primaryStage) {
        primaryStage.setOnShown(event -> primaryStageIsShown.set(true));

        //noinspection AssignmentToStaticFieldFromInstanceMethod
        mainStage = primaryStage;
        primaryStage.setTitle("Betty Interface");
        primaryStage.setHeight(2160);
        primaryStage.setWidth(3840);
        primaryStage.setMaximized(true);

        @NotNull final ObservableList<Screen> screens = Screen.getScreens();
        final int nScreens = screens.size();
        @NotNull final Screen chosenScreen;
        if (nScreens == 2) {
            chosenScreen = screens.get(1);
        } else {
//            logger.error("wrong nScreens in GUI initial window creation: {}", nScreens);
            chosenScreen = Screen.getPrimary();
        }
        @NotNull final Rectangle2D screenBounds = chosenScreen.getBounds();
        primaryStage.setX(screenBounds.getMinX());
        primaryStage.setY(screenBounds.getMinY());

        final VBox rootVBox = new VBox();
        final Scene mainScene = new Scene(rootVBox, Color.BLACK);
        mainScene.setFill(Color.BLACK);
        mainScene.setCursor(Cursor.CROSSHAIR);
        mainScene.getStylesheets().add("GUI.css");

        primaryStage.setScene(mainScene);

        final HBox topBar = new HBox();
        @NotNull final ObservableList<Node> rootNodesList = rootVBox.getChildren(), topBarNodesList = topBar.getChildren();
        rootNodesList.addAll(topBar, mainSplitPane);
        final VBox leftVBox = new VBox(leftTreeView);
        mainSplitPaneNodesList.addAll(leftVBox, mainScrollPane);

        // totalFunds € {} available € {} reserve € {} exposure € {}
        final Label fixedTotalFundsLabel = new Label("Total Funds "), fixedAvailableLabel = new Label(" Available "), fixedReserveLabel = new Label(" out of which safety reserve "), fixedExposureLabel = new Label(" Exposure ");

        final Pane hugeSpacer = new Pane();
        HBox.setHgrow(hugeSpacer, Priority.ALWAYS);

        refreshEventsButton.setMnemonicParsing(true);
        refreshEventsButton.setOnAction(event -> {
            refreshEventsButton.setDisable(true);
            Statics.sslClientThread.sendQueue.add(new SerializableObjectModification<>(SynchronizedMapModificationCommand.refresh, Event.class)); // send command to refresh event list

            @SuppressWarnings("AnonymousInnerClassMayBeStatic") final TimerTask task = new TimerTask() {
                public void run() {
                    addCommand(PlatformRunLaterEnum.refreshEventsButtonSetDisable);
                }
            };
            Statics.timer.schedule(task, 5_000L);
        });

        filterTextField.setPromptText("Enter filter text ...");
        filterTextField.setStyle("-fx-background-color: black;");
        GUIUtils.setOnKeyPressedFilterTextField(filterTextField, rightEventTreeRoot);
        rightVBox.getChildren().add(filterTextField);

        final Button rightPaneButton = new Button("Show events");
        rightPaneButton.setMnemonicParsing(true);
        rightPaneButton.setOnAction(event -> {
            if (rightPanelVisible) {
                //noinspection AssignmentToStaticFieldFromInstanceMethod
                GUI.rightPanelVisible = false;
                processMarketsForRemoval();
                processEventsForRemoval();
                mainSplitPaneNodesList.remove(rightVBox);
                topBarNodesList.remove(refreshEventsButton);
                rightPaneButton.setText("Show events");
            } else {
                //noinspection AssignmentToStaticFieldFromInstanceMethod
                GUI.rightPanelVisible = true; // needs to be in the beginning, else the following methods that rely on condition "rightPanelVisible is true" will not work

                refreshEventsButton.fire();

                mainSplitPaneNodesList.add(rightVBox);

//                mainSplitPane.setDividerPositions(.09, .91);
//                mainSplitPane.setDividerPosition(1, .91d);
                topBarNodesList.add(topBarNodesList.size() - 1, refreshEventsButton);
                rightPaneButton.setText("Hide events");
                initializeRightTreeView();

                filterTextField.requestFocus();
                filterTextField.end();
            }
            event.consume();
        });

        topBarNodesList.addAll(fixedTotalFundsLabel, totalFundsLabel, fixedAvailableLabel, availableLabel, fixedReserveLabel, reserveLabel, fixedExposureLabel, exposureLabel, hugeSpacer, rightPaneButton);
        mainScene.addMnemonic(new Mnemonic(rightPaneButton, new KeyCodeCombination(KeyCode.E, KeyCombination.ALT_DOWN))); // mnemonic added manually because when added automatically the textField doesn't get focus

//        rightTreeView.setOnMouseClicked(ae -> {
//            @NotNull final TreeItem<String> selectedItem = rightTreeView.getSelectionModel().getSelectedItem();
//            if (selectedItem == null) {
//                logger.info("likely not clicked on a tree member, null selectedItem in rightTreeView setOnMouseClicked");
//            } else {
//                selectedItem.setExpanded(true);
//                final String eventId = eventsTreeItemMap.getKey(selectedItem);
//                if (eventId == null) {
//                    final String marketId = marketsTreeItemMap.getKey(selectedItem);
//                    if (marketId == null) {
//                        if (Objects.equals(selectedItem.getValue(), DEFAULT_EVENT_NAME)) { // I have clicked the default event, normal behavior, nothing to be done
//                        } else {
//                            logger.error("null eventId and marketId in selected right treeItem: {}", selectedItem);
//                        }
//                    } else {
//                        Statics.sslClientThread.sendQueue.add(new SerializableObjectModification<>(SynchronizedMapModificationCommand.getMarkets, MarketCatalogue.class, marketId)); // send command to refresh market
//                    }
//                } else {
//                    final Event event = Statics.eventsMap.get(eventId);
//                    if (event == null) {
//                        logger.error("null event for eventId {} in selected right treeItem: {}", eventId, selectedItem);
//                    } else {
//                        Statics.sslClientThread.sendQueue.add(new SerializableObjectModification<>(SynchronizedMapModificationCommand.getMarkets, Event.class, event)); // send command to get markets for the selected event
//                    }
//                }
//            }
//        });

        rightTreeView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null) { // not needed to fill the logs; if the treeView doesn't fill all the height, and I click somewhere below, where there are no members, this error appears every time
//                logger.info("likely not clicked on a tree member, null newValue in rightTreeView addListener, oldValue: {}", oldValue);
            } else {
                newValue.setExpanded(true);
                final String eventId = eventsTreeItemMap.getKey(newValue);
                @Nullable final String marketId;
                if (eventId == null) {
                    marketId = marketsTreeItemMap.getKey(newValue);
                    if (marketId == null) {
                        if (Objects.equals(newValue.getValue(), DEFAULT_EVENT_NAME)) { // I have clicked the default event, normal behavior, nothing to be done
                        } else {
                            logger.error("null eventId and marketId in selected right treeItem: {}", newValue);
                        }
                    } else {
                        Statics.sslClientThread.sendQueue.add(new SerializableObjectModification<>(SynchronizedMapModificationCommand.getMarkets, MarketCatalogue.class, marketId)); // send command to refresh market
                    }
                } else {
                    marketId = null;
                    final Event event = Statics.eventsMap.get(eventId);
                    if (event == null) {
                        logger.error("null event for eventId {} in selected right treeItem: {}", eventId, newValue);
                    } else {
                        Statics.sslClientThread.sendQueue.add(new SerializableObjectModification<>(SynchronizedMapModificationCommand.getMarkets, Event.class, event)); // send command to get markets for the selected event
                    }
                }
//                Generic.threadSleepSegmented(1_000L, 100L, Statics.mustStop);
                final String filterTextValue = filterTextField.getText();
                if (filterTextValue == null || filterTextValue.isEmpty()) { // nothing to be done
                } else {
//                    filterTextField.requestFocus();
//                    filterTextField.end();
//                    while (!filterTextField.getText().isEmpty()) {
////                        filterTextField.deletePreviousChar();
//                        final KeyEvent pressBackSpace = new KeyEvent(null, filterTextField, KeyEvent.KEY_PRESSED, "", "", KeyCode.BACK_SPACE, false, false, false, false);
//                        filterTextField.fireEvent(pressBackSpace);
//                    }

                    final String oldMarketId = marketsTreeItemMap.getKey(oldValue);
                    if (oldMarketId == null) {
                        filterTextField.clear();
                        GUIUtils.setFilterPredicate(filterTextField, rightEventTreeRoot);
                    } else { // was market, I won't reset filter
                    }

//                    @Nullable final TreeItem<String> selectedItem;
                    if (eventId == null) {
                        if (marketId == null) {
//                            selectedItem = null; // error message was printed previously
                        } else {
//                            selectedItem = marketsTreeItemMap.get(marketId);
                        }
                    } else {
//                        selectedItem = eventsTreeItemMap.get(eventId);

                        final SelectionModel<TreeItem<String>> selectionModel = rightTreeView.getSelectionModel();
                        selectionModel.select(eventsTreeItemMap.get(eventId));
                        rightTreeView.scrollTo(selectionModel.getSelectedIndex());
                    }
                }
            }
        });

        final MenuItem entryRight1 = new MenuItem("_Add Managed Object");
        entryRight1.setMnemonicParsing(true);
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
                    SharedStatics.threadPoolExecutor.submit(() -> Utils.createManagedEvent(eventId));
                }
            } else {
                @Nullable final String eventId;
                if (parentNode == null) {
                    logger.error("null parentNode in entryRight for marketId {} treeItem: {}", marketId, treeItem);
                    eventId = null;
                } else {
                    eventId = eventsTreeItemMap.getKey(parentNode);
                }
                SharedStatics.threadPoolExecutor.submit(() -> Utils.createManagedMarket(marketId, eventId));
            }
        });
        final MenuItem entryRight2 = new MenuItem("Object in _Browser");
        entryRight2.setMnemonicParsing(true);
        entryRight2.setOnAction(ae -> {
            @NotNull final TreeItem<String> treeItem = rightTreeView.getSelectionModel().getSelectedItem();
            final String marketId = marketsTreeItemMap.getKey(treeItem);
            final TreeItem<String> parentNode = treeItem.getParent();
            if (marketId == null) {
                final String eventId = eventsTreeItemMap.getKey(treeItem);
                if (eventId == null) {
                    if (Objects.equals(treeItem.getValue(), DEFAULT_EVENT_NAME)) { // I have clicked the default event, normal behavior, nothing to be done
                    } else {
                        logger.error("null marketId and eventId in entryRight2 for: {} {} {}", Objects.equals(parentNode, rightEventTreeRoot), treeItem, parentNode);
                    }
                } else {
                    final String webLink = "https://www.betfair.ro/exchange/plus/football/event/" + eventId;
                    getInstance().getHostServices().showDocument(webLink);
                }
            } else {
//                @Nullable final String eventId;
                if (parentNode == null) {
                    logger.error("null parentNode in entryRight2 for marketId {} treeItem: {}", marketId, treeItem);
//                    eventId = null;
                } else { // no error
//                    eventId = eventsTreeItemMap.getKey(parentNode);
                }
                final String webLink = "https://www.betfair.ro/exchange/plus/market/" + marketId;
                getInstance().getHostServices().showDocument(webLink);
            }
        });
        final ContextMenu rightContextMenu = new ContextMenu(entryRight1, entryRight2);
        rightContextMenu.setOnShowing(ae -> {
            @NotNull final TreeItem<String> treeItem = rightTreeView.getSelectionModel().getSelectedItem();
            if (marketsTreeItemMap.containsValue(treeItem)) {
                entryRight1.setText("_Add Managed Market");
                entryRight2.setText("Market in _Browser");
            } else if (eventsTreeItemMap.containsValue(treeItem)) {
                entryRight1.setText("_Add Managed Event");
                entryRight2.setText("Event in _Browser");
            } else {
                logger.error("treeItem not contained in markets nor events maps in rightContextMenu: {} {}", treeItem, treeItem == null ? null : treeItem.getParent());
                entryRight1.setText("_Add Managed Object");
                entryRight2.setText("Object in _Browser");
            }
        });
        rightTreeView.setContextMenu(rightContextMenu);

        final MenuItem entryLeft1 = new MenuItem("_Remove Managed Object");
        entryLeft1.setMnemonicParsing(true);
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
                    SharedStatics.threadPoolExecutor.submit(() -> Utils.removeManagedEvent(eventId));
                }
            } else {
                SharedStatics.threadPoolExecutor.submit(() -> Utils.removeManagedMarket(marketId));
            }
        });
        final MenuItem entryLeft2 = new MenuItem("Object in _Browser");
        entryLeft2.setMnemonicParsing(true);
        entryLeft2.setOnAction(ae -> {
            @NotNull final TreeItem<String> treeItem = leftTreeView.getSelectionModel().getSelectedItem();
            final String marketId = managedMarketsTreeItemMap.getKey(treeItem);
            final TreeItem<String> parentNode = treeItem.getParent();
            if (marketId == null) {
                final String eventId = managedEventsTreeItemMap.getKey(treeItem);
                if (eventId == null) {
                    if (Objects.equals(treeItem.getValue(), DEFAULT_EVENT_NAME)) { // I have clicked the default event, normal behavior, nothing to be done
                    } else {
                        logger.error("null marketId and eventId in entryLeft2 for: {} {} {}", Objects.equals(parentNode, leftEventTreeRoot), treeItem, parentNode);
                    }
                } else {
                    final String webLink = "https://www.betfair.ro/exchange/plus/football/event/" + eventId;
                    getInstance().getHostServices().showDocument(webLink);
                }
            } else {
                if (parentNode == null) {
                    logger.error("null parentNode in entryLeft2 for marketId {} treeItem: {}", marketId, treeItem);
                } else { // no error
                }
                final String webLink = "https://www.betfair.ro/exchange/plus/market/" + marketId;
                getInstance().getHostServices().showDocument(webLink);
            }
        });
        final ContextMenu leftContextMenu = new ContextMenu(entryLeft1, entryLeft2);
        leftContextMenu.setOnShowing(ae -> {
            @NotNull final TreeItem<String> treeItem = leftTreeView.getSelectionModel().getSelectedItem();
            if (managedMarketsTreeItemMap.containsValue(treeItem)) {
                entryLeft1.setText("_Remove Managed Market");
                entryLeft2.setText("Market in _Browser");
            } else if (managedEventsTreeItemMap.containsValue(treeItem)) {
                entryLeft1.setText("_Remove Managed Event");
                entryLeft2.setText("Event in _Browser");
            } else {
                logger.error("treeItem not contained in markets nor events maps in leftContextMenu: {} {}", treeItem, treeItem == null ? null : treeItem.getParent());
                entryLeft1.setText("_Remove Managed Object");
                entryLeft2.setText("Object in _Browser");
            }
        });
        leftTreeView.setContextMenu(leftContextMenu);

        leftTreeView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null) { // not needed to fill the logs; if the treeView doesn't fill all the height, and I click somewhere below, where there are no members, this error appears every time
//                logger.info("likely not clicked on a tree member, null newValue in leftTreeView addListener, oldValue: {}", oldValue);
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

        leftTreeView.setPrefHeight(2160);
        leftVBox.setMaxWidth(300);
        SplitPane.setResizableWithParent(leftVBox, false);
        rightVBox.setMaxWidth(300);
        SplitPane.setResizableWithParent(rightVBox, false);
        rightTreeView.setPrefHeight(2160);

        mainScene.addEventFilter(KeyEvent.KEY_PRESSED, (KeyEvent event) -> {
            // key pressed
            if (event.getCode() == KeyCode.F2) {
                if (rightPanelVisible) {
                    filterTextField.requestFocus();
                    filterTextField.end();
                } else { // won't focus if the panel is not visible, nothing to be done
                }
//                event.consume();
            }
        });

//        mainScene.setOnKeyPressed(event -> {
//            if (event.getCode() == KeyCode.F2) {
//                if (rightPanelVisible) {
//                    filterTextField.requestFocus();
//                    filterTextField.end();
//                } else { // won't focus if the panel is not visible, nothing to be done
//                }
//                event.consume();
//            }
//        });

        rootVBox.requestFocus(); // removes focus from some element that was unwantedly focused by default

        primaryStage.show();
        primaryStage.toFront();

        //        mainSplitPane.setDividerPositions(.09, .91);
//        mainSplitPane.setDividerPosition(0, .09d);

        Statics.scheduledThreadPoolExecutor.scheduleAtFixedRate(GUI::checkTreeItemsWithNullName, 1L, 1L, TimeUnit.MINUTES);

        primaryStage.setOnCloseRequest(event -> {
            final StringBuilder contentText = GUIUtils.getCloseWindowContentText();
            if (contentText.isEmpty()) { // no alert to show
            } else {
                final Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Application Close");
                alert.setContentText(contentText.toString());
                alert.initModality(Modality.APPLICATION_MODAL);
                alert.initOwner(primaryStage);
                alert.setResizable(true);
                alert.getDialogPane().setPrefWidth(1920);
                alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
                alert.getDialogPane().setMinWidth(Region.USE_PREF_SIZE);
                alert.showAndWait();
            }
        });

        logger.info("GUI has started");
//        hasStarted.countDown();
    }
}
