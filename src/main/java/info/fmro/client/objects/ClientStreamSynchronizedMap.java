package info.fmro.client.objects;

import info.fmro.client.main.GUI;
import info.fmro.shared.stream.objects.EventInterface;
import info.fmro.shared.stream.objects.MarketCatalogueInterface;
import info.fmro.shared.stream.objects.StreamSynchronizedMap;
import info.fmro.shared.utility.Generic;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@SuppressWarnings({"WeakerAccess", "ClassTooDeepInInheritanceTree"})
public class ClientStreamSynchronizedMap<K extends Serializable, V extends Serializable>
        extends StreamSynchronizedMap<K, V>
        implements Serializable {
    private static final Logger logger = LoggerFactory.getLogger(ClientStreamSynchronizedMap.class);
    private static final long serialVersionUID = -6744224941270103751L;
    private final boolean isEventsMap;

    public ClientStreamSynchronizedMap(final Class<? super V> clazz) {
        super(clazz);
        this.isEventsMap = isEventsMap(clazz);
    }

    public ClientStreamSynchronizedMap(final Class<? super V> clazz, final int initialSize) {
        super(clazz, initialSize);
        this.isEventsMap = isEventsMap(clazz);
    }

    public ClientStreamSynchronizedMap(final Class<? super V> clazz, final Map<? extends K, ? extends V> map) {
        super(clazz, map);
        this.isEventsMap = isEventsMap(clazz);
    }

    private synchronized boolean isEventsMap(final Class<? super V> clazz) {
        final boolean result;
        //noinspection ChainOfInstanceofChecks
        if (clazz == MarketCatalogueInterface.class) {
            result = false;
        } else if (clazz == EventInterface.class) {
            result = true;
        } else {
            result = false;
            logger.error("unsupported class in isEventsMap: {}", clazz);
            Statics.mustStop.set(true); // fatal error, won't continue
        }

        return result;
    }

    @Override
    public synchronized boolean copyFrom(final StreamSynchronizedMap<? extends K, ? extends V> other) { // doesn't copy static final or transient; does update the map
        final boolean readSuccessful;
        if (other == null) {
            readSuccessful = false;
            logger.error("null other in copyFrom for: {}", Generic.objectToString(this));
        } else {
            @SuppressWarnings({"unchecked", "rawtypes"}) final boolean otherIsEventsMap = isEventsMap((Class) other.getClazz());
            if (otherIsEventsMap == this.isEventsMap) {
                readSuccessful = super.copyFrom(other);

                if (readSuccessful) {
                    if (this.isEventsMap) {
                        GUI.initializeEventsTreeView();
                    } else {
                        GUI.initializeMarketsTreeView();
                    }
                } else { // I probably shouldn't initialize if !readSuccessful
                }
            } else {
                readSuccessful = false;
                logger.error("mismatched isEventsMap in copyFrom for: {} {} {} {}", this.isEventsMap, otherIsEventsMap, Generic.objectToString(this), Generic.objectToString(other));
            }
        }
        return readSuccessful;
    }

    @Override
    public synchronized HashMap<K, V> clear() {
        final HashMap<K, V> result = super.clear();
        if (result.isEmpty()) { // no modification made, I won't send anything
        } else {
            if (this.isEventsMap) {
                GUI.clearEventsTreeView();
            } else {
                GUI.clearMarketsTreeView();
            }
        }
        return result;
    }

    @Override
    public synchronized V put(final K key, @NotNull final V value, final boolean intentionalPutInsteadOfPutIfAbsent) {
        final V result = super.put(key, value, intentionalPutInsteadOfPutIfAbsent);
        if (Objects.equals(value, result)) { // no modification made, I won't send anything
        } else {
            if (this.isEventsMap) {
                GUI.publicAddEvent((String) key, (EventInterface) value);
            } else {
                GUI.publicAddMarket((String) key, (MarketCatalogueInterface) value);
            }
        }
        return result;
    }

    @Override
    public synchronized V put(final K key, @NotNull final V value) {
        final V result = super.put(key, value);
        if (Objects.equals(value, result)) { // no modification made, I won't send anything
        } else {
            if (this.isEventsMap) {
                GUI.publicAddEvent((String) key, (EventInterface) value);
            } else {
                GUI.publicAddMarket((String) key, (MarketCatalogueInterface) value);
            }
        }
        return result;
    }

    @Override
    public synchronized V putIfAbsent(final K key, @NotNull final V value) {
        final V result = super.putIfAbsent(key, value);
        if (Objects.equals(value, result)) { // no modification made, I won't send anything
        } else {
            if (this.isEventsMap) {
                GUI.publicAddEvent((String) key, (EventInterface) value);
            } else {
                GUI.publicAddMarket((String) key, (MarketCatalogueInterface) value);
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    @Override
    public synchronized void putAll(final Map<? extends K, ? extends V> m) {
        super.putAll(m);
        if (this.isEventsMap) {
            GUI.publicPutAllEvent((Map<String, EventInterface>) m);
        } else {
            GUI.publicPutAllMarket((Map<String, MarketCatalogueInterface>) m);
        }
    }

    @Override
    public synchronized V remove(final K key) {
        if (containsKey(key)) {
            if (this.isEventsMap) {
                GUI.publicRemoveEvent((String) key);
            } else {
                GUI.publicRemoveMarket((String) key);
            }
        } else { // no key removed, nothing to be done
        }
        return super.remove(key);
    }

    @Override
    public synchronized boolean remove(final K key, final V value) {
        if (containsKey(key) && Objects.equals(get(key), value)) {
            if (this.isEventsMap) {
                GUI.publicRemoveEvent((String) key);
            } else {
                GUI.publicRemoveMarket((MarketCatalogueInterface) value);
            }
        } else { // no modification made, I won't send anything
        }
        return super.remove(key, value);
    }

    @Override
    public synchronized boolean removeEntry(final Map.Entry<K, V> entry) {
        if (containsEntry(entry)) {
            if (this.isEventsMap) {
                GUI.publicRemoveEvent((String) entry.getKey());
            } else {
                GUI.publicRemoveMarket((MarketCatalogueInterface) entry.getValue());
            }
        } else { // no modification made, I won't send anything
        }
        return super.removeEntry(entry);
    }

    @SuppressWarnings({"unchecked", "OverlyStrongTypeCast"})
    @Override
    public synchronized boolean removeAllEntries(final Collection<?> c) {
        if (this.isEventsMap) {
            GUI.publicRemoveEntriesEvent((Collection<Map.Entry<String, EventInterface>>) c);
        } else {
            GUI.publicRemoveEntriesMarket((Collection<Map.Entry<String, MarketCatalogueInterface>>) c);
        }
        return super.removeAllEntries(c);
    }

    @SuppressWarnings("unchecked")
    @Override
    public synchronized boolean retainAllEntries(final Collection<?> c) {
        if (this.isEventsMap) {
            GUI.publicRetainEntriesEvent((Collection<Map.Entry<String, EventInterface>>) c);
        } else {
            GUI.publicRetainEntriesMarket((Collection<Map.Entry<String, MarketCatalogueInterface>>) c);
        }
        return super.retainAllEntries(c);
    }

    @SuppressWarnings({"unchecked", "OverlyStrongTypeCast"})
    @Override
    public synchronized boolean removeAllKeys(final Collection<?> c) {
        if (this.isEventsMap) {
            GUI.publicRemoveKeysEvent((Collection<String>) c);
        } else {
            GUI.publicRemoveKeysMarket((Collection<String>) c);
        }
        return super.removeAllKeys(c);
    }

    @SuppressWarnings("unchecked")
    @Override
    public synchronized boolean retainAllKeys(final Collection<?> c) {
        if (this.isEventsMap) {
            GUI.publicRetainKeysEvent((Collection<String>) c);
        } else {
            GUI.publicRetainKeysMarket((Collection<String>) c);
        }
        return super.retainAllKeys(c);
    }

    @Override
    public synchronized boolean removeValue(final V value) {
        if (containsValue(value)) {
            if (this.isEventsMap) {
                GUI.publicRemoveEvent((EventInterface) value);
            } else {
                GUI.publicRemoveMarket((MarketCatalogueInterface) value);
            }
        } else { // no modification made, I won't send anything
        }
        return super.removeValue(value);
    }

    @Override
    public synchronized boolean removeValueAll(final V value) {
        if (containsValue(value)) {
            if (this.isEventsMap) {
                GUI.publicRemoveValueAllEvent((EventInterface) value);
            } else {
                GUI.publicRemoveValueAllMarket((MarketCatalogueInterface) value);
            }
        } else { // no modification made, I won't send anything
        }
        return super.removeValueAll(value);
    }

    @SuppressWarnings({"unchecked", "OverlyStrongTypeCast"})
    @Override
    public synchronized boolean removeAllValues(final Collection<?> c) {
        if (this.isEventsMap) {
            GUI.publicRemoveValuesEvent((Collection<EventInterface>) c);
        } else {
            GUI.publicRemoveValuesMarket((Collection<MarketCatalogueInterface>) c);
        }
        return super.removeAllValues(c);
    }

    @SuppressWarnings("unchecked")
    @Override
    public synchronized boolean retainAllValues(final Collection<?> c) {
        if (this.isEventsMap) {
            GUI.publicRetainValuesEvent((Collection<EventInterface>) c);
        } else {
            GUI.publicRetainValuesMarket((Collection<MarketCatalogueInterface>) c);
        }
        return super.retainAllValues(c);
    }
}
