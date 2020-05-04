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
import info.fmro.shared.stream.enums.Side;
import info.fmro.shared.stream.objects.SerializableObjectModification;
import info.fmro.shared.utility.Formulas;
import info.fmro.shared.utility.Generic;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.GridPane;
import javafx.scene.text.TextAlignment;
import javafx.util.StringConverter;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

@SuppressWarnings({"UtilityClass", "unused"})
final class GUIUtils {
    private static final Logger logger = LoggerFactory.getLogger(GUIUtils.class);
    public static final String EXPIRED_MARKER = "zzz ";

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
            name = managedMarket.getMarketName(Statics.marketCache, Statics.rulesManager, Statics.marketCataloguesMap, Statics.PROGRAM_START_TIME);
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
                    final Market market = managedMarket.getMarket(Statics.marketCache, Statics.rulesManager, Statics.marketCataloguesMap, Statics.PROGRAM_START_TIME);
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

    static boolean marketExistsInMap(final String marketId) {
        return Statics.marketCataloguesMap.containsKey(marketId);
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
                logger.info("name not marked as expired in markNameAsNotExpired: {}", name);
                result = name;
            }
        }
//        logger.info("notExpired: {} -> {}", name, result);
        return result;
    }

    static boolean nameIsMarkedAsExpired(@NotNull final String name) {
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

    @SuppressWarnings("FloatingPointEquality")
    static <T extends Enum<T>> void setOnKeyPressedTextField(@NotNull final TextField textField, final double initialValue, final T command, final Serializable... objects) {
        textField.setOnKeyPressed(ae -> {
            if (ae.getCode() == KeyCode.ENTER) {
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
            } else if (ae.getCode() == KeyCode.ESCAPE) {
                textField.setText(GUI.decimalFormatTextField.format(Generic.keepDoubleWithinRange(initialValue)));
            } else { // unsupported key, nothing to be done
            }
        });
        textField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                textField.setText(GUI.decimalFormatTextField.format(Generic.keepDoubleWithinRange(initialValue)));
            }
        });
    }

    static <T extends Enum<T>> void setOnKeyPressedTextFieldOdds(@NotNull final Side side, @NotNull final TextField textField, @NotNull final AtomicInteger columnIndex, @NotNull final AtomicInteger rowIndex, final double initialValue, final T command,
                                                                 final Serializable... objects) {
        textField.setOnKeyPressed(ae -> {
            if (ae.getCode() == KeyCode.UP) {
                final double newValue = Formulas.getNextOddsUp(initialValue, side);
                //noinspection FloatingPointEquality
                if (newValue == initialValue) { // nothing changed
                    textField.end();
                } else {
                    textField.setText(GUI.decimalFormatTextField.format(newValue));
                    columnIndex.set(GridPane.getColumnIndex(textField));
                    rowIndex.set(GridPane.getRowIndex(textField));
                    Statics.sslClientThread.sendQueue.add(new SerializableObjectModification<>(command, Generic.concatArrays(objects, new Serializable[]{newValue})));
                }
            } else if (ae.getCode() == KeyCode.DOWN) {
                final double newValue = Formulas.getNextOddsDown(initialValue, side);
                //noinspection FloatingPointEquality
                if (newValue == initialValue) { // nothing changed
                } else {
                    textField.setText(GUI.decimalFormatTextField.format(newValue));
                    columnIndex.set(GridPane.getColumnIndex(textField));
                    rowIndex.set(GridPane.getRowIndex(textField));
                    Statics.sslClientThread.sendQueue.add(new SerializableObjectModification<>(command, Generic.concatArrays(objects, new Serializable[]{newValue})));
                }
            } else if (ae.getCode() == KeyCode.ESCAPE || ae.getCode() == KeyCode.ENTER) {
                textField.setText(GUI.decimalFormatTextField.format(Formulas.getClosestOdds(initialValue, side)));
            } else { // unsupported key, nothing to be done
            }
        });
        textField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                textField.setText(GUI.decimalFormatTextField.format(Generic.keepDoubleWithinRange(initialValue)));
            }
        });
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
