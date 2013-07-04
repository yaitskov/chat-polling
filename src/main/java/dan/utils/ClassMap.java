package dan.utils;

import java.util.*;

/**
 *
 * Fast entry search of nearest superclass.
 *
 * Map is not intended for intensive inserts.
 * It fast for lookup.
 *
 * Daneel Yaitskov
 */
public class ClassMap<V> implements Map<Class, V> {

    /**
     * Distance for Object parent => classes with such distance.
     */
    private Map<Integer, Map<Class, V>> hopClassValues;
    private int size = 0;

    public ClassMap() {
        hopClassValues = new HashMap<Integer, Map<Class, V>>();
    }

    public ClassMap(ClassMap source) {
        this();
        putAll(source);
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * Returns true if map has entry superclass of key class.
     * @param key key class
     * @return true if map has entry superclass of key class
     */
    @Override
    public boolean containsKey(Object key) {
        if (!(key instanceof Class)) {
            return false;
        }
        int distance = ReflectionUtil.distance((Class)key, Object.class);
        return find((Class)key, distance) != null;
    }

    protected V find(Class key, int level) {
        if (level < 0)
            return null;
        Map<Class, V> m = hopClassValues.get(level);
        if (m != null) {
            V v = m.get(key);
            if (v != null)
                return v;
        }
        return find(key.getSuperclass(), level - 1);
    }

    @Override
    public boolean containsValue(Object value) {
        for (Map<Class, V> m : hopClassValues.values()) {
            if (m.containsValue(value))
                return true;
        }
        return false;
    }

    /**
     * Returns entry value with nearest superclass of the key.
     * @param key key class
     * @return entry value with nearest superclass of the key
     */
    @Override
    public V get(Object key) {
        if (!(key instanceof Class)) {
            return null;
        }
        int distance = ReflectionUtil.distance((Class)key, Object.class);
        return find((Class)key, distance);
    }

    @Override
    public V put(Class key, V value) {
        int distance = ReflectionUtil.distance(key, Object.class);
        Map<Class, V> m = hopClassValues.get(distance);
        if (m == null) {
            m = new HashMap<Class, V>(1);
            hopClassValues.put(distance, m);
        }
        int oldSize = m.size();
        V result = m.put(key, value);
        size += m.size() - oldSize;
        return result;
    }

    @Override
    public V remove(Object key) {
        if (!(key instanceof Class)) {
            return null;
        }
        int distance = ReflectionUtil.distance((Class)key, Object.class);
        Map<Class, V> m = hopClassValues.get(distance);
        if (m == null)
            return null;
        int oldSize = m.size();
        V result = m.remove(key);
        size += m.size() - oldSize;
        if (m.isEmpty()) {
            hopClassValues.remove(distance);
        }
        return result;
    }

    @Override
    public void putAll(Map<? extends Class, ? extends V> m) {
        for (Entry<? extends Class, ? extends V> entry : m.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void clear() {
        size = 0;
        hopClassValues.clear();
    }

    @Override
    public Set<Class> keySet() {
        Set<Class> result = new HashSet<Class>(size());
        for (Map<Class, V> m  : hopClassValues.values()) {
            result.addAll(m.keySet());
        }
        return result;
    }

    @Override
    public Collection<V> values() {
        Collection<V> result = new ArrayList<V>(size());
        for (Map<Class, V> m  : hopClassValues.values()) {
            result.addAll(m.values());
        }
        return result;
    }

    @Override
    public Set<Entry<Class, V>> entrySet() {
        Set<Entry<Class,V>> result = new HashSet<Entry<Class, V>>(size());
        for (Map<Class, V> m  : hopClassValues.values()) {
            result.addAll(m.entrySet());
        }
        return result;
    }
}
