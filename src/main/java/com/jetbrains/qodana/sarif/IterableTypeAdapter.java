package com.jetbrains.qodana.sarif;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.jetbrains.qodana.sarif.model.Result;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class IterableTypeAdapter extends TypeAdapter<Iterable<?>> {
    private static final Gson embedded = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    public void write(JsonWriter out, Iterable<?> values) throws IOException {
        out.beginArray();
        for (Object value : values) {
            embedded.toJson(embedded.toJsonTree(value), out);
        }
        out.endArray();
    }

    public List<Result> read(JsonReader reader) throws IOException {
        List<Result> result = new ArrayList<>();
        reader.beginArray();
        while (reader.hasNext()) {
            result.add(embedded.fromJson(reader, Result.class));
        }
        reader.endArray();
        return result;
    }
}