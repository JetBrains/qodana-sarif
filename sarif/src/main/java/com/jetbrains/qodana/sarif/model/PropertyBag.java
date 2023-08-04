package com.jetbrains.qodana.sarif.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.util.*;


/**
 * Key/value pairs that provide additional information about the object.
 * Tags - reserved key for additional tags.
 */
public class PropertyBag implements Map<String, Object> {
    public static final String TAGS_KEY = "tags";

    private final Map<String, Object> properties = new HashMap<>();

    private final Set<String> tags = new LinkedHashSet<>();

    public Set<String> getTags() {
        return tags;
    }

    @Override
    public int size() {
        return properties.size();
    }

    @Override
    public boolean isEmpty() {
        return properties.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return properties.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return properties.containsValue(value);
    }

    @Override
    public Object get(Object key) {
        if (TAGS_KEY.equals(key)) throw new IllegalArgumentException(TAGS_KEY + " is a reserved key");
        return properties.get(key);
    }

    @Override
    public Object put(String key, Object value) {
        if (TAGS_KEY.equals(key)) throw new IllegalArgumentException(TAGS_KEY + " is a reserved key");
        return properties.put(key, value);
    }

    @Override
    public Object remove(Object key) {
        if (TAGS_KEY.equals(key)) throw new IllegalArgumentException(TAGS_KEY + " is a reserved key");
        return properties.remove(key);
    }

    @Override
    public void putAll(Map<? extends String, ?> m) {
        if (m.containsKey(TAGS_KEY)) throw new IllegalArgumentException(TAGS_KEY + " is a reserved key");

        properties.putAll(m);
    }

    @Override
    public void clear() {
        properties.clear();
    }

    @Override
    public Set<String> keySet() {
        return properties.keySet();
    }

    @Override
    public Collection<Object> values() {
        return properties.values();
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        return properties.entrySet();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PropertyBag that = (PropertyBag) o;
        return Objects.equals(properties, that.properties) && Objects.equals(tags, that.tags);
    }

    @Override
    public int hashCode() {
        return Objects.hash(properties, tags);
    }


    public static class PropertyBagTypeAdapter extends TypeAdapter<PropertyBag> {
        private static final Gson embedded = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

        public void write(JsonWriter out, PropertyBag bag) {
            HashMap<String, Object> toSerialize = new HashMap<>(bag);
            if (!bag.tags.isEmpty()) {
                toSerialize.put(TAGS_KEY, bag.getTags());
            }
            embedded.toJson(toSerialize, Map.class, out);
        }

        public PropertyBag read(JsonReader reader) {
            PropertyBag result = new PropertyBag();
            Map<String, Object> map = embedded.fromJson(reader, Map.class);
            Object tags = map.remove(TAGS_KEY);
            result.putAll(map);
            if (tags instanceof List) {
                //noinspection unchecked
                result.tags.addAll((Collection<? extends String>) tags);
            }
            return result;
        }
    }
}
