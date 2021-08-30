package com.jetbrains.qodana.sarif.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.*;


/**
 * Key/value pairs that provide additional information about the object.
 * Additional tags  - tags provided by parent run object.
 */
public class PropertyBag implements Map<String, Object> {
    private final Map<String, Object> properties = new HashMap<>();

    private final Set<String> additionalTags = new LinkedHashSet<>();

    public Set<String> getTags() {
        HashSet<String> result = new HashSet<>(properties.keySet());
        result.addAll(additionalTags);
        return result;
    }

    public Set<String> getAdditionalTags() {
        return additionalTags;
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
        return properties.get(key);
    }

    @Override
    public Object put(String key, Object value) {
        return properties.put(key, value);
    }

    @Override
    public Object remove(Object key) {
        return properties.remove(key);
    }

    @Override
    public void putAll(Map<? extends String, ?> m) {
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
        return Objects.equals(properties, that.properties) && Objects.equals(additionalTags, that.additionalTags);
    }

    @Override
    public int hashCode() {
        return Objects.hash(properties, additionalTags);
    }


    public static class PropertyBagTypeAdapter extends TypeAdapter<PropertyBag> {
        private static final Gson embedded = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

        public void write(JsonWriter out, PropertyBag bag) throws IOException {
            HashMap<String, Object> toSerialize = new HashMap<>(bag);
            toSerialize.put("tags", bag.getTags());
            embedded.toJson(toSerialize, Map.class, out);
        }

        public PropertyBag read(JsonReader reader) throws IOException {
            PropertyBag result = new PropertyBag();
            Map<String, Object> map = embedded.fromJson(reader, Map.class);
            Object tags = map.remove("tags");
            result.putAll(map);
            if (tags instanceof String[]) {
                for (String tag : (String[]) tags) {
                    if (!map.containsKey(tag)) {
                        result.additionalTags.add(tag);
                    }
                }
            }
            return result;
        }
    }
}
