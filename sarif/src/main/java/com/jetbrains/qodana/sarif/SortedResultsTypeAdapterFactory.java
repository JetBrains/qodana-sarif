package com.jetbrains.qodana.sarif;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.jetbrains.qodana.sarif.model.Result;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Writes every list of results in {@link ResultOrder#CANONICAL} order, leaving the caller's own list untouched. */
class SortedResultsTypeAdapterFactory implements TypeAdapterFactory {
    private static final TypeToken<List<Result>> RESULT_LIST = new TypeToken<List<Result>>() {
    };

    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        if (!List.class.isAssignableFrom(type.getRawType())) return null;

        TypeAdapter<T> delegate = gson.getDelegateAdapter(this, type);
        TypeAdapter<List<Result>> resultsDelegate = gson.getDelegateAdapter(this, RESULT_LIST);
        return new TypeAdapter<T>() {
            @Override
            public void write(JsonWriter out, T value) throws IOException {
                List<Result> results = resultsIn(value);
                if (results == null) delegate.write(out, value);
                else resultsDelegate.write(out, ResultOrder.sorted(results));
            }

            @Override
            public T read(JsonReader in) throws IOException {
                return delegate.read(in);
            }
        };
    }

    /** Checks whether {@code value} is a list of results: returns the results if it is, or {@code null} if it is not. */
    private static List<Result> resultsIn(Object value) {
        if (!(value instanceof List)) return null;
        List<Result> results = new ArrayList<>();
        for (Object element : (List<?>) value) {
            if (element != null && !(element instanceof Result)) return null;
            results.add((Result) element);
        }
        return results;
    }
}
