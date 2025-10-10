package hu.uni_obuda.thesis.railways.data.raildatacollector.util.resource;

import java.lang.reflect.Array;
import java.util.*;

public final class DefaultGraphQlVariables {

    private final Map<String, Object> values;

    public DefaultGraphQlVariables(Map<String, Object> values) {
        this.values = deepFreezeMap(values);
    }

    public Map<String, Object> asMap() {
        return values;
    }

    private static Map<String, Object> deepFreezeMap(Map<?, ?> source) {
        if (source == null) return Map.of();
        Map<String, Object> copy = new LinkedHashMap<>(Math.max(16, source.size()));
        for (Map.Entry<?, ?> e : source.entrySet()) {
            copy.put(String.valueOf(e.getKey()), deepFreeze(e.getValue()));
        }
        return Collections.unmodifiableMap(copy);
    }

    private static Object deepFreeze(Object value) {
        if (value == null) return null;

        if (value instanceof Map<?, ?> map) {
            Map<String, Object> copy = new LinkedHashMap<>(Math.max(16, map.size()));
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                copy.put(String.valueOf(entry.getKey()), deepFreeze(entry.getValue()));
            }
            return Collections.unmodifiableMap(copy);
        }

        if (value instanceof List<?> list) {
            List<Object> copy = new ArrayList<>(list.size());
            for (Object object : list) copy.add(deepFreeze(object));
            return Collections.unmodifiableList(copy);
        }

        if (value instanceof Set<?> set) {
            Set<Object> copy = new LinkedHashSet<>(Math.max(16, (int) (set.size() / 0.75f) + 1));
            for (Object object : set) copy.add(deepFreeze(object));
            return Collections.unmodifiableSet(copy);
        }

        if (value.getClass().isArray()) {
            int length = Array.getLength(value);
            List<Object> copy = new ArrayList<>(length);
            for (int i = 0; i < length; i++) copy.add(deepFreeze(Array.get(value, i)));
            return Collections.unmodifiableList(copy);
        }

        return value;
    }
}
