package com.jetbrains.qodana.sarif.model;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
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

    public static class PropertyBagTypeAdapterFactory implements TypeAdapterFactory {

        private final Set<String> ignoreKeys;

        public PropertyBagTypeAdapterFactory(Collection<String> ignoreKeys) {
            this.ignoreKeys = new HashSet<>(ignoreKeys);
        }

        @Override
        public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> typeToken) {
            Class<? super T> rawType = typeToken.getRawType();
            if (!PropertyBag.class.equals(rawType)) {
                return null;
            } else {
                return (TypeAdapter<T>) new PropertyBag.PropertyBagTypeAdapterFactory.PropertyBagTypeAdapter(ignoreKeys, gson);
            }
        }

        public static class PropertyBagTypeAdapter extends TypeAdapter<PropertyBag> {
            private final Gson gson;
            private final Set<String> ignoreKeys;

            public PropertyBagTypeAdapter(Set<String> ignoreKeys, Gson gson) {
                this.ignoreKeys = ignoreKeys;
                this.gson = gson;
            }

            public void write(JsonWriter out, PropertyBag bag) throws IOException {
                if (bag == null) {
                    out.nullValue();
                    return;
                }
                HashMap<String, Object> toSerialize = new HashMap<>(bag);

                if (!bag.tags.isEmpty()) {
                    toSerialize.put(TAGS_KEY, bag.getTags());
                }

                TypeAdapter<Object> objectTypeAdapter = gson.getAdapter(Object.class);

                out.beginObject();

                for (Map.Entry<String, Object> entry : toSerialize.entrySet()) {
                    if (!ignoreKeys.contains(entry.getKey())) {
                        out.name(String.valueOf(entry.getKey()));
                        objectTypeAdapter.write(out, entry.getValue());
                    }
                }

                out.endObject();
            }

            public PropertyBag read(JsonReader in) throws IOException {
                JsonToken peek = in.peek();
                if (peek == JsonToken.NULL) {
                    in.nextNull();
                    return null;
                }
                PropertyBag result = new PropertyBag();
                Map<String, Object> serializedMap = new HashMap<>();
                TypeAdapter<Object> objectTypeAdapter = gson.getAdapter(Object.class);
                in.beginObject();

                while (in.hasNext()) {
                    String key = in.nextName();
                    if (!ignoreKeys.contains(key)) {
                        Object value = objectTypeAdapter.read(in);
                        Object replaced = serializedMap.put(key, value);
                        if (replaced != null) {
                            throw new JsonSyntaxException("Duplicate key: " + key);
                        }
                    } else {
                        in.skipValue();
                    }
                }

                in.endObject();

                Object tags = serializedMap.remove(TAGS_KEY);
                result.putAll(serializedMap);
                if (tags instanceof List) {
                    //noinspection unchecked
                    result.tags.addAll((Collection<? extends String>) tags);
                }
                return result;
            }
        }
    }

}
