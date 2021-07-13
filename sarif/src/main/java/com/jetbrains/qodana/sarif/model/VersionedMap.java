package com.jetbrains.qodana.sarif.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

public class VersionedMap<V> {
    private final Map<String, TreeMap<Integer, V>> map = new HashMap<>();

    public void put(String key, Integer version, V value) {
        Map<Integer, V> values = map.computeIfAbsent(key, k -> new TreeMap<>());
        values.put(version, value);
    }

    public V get(String key, Integer version) {
        Map<Integer, V> values = map.get(key);
        if (values == null) return null;
        return values.get(version);
    }

    public Integer getLastVersion(String key) {
        TreeMap<Integer, V> values = map.get(key);
        if (values == null) return null;
        return values.lastKey();
    }

    public V getLastValue(String key) {
        TreeMap<Integer, V> values = map.get(key);
        if (values == null) return null;
        Integer lastKey = values.lastKey();
        if (lastKey == null) return null;
        return values.get(lastKey);
    }

    public Map<Integer, V> getValues(String key) {
        return map.get(key);
    }

    public Map<String, V> getHierarchyStringsMap() {
        HashMap<String, V> result = new HashMap<>();
        map.forEach((key, versions) -> {
            versions.forEach((version, value) -> result.put(key + "/v" + version, value));
        });
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VersionedMap<?> that = (VersionedMap<?>) o;
        return Objects.equals(map, that.map);
    }

    @Override
    public int hashCode() {
        return Objects.hash(map);
    }

    public static class VersionedMapTypeAdapter<V> extends TypeAdapter<VersionedMap<V>> {
        private static final Gson embedded = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

        public void write(JsonWriter out, VersionedMap<V> map) {
            embedded.toJson(map.getHierarchyStringsMap(), Map.class, out);
        }

        public VersionedMap<V> read(JsonReader reader) throws IOException {
            VersionedMap<V> result = new VersionedMap<>();
            Map<String, V> map = embedded.fromJson(reader, Map.class);
            map.forEach((key, value) -> {
                String[] split = key.split("/v");
                if (split.length != 2) throw new JsonParseException("VersionedMap key should be formatted like '%key/v%number%'. Actual value : " + key);
                Integer version = Integer.valueOf(split[1]);
                result.put(split[0], version, value);
            });
            return result;
        }
    }
}
