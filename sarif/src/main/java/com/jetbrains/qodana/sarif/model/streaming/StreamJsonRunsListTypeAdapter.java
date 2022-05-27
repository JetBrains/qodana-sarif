package com.jetbrains.qodana.sarif.model.streaming;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.jetbrains.qodana.sarif.model.Run;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class StreamJsonRunsListTypeAdapter extends TypeAdapter<List<Run>> {
    private final Gson gson;

    private StreamJsonRunsListTypeAdapter(Gson gson) {
        this.gson = gson;
    }

    @Override
    public void write(JsonWriter out, List<Run> value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Run> read(JsonReader in) throws IOException {
        ArrayList<Run> runs = new ArrayList<>();
        in.beginArray();
        while (in.peek() != JsonToken.END_ARRAY)  {
            Run run = gson.fromJson(in, Run.class);
            runs.add(run);
        }
        in.endArray();
        return runs;
    }

    public static TypeAdapterFactory makeFactory() {
        return new TypeAdapterFactory() {
            @SuppressWarnings("unchecked")
            @Override
            public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
                if (!type.equals(TypeToken.getParameterized(List.class, Run.class))) {
                    return null;
                }
                return (TypeAdapter<T>) new StreamJsonRunsListTypeAdapter(gson);
            }
        };
    }
}
