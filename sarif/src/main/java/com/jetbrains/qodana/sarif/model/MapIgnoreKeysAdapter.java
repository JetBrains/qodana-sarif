package com.jetbrains.qodana.sarif.model;

import com.google.gson.*;
import com.google.gson.internal.bind.ObjectTypeAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.*;

public class MapIgnoreKeysAdapter extends TypeAdapter<Map<String, Object>> {

    private static final Gson embedded = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final TypeAdapter<Object> objectTypeAdapter = ObjectTypeAdapter.getFactory(ToNumberPolicy.DOUBLE).create(embedded, TypeToken.get(Object.class));
    private final Set<String> ignoreKeys;

    public MapIgnoreKeysAdapter(Collection<String> ignoreKeys) {
        this.ignoreKeys = new HashSet<>(ignoreKeys);
    }

    @Override
    public void write(JsonWriter out, Map<String, Object> map) throws IOException {
        if (map == null) {
            out.nullValue();
            return;
        }
        out.beginObject();

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!ignoreKeys.contains(entry.getKey())) {
                out.name(String.valueOf(entry.getKey()));
                objectTypeAdapter.write(out, entry.getValue());
            }
        }

        out.endObject();
    }

    @Override
    public Map<String, Object> read(JsonReader in) throws IOException {
        JsonToken peek = in.peek();
        if (peek == JsonToken.NULL) {
            in.nextNull();
            return null;
        } else {
            Map<String, Object> map = new HashMap<>();
            if (peek == JsonToken.BEGIN_ARRAY) {
                throw new JsonSyntaxException("Expected json object");
            } else {
                in.beginObject();

                while (in.hasNext()) {
                    String key = in.nextName();
                    if (!ignoreKeys.contains(key)) {
                        Object value = objectTypeAdapter.read(in);
                        Object replaced = map.put(key, value);
                        if (replaced != null) {
                            throw new JsonSyntaxException("Duplicate key: " + key);
                        }
                    } else {
                        in.skipValue();
                    }
                }

                in.endObject();
            }

            return map;
        }
    }
}
