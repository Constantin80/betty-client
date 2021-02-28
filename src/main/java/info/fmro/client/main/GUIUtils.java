package info.fmro.client.main;

import info.fmro.client.objects.FocusPosition;
import info.fmro.client.objects.Statics;
import info.fmro.shared.entities.Event;
import info.fmro.shared.entities.MarketCatalogue;
import info.fmro.shared.entities.MarketDescription;
import info.fmro.shared.enums.RulesManagerModificationCommand;
import info.fmro.shared.logic.ManagedEvent;
import info.fmro.shared.logic.ManagedMarket;
import info.fmro.shared.logic.ManagedRunner;
import info.fmro.shared.objects.SharedStatics;
import info.fmro.shared.stream.cache.market.Market;
import info.fmro.shared.stream.cache.order.OrderMarket;
import info.fmro.shared.stream.definitions.MarketDefinition;
import info.fmro.shared.stream.enums.Side;
import info.fmro.shared.stream.objects.RunnerId;
import info.fmro.shared.stream.objects.SerializableObjectModification;
import info.fmro.shared.utility.Formulas;
import info.fmro.shared.utility.Generic;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TreeItem;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.text.TextAlignment;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

@SuppressWarnings({"UtilityClass", "OverlyComplexClass", "unused"})
final class GUIUtils {
    private static final Logger logger = LoggerFactory.getLogger(GUIUtils.class);
    public static final String EXPIRED_MARKER = "zzz ";

    private GUIUtils() {
    }

    @Nullable
    static String getManagedMarketName(@NotNull final ManagedMarket managedMarket) {
        final String marketId = managedMarket.getMarketId();
        return getManagedMarketName(marketId, managedMarket);
    }

    @Nullable
    static String getManagedMarketName(final String marketId, @NotNull final ManagedMarket managedMarket) {
        @Nullable String name = managedMarket.simpleGetMarketName();
        if (name == null) {
            name = managedMarket.getMarketName(Statics.rulesManager, Statics.marketCataloguesMap);
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
                    final Market market = managedMarket.getMarket(Statics.rulesManager, Statics.marketCataloguesMap);
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
            name = managedEvent.getEventName(Statics.eventsMap, Statics.rulesManager.listOfQueues);
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
            Statics.marketCataloguesMap.superRemoveValueAll(null);
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

    static boolean eventExistsInMapAndNotMarkedForRemoval(final String eventId) {
        return eventExistsInMap(eventId) && !GUI.toBeRemovedEventIds.contains(eventId);
    }

    static boolean marketExistsInMap(final String marketId) {
        return Statics.marketCataloguesMap.containsKey(marketId);
    }

    static boolean marketExistsInMapAndNotMarkedForRemoval(final String marketId) {
        return marketExistsInMap(marketId) && !GUI.toBeRemovedMarketIds.contains(marketId);
    }

    @Nullable
    static String markNameAsExpired(final String name) {
        @Nullable final String result;
        if (name == null) {
            logger.info("null name in markNameAsExpired");
            result = null;
        } else {
            // logger.error("name already marked as expired: {}", name); // normal behavior, no need to print anything
            result = nameIsMarkedAsExpired(name) ? name : addExpiredMarker(name);
        }
//        logger.info("expired: {} -> {}", name, result);
        return result;
    }

    @Nullable
    static String markNameAsNotExpired(final String name) {
        @Nullable final String result;
        if (name == null) {
            logger.info("null name in markNameAsNotExpired");
            result = null;
        } else {
            if (nameIsMarkedAsExpired(name)) {
                result = removeExpiredMarker(name);
            } else {
//                logger.info("name not marked as expired in markNameAsNotExpired: {}", name);
                result = name;
            }
        }
//        logger.info("notExpired: {} -> {}", name, result);
        return result;
    }

    private static boolean nameIsMarkedAsExpired(@NotNull final String name) {
        return name.startsWith(EXPIRED_MARKER);
    }

    @NotNull
    @Contract(value = "_ -> new", pure = true)
    private static String addExpiredMarker(@NotNull final String name) {
        return new String(EXPIRED_MARKER + name);
    }

    @NotNull
    @Contract("_ -> new")
    private static String removeExpiredMarker(@NotNull final String name) {
        return new String(name.substring(EXPIRED_MARKER.length()));
    }

    static void setFilterPredicate(@NotNull final TextField textField, @NotNull final TreeItem<String> eventTreeRoot) {
        final String filterText = textField.getText();
        final String currentFilterValue = GUI.currentFilterValue.getAndSet(filterText);
//        logger.info ("filterText: {} currentFilterValue: {}", filterText, currentFilterValue);
        if (Objects.equals(filterText, currentFilterValue)) { // won't change the predicate to the same value, nothing to be done
        } else {
            GUI.rightPanelVisible = false;
            GUI.clearRightTreeView();
//            eventTreeRoot.predicateProperty().set(TreeItemPredicate.create(actor -> filterText == null || filterText.isEmpty() || StringUtils.containsIgnoreCase(actor.toString(), filterText)));
            GUI.rightPanelVisible = true;
            GUI.initializeRightTreeView(true);
        }
    }

    static void setOnKeyPressedFilterTextField(@NotNull final TextField textField, @NotNull final TreeItem<String> eventTreeRoot) {
        textField.setOnKeyPressed(ae -> {
            if (ae.getCode() == KeyCode.ENTER) {
                ae.consume();
                setFilterPredicate(textField, eventTreeRoot);
            } else if (ae.getCode() == KeyCode.ESCAPE) {
                ae.consume();
                textField.clear();
                setFilterPredicate(textField, eventTreeRoot);
            } else { // unsupported key, nothing to be done
            }
        });
    }

    @SuppressWarnings("FloatingPointEquality")
    static <T extends Enum<T>> void setOnKeyPressedTextField(final String id, @NotNull final TextField textField, final double initialValue, final T command, final Serializable... objects) {
        final AtomicBoolean enterBeingPressed = new AtomicBoolean();
        textField.setOnKeyPressed(ae -> {
            if (ae.getCode() == KeyCode.ENTER) {
                enterBeingPressed.set(true);
                double primitive = -1d;
                try {
                    primitive = Double.parseDouble(textField.getText());
                } catch (NumberFormatException e) {
                    logger.error("NumberFormatException while parsing textField for: {}", textField.getText(), e);
                }
                final double withinRangePrimitive = Generic.keepDoubleWithinRange(primitive);
                if (withinRangePrimitive == primitive) { // no modification made by withinRange method, nothing to be done
                } else {
                    textField.setText(GUI.decimalFormatTextField.format(withinRangePrimitive));
                }

                if (withinRangePrimitive == initialValue) { // value didn't change, won't send any command
                } else {
                    Statics.sslClientThread.sendQueue.add(new SerializableObjectModification<>(command, Generic.concatArrays(objects, new Serializable[]{withinRangePrimitive})));
                }
//                focusPosition.reset(id, GridPane.getColumnIndex(textField), GridPane.getRowIndex(textField));
                textField.getParent().requestFocus();
//                GUI.resumeRefresh();
            } else if (ae.getCode() == KeyCode.ESCAPE) {
                textField.setText(GUI.decimalFormatTextField.format(Generic.keepDoubleWithinRange(initialValue)));
//                focusPosition.reset(id, GridPane.getColumnIndex(textField), GridPane.getRowIndex(textField));
                textField.getParent().requestFocus();
//                GUI.resumeRefresh();
            } else { // unsupported key, nothing to be done
            }
        });
        textField.setOnMousePressed(ae -> {
            textField.requestFocus();
            ae.consume();
        });
        textField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) { // got focus
//                focusPosition.setFocus(id, GridPane.getColumnIndex(textField), GridPane.getRowIndex(textField), textField.getCaretPosition());
                GUI.pauseRefresh();
            } else { // lost focus
                if (enterBeingPressed.get()) { // it's handled in the KeyCode.ENTER block
                } else {
                    textField.setText(GUI.decimalFormatTextField.format(Generic.keepDoubleWithinRange(initialValue)));
                }
//                focusPosition.reset(id, GridPane.getColumnIndex(textField), GridPane.getRowIndex(textField));
//                textField.getParent().requestFocus();
                GUI.resumeRefresh();
            }
        });
    }

    static <T extends Enum<T>> void setOnKeyPressedTextFieldOdds(final String id, @NotNull final Side side, @NotNull final TextField textField, @NotNull final FocusPosition focusPosition, final double initialValue, final T command,
                                                                 final Serializable... objects) {
        final AtomicBoolean enterBeingPressed = new AtomicBoolean();
        textField.setOnKeyPressed(ae -> {
            if (ae.getCode() == KeyCode.UP) {
                final double newValue = Math.max(1.01d, Formulas.getNextOddsUp(textField.getText(), side));
//                if (newValue == initialValue) { // nothing changed
//                    textField.end();
//                } else {
//                focusPosition.setFocus(id, GridPane.getColumnIndex(textField), GridPane.getRowIndex(textField), textField.getCaretPosition());
                textField.setText(GUI.decimalFormatTextField.format(newValue));
//                    Statics.sslClientThread.sendQueue.add(new SerializableObjectModification<>(command, Generic.concatArrays(objects, new Serializable[]{newValue})));
//                }
                ae.consume();
//                GUI.resumeRefresh();
            } else if (ae.getCode() == KeyCode.DOWN) {
                final double newValue = Math.min(1_000d, Formulas.getNextOddsDown(textField.getText(), side));
//                if (newValue == initialValue) { // nothing changed
//                    textField.home();
//                } else {
//                focusPosition.setFocus(id, GridPane.getColumnIndex(textField), GridPane.getRowIndex(textField), textField.getCaretPosition());
                textField.setText(GUI.decimalFormatTextField.format(newValue));
//                    Statics.sslClientThread.sendQueue.add(new SerializableObjectModification<>(command, Generic.concatArrays(objects, new Serializable[]{newValue})));
//                }
                ae.consume();
//                GUI.resumeRefresh();
            } else if (ae.getCode() == KeyCode.ESCAPE) {
                textField.setText(GUI.decimalFormatTextField.format(Formulas.getClosestOdds(initialValue, side)));
                focusPosition.reset(id, GridPane.getColumnIndex(textField), GridPane.getRowIndex(textField));
                textField.getParent().requestFocus();
//                GUI.resumeRefresh();
            } else if (ae.getCode() == KeyCode.ENTER) {
                enterBeingPressed.set(true);
                final double newValue = Formulas.getClosestOdds(textField.getText(), side);
                //noinspection FloatingPointEquality
                if (newValue == initialValue) { // nothing changed
                } else {
                    final Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Change " + (side == Side.B ? "BACK" : "LAY") + " odds from " + initialValue + " to " + newValue + " ?", ButtonType.YES, ButtonType.NO);
                    alert.setHeaderText("Odds modification confirmation");

                    //Deactivate Defaultbehavior for yes-Button:
                    final Button yesButton = (Button) alert.getDialogPane().lookupButton(ButtonType.YES);
                    yesButton.setDefaultButton(false);
                    //Activate Defaultbehavior for no-Button:
                    final Button noButton = (Button) alert.getDialogPane().lookupButton(ButtonType.NO);
                    noButton.setDefaultButton(true);
                    alert.initOwner(GUI.mainStage);

                    final Optional<ButtonType> result = alert.showAndWait();
                    if (result.isPresent()) {
//                        focusPosition.setFocus(id, GridPane.getColumnIndex(textField), GridPane.getRowIndex(textField), textField.getCaretPosition());
                        if (result.get() == ButtonType.YES) {
                            textField.setText(GUI.decimalFormatTextField.format(newValue));
                            Statics.sslClientThread.sendQueue.add(new SerializableObjectModification<>(command, Generic.concatArrays(objects, new Serializable[]{newValue})));
                        } else {
                            textField.setText(GUI.decimalFormatTextField.format(Formulas.getClosestOdds(initialValue, side)));
                        }
                    } else {
                        logger.error("result not present during setOnKeyPressedTextFieldOdds alert for: {} {} {} {}", id, side, textField.getText(), newValue);
                        textField.setText(GUI.decimalFormatTextField.format(Formulas.getClosestOdds(initialValue, side)));
                    }
                }
                focusPosition.reset(id, GridPane.getColumnIndex(textField), GridPane.getRowIndex(textField));
                textField.getParent().requestFocus();
                GUI.resumeRefresh();
            } else { // unsupported key, nothing to be done
            }
        });
        textField.setOnMousePressed(ae -> {
//            focusPosition.setFocus(id, GridPane.getColumnIndex(textField), GridPane.getRowIndex(textField), textField.getCaretPosition());
            textField.requestFocus();
            ae.consume();
        });
        textField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) { // got focus
                GUI.pauseRefresh();
            } else { // lost focus
                if (enterBeingPressed.get()) { // it's handled in the KeyCode.ENTER block
                } else {
                    textField.setText(GUI.decimalFormatTextField.format(Generic.keepDoubleWithinRange(initialValue)));
                    GUI.resumeRefresh();
                }
            }
        });
    }

    static StringBuilder getCloseWindowContentText() {
        final StringBuilder contentText = new StringBuilder(0);
        final Set<String> managedMarketIds = Statics.rulesManager.getManagedMarketIds();
        final HashMap<String, OrderMarket> orderMarketsWithoutManagedMarket = SharedStatics.orderCache.markets.copy();
        orderMarketsWithoutManagedMarket.keySet().removeAll(managedMarketIds);
        if (orderMarketsWithoutManagedMarket.isEmpty()) { // no error, nothing to be done
        } else {
            final int size = orderMarketsWithoutManagedMarket.size();
            logger.error("{} orderMarketsWithoutManagedMarket: {}", size, Generic.objectToString(orderMarketsWithoutManagedMarket));
            contentText.append("ERROR: ").append(size).append(" orderMarketsWithoutManagedMarket present\n\n");
        }
        final HashMap<String, ManagedMarket> managedMarkets = Statics.rulesManager.markets.copy();
        for (final ManagedMarket managedMarket : managedMarkets.values()) {
            if (managedMarket == null) { // error message is printed further down
            } else {
                managedMarket.calculateExposure(Statics.rulesManager, true);
            }
        }
        final StringBuilder managedEventsOverLimit = new StringBuilder(0);
        final HashMap<String, ManagedEvent> managedEvents = Statics.rulesManager.events.copy();
        for (final Map.Entry<String, ManagedEvent> entry : managedEvents.entrySet()) {
            final String eventId = entry.getKey();
            final ManagedEvent managedEvent = entry.getValue();
            if (managedEvent == null) {
                logger.error("null managedEvent in getCloseWindowContentText for: {}", eventId);
            } else {
                final double eventExposure = managedEvent.calculateExposure();
                final double eventLimit = managedEvent.getAmountLimit(Statics.existingFunds);
                final double amountOverLimit = eventExposure - eventLimit;
                if (amountOverLimit >= .01d) {
                    managedEventsOverLimit.append(eventId).append(" name:").append(managedEvent.simpleGetEventName()).append(" amount:").append(amountOverLimit).append("\n");
                } else { // limit is not breached, nothing to be done
                }
            }
        }
        final StringBuilder unmatchedBetsOnUnmanagedMarkets = new StringBuilder(0), unmanagedMarketsWithMatchedExposure = new StringBuilder(0), managedMarketsAndRunnersOverLimit = new StringBuilder(0);
        for (final Map.Entry<String, ManagedMarket> entry : managedMarkets.entrySet()) {
            final String marketId = entry.getKey();
            final ManagedMarket managedMarket = entry.getValue();
            if (managedMarket == null) {
                logger.error("null managedMarket {} in getCloseWindowContentText", marketId);
            } else {
                final double marketTotalExposure = managedMarket.getMarketTotalExposure();
                if (managedMarket.isEnabledMarket()) {
                    final double marketLimit = managedMarket.getCalculatedLimit();
                    final double amountOverLimit = marketTotalExposure - marketLimit;
                    if (amountOverLimit >= .01d) {
                        managedMarketsAndRunnersOverLimit.append(marketId).append(" marketName:").append(managedMarket.simpleGetMarketName()).append(" amount:").append(amountOverLimit).append("\n");
                    } else { // limit is not breached, nothing to be done
                    }
                    final HashMap<RunnerId, ManagedRunner> runners = managedMarket.simpleGetRunnersMap();
                    for (final Map.Entry<RunnerId, ManagedRunner> runnerEntry : runners.entrySet()) {
                        final RunnerId runnerId = runnerEntry.getKey();
                        final ManagedRunner managedRunner = runnerEntry.getValue();
                        if (managedRunner == null) {
                            logger.error("null managedRunner {} {} in getCloseWindowContentText", marketId, runnerId);
                        } else {
                            final double backRunnerLimit = managedRunner.getBackAmountLimit(), layRunnerLimit = managedRunner.getLayAmountLimit(), backExposure = managedRunner.getBackTotalExposure(), layExposure = managedRunner.getLayTotalExposure();
                            final double backOverLimit = backExposure - backRunnerLimit, layOverLimit = layExposure - layRunnerLimit;
                            if (backOverLimit >= .01d) {
                                managedMarketsAndRunnersOverLimit.append(marketId).append(" marketName:").append(managedMarket.simpleGetMarketName()).append(runnerId).append(" back amount:").append(backOverLimit).append("\n");
                            } else { // limit is not breached, nothing to be done
                            }
                            if (layOverLimit >= .01d) {
                                managedMarketsAndRunnersOverLimit.append(marketId).append(" marketName:").append(managedMarket.simpleGetMarketName()).append(runnerId).append(" lay amount:").append(layOverLimit).append("\n");
                            } else { // limit is not breached, nothing to be done
                            }
                        }
                    }
                } else {
                    final double marketMatchedExposure = managedMarket.getMarketMatchedExposure();
                    final double marketUnmatchedExposure = marketTotalExposure - marketMatchedExposure;
                    if (marketMatchedExposure >= .01d) {
                        unmanagedMarketsWithMatchedExposure.append(marketId).append(" marketName:").append(managedMarket.simpleGetMarketName()).append(" amount:").append(marketMatchedExposure).append("\n");
                    } else { // limit is not breached, nothing to be done
                    }
                    if (marketUnmatchedExposure >= .01d) {
                        unmatchedBetsOnUnmanagedMarkets.append(marketId).append(" marketName:").append(managedMarket.simpleGetMarketName()).append(" amount:").append(marketUnmatchedExposure).append("\n");
                    } else { // limit is not breached, nothing to be done
                    }
                }
            }
        }

        if (unmatchedBetsOnUnmanagedMarkets.length() > 0) {
            contentText.append("Unmatched bets on unmanaged markets:\n").append(unmatchedBetsOnUnmanagedMarkets).append("\n");
        } else { // will only append if builder not empty
        }
        if (unmanagedMarketsWithMatchedExposure.length() > 0) {
            contentText.append("Unmanaged markets with matched exposure:\n").append(unmanagedMarketsWithMatchedExposure).append("\n");
        } else { // will only append if builder not empty
        }
        if (managedEventsOverLimit.length() > 0) {
            contentText.append("Managed events over limit:\n").append(managedEventsOverLimit).append("\n");
        } else { // will only append if builder not empty
        }
        if (managedMarketsAndRunnersOverLimit.length() > 0) {
            contentText.append("Managed markets and runners over limit:\n").append(managedMarketsAndRunnersOverLimit).append("\n");
        } else { // will only append if builder not empty
        }
        return contentText;
    }

    static void handleDoubleTextField(@NotNull final TextField textField, final double value) {
        @SuppressWarnings("RegExpAnonymousGroup") final Pattern validEditingState = Pattern.compile("-?(([1-9][0-9]*)|0)?(\\.[0-9]*)?"); // "-?(([1-9][0-9]*)|0)?(\\.[0-9]*)?"
        final UnaryOperator<TextFormatter.Change> filter = c -> {
            final String text = c.getControlNewText();
            return validEditingState.matcher(text).matches() ? c : null;
        };

        //noinspection QuestionableName
        @SuppressWarnings("OverlyComplexAnonymousInnerClass") final StringConverter<String> converter = new StringConverter<>() {
            @NotNull
            @Override
            public String fromString(@NotNull final String string) {
                return string.isEmpty() || "-".equals(string) || ".".equals(string) || "-.".equals(string) ? "0" : string;
            }

            @NotNull
            @Contract(pure = true)
            @Override
            public String toString(@NotNull final String object) {
                return object.toString();
            }
        };

        final TextFormatter<String> textFormatter = new TextFormatter<>(converter, GUI.decimalFormatTextField.format(Generic.keepDoubleWithinRange(value)), filter);
        textField.setTextFormatter(textFormatter);
    }

    static void setCenterLabel(@NotNull final Label label) {
        label.setTextAlignment(TextAlignment.CENTER);
        label.setAlignment(Pos.BASELINE_CENTER);
        label.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        GridPane.setFillWidth(label, true);
        GridPane.setFillHeight(label, true);
    }

    static Node getNodeByRowColumnIndex(@NotNull final GridPane gridPane, final int column, final int row) {
        Node result = null;
        final ObservableList<Node> children = gridPane.getChildren();

        for (final Node node : children) {
            if (GridPane.getColumnIndex(node) == column && GridPane.getRowIndex(node) == row) {
                result = node;
                break;
            } else { // not the node I look for, nothing to be done
            }
        }
        return result;
    }

    static String decimalFormat(final double value) {
        final String returnValue;
        if (value < 999.995d) {
            returnValue = GUI.decimalFormatLabelLow.format(value);
        } else if (value < 9999.95d) {
            returnValue = GUI.decimalFormatLabelMedium.format(value);
        } else {
            returnValue = GUI.decimalFormatLabelHigh.format(value);
        }
        return returnValue;
    }

    static void centerButtons(@NotNull final DialogPane dialogPane) {
        final Region spacer = new Region();
        ButtonBar.setButtonData(spacer, ButtonBar.ButtonData.BIG_GAP);
        HBox.setHgrow(spacer, Priority.ALWAYS);
        dialogPane.applyCss();
        final HBox hBoxDialogPane = (HBox) dialogPane.lookup(".container");
        hBoxDialogPane.getChildren().add(spacer);
    }

    static CheckBox standardCheckBoxFactory(@NotNull final Stage mainStage, @NotNull final Label parentLabel, final String eventName, final String marketId, final String marketName, final boolean booleanMarker,
                                            @NotNull final RulesManagerModificationCommand rulesManagerModificationCommand, @NotNull final String stringMarker) {
        return standardCheckBoxFactory(mainStage, parentLabel, eventName, marketId, marketName, null, null, booleanMarker, rulesManagerModificationCommand, stringMarker);
    }

    @SuppressWarnings("UnusedReturnValue")
    static CheckBox standardCheckBoxFactory(@NotNull final Stage mainStage, @NotNull final Label parentLabel, final String marketId, final RunnerId runnerId, final String runnerName,
                                            final boolean booleanMarker, @NotNull final RulesManagerModificationCommand rulesManagerModificationCommand, @NotNull final String stringMarker) {
        return standardCheckBoxFactory(mainStage, parentLabel, null, marketId, null, runnerId, runnerName, booleanMarker, rulesManagerModificationCommand, stringMarker);
    }

    private static CheckBox standardCheckBoxFactory(@NotNull final Stage mainStage, @NotNull final Label parentLabel, final String eventName, final String marketId, final String marketName, final RunnerId runnerId, final String runnerName,
                                                    final boolean booleanMarker, @NotNull final RulesManagerModificationCommand rulesManagerModificationCommand, @NotNull final String stringMarker) {
        final boolean isRunner = runnerId != null;
        final String processedStringMarker = stringMarker.isEmpty() ? stringMarker : stringMarker + " on ";
        final CheckBox checkBox = new CheckBox();
        parentLabel.setGraphic(checkBox);
        checkBox.setSelected(booleanMarker);
        checkBox.setOnAction(actionEvent -> {
            checkBox.setSelected(booleanMarker);
            final Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Confirmation Dialog");
            if (isRunner) {
                //noinspection SpellCheckingInspection
                alert.setHeaderText("You are " + (booleanMarker ? "DIS" : "EN") + "ABLING " + processedStringMarker + "runner: " + runnerName + " (marketId:" + marketId + " " + runnerId + ")");
            } else {
                //noinspection SpellCheckingInspection
                alert.setHeaderText("You are " + (booleanMarker ? "DIS" : "EN") + "ABLING " + processedStringMarker + "market: " + marketName + " (id:" + marketId + ")");
            }
            alert.setContentText("Are you ok with this?");
            alert.initModality(Modality.APPLICATION_MODAL);
            alert.initOwner(mainStage);
            final Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent()) {
                if (result.get() == ButtonType.OK) {
                    if (isRunner) {
                        Statics.sslClientThread.sendQueue.add(new SerializableObjectModification<>(rulesManagerModificationCommand, marketId, runnerId, !booleanMarker));
                    } else {
                        Statics.sslClientThread.sendQueue.add(new SerializableObjectModification<>(rulesManagerModificationCommand, marketId, !booleanMarker));
                    }
                } else { // user chose CANCEL or closed the dialog
                }
            } else {
                if (isRunner) {
                    logger.error("result not present during runner {} alert for: {} {} {} {}", stringMarker.isEmpty() ? "enable" : stringMarker, booleanMarker, marketId, runnerName, runnerId);
                } else {
                    logger.error("result not present during market {} alert for: {} {} {} {}", stringMarker.isEmpty() ? "enable" : stringMarker, booleanMarker, marketId, marketName, eventName);
                }
            }
            alert.close();
        });
        checkBox.setOnMousePressed(mouseEvent -> {
            checkBox.setSelected(booleanMarker);
            final Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Confirmation Dialog");
            if (isRunner) {
                //noinspection SpellCheckingInspection
                alert.setHeaderText("You are " + (booleanMarker ? "DIS" : "EN") + "ABLING " + processedStringMarker + "runner: " + runnerName + " (marketId:" + marketId + " " + runnerId + ")");
            } else {
                //noinspection SpellCheckingInspection
                alert.setHeaderText("You are " + (booleanMarker ? "DIS" : "EN") + "ABLING " + processedStringMarker + "market: " + marketName + " (id:" + marketId + ")");
            }
            alert.setContentText("Are you ok with this?");
            alert.initModality(Modality.APPLICATION_MODAL);
            alert.initOwner(mainStage);
            final Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent()) {
                if (result.get() == ButtonType.OK) {
                    if (isRunner) {
                        Statics.sslClientThread.sendQueue.add(new SerializableObjectModification<>(rulesManagerModificationCommand, marketId, runnerId, !booleanMarker));
                    } else {
                        Statics.sslClientThread.sendQueue.add(new SerializableObjectModification<>(rulesManagerModificationCommand, marketId, !booleanMarker));
                    }
                } else { // user chose CANCEL or closed the dialog
                }
            } else {
                if (isRunner) {
                    logger.error("result not present during runner {} alert for: {} {} {} {}", stringMarker.isEmpty() ? "enable" : stringMarker, booleanMarker, marketId, runnerName, runnerId);
                } else {
                    logger.error("result not present during market {} alert for: {} {} {} {}", stringMarker.isEmpty() ? "enable" : stringMarker, booleanMarker, marketId, marketName, eventName);
                }
            }
            alert.close();
        });
        return checkBox;
    }

//    static <TV> void triggerTreeItemRefresh(final TreeItem<TV> item) {
//        // TreeView or TreeItem has no 'refresh()', update or redraw method.
//        // 'setValue' only triggers a refresh of the item if value is different
//        //
//        // final TV value = item.getValue();
//        // item.setValue(null);
//        // item.setValue(value);
//
//        // The API does expose the valueChangedEvent(), so send that
////        javafx.event.Event.fireEvent(item, new TreeItem.TreeModificationEvent<>(TreeItem.<TV>valueChangedEvent(), item, item.getValue()));
////        javafx.event.Event.fireEvent(item, new TreeItem.TreeModificationEvent<>(TreeItem.<TV>graphicChangedEvent(), item, item.getValue()));
////        javafx.event.Event.fireEvent(item, new TreeItem.TreeModificationEvent<>(TreeItem.<TV>branchExpandedEvent(), item, item.getValue()));
////        javafx.event.Event.fireEvent(item, new TreeItem.TreeModificationEvent<>(TreeItem.<TV>branchCollapsedEvent(), item, item.getValue()));
////        item.setExpanded(false);
////        item.setExpanded(true);
////        final int index = item.getParent().getChildren().indexOf(item);
////        item.getParent().getChildren().set(index, item);
//    }
}
