package info.fmro.client.objects;

import info.fmro.client.main.GUI;
import info.fmro.shared.entities.Event;
import info.fmro.shared.entities.MarketCatalogue;
import info.fmro.shared.objects.SharedStatics;
import info.fmro.shared.stream.objects.StreamSynchronizedMap;
import info.fmro.shared.utility.Generic;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;

@SuppressWarnings({"WeakerAccess", "ClassTooDeepInInheritanceTree", "unchecked", "OverlyComplexClass"})
public class ClientStreamSynchronizedMap<K extends Serializable, V extends Serializable>
        extends StreamSynchronizedMap<K, V>
        implements Serializable {
    private static final Logger logger = LoggerFactory.getLogger(ClientStreamSynchronizedMap.class);
    @Serial
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
        if (clazz == MarketCatalogue.class) {
            result = false;
        } else if (clazz == Event.class) {
            result = true;
        } else {
            result = false;
            logger.error("unsupported class in isEventsMap: {}", clazz);
            SharedStatics.mustStop.set(true); // fatal error, won't continue
        }

        return result;
    }

//    @Override
//    public synchronized boolean copyFrom(final StreamSynchronizedMap<? extends K, ? extends V> other) {
//        return copyFrom(other, false);
//    }

    @Override
    public synchronized boolean copyFrom(final StreamSynchronizedMap<? extends K, ? extends V> other, final boolean mapAllowedToBeNotEmpty) { // doesn't copy static final or transient; does update the map
        final boolean readSuccessful;
        if (other == null) {
            readSuccessful = false;
            logger.error("null other in copyFrom for: {}", Generic.objectToString(this));
        } else {
            @SuppressWarnings({"unchecked", "rawtypes"}) final boolean otherIsEventsMap = isEventsMap((Class) other.getClazz());
            if (otherIsEventsMap == this.isEventsMap) {
                readSuccessful = super.copyFrom(other, mapAllowedToBeNotEmpty);

                if (readSuccessful) {
                    if (this.isEventsMap) {
                        GUI.initializeEventsTreeView();
                    } else {
                        GUI.initializeMarketsTreeView();
                    }
                    GUI.publicMarkAllManagedItemsAsExpired(this.isEventsMap);
                    GUI.publicMarkManagedItemsAsNotExpired((HashSet<String>) this.keySetCopy(), this.isEventsMap);
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
            GUI.publicMarkAllManagedItemsAsExpired(this.isEventsMap);
        }
        return result;
    }

    @Override
    public synchronized V put(final K key, @NotNull final V value, final boolean intentionalPutInsteadOfPutIfAbsent) {
        final V result = super.put(key, value, intentionalPutInsteadOfPutIfAbsent);
        if (Objects.equals(value, result)) { // no modification made, I won't send anything
        } else {
            if (this.isEventsMap) {
                GUI.publicAddEvent((String) key, (Event) value);
            } else {
                GUI.publicAddMarket((String) key, (MarketCatalogue) value);
            }
            GUI.publicMarkManagedItemAsNotExpired((String) key, this.isEventsMap);
        }
        return result;
    }

    @Override
    public synchronized V put(final K key, @NotNull final V value) {
        final V result = super.put(key, value);
        if (Objects.equals(value, result)) { // no modification made, I won't send anything
        } else {
            if (this.isEventsMap) {
                GUI.publicAddEvent((String) key, (Event) value);
            } else {
                GUI.publicAddMarket((String) key, (MarketCatalogue) value);
            }
            GUI.publicMarkManagedItemAsNotExpired((String) key, this.isEventsMap);
        }
        return result;
    }

    @Override
    public synchronized V putIfAbsent(final K key, @NotNull final V value) {
        final V result = super.putIfAbsent(key, value);
        if (Objects.equals(value, result)) { // no modification made, I won't send anything
        } else {
            if (this.isEventsMap) {
                GUI.publicAddEvent((String) key, (Event) value);
            } else {
                GUI.publicAddMarket((String) key, (MarketCatalogue) value);
            }
            GUI.publicMarkManagedItemAsNotExpired((String) key, this.isEventsMap);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    @Override
    public synchronized void putAll(final Map<? extends K, ? extends V> m) {
        super.putAll(m);
        if (this.isEventsMap) {
            GUI.publicPutAllEvent((HashMap<String, Event>) m);
        } else {
            GUI.publicPutAllMarket((HashMap<String, MarketCatalogue>) m);
        }
        if (m == null) {
            logger.error("null map in putAll isEventsMap for: {}", Generic.objectToString(this));
        } else {
            GUI.publicMarkManagedItemsAsNotExpired(new HashSet<>((Collection<String>) m.keySet()), this.isEventsMap);
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
            GUI.publicMarkManagedItemAsExpired((String) key, this.isEventsMap);
        } else { // no key removed, nothing to be done
        }
//        return super.remove(key);
        return get(key);
    }

    public synchronized V superRemove(final K key) {
        return super.remove(key);
    }

    @Override
    public synchronized boolean remove(final K key, final V value) {
//        final boolean modified = super.remove(key, value);
        final boolean modified = this.containsKey(key) && Objects.equals(this.get(key), value);
        if (modified) {
            if (this.isEventsMap) {
                GUI.publicRemoveEvent((String) key);
            } else {
                GUI.publicRemoveMarket((MarketCatalogue) value);
            }
            GUI.publicMarkManagedItemAsExpired((String) key, this.isEventsMap);
        } else { // no modification made, I won't send anything
        }
        return modified;
    }

    @Override
    public synchronized boolean removeEntry(final Map.Entry<K, V> entry) {
//        final boolean modified = super.removeEntry(entry);
        final boolean modified = this.containsEntry(entry);
        if (modified) {
            if (this.isEventsMap) {
                GUI.publicRemoveEvent((String) entry.getKey());
            } else {
                GUI.publicRemoveMarket((MarketCatalogue) entry.getValue());
            }
            GUI.publicMarkManagedItemAsExpired((String) entry.getKey(), this.isEventsMap);
        } else { // no modification made, I won't send anything
        }
        return modified;
    }

    @SuppressWarnings({"unchecked", "OverlyStrongTypeCast"})
    @Override
    public synchronized boolean removeAllEntries(final Collection<?> c) {
        if (this.isEventsMap) {
            GUI.publicRemoveEntriesEvent((HashSet<Map.Entry<String, Event>>) c);
        } else {
            GUI.publicRemoveEntriesMarket((HashSet<Map.Entry<String, MarketCatalogue>>) c);
        }
        if (c == null) {
            logger.error("null collection in removeAllEntries isEventsMap for: {}", Generic.objectToString(this));
        } else {
            final HashSet<String> ids = new HashSet<>(Generic.getCollectionCapacity(c));
            for (final Map.Entry<String, Event> entry : (Collection<Map.Entry<String, Event>>) c) {
                if (entry == null) {
                    logger.error("null entry in collection in removeAllEntries isEventsMap for: {} {}", Generic.objectToString(c), Generic.objectToString(this));
                } else {
                    ids.add(entry.getKey());
                }
            }
            if (ids.isEmpty()) { // nothing to mark as expired
            } else {
                GUI.publicMarkManagedItemsAsExpired(ids, this.isEventsMap);
            }
        }
//        return super.removeAllEntries(c);
        return true;
    }

    @SuppressWarnings("unchecked")
    @Override
    public synchronized boolean retainAllEntries(final Collection<?> c) {
        if (this.isEventsMap) {
            GUI.publicRetainEntriesEvent((HashSet<Map.Entry<String, Event>>) c);
        } else {
            GUI.publicRetainEntriesMarket((HashSet<Map.Entry<String, MarketCatalogue>>) c);
        }
        if (c == null) {
            logger.error("null collection in retainAllEntries isEventsMap for: {}", Generic.objectToString(this));
        } else {
            final HashSet<String> keysToRemove = new HashSet<>(Generic.getCollectionCapacity(size()));
            for (final Map.Entry<K, V> entry : entrySetCopy()) {
                if (c.contains(entry)) { // not the value I look for, will continue
                } else {
                    keysToRemove.add((String) entry.getKey());
                }
            }
            if (keysToRemove.isEmpty()) { // nothing to mark as expired
            } else {
                GUI.publicMarkManagedItemsAsExpired(keysToRemove, this.isEventsMap);
            }
        }
//        return super.retainAllEntries(c);
        return true;
    }

    @SuppressWarnings({"unchecked", "OverlyStrongTypeCast"})
    @Override
    public synchronized boolean removeAllKeys(final Collection<?> c) {
//        final boolean modified = super.removeAllKeys(c);
//        if (modified) {
        if (this.isEventsMap) {
            GUI.publicRemoveKeysEvent((HashSet<String>) c);
        } else {
            GUI.publicRemoveKeysMarket((HashSet<String>) c);
        }
        GUI.publicMarkManagedItemsAsExpired((HashSet<String>) c, this.isEventsMap);
//        } else { // no modification made, nothing to be done
//        }
//        return modified;
        return true;
    }

    @SuppressWarnings("unchecked")
    @Override
    public synchronized boolean retainAllKeys(final Collection<?> c) {
        if (this.isEventsMap) {
            GUI.publicRetainKeysEvent((HashSet<String>) c);
        } else {
            GUI.publicRetainKeysMarket((HashSet<String>) c);
        }
        if (c == null) {
            logger.error("null collection in retainAllKeys isEventsMap for: {}", Generic.objectToString(this));
        } else {
            final HashSet<String> keysToRemove = new HashSet<>(Generic.getCollectionCapacity(size()));
            for (final K key : keySetCopy()) {
                if (c.contains(key)) { // not the value I look for, will continue
                } else {
                    keysToRemove.add((String) key);
                }
            }
            if (keysToRemove.isEmpty()) { // nothing to mark as expired
            } else {
                GUI.publicMarkManagedItemsAsExpired(keysToRemove, this.isEventsMap);
            }
        }
//        return super.retainAllKeys(c);
        return true;
    }

    @Override
    public synchronized boolean removeValue(final V value) {
        final boolean modified = containsValue(value);
        if (modified) {
            if (this.isEventsMap) {
                GUI.publicRemoveEvent((Event) value);
            } else {
                GUI.publicRemoveMarket((MarketCatalogue) value);
            }
            for (final Map.Entry<K, V> entry : entrySetCopy()) {
                if (Objects.equals(entry.getValue(), value)) {
                    GUI.publicMarkManagedItemAsExpired((String) entry.getKey(), this.isEventsMap);
                    break;
                } else { // not the value I look for, will continue
                }
            }
        } else { // no modification made, I won't send anything
        }
//        return super.removeValue(value);
        return modified;
    }

    public synchronized boolean superRemoveValueAll(final V value) {
        return super.removeValueAll(value);
    }

    @Override
    public synchronized boolean removeValueAll(final V value) {
        final boolean modified = containsValue(value);
        if (modified) {
            if (this.isEventsMap) {
                GUI.publicRemoveValueAllEvent((Event) value);
            } else {
                GUI.publicRemoveValueAllMarket((MarketCatalogue) value);
            }
            final HashSet<String> keysToRemove = new HashSet<>(2);
            for (final Map.Entry<K, V> entry : entrySetCopy()) {
                if (Objects.equals(entry.getValue(), value)) {
                    keysToRemove.add((String) entry.getKey());
                } else { // not the value I look for, will continue
                }
            }
            if (keysToRemove.isEmpty()) { // nothing to mark as expired
            } else {
                GUI.publicMarkManagedItemsAsExpired(keysToRemove, this.isEventsMap);
            }
        } else { // no modification made, I won't send anything
        }
//        return super.removeValueAll(value);
        if (value == null) { // safety to make sure null values are removed
            super.removeValueAll(value);
        }
        return modified;
    }

    @SuppressWarnings({"unchecked", "OverlyStrongTypeCast"})
    @Override
    public synchronized boolean removeAllValues(final Collection<?> c) {
        if (this.isEventsMap) {
            GUI.publicRemoveValuesEvent((HashSet<Event>) c);
        } else {
            GUI.publicRemoveValuesMarket((HashSet<MarketCatalogue>) c);
        }
        if (c == null) {
            logger.error("null collection in removeAllValues isEventsMap for: {}", Generic.objectToString(this));
        } else {
            final HashSet<String> keysToRemove = new HashSet<>(Generic.getCollectionCapacity(c));
            for (final Map.Entry<K, V> entry : entrySetCopy()) {
                if (c.contains(entry.getValue())) {
                    keysToRemove.add((String) entry.getKey());
                } else { // not the value I look for, will continue
                }
            }
            if (keysToRemove.isEmpty()) { // nothing to mark as expired
            } else {
                GUI.publicMarkManagedItemsAsExpired(keysToRemove, this.isEventsMap);
            }
        }
//        return super.removeAllValues(c);
        return true;
    }

    @SuppressWarnings("unchecked")
    @Override
    public synchronized boolean retainAllValues(final Collection<?> c) {
        if (this.isEventsMap) {
            GUI.publicRetainValuesEvent((HashSet<Event>) c);
        } else {
            GUI.publicRetainValuesMarket((HashSet<MarketCatalogue>) c);
        }
        if (c == null) {
            logger.error("null collection in retainAllValues isEventsMap for: {}", Generic.objectToString(this));
        } else {
            final HashSet<String> keysToRemove = new HashSet<>(Generic.getCollectionCapacity(size()));
            for (final Map.Entry<K, V> entry : entrySetCopy()) {
                if (c.contains(entry.getValue())) { // not the value I look for, will continue
                } else {
                    keysToRemove.add((String) entry.getKey());
                }
            }
            if (keysToRemove.isEmpty()) { // nothing to mark as expired
            } else {
                GUI.publicMarkManagedItemsAsExpired(keysToRemove, this.isEventsMap);
            }
        }
//        return super.retainAllValues(c);
        return true;
    }
}
